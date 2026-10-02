package com.pm.billingservice.dto;

import java.math.BigDecimal;

/** Cancelled invoices are excluded from the totals. */
public record AccountSummary(BigDecimal paidTotal, BigDecimal pendingTotal,
                             BigDecimal overallTotal, long invoiceCount) {
}
