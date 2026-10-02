package com.pm.authservice.util;

import java.time.Instant;

public record IssuedToken(String token, Instant expiresAt) {
}
