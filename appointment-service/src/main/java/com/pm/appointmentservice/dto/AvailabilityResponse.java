package com.pm.appointmentservice.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Free slots for one doctor on one day, within clinic hours. */
public record AvailabilityResponse(UUID doctorId, String doctorName, LocalDate date,
                                   String timezone, int slotMinutes,
                                   List<Slot> slots) {

  public record Slot(Instant start, Instant end) {
  }
}
