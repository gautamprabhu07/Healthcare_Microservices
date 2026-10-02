package com.pm.billingservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.pm.billingservice.service.BillingAccountService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

/** Keeps billing accounts in step with patient changes made elsewhere. */
@Service
public class PatientEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(
      PatientEventConsumer.class);

  private final BillingAccountService accountService;

  public PatientEventConsumer(BillingAccountService accountService) {
    this.accountService = accountService;
  }

  @KafkaListener(topics = "patient", groupId = "billing-service")
  public void consume(byte[] payload) {
    PatientEvent event;
    try {
      event = PatientEvent.parseFrom(payload);
    } catch (InvalidProtocolBufferException e) {
      log.error("Skipping unreadable patient event: {}", e.getMessage());
      return;
    }

    UUID patientId;
    try {
      patientId = UUID.fromString(event.getPatientId());
    } catch (IllegalArgumentException e) {
      log.error("Skipping patient event {} with invalid patientId '{}'",
          event.getEventId(), event.getPatientId());
      return;
    }

    log.info("Received {} (eventId={}, patientId={})", event.getEventType(),
        event.getEventId(), patientId);

    switch (event.getEventType()) {
      case "PATIENT_DELETED" -> accountService.closeForPatient(patientId);
      case "PATIENT_UPDATED" -> accountService.refreshPatientDetails(patientId,
          event.getName(), event.getEmail());
      default -> {
        // PATIENT_CREATED: the account is created synchronously over gRPC
      }
    }
  }
}
