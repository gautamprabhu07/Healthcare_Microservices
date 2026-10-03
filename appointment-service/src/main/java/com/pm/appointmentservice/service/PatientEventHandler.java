package com.pm.appointmentservice.service;

import com.pm.appointmentservice.kafka.AppointmentEventProducer;
import com.pm.appointmentservice.model.Appointment;
import com.pm.appointmentservice.model.AppointmentStatus;
import com.pm.appointmentservice.repository.AppointmentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import patient.events.PatientEvent;

/** Reacts to patient changes made in patient-service. */
@Service
public class PatientEventHandler {

  public static final String PATIENT_DELETED_REASON = "Patient record deleted";

  private static final Logger log = LoggerFactory.getLogger(PatientEventHandler.class);

  private final EventIdempotency idempotency;
  private final AppointmentRepository appointmentRepository;
  private final AppointmentEventProducer eventProducer;

  public PatientEventHandler(EventIdempotency idempotency,
      AppointmentRepository appointmentRepository,
      AppointmentEventProducer eventProducer) {
    this.idempotency = idempotency;
    this.appointmentRepository = appointmentRepository;
    this.eventProducer = eventProducer;
  }

  /** One transaction: the "already processed" marker and the change commit together. */
  @Transactional
  public void handle(PatientEvent event) {
    UUID patientId = UUID.fromString(event.getPatientId());

    if (!idempotency.firstTime(event.getEventId())) {
      log.info("Skipping duplicate {} (eventId={})", event.getEventType(),
          event.getEventId());
      return;
    }

    switch (event.getEventType()) {
      case "PATIENT_DELETED" -> cancelUpcoming(patientId);
      case "PATIENT_UPDATED" -> {
        int updated = appointmentRepository.updatePatientDetails(patientId,
            event.getName(), event.getEmail());
        log.info("Refreshed patient details on {} appointment(s) of patient {}",
            updated, patientId);
      }
      default -> {
        // PATIENT_CREATED needs nothing here
      }
    }
  }

  /** Cancels future SCHEDULED appointments; past and finished ones stay as history. */
  private void cancelUpcoming(UUID patientId) {
    List<Appointment> upcoming = appointmentRepository
        .findByPatientIdAndStatusAndStartTimeAfter(patientId,
            AppointmentStatus.SCHEDULED, Instant.now());

    for (Appointment appointment : upcoming) {
      appointment.setStatus(AppointmentStatus.CANCELLED);
      appointment.setCancelReason(PATIENT_DELETED_REASON);
      Appointment saved = appointmentRepository.saveAndFlush(appointment);
      eventProducer.publishAfterCommit(saved, AppointmentEventProducer.CANCELLED);
    }
    log.info("Cancelled {} upcoming appointment(s) of deleted patient {}",
        upcoming.size(), patientId);
  }
}
