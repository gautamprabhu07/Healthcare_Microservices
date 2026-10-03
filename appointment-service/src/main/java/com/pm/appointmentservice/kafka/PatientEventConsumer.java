package com.pm.appointmentservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.pm.appointmentservice.service.PatientEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

/**
 * Failures are retried and then dead-lettered by the DefaultErrorHandler (see
 * KafkaConsumerConfig), so unreadable messages are thrown, not swallowed.
 */
@Service
public class PatientEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(
      PatientEventConsumer.class);

  private final PatientEventHandler handler;

  public PatientEventConsumer(PatientEventHandler handler) {
    this.handler = handler;
  }

  @KafkaListener(topics = "patient", groupId = "appointment-service")
  public void consume(byte[] payload) throws InvalidProtocolBufferException {
    PatientEvent event = PatientEvent.parseFrom(payload);
    log.info("Received {} (eventId={}, patientId={})", event.getEventType(),
        event.getEventId(), event.getPatientId());
    handler.handle(event);
  }
}
