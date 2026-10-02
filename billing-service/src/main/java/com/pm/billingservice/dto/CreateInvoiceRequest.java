package com.pm.billingservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** currency defaults to USD and dueDate to 30 days from today when omitted. */
public record CreateInvoiceRequest(
    @NotNull(message = "Patient id is required")
    UUID patientId,

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    String description,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Digits(integer = 8, fraction = 2,
        message = "Amount must have at most 8 digits and 2 decimals")
    BigDecimal amount,

    @Pattern(regexp = "[A-Z]{3}",
        message = "Currency must be a 3-letter code such as USD")
    String currency,

    LocalDate dueDate) {
}
