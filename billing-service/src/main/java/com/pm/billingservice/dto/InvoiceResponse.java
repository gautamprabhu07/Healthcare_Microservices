package com.pm.billingservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponse(UUID id, String invoiceNumber, UUID accountId,
                              UUID patientId, String patientName,
                              UUID appointmentId, String description,
                              BigDecimal amount, String currency, String status,
                              Instant issuedAt, LocalDate dueDate,
                              Instant paidAt) {
}
