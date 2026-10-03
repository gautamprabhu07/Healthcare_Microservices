package com.pm.appointmentservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Puts the gateway's X-Request-Id into the logging MDC (the gateway returns it to the client). */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

  private static final String HEADER = "X-Request-Id";
  private static final String MDC_KEY = "requestId";
  private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._-]{1,100}");

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String incoming = request.getHeader(HEADER);
    String requestId = incoming != null && VALID_ID.matcher(incoming).matches()
        ? incoming : UUID.randomUUID().toString();

    MDC.put(MDC_KEY, requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}
