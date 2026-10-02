package com.pm.patientservice.controller;

import com.pm.patientservice.dto.PageResponse;
import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.security.RequiredRole;
import com.pm.patientservice.security.Role;
import com.pm.patientservice.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patients")
@Tag(name = "Patient", description = "API for managing Patients")
public class PatientController {

  private final PatientService patientService;

  public PatientController(PatientService patientService) {
    this.patientService = patientService;
  }

  @RequiredRole({Role.ADMIN, Role.RECEPTIONIST, Role.DOCTOR})
  @GetMapping
  @Operation(summary = "List patients (paged, searchable, sortable)")
  public ResponseEntity<PageResponse<PatientResponseDTO>> getPatients(
      @Parameter(description = "Case-insensitive match on name or email")
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @Parameter(description = "field,direction. Fields: name, email, "
          + "dateOfBirth, registeredDate, createdAt. Direction: asc or desc")
      @RequestParam(defaultValue = "name,asc") String sort) {
    return ResponseEntity.ok(patientService.getPatients(search, page, size, sort));
  }

  @RequiredRole({Role.ADMIN, Role.RECEPTIONIST, Role.DOCTOR})
  @GetMapping("/{id}")
  @Operation(summary = "Get a Patient by id")
  public ResponseEntity<PatientResponseDTO> getPatient(@PathVariable UUID id) {
    return ResponseEntity.ok(patientService.getPatient(id));
  }

  @RequiredRole({Role.ADMIN, Role.RECEPTIONIST})
  @PostMapping
  @Operation(summary = "Create a new Patient")
  public ResponseEntity<PatientResponseDTO> createPatient(
      @Valid @RequestBody PatientRequestDTO patientRequestDTO) {
    return ResponseEntity.ok(patientService.createPatient(patientRequestDTO));
  }

  @RequiredRole({Role.ADMIN, Role.RECEPTIONIST})
  @PutMapping("/{id}")
  @Operation(summary = "Update a Patient")
  public ResponseEntity<PatientResponseDTO> updatePatient(@PathVariable UUID id,
      @Valid @RequestBody PatientRequestDTO patientRequestDTO) {
    return ResponseEntity.ok(patientService.updatePatient(id, patientRequestDTO));
  }

  @RequiredRole(Role.ADMIN)
  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a Patient")
  public ResponseEntity<Void> deletePatient(@PathVariable UUID id) {
    patientService.deletePatient(id);
    return ResponseEntity.noContent().build();
  }
}
