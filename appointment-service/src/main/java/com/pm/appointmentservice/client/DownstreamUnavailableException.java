package com.pm.appointmentservice.client;

/** A dependency (auth-service) could not be reached or returned a server error. */
public class DownstreamUnavailableException extends RuntimeException {

  public DownstreamUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
