package com.pm.appointmentservice.service;

import com.pm.appointmentservice.client.DoctorInfo;
import com.pm.appointmentservice.config.ClinicProperties;
import com.pm.appointmentservice.dto.AvailabilityResponse;
import com.pm.appointmentservice.dto.AvailabilityResponse.Slot;
import com.pm.appointmentservice.model.Appointment;
import com.pm.appointmentservice.model.AppointmentStatus;
import com.pm.appointmentservice.repository.AppointmentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Free slots for a doctor on a given day, inside clinic hours. */
@Service
public class AvailabilityService {

  private final AppointmentRepository appointmentRepository;
  private final AppointmentService appointmentService;
  private final ClinicProperties clinic;
  private final ClinicRules clinicRules;

  public AvailabilityService(AppointmentRepository appointmentRepository,
      AppointmentService appointmentService, ClinicProperties clinic,
      ClinicRules clinicRules) {
    this.appointmentRepository = appointmentRepository;
    this.appointmentService = appointmentService;
    this.clinic = clinic;
    this.clinicRules = clinicRules;
  }

  public AvailabilityResponse availability(UUID doctorId, LocalDate date) {
    DoctorInfo doctor = appointmentService.requireDoctor(doctorId);

    List<Slot> slots = new ArrayList<>();
    if (clinicRules.isWorkingDay(date)) {
      ZonedDateTime open = date.atTime(clinic.openTime()).atZone(clinic.timezone());
      ZonedDateTime close = date.atTime(clinic.closeTime()).atZone(clinic.timezone());

      List<Appointment> booked = appointmentRepository
          .findByDoctorIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
              doctorId, AppointmentStatus.SCHEDULED, close.toInstant(),
              open.toInstant());

      Instant now = Instant.now();
      ZonedDateTime slotStart = open;
      while (!slotStart.plusMinutes(clinic.slotMinutes()).isAfter(close)) {
        Instant start = slotStart.toInstant();
        Instant end = slotStart.plusMinutes(clinic.slotMinutes()).toInstant();

        boolean taken = booked.stream().anyMatch(
            a -> a.getStartTime().isBefore(end) && a.getEndTime().isAfter(start));
        if (start.isAfter(now) && !taken) {
          slots.add(new Slot(start, end));
        }
        slotStart = slotStart.plusMinutes(clinic.slotMinutes());
      }
    }

    return new AvailabilityResponse(doctor.id(), doctor.fullName(), date,
        clinic.timezone().getId(), clinic.slotMinutes(), slots);
  }
}
