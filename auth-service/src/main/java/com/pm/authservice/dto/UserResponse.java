package com.pm.authservice.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String email, String firstName,
                           String lastName, String role, String specialization,
                           boolean enabled, Instant createdAt) {
}
