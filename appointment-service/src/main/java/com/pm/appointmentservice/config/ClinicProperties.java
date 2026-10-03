package com.pm.appointmentservice.config;

import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Clinic opening rules. Appointments are Monday to Friday, in the clinic's time zone. */
@ConfigurationProperties(prefix = "clinic")
public record ClinicProperties(
    @DefaultValue("UTC") ZoneId timezone,
    @DefaultValue("09:00") LocalTime openTime,
    @DefaultValue("17:00") LocalTime closeTime,
    @DefaultValue("true") boolean enforceHours,
    @DefaultValue("30") int slotMinutes) {
}
