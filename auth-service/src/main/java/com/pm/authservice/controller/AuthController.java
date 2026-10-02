package com.pm.authservice.controller;

import com.pm.authservice.dto.ChangePasswordRequest;
import com.pm.authservice.dto.ClaimsResponse;
import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.dto.LoginResponseDTO;
import com.pm.authservice.dto.UserResponse;
import com.pm.authservice.security.CurrentUser;
import com.pm.authservice.security.RequiredRole;
import com.pm.authservice.service.AuthService;
import com.pm.authservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

  private final AuthService authService;
  private final UserService userService;

  public AuthController(AuthService authService, UserService userService) {
    this.authService = authService;
    this.userService = userService;
  }

  @Operation(summary = "Generate token on user login")
  @PostMapping("/login")
  public ResponseEntity<LoginResponseDTO> login(
      @Valid @RequestBody LoginRequestDTO loginRequestDTO) {
    return ResponseEntity.ok(authService.login(loginRequestDTO));
  }

  @Operation(summary = "Validate token and return the caller's identity")
  @GetMapping("/validate")
  public ResponseEntity<ClaimsResponse> validateToken(
      @RequestHeader(value = "Authorization", required = false)
      String authHeader) {
    return ResponseEntity.ok(authService.validate(authHeader));
  }

  @Operation(summary = "Get the current user's profile")
  @RequiredRole
  @GetMapping("/auth/me")
  public ResponseEntity<UserResponse> me(CurrentUser currentUser) {
    return ResponseEntity.ok(userService.getById(currentUser.id()));
  }

  @Operation(summary = "Change the current user's password")
  @RequiredRole
  @PutMapping("/auth/me/password")
  public ResponseEntity<Void> changePassword(CurrentUser currentUser,
      @Valid @RequestBody ChangePasswordRequest request) {
    userService.changePassword(currentUser.id(), request);
    return ResponseEntity.noContent().build();
  }
}
