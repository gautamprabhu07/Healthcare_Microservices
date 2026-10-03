package com.pm.billingservice.service;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import patient.events.PatientEvent;

/** Keeps billing accounts in step with patient changes made elsewhere. */
@Service
public class PatientEventHandler {

  private static final Logger log = LoggerFactory.getLogger(PatientEventHandler.class);

  private final EventIdempotency idempotency;
  private final BillingAccountService accountService;

  public PatientEventHandler(EventIdempotency idempotency,
      BillingAccountService accountService) {
    this.idempotency = idempotency;
    this.accountService = accountService;
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
      case "PATIENT_DELETED" -> accountService.closeForPatient(patientId);
      case "PATIENT_UPDATED" -> accountService.refreshPatientDetails(patientId,
          event.getName(), event.getEmail());
      default -> {
        // PATIENT_CREATED: the account is created synchronously over gRPC
      }
    }
  }
}
