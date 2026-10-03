package com.pm.appointmentservice.exception;

public class ServiceUnavailableException extends RuntimeException {

  public ServiceUnavailableException(String message) {
    super(message);
  }
}
