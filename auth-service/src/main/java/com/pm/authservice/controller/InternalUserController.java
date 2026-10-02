package com.pm.authservice.controller;

import com.pm.authservice.dto.UserResponse;
import com.pm.authservice.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service lookups on the internal Docker network. The API gateway
 * must never route /internal/**.
 */
@Hidden
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

  private final UserService userService;

  public InternalUserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(userService.getById(id));
  }
}
