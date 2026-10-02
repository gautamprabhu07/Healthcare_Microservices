package com.pm.billingservice.service;

import com.pm.billingservice.dto.CreateInvoiceRequest;
import com.pm.billingservice.dto.InvoiceResponse;
import com.pm.billingservice.dto.PageResponse;
import com.pm.billingservice.exception.InvalidRequestException;
import com.pm.billingservice.exception.InvalidStateException;
import com.pm.billingservice.exception.ResourceNotFoundException;
import com.pm.billingservice.kafka.BillingEventProducer;
import com.pm.billingservice.mapper.BillingMapper;
import com.pm.billingservice.model.AccountStatus;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.model.Invoice;
import com.pm.billingservice.model.InvoiceStatus;
import com.pm.billingservice.repository.BillingAccountRepository;
import com.pm.billingservice.repository.InvoiceRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

  private static final int MAX_PAGE_SIZE = 100;
  private static final int DEFAULT_DUE_DAYS = 30;

  private final InvoiceRepository invoiceRepository;
  private final BillingAccountRepository accountRepository;
  private final BillingEventProducer eventProducer;
  private final String defaultCurrency;

  public InvoiceService(InvoiceRepository invoiceRepository,
      BillingAccountRepository accountRepository,
      BillingEventProducer eventProducer,
      @Value("${billing.default-currency:USD}") String defaultCurrency) {
    this.invoiceRepository = invoiceRepository;
    this.accountRepository = accountRepository;
    this.eventProducer = eventProducer;
    this.defaultCurrency = defaultCurrency;
  }

  @Transactional(readOnly = true)
  public PageResponse<InvoiceResponse> list(UUID patientId, InvoiceStatus status,
      int page, int size) {

    Specification<Invoice> spec = Specification.where(null);
    if (patientId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("patientId"), patientId));
    }
    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }

    PageRequest pageable = PageRequest.of(Math.max(page, 0),
        Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
        Sort.by(Sort.Order.desc("issuedAt"), Sort.Order.asc("id")));

    Page<Invoice> invoices = invoiceRepository.findAll(spec, pageable);
    return PageResponse.of(invoices.map(BillingMapper::toResponse));
  }

  @Transactional(readOnly = true)
  public InvoiceResponse get(UUID id) {
    return BillingMapper.toResponse(findOrThrow(id));
  }

  @Transactional
  public InvoiceResponse create(CreateInvoiceRequest request) {
    BillingAccount account = accountRepository.findByPatientId(request.patientId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Billing account not found for patient: " + request.patientId()));

    if (account.getStatus() == AccountStatus.CLOSED) {
      throw new InvalidStateException(
          "The billing account for this patient is closed");
    }

    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    LocalDate dueDate = request.dueDate() == null
        ? today.plusDays(DEFAULT_DUE_DAYS) : request.dueDate();
    if (dueDate.isBefore(today)) {
      throw new InvalidRequestException("dueDate", "Due date cannot be in the past");
    }

    Instant now = Instant.now();
    Invoice invoice = new Invoice();
    invoice.setAccount(account);
    invoice.setPatientId(account.getPatientId());
    invoice.setInvoiceNumber(nextInvoiceNumber(now));
    invoice.setDescription(request.description().trim());
    invoice.setAmount(request.amount());
    invoice.setCurrency(request.currency() == null ? defaultCurrency
        : request.currency());
    invoice.setStatus(InvoiceStatus.PENDING);
    invoice.setIssuedAt(now);
    invoice.setDueDate(dueDate);

    Invoice saved = invoiceRepository.save(invoice);
    eventProducer.publishAfterCommit(saved, BillingEventProducer.INVOICE_CREATED);
    return BillingMapper.toResponse(saved);
  }

  @Transactional
  public InvoiceResponse pay(UUID id) {
    Invoice invoice = findOrThrow(id);
    requirePending(invoice, "paid");

    invoice.setStatus(InvoiceStatus.PAID);
    invoice.setPaidAt(Instant.now());

    Invoice saved = invoiceRepository.saveAndFlush(invoice);
    eventProducer.publishAfterCommit(saved, BillingEventProducer.INVOICE_PAID);
    return BillingMapper.toResponse(saved);
  }

  @Transactional
  public InvoiceResponse cancel(UUID id) {
    Invoice invoice = findOrThrow(id);
    requirePending(invoice, "cancelled");
    return BillingMapper.toResponse(markCancelled(invoice));
  }

  /** Cancels every PENDING invoice of the account; returns how many. */
  @Transactional
  public int cancelPending(BillingAccount account) {
    List<Invoice> pending = new ArrayList<>(invoiceRepository
        .findByAccountIdAndStatus(account.getId(), InvoiceStatus.PENDING));
    pending.forEach(this::markCancelled);
    return pending.size();
  }

  private Invoice markCancelled(Invoice invoice) {
    invoice.setStatus(InvoiceStatus.CANCELLED);
    Invoice saved = invoiceRepository.saveAndFlush(invoice);
    eventProducer.publishAfterCommit(saved, BillingEventProducer.INVOICE_CANCELLED);
    return saved;
  }

  private static void requirePending(Invoice invoice, String action) {
    if (invoice.getStatus() != InvoiceStatus.PENDING) {
      throw new InvalidStateException(
          "Invoice " + invoice.getInvoiceNumber() + " is "
              + invoice.getStatus() + " and cannot be " + action
              + "; only PENDING invoices can be " + action);
    }
  }

  private Invoice findOrThrow(UUID id) {
    return invoiceRepository.findWithAccountById(id).orElseThrow(
        () -> new ResourceNotFoundException("Invoice not found with ID: " + id));
  }

  private String nextInvoiceNumber(Instant issuedAt) {
    return "INV-%d-%06d".formatted(issuedAt.atZone(ZoneOffset.UTC).getYear(),
        invoiceRepository.nextInvoiceSequence());
  }
}
