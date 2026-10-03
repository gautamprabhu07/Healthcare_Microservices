package com.pm.analyticsservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaConsumer {

  private static final Logger log = LoggerFactory.getLogger(
      KafkaConsumer.class);

  /**
   * Logging is naturally idempotent, so redelivery is harmless for now (Step 11
   * adds an event log keyed by eventId). Unreadable messages are thrown so the
   * error handler can dead-letter them instead of losing them silently.
   */
  @KafkaListener(topics="patient", groupId = "analytics-service")
  public void consumeEvent(byte[] event) throws InvalidProtocolBufferException {
    PatientEvent patientEvent = PatientEvent.parseFrom(event);

    log.info("Received Patient Event: [EventId={},Type={},OccurredAt={},PatientId={},PatientName={},PatientEmail={}]",
        patientEvent.getEventId(),
        patientEvent.getEventType(),
        patientEvent.getOccurredAt(),
        patientEvent.getPatientId(),
        patientEvent.getName(),
        patientEvent.getEmail());
  }
}
