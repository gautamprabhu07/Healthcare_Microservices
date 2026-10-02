package com.pm.patientservice.kafka;

import com.pm.patientservice.model.Patient;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaProducer {

  public static final String TOPIC = "patient";
  public static final String PATIENT_CREATED = "PATIENT_CREATED";
  public static final String PATIENT_UPDATED = "PATIENT_UPDATED";
  public static final String PATIENT_DELETED = "PATIENT_DELETED";

  private static final Logger log = LoggerFactory.getLogger(
      KafkaProducer.class);
  private final KafkaTemplate<String, byte[]> kafkaTemplate;

  public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  /** Publishes asynchronously, keyed by patientId so one patient's events stay ordered. */
  public void sendEvent(Patient patient, String eventType) {
    PatientEvent event = PatientEvent.newBuilder()
        .setPatientId(patient.getId().toString())
        .setName(patient.getName())
        .setEmail(patient.getEmail())
        .setEventType(eventType)
        .setEventId(UUID.randomUUID().toString())
        .setOccurredAt(Instant.now().toString())
        .build();

    try {
      kafkaTemplate.send(TOPIC, event.getPatientId(), event.toByteArray())
          .whenComplete((result, ex) -> {
            if (ex != null) {
              log.error("Failed to publish {} (eventId={}, patientId={}): {}",
                  eventType, event.getEventId(), event.getPatientId(),
                  ex.toString());
            } else {
              log.info("Published {} (eventId={}, patientId={}, partition={}, offset={})",
                  eventType, event.getEventId(), event.getPatientId(),
                  result.getRecordMetadata().partition(),
                  result.getRecordMetadata().offset());
            }
          });
    } catch (Exception e) {
      log.error("Failed to publish {} (eventId={}, patientId={}): {}",
          eventType, event.getEventId(), event.getPatientId(), e.toString());
    }
  }
}
