package com.pm.patientservice.mapper;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.model.Gender;
import com.pm.patientservice.model.Patient;
import java.time.LocalDate;
import java.util.Locale;

public class PatientMapper {

  public static PatientResponseDTO toDTO(Patient patient) {
    return new PatientResponseDTO(
        patient.getId().toString(),
        patient.getName(),
        patient.getEmail(),
        patient.getAddress(),
        patient.getDateOfBirth().toString(),
        patient.getPhoneNumber(),
        patient.getGender() == null ? null : patient.getGender().name(),
        patient.getRegisteredDate().toString(),
        patient.getCreatedAt().toString(),
        patient.getUpdatedAt().toString());
  }

  /** Builds a new patient; registeredDate defaults to today when omitted. */
  public static Patient toModel(PatientRequestDTO dto) {
    Patient patient = new Patient();
    applyEditableFields(patient, dto);
    patient.setRegisteredDate(dto.registeredDate() == null
        ? LocalDate.now() : LocalDate.parse(dto.registeredDate()));
    return patient;
  }

  /** Copies the fields a client may edit; registeredDate is never changed. */
  public static void applyEditableFields(Patient patient, PatientRequestDTO dto) {
    patient.setName(dto.name().trim());
    patient.setEmail(dto.email().trim().toLowerCase(Locale.ROOT));
    patient.setAddress(dto.address().trim());
    patient.setDateOfBirth(LocalDate.parse(dto.dateOfBirth()));
    patient.setPhoneNumber(dto.phoneNumber() == null ? null : dto.phoneNumber().trim());
    patient.setGender(dto.gender() == null ? null : Gender.valueOf(dto.gender()));
  }
}
