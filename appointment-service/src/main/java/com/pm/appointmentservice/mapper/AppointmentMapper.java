package com.pm.appointmentservice.mapper;

import com.pm.appointmentservice.dto.AppointmentResponse;
import com.pm.appointmentservice.model.Appointment;
import java.time.Duration;

public class AppointmentMapper {

  public static AppointmentResponse toResponse(Appointment a) {
    return new AppointmentResponse(a.getId(), a.getPatientId(), a.getPatientName(),
        a.getDoctorId(), a.getDoctorName(), a.getStartTime(), a.getEndTime(),
        (int) Duration.between(a.getStartTime(), a.getEndTime()).toMinutes(),
        a.getReason(), a.getNotes(), a.getStatus().name(), a.getCancelReason(),
        a.getCreatedBy(), a.getCreatedAt(), a.getUpdatedAt());
  }
}
