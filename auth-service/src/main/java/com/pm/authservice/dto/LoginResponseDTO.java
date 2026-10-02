package com.pm.authservice.dto;

import java.time.Instant;

public record LoginResponseDTO(String token, Instant expiresAt,
                               UserResponse user) {
}
