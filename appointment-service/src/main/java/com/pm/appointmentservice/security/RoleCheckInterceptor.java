package com.pm.appointmentservice.security;

import com.pm.appointmentservice.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class RoleCheckInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request,
      HttpServletResponse response, Object handler) {

    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    RequiredRole required = handlerMethod.getMethodAnnotation(RequiredRole.class);
    if (required == null) {
      required = handlerMethod.getBeanType().getAnnotation(RequiredRole.class);
    }
    if (required == null) {
      return true;
    }

    CurrentUser user = CurrentUser.fromRequest(request);
    if (required.value().length > 0
        && !Arrays.asList(required.value()).contains(user.role())) {
      throw new ForbiddenException(
          "Your role (" + user.role() + ") is not allowed to perform this action");
    }
    return true;
  }
}
