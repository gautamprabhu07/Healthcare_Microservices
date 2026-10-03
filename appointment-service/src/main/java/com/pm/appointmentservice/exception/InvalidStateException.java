package com.pm.appointmentservice.exception;

public class InvalidStateException extends RuntimeException {

  public InvalidStateException(String message) {
    super(message);
  }
}
