package com.pm.appointmentservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Reschedules and/or edits a SCHEDULED appointment. The patient and doctor cannot be changed. */
public record UpdateAppointmentRequest(
    @NotNull(message = "Start time is required") Instant startTime,

    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Max(value = 120, message = "Duration cannot exceed 120 minutes")
    Integer durationMinutes,

    @Size(max = 255, message = "Reason cannot exceed 255 characters") String reason,

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters") String notes) {
}
