package com.pm.billingservice.service;

import appointment.events.AppointmentEvent;
import com.pm.billingservice.model.AccountStatus;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.repository.InvoiceRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bills a consultation when an appointment is completed. */
@Service
public class AppointmentEventHandler {

  private static final Logger log = LoggerFactory.getLogger(
      AppointmentEventHandler.class);

  private final EventIdempotency idempotency;
  private final BillingAccountService accountService;
  private final InvoiceService invoiceService;
  private final InvoiceRepository invoiceRepository;

  public AppointmentEventHandler(EventIdempotency idempotency,
      BillingAccountService accountService, InvoiceService invoiceService,
      InvoiceRepository invoiceRepository) {
    this.idempotency = idempotency;
    this.accountService = accountService;
    this.invoiceService = invoiceService;
    this.invoiceRepository = invoiceRepository;
  }

  /**
   * Two layers keep this idempotent: the processed-event marker (same event
   * delivered twice) and the appointment_id check/unique constraint (a different
   * event for an appointment that is already invoiced).
   */
  @Transactional
  public void handle(AppointmentEvent event) {
    UUID appointmentId = UUID.fromString(event.getAppointmentId());
    UUID patientId = UUID.fromString(event.getPatientId());

    if (!idempotency.firstTime(event.getEventId())) {
      log.info("Skipping duplicate {} (eventId={})", event.getEventType(),
          event.getEventId());
      return;
    }

    if (!"APPOINTMENT_COMPLETED".equals(event.getEventType())) {
      return;
    }

    if (invoiceRepository.existsByAppointmentId(appointmentId)) {
      log.info("Invoice for appointment {} already exists; not creating another",
          appointmentId);
      return;
    }

    BillingAccount account = accountService.createOrGet(patientId,
        event.getPatientName(), event.getPatientEmail());
    if (account.getStatus() == AccountStatus.CLOSED) {
      log.warn("Billing account {} is closed; no invoice for appointment {}",
          account.getId(), appointmentId);
      return;
    }

    String date = Instant.parse(event.getStartTime()).atZone(ZoneOffset.UTC)
        .toLocalDate().toString();
    String description = "Consultation with Dr. %s on %s".formatted(
        event.getDoctorName(), date);

    invoiceService.createForAppointment(account, appointmentId, description);
    log.info("Created consultation invoice for appointment {} (eventId={})",
        appointmentId, event.getEventId());
  }
}
