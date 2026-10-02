package com.pm.billingservice.dto;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(UUID id, UUID patientId, String patientName,
                              String patientEmail, String status,
                              Instant createdAt, AccountSummary summary) {
}
