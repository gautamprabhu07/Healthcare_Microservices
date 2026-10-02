package com.pm.authservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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

    String message = ex instanceof ErrorResponse errorResponse
        && errorResponse.getBody().getDetail() != null
        ? errorResponse.getBody().getDetail()
        : HttpStatus.valueOf(statusCode.value()).getReasonPhrase();

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

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<Object> handleUserNotFound(UserNotFoundException ex,
      WebRequest request) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
  }

  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<Object> handleEmailAlreadyExists(
      EmailAlreadyExistsException ex, WebRequest request) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<Object> handleBusinessRule(BusinessRuleException ex,
      WebRequest request) {
    return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request,
        null);
  }

  @ExceptionHandler(ApiValidationException.class)
  public ResponseEntity<Object> handleApiValidation(ApiValidationException ex,
      WebRequest request) {
    return build(HttpStatus.BAD_REQUEST, "Validation failed", request,
        ex.getFieldErrors());
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
