package com.pm.authservice.service;

import com.pm.authservice.dto.ClaimsResponse;
import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.dto.LoginResponseDTO;
import com.pm.authservice.exception.UnauthorizedException;
import com.pm.authservice.mapper.UserMapper;
import com.pm.authservice.model.User;
import com.pm.authservice.util.IssuedToken;
import com.pm.authservice.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final UserService userService;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  public AuthService(UserService userService, PasswordEncoder passwordEncoder,
      JwtUtil jwtUtil) {
    this.userService = userService;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
  }

  /** Unknown email, wrong password and disabled account all fail the same way. */
  public LoginResponseDTO login(LoginRequestDTO request) {
    User user = userService.findByEmail(request.getEmail())
        .filter(User::isEnabled)
        .filter(u -> passwordEncoder.matches(request.getPassword(),
            u.getPassword()))
        .orElseThrow(
            () -> new UnauthorizedException("Invalid email or password"));

    IssuedToken issued = jwtUtil.generateToken(user);
    return new LoginResponseDTO(issued.token(), issued.expiresAt(),
        UserMapper.toResponse(user));
  }

  /**
   * Checks the token and that the user still exists and is enabled, then
   * returns the user's current role so role changes apply immediately.
   */
  public ClaimsResponse validate(String authorizationHeader) {
    if (authorizationHeader == null
        || !authorizationHeader.startsWith("Bearer ")) {
      throw new UnauthorizedException("Missing or malformed Authorization header");
    }

    Claims claims;
    try {
      claims = jwtUtil.parseClaims(authorizationHeader.substring(7));
    } catch (JwtException | IllegalArgumentException e) {
      throw new UnauthorizedException("Invalid or expired token");
    }

    User user = parseUserId(claims.get("userId", String.class))
        .flatMap(userService::findById)
        .filter(User::isEnabled)
        .orElseThrow(() -> new UnauthorizedException("Invalid or expired token"));

    return new ClaimsResponse(user.getId().toString(), user.getEmail(),
        user.getRole());
  }

  private static Optional<UUID> parseUserId(String value) {
    try {
      return value == null ? Optional.empty()
          : Optional.of(UUID.fromString(value));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
