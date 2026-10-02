package com.pm.billingservice.kafka;

import billing.events.BillingEvent;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.model.Invoice;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class BillingEventProducer {

  public static final String TOPIC = "billing";
  public static final String INVOICE_CREATED = "INVOICE_CREATED";
  public static final String INVOICE_PAID = "INVOICE_PAID";
  public static final String INVOICE_CANCELLED = "INVOICE_CANCELLED";

  private static final Logger log = LoggerFactory.getLogger(
      BillingEventProducer.class);

  private final KafkaTemplate<String, byte[]> kafkaTemplate;

  public BillingEventProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  /**
   * Builds the event now (inside the transaction, while the data is loaded) and
   * sends it only after the transaction commits, so events describe committed
   * data only.
   */
  public void publishAfterCommit(Invoice invoice, String eventType) {
    BillingEvent event = build(invoice, eventType);

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

  private void send(BillingEvent event) {
    try {
      kafkaTemplate.send(TOPIC, event.getInvoiceId(), event.toByteArray())
          .whenComplete((result, ex) -> {
            if (ex != null) {
              log.error("Failed to publish {} (eventId={}, invoice={}): {}",
                  event.getEventType(), event.getEventId(),
                  event.getInvoiceNumber(), ex.toString());
            } else {
              log.info("Published {} (eventId={}, invoice={}, partition={}, offset={})",
                  event.getEventType(), event.getEventId(),
                  event.getInvoiceNumber(),
                  result.getRecordMetadata().partition(),
                  result.getRecordMetadata().offset());
            }
          });
    } catch (Exception e) {
      log.error("Failed to publish {} (eventId={}, invoice={}): {}",
          event.getEventType(), event.getEventId(), event.getInvoiceNumber(),
          e.toString());
    }
  }

  private static BillingEvent build(Invoice invoice, String eventType) {
    BillingAccount account = invoice.getAccount();
    return BillingEvent.newBuilder()
        .setEventId(UUID.randomUUID().toString())
        .setEventType(eventType)
        .setOccurredAt(Instant.now().toString())
        .setInvoiceId(invoice.getId().toString())
        .setInvoiceNumber(invoice.getInvoiceNumber())
        .setAccountId(account.getId().toString())
        .setPatientId(invoice.getPatientId().toString())
        .setPatientName(account.getPatientName())
        .setPatientEmail(account.getPatientEmail())
        .setAppointmentId(invoice.getAppointmentId() == null
            ? "" : invoice.getAppointmentId().toString())
        .setDescription(invoice.getDescription())
        .setAmount(invoice.getAmount().toPlainString())
        .setCurrency(invoice.getCurrency())
        .setStatus(invoice.getStatus().name())
        .setDueDate(invoice.getDueDate().toString())
        .build();
  }
}
