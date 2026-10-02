package com.pm.billingservice.mapper;

import com.pm.billingservice.dto.AccountResponse;
import com.pm.billingservice.dto.AccountSummary;
import com.pm.billingservice.dto.InvoiceResponse;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.model.Invoice;

public class BillingMapper {

  public static AccountResponse toResponse(BillingAccount account,
      AccountSummary summary) {
    return new AccountResponse(account.getId(), account.getPatientId(),
        account.getPatientName(), account.getPatientEmail(),
        account.getStatus().name(), account.getCreatedAt(), summary);
  }

  public static InvoiceResponse toResponse(Invoice invoice) {
    BillingAccount account = invoice.getAccount();
    return new InvoiceResponse(invoice.getId(), invoice.getInvoiceNumber(),
        account.getId(), invoice.getPatientId(), account.getPatientName(),
        invoice.getAppointmentId(), invoice.getDescription(),
        invoice.getAmount(), invoice.getCurrency(), invoice.getStatus().name(),
        invoice.getIssuedAt(), invoice.getDueDate(), invoice.getPaidAt());
  }
}
