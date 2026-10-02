package com.pm.apigateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ApiErrorWriter {

  private final ObjectMapper objectMapper;

  public ApiErrorWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Mono<Void> write(ServerWebExchange exchange, HttpStatus status,
      String message) {
    if (exchange.getResponse().isCommitted()) {
      return Mono.empty();
    }

    ApiError error = ApiError.of(status.value(), status.getReasonPhrase(),
        message, exchange.getRequest().getPath().value());

    byte[] bytes;
    try {
      bytes = objectMapper.writeValueAsBytes(error);
    } catch (JsonProcessingException e) {
      bytes = ("{\"status\":" + status.value() + "}")
          .getBytes(StandardCharsets.UTF_8);
    }

    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
    return exchange.getResponse().writeWith(Mono.just(buffer));
  }
}
