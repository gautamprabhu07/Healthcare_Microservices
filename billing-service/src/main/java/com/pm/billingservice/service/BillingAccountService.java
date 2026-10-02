package com.pm.billingservice.service;

import com.pm.billingservice.dto.AccountResponse;
import com.pm.billingservice.dto.AccountSummary;
import com.pm.billingservice.exception.ResourceNotFoundException;
import com.pm.billingservice.mapper.BillingMapper;
import com.pm.billingservice.model.AccountStatus;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.model.InvoiceStatus;
import com.pm.billingservice.repository.BillingAccountRepository;
import com.pm.billingservice.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingAccountService {

  private static final Logger log = LoggerFactory.getLogger(
      BillingAccountService.class);

  private final BillingAccountRepository accountRepository;
  private final InvoiceRepository invoiceRepository;
  private final InvoiceService invoiceService;

  public BillingAccountService(BillingAccountRepository accountRepository,
      InvoiceRepository invoiceRepository, InvoiceService invoiceService) {
    this.accountRepository = accountRepository;
    this.invoiceRepository = invoiceRepository;
    this.invoiceService = invoiceService;
  }

  /**
   * Idempotent: returns the existing account when the patient already has one.
   * Not transactional on purpose, so a unique-constraint race can be recovered
   * by simply reading the winner's row.
   */
  public BillingAccount createOrGet(UUID patientId, String name, String email) {
    return accountRepository.findByPatientId(patientId).orElseGet(() -> {
      BillingAccount account = new BillingAccount();
      account.setPatientId(patientId);
      account.setPatientName(name);
      account.setPatientEmail(email);
      try {
        BillingAccount saved = accountRepository.save(account);
        log.info("Created billing account {} for patient {}", saved.getId(),
            patientId);
        return saved;
      } catch (DataIntegrityViolationException e) {
        return accountRepository.findByPatientId(patientId).orElseThrow(() -> e);
      }
    });
  }

  @Transactional(readOnly = true)
  public AccountResponse getAccount(UUID patientId) {
    BillingAccount account = accountRepository.findByPatientId(patientId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Billing account not found for patient: " + patientId));

    return BillingMapper.toResponse(account, summarize(account.getId()));
  }

  /** Closes the account and cancels everything still unpaid. Safe to repeat. */
  @Transactional
  public void closeForPatient(UUID patientId) {
    accountRepository.findByPatientId(patientId).ifPresentOrElse(account -> {
      if (account.getStatus() == AccountStatus.CLOSED) {
        return;
      }
      account.setStatus(AccountStatus.CLOSED);
      int cancelled = invoiceService.cancelPending(account);
      log.info("Closed billing account {} for deleted patient {} ({} pending invoice(s) cancelled)",
          account.getId(), patientId, cancelled);
    }, () -> log.info("No billing account for deleted patient {}", patientId));
  }

  @Transactional
  public void refreshPatientDetails(UUID patientId, String name, String email) {
    accountRepository.findByPatientId(patientId).ifPresent(account -> {
      account.setPatientName(name);
      account.setPatientEmail(email);
    });
  }

  private AccountSummary summarize(UUID accountId) {
    BigDecimal paid = BigDecimal.ZERO;
    BigDecimal pending = BigDecimal.ZERO;
    long count = 0;

    for (Object[] row : invoiceRepository.summarize(accountId)) {
      InvoiceStatus status = (InvoiceStatus) row[0];
      BigDecimal total = (BigDecimal) row[1];
      count += (Long) row[2];
      if (status == InvoiceStatus.PAID) {
        paid = total;
      } else if (status == InvoiceStatus.PENDING) {
        pending = total;
      }
    }
    return new AccountSummary(paid, pending, paid.add(pending), count);
  }
}
