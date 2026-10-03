package com.pm.appointmentservice.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(
      GlobalExceptionHandler.class);

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, HttpHeaders headers,
      HttpStatusCode status, WebRequest request) {

    Map<String, String> fieldErrors = new LinkedHashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(
        error -> fieldErrors.putIfAbsent(error.getField(),
            error.getDefaultMessage()));

    return build(HttpStatus.BAD_REQUEST, "Validation failed", request,
        fieldErrors);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
      Object body, HttpHeaders headers, HttpStatusCode statusCode,
      WebRequest request) {

    String message;
    if (body instanceof ProblemDetail problem && problem.getDetail() != null) {
      message = problem.getDetail();
    } else if (ex instanceof ErrorResponse errorResponse
        && errorResponse.getBody().getDetail() != null) {
      message = errorResponse.getBody().getDetail();
    } else {
      message = HttpStatus.valueOf(statusCode.value()).getReasonPhrase();
    }

    return build(statusCode, message, request, null);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<Object> handleUnauthorized(UnauthorizedException ex,
      WebRequest request) {
    return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<Object> handleForbidden(ForbiddenException ex,
      WebRequest request) {
    return build(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Object> handleNotFound(ResourceNotFoundException ex,
      WebRequest request) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<Object> handleConflict(ConflictException ex,
      WebRequest request) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
  }

  @ExceptionHandler(InvalidStateException.class)
  public ResponseEntity<Object> handleInvalidState(InvalidStateException ex,
      WebRequest request) {
    return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, null);
  }

  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<Object> handleInvalidRequest(InvalidRequestException ex,
      WebRequest request) {
    return build(HttpStatus.BAD_REQUEST, "Validation failed", request,
        ex.getFieldErrors());
  }

  @ExceptionHandler(ServiceUnavailableException.class)
  public ResponseEntity<Object> handleServiceUnavailable(
      ServiceUnavailableException ex, WebRequest request) {
    return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request, null);
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<Object> handleConcurrentUpdate(
      ObjectOptimisticLockingFailureException ex, WebRequest request) {
    return build(HttpStatus.CONFLICT,
        "The appointment was changed by someone else. Reload it and try again",
        request, null);
  }

  @ExceptionHandler(CallNotPermittedException.class)
  public ResponseEntity<Object> handleCircuitOpen(CallNotPermittedException ex,
      WebRequest request) {
    log.error("Circuit breaker open: {}", ex.getMessage());
    return build(HttpStatus.SERVICE_UNAVAILABLE,
        "A required service is unavailable. Please try again shortly", request,
        null);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnexpected(Exception ex,
      WebRequest request) {
    log.error("Unexpected error", ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred", request, null);
  }

  private ResponseEntity<Object> build(HttpStatusCode statusCode,
      String message, WebRequest request, Map<String, String> fieldErrors) {
    HttpStatus status = HttpStatus.valueOf(statusCode.value());
    HttpServletRequest servletRequest =
        ((ServletWebRequest) request).getRequest();

    ApiError body = ApiError.of(status.value(), status.getReasonPhrase(),
        message, servletRequest.getRequestURI(), fieldErrors);
    return ResponseEntity.status(status).body(body);
  }
}
