package com.pm.appointmentservice.kafka;

import appointment.events.AppointmentEvent;
import com.pm.appointmentservice.model.Appointment;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AppointmentEventProducer {

  public static final String TOPIC = "appointment";
  public static final String BOOKED = "APPOINTMENT_BOOKED";
  public static final String RESCHEDULED = "APPOINTMENT_RESCHEDULED";
  public static final String COMPLETED = "APPOINTMENT_COMPLETED";
  public static final String CANCELLED = "APPOINTMENT_CANCELLED";
  public static final String NO_SHOW = "APPOINTMENT_NO_SHOW";

  private static final Logger log = LoggerFactory.getLogger(
      AppointmentEventProducer.class);

  private final KafkaTemplate<String, byte[]> kafkaTemplate;

  public AppointmentEventProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  /**
   * Builds the event inside the transaction and sends it only after the commit,
   * so events always describe committed data.
   */
  public void publishAfterCommit(Appointment appointment, String eventType) {
    AppointmentEvent event = build(appointment, eventType);

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              send(event);
            }
          });
    } else {
      send(event);
    }
  }

  private void send(AppointmentEvent event) {
    try {
      kafkaTemplate.send(TOPIC, event.getAppointmentId(), event.toByteArray())
          .whenComplete((result, ex) -> {
            if (ex != null) {
              log.error("Failed to publish {} (eventId={}, appointmentId={}): {}",
                  event.getEventType(), event.getEventId(),
                  event.getAppointmentId(), ex.toString());
            } else {
              log.info("Published {} (eventId={}, appointmentId={}, partition={}, offset={})",
                  event.getEventType(), event.getEventId(),
                  event.getAppointmentId(),
                  result.getRecordMetadata().partition(),
                  result.getRecordMetadata().offset());
            }
          });
    } catch (Exception e) {
      log.error("Failed to publish {} (eventId={}, appointmentId={}): {}",
          event.getEventType(), event.getEventId(), event.getAppointmentId(),
          e.toString());
    }
  }

  private static AppointmentEvent build(Appointment a, String eventType) {
    return AppointmentEvent.newBuilder()
        .setEventId(UUID.randomUUID().toString())
        .setEventType(eventType)
        .setOccurredAt(Instant.now().toString())
        .setAppointmentId(a.getId().toString())
        .setPatientId(a.getPatientId().toString())
        .setPatientName(a.getPatientName())
        .setPatientEmail(a.getPatientEmail())
        .setDoctorId(a.getDoctorId().toString())
        .setDoctorName(a.getDoctorName())
        .setStartTime(a.getStartTime().toString())
        .setEndTime(a.getEndTime().toString())
        .setReason(a.getReason() == null ? "" : a.getReason())
        .setStatus(a.getStatus().name())
        .setCancelReason(a.getCancelReason() == null ? "" : a.getCancelReason())
        .build();
  }
}
