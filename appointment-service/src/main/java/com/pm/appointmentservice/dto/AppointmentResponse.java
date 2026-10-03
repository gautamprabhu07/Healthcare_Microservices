package com.pm.appointmentservice.dto;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(UUID id, UUID patientId, String patientName,
                                  UUID doctorId, String doctorName,
                                  Instant startTime, Instant endTime,
                                  int durationMinutes, String reason, String notes,
                                  String status, String cancelReason,
                                  UUID createdBy, Instant createdAt,
                                  Instant updatedAt) {
}
