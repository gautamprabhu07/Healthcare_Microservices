package com.pm.authservice.dto;

import com.pm.authservice.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be a valid email address")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    String password,

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    String lastName,

    @NotNull(message = "Role is required")
    Role role,

    @Size(max = 100, message = "Specialization cannot exceed 100 characters")
    String specialization) {
}
