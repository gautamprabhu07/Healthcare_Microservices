package com.pm.patientservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create/update payload. phoneNumber, gender and registeredDate are optional
 * (send null or omit them). registeredDate defaults to today on create and is
 * ignored on update.
 */
public record PatientRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    String name,

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    String email,

    @NotBlank(message = "Address is required")
    String address,

    @NotBlank(message = "Date of birth is required")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}",
        message = "Date of birth must use the format YYYY-MM-DD")
    String dateOfBirth,

    @Pattern(regexp = "\\+?[0-9][0-9 ()\\-]{6,19}",
        message = "Phone number is not valid")
    String phoneNumber,

    @Pattern(regexp = "MALE|FEMALE|OTHER",
        message = "Gender must be MALE, FEMALE or OTHER")
    String gender,

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}",
        message = "Registered date must use the format YYYY-MM-DD")
    String registeredDate) {
}
