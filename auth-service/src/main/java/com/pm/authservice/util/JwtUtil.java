package com.pm.authservice.util;

import com.pm.authservice.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  private final SecretKey secretKey;
  private final long expirationMs;

  public JwtUtil(@Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration-ms:28800000}") long expirationMs) {
    byte[] keyBytes = Base64.getDecoder()
        .decode(secret.getBytes(StandardCharsets.UTF_8));
    this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    this.expirationMs = expirationMs;
  }

  public IssuedToken generateToken(User user) {
    Instant issuedAt = Instant.now();
    Instant expiresAt = issuedAt.plusMillis(expirationMs);

    String token = Jwts.builder()
        .subject(user.getEmail())
        .claim("userId", user.getId().toString())
        .claim("email", user.getEmail())
        .claim("role", user.getRole())
        .claim("name", user.getFirstName() + " " + user.getLastName())
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(expiresAt))
        .signWith(secretKey)
        .compact();

    return new IssuedToken(token, expiresAt);
  }

  /** Verifies signature and expiry; throws {@link JwtException} when invalid. */
  public Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(secretKey).build()
        .parseSignedClaims(token).getPayload();
  }
}
