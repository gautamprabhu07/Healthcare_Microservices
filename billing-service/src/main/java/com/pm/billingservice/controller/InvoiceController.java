package com.pm.billingservice.controller;

import com.pm.billingservice.dto.CreateInvoiceRequest;
import com.pm.billingservice.dto.InvoiceResponse;
import com.pm.billingservice.dto.PageResponse;
import com.pm.billingservice.model.InvoiceStatus;
import com.pm.billingservice.security.RequiredRole;
import com.pm.billingservice.security.Role;
import com.pm.billingservice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/billing/invoices")
@RequiredRole({Role.ADMIN, Role.RECEPTIONIST})
@Tag(name = "Invoices", description = "Invoices: PENDING -> PAID or CANCELLED")
public class InvoiceController {

  private final InvoiceService invoiceService;

  public InvoiceController(InvoiceService invoiceService) {
    this.invoiceService = invoiceService;
  }

  @GetMapping
  @Operation(summary = "List invoices (newest first), optionally by patient and status")
  public ResponseEntity<PageResponse<InvoiceResponse>> list(
      @RequestParam(required = false) UUID patientId,
      @RequestParam(required = false) InvoiceStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.ok(invoiceService.list(patientId, status, page, size));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get an invoice by id")
  public ResponseEntity<InvoiceResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(invoiceService.get(id));
  }

  @PostMapping
  @Operation(summary = "Create a manual invoice for a patient")
  public ResponseEntity<InvoiceResponse> create(
      @Valid @RequestBody CreateInvoiceRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(invoiceService.create(request));
  }

  @PostMapping("/{id}/pay")
  @Operation(summary = "Mark a PENDING invoice as PAID")
  public ResponseEntity<InvoiceResponse> pay(@PathVariable UUID id) {
    return ResponseEntity.ok(invoiceService.pay(id));
  }

  @PostMapping("/{id}/cancel")
  @Operation(summary = "Cancel a PENDING invoice")
  public ResponseEntity<InvoiceResponse> cancel(@PathVariable UUID id) {
    return ResponseEntity.ok(invoiceService.cancel(id));
  }
}
