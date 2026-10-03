package com.pm.appointmentservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DoctorInfo(UUID id, String firstName, String lastName, String role,
                         boolean enabled) {

  public String fullName() {
    return firstName + " " + lastName;
  }

  public boolean isActiveDoctor() {
    return enabled && "DOCTOR".equals(role);
  }
}
