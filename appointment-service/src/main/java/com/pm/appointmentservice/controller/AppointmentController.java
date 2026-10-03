package com.pm.appointmentservice.controller;

import com.pm.appointmentservice.dto.AppointmentResponse;
import com.pm.appointmentservice.dto.AvailabilityResponse;
import com.pm.appointmentservice.dto.BookAppointmentRequest;
import com.pm.appointmentservice.dto.PageResponse;
import com.pm.appointmentservice.dto.UpdateAppointmentRequest;
import com.pm.appointmentservice.dto.UpdateStatusRequest;
import com.pm.appointmentservice.model.AppointmentStatus;
import com.pm.appointmentservice.security.CurrentUser;
import com.pm.appointmentservice.security.RequiredRole;
import com.pm.appointmentservice.security.Role;
import com.pm.appointmentservice.service.AppointmentService;
import com.pm.appointmentservice.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/appointments")
@RequiredRole({Role.ADMIN, Role.RECEPTIONIST, Role.DOCTOR})
@Tag(name = "Appointments", description = "Book and manage appointments")
public class AppointmentController {

  private final AppointmentService appointmentService;
  private final AvailabilityService availabilityService;

  public AppointmentController(AppointmentService appointmentService,
      AvailabilityService availabilityService) {
    this.appointmentService = appointmentService;
    this.availabilityService = availabilityService;
  }

  @GetMapping
  @Operation(summary = "List appointments (doctors only see their own)")
  public ResponseEntity<PageResponse<AppointmentResponse>> list(
      @RequestParam(required = false) UUID patientId,
      @RequestParam(required = false) UUID doctorId,
      @RequestParam(required = false) AppointmentStatus status,
      @Parameter(description = "Start of range (inclusive), ISO-8601 instant")
      @RequestParam(required = false) Instant from,
      @Parameter(description = "End of range (exclusive), ISO-8601 instant")
      @RequestParam(required = false) Instant to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      CurrentUser currentUser) {
    return ResponseEntity.ok(appointmentService.list(patientId, doctorId, status,
        from, to, page, size, currentUser));
  }

  @GetMapping("/availability")
  @Operation(summary = "Free slots for a doctor on a day (clinic hours, Mon-Fri)")
  public ResponseEntity<AvailabilityResponse> availability(
      @RequestParam UUID doctorId,
      @Parameter(description = "Day as YYYY-MM-DD")
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(availabilityService.availability(doctorId, date));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get an appointment by id")
  public ResponseEntity<AppointmentResponse> get(@PathVariable UUID id,
      CurrentUser currentUser) {
    return ResponseEntity.ok(appointmentService.get(id, currentUser));
  }

  @PostMapping
  @RequiredRole({Role.ADMIN, Role.RECEPTIONIST})
  @Operation(summary = "Book an appointment")
  public ResponseEntity<AppointmentResponse> book(
      @Valid @RequestBody BookAppointmentRequest request, CurrentUser currentUser) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(appointmentService.book(request, currentUser));
  }

  @PutMapping("/{id}")
  @RequiredRole({Role.ADMIN, Role.RECEPTIONIST})
  @Operation(summary = "Reschedule a SCHEDULED appointment or edit its details")
  public ResponseEntity<AppointmentResponse> reschedule(@PathVariable UUID id,
      @Valid @RequestBody UpdateAppointmentRequest request) {
    return ResponseEntity.ok(appointmentService.reschedule(id, request));
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Complete, cancel or mark no-show (SCHEDULED appointments only)")
  public ResponseEntity<AppointmentResponse> updateStatus(@PathVariable UUID id,
      @Valid @RequestBody UpdateStatusRequest request, CurrentUser currentUser) {
    return ResponseEntity.ok(appointmentService.updateStatus(id, request, currentUser));
  }
}
