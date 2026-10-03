package com.pm.appointmentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequest(
    @NotNull(message = "Status is required") TargetStatus status,

    @Size(max = 255, message = "Cancel reason cannot exceed 255 characters")
    String cancelReason,

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters") String notes) {

  /** The only statuses an appointment can move to. */
  public enum TargetStatus {
    COMPLETED,
    CANCELLED,
    NO_SHOW
  }
}
