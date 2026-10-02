package com.pm.apigateway.exception;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(
      GlobalErrorWebExceptionHandler.class);

  private final ApiErrorWriter errorWriter;

  public GlobalErrorWebExceptionHandler(ApiErrorWriter errorWriter) {
    this.errorWriter = errorWriter;
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    HttpStatus status;
    String message;

    if (ex instanceof ResponseStatusException rse) {
      status = HttpStatus.valueOf(rse.getStatusCode().value());
      message = status.is5xxServerError() ? serverErrorMessage(status)
          : status.getReasonPhrase();
    } else if (hasCause(ex, ConnectException.class)) {
      status = HttpStatus.SERVICE_UNAVAILABLE;
      message = serverErrorMessage(status);
    } else if (hasCause(ex, TimeoutException.class)) {
      status = HttpStatus.GATEWAY_TIMEOUT;
      message = serverErrorMessage(status);
    } else {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
      message = serverErrorMessage(status);
    }

    if (status.is5xxServerError()) {
      log.error("Gateway error on {}: {}", exchange.getRequest().getPath(),
          ex.toString());
    }

    return errorWriter.write(exchange, status, message);
  }

  private static String serverErrorMessage(HttpStatus status) {
    return switch (status) {
      case SERVICE_UNAVAILABLE -> "A downstream service is unavailable";
      case GATEWAY_TIMEOUT -> "A downstream service timed out";
      case BAD_GATEWAY -> "Bad response from a downstream service";
      default -> "An unexpected error occurred";
    };
  }

  private static boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
    for (Throwable t = ex; t != null; t = t.getCause()) {
      if (type.isInstance(t)) {
        return true;
      }
    }
    return false;
  }
}
