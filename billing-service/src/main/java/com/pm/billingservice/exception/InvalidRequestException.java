package com.pm.billingservice.exception;

import java.util.Map;

/** A business-level validation failure that is reported as a 400 with field errors. */
public class InvalidRequestException extends RuntimeException {

  private final Map<String, String> fieldErrors;

  public InvalidRequestException(String field, String message) {
    super(message);
    this.fieldErrors = Map.of(field, message);
  }

  public Map<String, String> getFieldErrors() {
    return fieldErrors;
  }
}
