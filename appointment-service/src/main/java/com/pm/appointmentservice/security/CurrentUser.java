package com.pm.appointmentservice.security;

import com.pm.appointmentservice.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/** The caller's identity, read from the X-User-* headers set by the gateway. */
public record CurrentUser(UUID id, String email, Role role) {

  public static final String HEADER_ID = "X-User-Id";
  public static final String HEADER_EMAIL = "X-User-Email";
  public static final String HEADER_ROLE = "X-User-Role";
  static final String REQUEST_ATTRIBUTE = CurrentUser.class.getName();

  public static CurrentUser fromRequest(HttpServletRequest request) {
    Object cached = request.getAttribute(REQUEST_ATTRIBUTE);
    if (cached instanceof CurrentUser currentUser) {
      return currentUser;
    }

    String id = request.getHeader(HEADER_ID);
    String email = request.getHeader(HEADER_EMAIL);
    String role = request.getHeader(HEADER_ROLE);
    if (id == null || email == null || role == null) {
      throw new UnauthorizedException("Authentication required");
    }

    try {
      CurrentUser currentUser = new CurrentUser(UUID.fromString(id), email,
          Role.valueOf(role));
      request.setAttribute(REQUEST_ATTRIBUTE, currentUser);
      return currentUser;
    } catch (IllegalArgumentException e) {
      throw new UnauthorizedException("Authentication required");
    }
  }

  public boolean hasRole(Role expected) {
    return role == expected;
  }
}
