package com.pm.patientservice.service;

import com.pm.patientservice.dto.PageResponse;
import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.InvalidRequestException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.KafkaProducer;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PatientService {

  private static final int MAX_PAGE_SIZE = 100;
  private static final Set<String> TEXT_SORT_FIELDS = Set.of("name", "email");
  private static final Set<String> SORT_FIELDS = Set.of("name", "email",
      "dateOfBirth", "registeredDate", "createdAt");

  private final PatientRepository patientRepository;
  private final BillingServiceGrpcClient billingServiceGrpcClient;
  private final KafkaProducer kafkaProducer;

  public PatientService(PatientRepository patientRepository,
      BillingServiceGrpcClient billingServiceGrpcClient,
      KafkaProducer kafkaProducer) {
    this.patientRepository = patientRepository;
    this.billingServiceGrpcClient = billingServiceGrpcClient;
    this.kafkaProducer = kafkaProducer;
  }

  @Transactional(readOnly = true)
  public PageResponse<PatientResponseDTO> getPatients(String search, int page,
      int size, String sort) {

    PageRequest pageable = PageRequest.of(Math.max(page, 0),
        Math.min(Math.max(size, 1), MAX_PAGE_SIZE), parseSort(sort));

    Page<Patient> patients = search == null || search.isBlank()
        ? patientRepository.findAll(pageable)
        : patientRepository.search(escapeLike(search.trim()), pageable);

    return PageResponse.of(patients.map(PatientMapper::toDTO));
  }

  @Transactional(readOnly = true)
  public PatientResponseDTO getPatient(UUID id) {
    return PatientMapper.toDTO(findOrThrow(id));
  }

  /**
   * Saves the patient, creates the billing account over gRPC, and only then
   * publishes the Kafka event. If billing fails the transaction rolls back (no
   * patient row, no event).
   */
  @Transactional
  public PatientResponseDTO createPatient(PatientRequestDTO request) {
    String email = normalizeEmail(request.email());
    if (patientRepository.existsByEmail(email)) {
      throw new EmailAlreadyExistsException(
          "A patient with this email already exists: " + email);
    }
    validateDateOfBirth(request.dateOfBirth());

    Patient newPatient = patientRepository.save(PatientMapper.toModel(request));

    billingServiceGrpcClient.createBillingAccount(newPatient.getId().toString(),
        newPatient.getName(), newPatient.getEmail());

    publishAfterCommit(newPatient, KafkaProducer.PATIENT_CREATED);

    return PatientMapper.toDTO(newPatient);
  }

  @Transactional
  public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO request) {
    Patient patient = findOrThrow(id);

    String email = normalizeEmail(request.email());
    if (patientRepository.existsByEmailAndIdNot(email, id)) {
      throw new EmailAlreadyExistsException(
          "A patient with this email already exists: " + email);
    }
    validateDateOfBirth(request.dateOfBirth());

    PatientMapper.applyEditableFields(patient, request);
    Patient updatedPatient = patientRepository.saveAndFlush(patient);

    publishAfterCommit(updatedPatient, KafkaProducer.PATIENT_UPDATED);

    return PatientMapper.toDTO(updatedPatient);
  }

  @Transactional
  public void deletePatient(UUID id) {
    Patient patient = findOrThrow(id);
    patientRepository.delete(patient);

    publishAfterCommit(patient, KafkaProducer.PATIENT_DELETED);
  }

  private Patient findOrThrow(UUID id) {
    return patientRepository.findById(id).orElseThrow(
        () -> new PatientNotFoundException("Patient not found with ID: " + id));
  }

  /** Events describe committed data only, so they are sent after the commit. */
  private void publishAfterCommit(Patient patient, String eventType) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              kafkaProducer.sendEvent(patient, eventType);
            }
          });
    } else {
      kafkaProducer.sendEvent(patient, eventType);
    }
  }

  private static Sort parseSort(String sort) {
    String[] parts = sort == null || sort.isBlank()
        ? new String[] {"name", "asc"} : sort.split(",");
    String field = parts[0].trim();
    if (!SORT_FIELDS.contains(field)) {
      throw new InvalidRequestException("sort",
          "Sort field must be one of: " + String.join(", ",
              SORT_FIELDS.stream().sorted().toList()));
    }

    Sort.Direction direction = Sort.Direction.ASC;
    if (parts.length > 1) {
      String value = parts[1].trim().toLowerCase(Locale.ROOT);
      if (value.equals("desc")) {
        direction = Sort.Direction.DESC;
      } else if (!value.equals("asc")) {
        throw new InvalidRequestException("sort",
            "Sort direction must be asc or desc");
      }
    }

    Sort.Order order = new Sort.Order(direction, field);
    if (TEXT_SORT_FIELDS.contains(field)) {
      order = order.ignoreCase();
    }
    // id as a tie-breaker keeps page boundaries stable
    return Sort.by(order, Sort.Order.asc("id"));
  }

  private static void validateDateOfBirth(String dateOfBirth) {
    if (LocalDate.parse(dateOfBirth).isAfter(LocalDate.now())) {
      throw new InvalidRequestException("dateOfBirth",
          "Date of birth cannot be in the future");
    }
  }

  private static String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private static String escapeLike(String term) {
    return term.replace("!", "!!").replace("%", "!%").replace("_", "!_");
  }
}
