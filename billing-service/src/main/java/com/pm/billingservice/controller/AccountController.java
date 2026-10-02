package com.pm.billingservice.controller;

import com.pm.billingservice.dto.AccountResponse;
import com.pm.billingservice.security.RequiredRole;
import com.pm.billingservice.security.Role;
import com.pm.billingservice.service.BillingAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/billing/accounts")
@RequiredRole({Role.ADMIN, Role.RECEPTIONIST})
@Tag(name = "Billing accounts", description = "One billing account per patient")
public class AccountController {

  private final BillingAccountService accountService;

  public AccountController(BillingAccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping("/{patientId}")
  @Operation(summary = "Get a patient's billing account with paid/pending totals")
  public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID patientId) {
    return ResponseEntity.ok(accountService.getAccount(patientId));
  }
}
