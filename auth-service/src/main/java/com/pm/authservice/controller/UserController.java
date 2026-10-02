package com.pm.authservice.controller;

import com.pm.authservice.dto.CreateUserRequest;
import com.pm.authservice.dto.PageResponse;
import com.pm.authservice.dto.UpdateStatusRequest;
import com.pm.authservice.dto.UpdateUserRequest;
import com.pm.authservice.dto.UserResponse;
import com.pm.authservice.security.CurrentUser;
import com.pm.authservice.security.RequiredRole;
import com.pm.authservice.security.Role;
import com.pm.authservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Staff user management")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @Operation(summary = "List users (ADMIN)")
  @RequiredRole(Role.ADMIN)
  @GetMapping
  public ResponseEntity<PageResponse<UserResponse>> list(
      @RequestParam(required = false) Role role,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.ok(userService.list(role, page, size));
  }

  @Operation(summary = "List enabled doctors (any authenticated role)")
  @RequiredRole
  @GetMapping("/doctors")
  public ResponseEntity<List<UserResponse>> doctors() {
    return ResponseEntity.ok(userService.listDoctors());
  }

  @Operation(summary = "Get a user by id (ADMIN)")
  @RequiredRole(Role.ADMIN)
  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(userService.getById(id));
  }

  @Operation(summary = "Create a staff user (ADMIN)")
  @RequiredRole(Role.ADMIN)
  @PostMapping
  public ResponseEntity<UserResponse> create(
      @Valid @RequestBody CreateUserRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(userService.create(request));
  }

  @Operation(summary = "Update name, role and specialization (ADMIN)")
  @RequiredRole(Role.ADMIN)
  @PutMapping("/{id}")
  public ResponseEntity<UserResponse> update(@PathVariable UUID id,
      @Valid @RequestBody UpdateUserRequest request, CurrentUser currentUser) {
    return ResponseEntity.ok(userService.update(id, request, currentUser));
  }

  @Operation(summary = "Enable or disable a user (ADMIN)")
  @RequiredRole(Role.ADMIN)
  @PatchMapping("/{id}/status")
  public ResponseEntity<UserResponse> updateStatus(@PathVariable UUID id,
      @Valid @RequestBody UpdateStatusRequest request,
      CurrentUser currentUser) {
    return ResponseEntity.ok(
        userService.setEnabled(id, request.enabled(), currentUser));
  }
}
