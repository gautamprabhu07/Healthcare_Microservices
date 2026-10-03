package com.pm.appointmentservice.service;

import com.pm.appointmentservice.config.ClinicProperties;
import com.pm.appointmentservice.exception.InvalidRequestException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/** The clinic's booking rules: future start, working days and opening hours. */
@Component
public class ClinicRules {

  private final ClinicProperties clinic;

  public ClinicRules(ClinicProperties clinic) {
    this.clinic = clinic;
  }

  public boolean isWorkingDay(LocalDate date) {
    DayOfWeek day = date.getDayOfWeek();
    return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
  }

  /** Throws a 400 (field startTime) when [start, end) cannot be booked. */
  public void validateBookableSlot(Instant start, Instant end) {
    if (!start.isAfter(Instant.now())) {
      throw new InvalidRequestException("startTime",
          "Appointment must start in the future");
    }
    if (!clinic.enforceHours()) {
      return;
    }

    ZonedDateTime localStart = start.atZone(clinic.timezone());
    ZonedDateTime localEnd = end.atZone(clinic.timezone());

    if (!isWorkingDay(localStart.toLocalDate())) {
      throw new InvalidRequestException("startTime",
          "Appointments are only available Monday to Friday");
    }
    if (localStart.toLocalTime().isBefore(clinic.openTime())
        || !localEnd.toLocalDate().equals(localStart.toLocalDate())
        || localEnd.toLocalTime().isAfter(clinic.closeTime())) {
      throw new InvalidRequestException("startTime",
          "Appointments must be within clinic hours (%s-%s %s)".formatted(
              clinic.openTime(), clinic.closeTime(), clinic.timezone()));
    }
  }
}
