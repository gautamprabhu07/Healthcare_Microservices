package com.pm.patientservice.exception;

import io.grpc.StatusRuntimeException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(
      GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidationException(
      MethodArgumentNotValidException ex) {

    Map<String, String> errors = new HashMap<>();

    ex.getBindingResult().getFieldErrors().forEach(
        error -> errors.put(error.getField(), error.getDefaultMessage()));

    return ResponseEntity.badRequest().body(errors);
  }

  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<Map<String, String>> handleEmailAlreadyExistsException(
      EmailAlreadyExistsException ex) {

    log.warn("Email address already exist {}", ex.getMessage());
    Map<String, String> errors = new HashMap<>();
    errors.put("message", "Email address already exists");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errors);
  }

  @ExceptionHandler(PatientNotFoundException.class)
  public ResponseEntity<Map<String, String>> handlePatientNotFoundException(
      PatientNotFoundException ex) {
    log.warn("Patient not found {}", ex.getMessage());

    Map<String, String> errors = new HashMap<>();
    errors.put("message", "Patient not found");
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
  }

  @ExceptionHandler(StatusRuntimeException.class)
  public ResponseEntity<Map<String, String>> handleGrpcException(
      StatusRuntimeException ex) {
    log.error("Billing service call failed: {}", ex.getStatus());

    Map<String, String> errors = new HashMap<>();
    errors.put("message", "Billing service unavailable, patient not created");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errors);
  }
}
