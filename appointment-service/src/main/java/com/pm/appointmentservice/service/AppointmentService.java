package com.pm.appointmentservice.service;

import com.pm.appointmentservice.client.DoctorClient;
import com.pm.appointmentservice.client.DoctorInfo;
import com.pm.appointmentservice.client.DownstreamUnavailableException;
import com.pm.appointmentservice.client.PatientClient;
import com.pm.appointmentservice.client.PatientInfo;
import com.pm.appointmentservice.dto.AppointmentResponse;
import com.pm.appointmentservice.dto.BookAppointmentRequest;
import com.pm.appointmentservice.dto.PageResponse;
import com.pm.appointmentservice.dto.UpdateAppointmentRequest;
import com.pm.appointmentservice.dto.UpdateStatusRequest;
import com.pm.appointmentservice.exception.ConflictException;
import com.pm.appointmentservice.exception.ForbiddenException;
import com.pm.appointmentservice.exception.InvalidRequestException;
import com.pm.appointmentservice.exception.InvalidStateException;
import com.pm.appointmentservice.exception.ResourceNotFoundException;
import com.pm.appointmentservice.exception.ServiceUnavailableException;
import com.pm.appointmentservice.kafka.AppointmentEventProducer;
import com.pm.appointmentservice.mapper.AppointmentMapper;
import com.pm.appointmentservice.model.Appointment;
import com.pm.appointmentservice.model.AppointmentStatus;
import com.pm.appointmentservice.repository.AppointmentRepository;
import com.pm.appointmentservice.security.CurrentUser;
import com.pm.appointmentservice.security.Role;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Remote lookups (patient over gRPC, doctor over REST) happen BEFORE the
 * database transaction opens, so no connection is held while waiting on the
 * network. The overlap checks and the insert then run in one short
 * transaction, guarded by advisory locks.
 */
@Service
public class AppointmentService {

  private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);
  private static final int DEFAULT_DURATION_MINUTES = 30;
  private static final int MAX_PAGE_SIZE = 100;

  private final AppointmentRepository appointmentRepository;
  private final PatientClient patientClient;
  private final DoctorClient doctorClient;
  private final AppointmentEventProducer eventProducer;
  private final ClinicRules clinicRules;
  private final TransactionTemplate tx;

  public AppointmentService(AppointmentRepository appointmentRepository,
      PatientClient patientClient, DoctorClient doctorClient,
      AppointmentEventProducer eventProducer, ClinicRules clinicRules,
      TransactionTemplate tx) {
    this.appointmentRepository = appointmentRepository;
    this.patientClient = patientClient;
    this.doctorClient = doctorClient;
    this.eventProducer = eventProducer;
    this.clinicRules = clinicRules;
    this.tx = tx;
  }

  public PageResponse<AppointmentResponse> list(UUID patientId, UUID doctorId,
      AppointmentStatus status, Instant from, Instant to, int page, int size,
      CurrentUser actor) {

    // A doctor only ever sees their own appointments, whatever they ask for.
    UUID effectiveDoctorId = actor.role() == Role.DOCTOR ? actor.id() : doctorId;

    Specification<Appointment> spec = Specification.where(null);
    if (patientId != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("patientId"), patientId));
    }
    if (effectiveDoctorId != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("doctorId"), effectiveDoctorId));
    }
    if (status != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
    }
    if (from != null) {
      spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("startTime"), from));
    }
    if (to != null) {
      spec = spec.and((root, q, cb) -> cb.lessThan(root.get("startTime"), to));
    }

    PageRequest pageable = PageRequest.of(Math.max(page, 0),
        Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
        Sort.by(Sort.Order.asc("startTime"), Sort.Order.asc("id")));

    Page<Appointment> result = appointmentRepository.findAll(spec, pageable);
    return PageResponse.of(result.map(AppointmentMapper::toResponse));
  }

  public AppointmentResponse get(UUID id, CurrentUser actor) {
    Appointment appointment = find(id);
    if (actor.role() == Role.DOCTOR && !appointment.getDoctorId().equals(actor.id())) {
      throw new ForbiddenException("You can only view your own appointments");
    }
    return AppointmentMapper.toResponse(appointment);
  }

  public AppointmentResponse book(BookAppointmentRequest request, CurrentUser actor) {
    int minutes = request.durationMinutes() == null
        ? DEFAULT_DURATION_MINUTES : request.durationMinutes();
    Instant start = request.startTime();
    Instant end = start.plus(Duration.ofMinutes(minutes));

    clinicRules.validateBookableSlot(start, end);
    PatientInfo patient = requirePatient(request.patientId());
    DoctorInfo doctor = requireDoctor(request.doctorId());

    return tx.execute(status -> {
      lockFor(doctor.id(), request.patientId());
      ensureFree(doctor.id(), doctor.fullName(), request.patientId(), start, end,
          AppointmentRepository.NO_APPOINTMENT);

      Appointment appointment = new Appointment();
      appointment.setPatientId(request.patientId());
      appointment.setPatientName(patient.name());
      appointment.setPatientEmail(patient.email());
      appointment.setDoctorId(doctor.id());
      appointment.setDoctorName(doctor.fullName());
      appointment.setStartTime(start);
      appointment.setEndTime(end);
      appointment.setReason(blankToNull(request.reason()));
      appointment.setNotes(blankToNull(request.notes()));
      appointment.setStatus(AppointmentStatus.SCHEDULED);
      appointment.setCreatedBy(actor.id());

      Appointment saved = appointmentRepository.saveAndFlush(appointment);
      eventProducer.publishAfterCommit(saved, AppointmentEventProducer.BOOKED);
      return AppointmentMapper.toResponse(saved);
    });
  }

  public AppointmentResponse reschedule(UUID id, UpdateAppointmentRequest request) {
    Appointment current = find(id);
    int minutes = request.durationMinutes() != null ? request.durationMinutes()
        : (int) Duration.between(current.getStartTime(), current.getEndTime()).toMinutes();
    Instant start = request.startTime();
    Instant end = start.plus(Duration.ofMinutes(minutes));

    clinicRules.validateBookableSlot(start, end);

    return tx.execute(status -> {
      Appointment appointment = find(id);
      requireScheduled(appointment, "rescheduled");

      lockFor(appointment.getDoctorId(), appointment.getPatientId());
      ensureFree(appointment.getDoctorId(), appointment.getDoctorName(),
          appointment.getPatientId(), start, end, appointment.getId());

      appointment.setStartTime(start);
      appointment.setEndTime(end);
      if (request.reason() != null) {
        appointment.setReason(blankToNull(request.reason()));
      }
      if (request.notes() != null) {
        appointment.setNotes(blankToNull(request.notes()));
      }

      Appointment saved = appointmentRepository.saveAndFlush(appointment);
      eventProducer.publishAfterCommit(saved, AppointmentEventProducer.RESCHEDULED);
      return AppointmentMapper.toResponse(saved);
    });
  }

  public AppointmentResponse updateStatus(UUID id, UpdateStatusRequest request,
      CurrentUser actor) {

    return tx.execute(status -> {
      Appointment appointment = find(id);
      authorizeStatusChange(appointment, request.status(), actor);
      requireScheduled(appointment, request.status().name().toLowerCase().replace('_', ' '));

      String eventType;
      switch (request.status()) {
        case COMPLETED -> {
          appointment.setStatus(AppointmentStatus.COMPLETED);
          eventType = AppointmentEventProducer.COMPLETED;
        }
        case NO_SHOW -> {
          appointment.setStatus(AppointmentStatus.NO_SHOW);
          eventType = AppointmentEventProducer.NO_SHOW;
        }
        default -> {
          appointment.setStatus(AppointmentStatus.CANCELLED);
          appointment.setCancelReason(blankToNull(request.cancelReason()));
          eventType = AppointmentEventProducer.CANCELLED;
        }
      }
      if (request.notes() != null) {
        appointment.setNotes(blankToNull(request.notes()));
      }

      Appointment saved = appointmentRepository.saveAndFlush(appointment);
      eventProducer.publishAfterCommit(saved, eventType);
      return AppointmentMapper.toResponse(saved);
    });
  }

  /**
   * Cancelling is for reception/admin. Completing or marking a no-show is for
   * admin or the appointment's own doctor.
   */
  private void authorizeStatusChange(Appointment appointment,
      UpdateStatusRequest.TargetStatus target, CurrentUser actor) {

    if (target == UpdateStatusRequest.TargetStatus.CANCELLED) {
      if (actor.role() == Role.DOCTOR) {
        throw new ForbiddenException(
            "Doctors cannot cancel appointments; please contact reception");
      }
      return;
    }

    if (actor.role() == Role.RECEPTIONIST) {
      throw new ForbiddenException(
          "Only a doctor or an admin can mark an appointment as " + target);
    }
    if (actor.role() == Role.DOCTOR && !appointment.getDoctorId().equals(actor.id())) {
      throw new ForbiddenException("You can only update your own appointments");
    }
  }

  private void requireScheduled(Appointment appointment, String action) {
    if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
      throw new InvalidStateException("The appointment is " + appointment.getStatus()
          + " and cannot be " + action + "; only SCHEDULED appointments can be changed");
    }
  }

  private void ensureFree(UUID doctorId, String doctorName, UUID patientId,
      Instant start, Instant end, UUID excludeId) {
    if (appointmentRepository.doctorHasOverlap(doctorId, AppointmentStatus.SCHEDULED,
        start, end, excludeId)) {
      throw new ConflictException(
          doctorName + " already has an appointment overlapping that time");
    }
    if (appointmentRepository.patientHasOverlap(patientId, AppointmentStatus.SCHEDULED,
        start, end, excludeId)) {
      throw new ConflictException(
          "The patient already has an appointment overlapping that time");
    }
  }

  /** Serialises concurrent bookings for the same doctor/patient (consistent lock order). */
  private void lockFor(UUID doctorId, UUID patientId) {
    List<String> keys = List.of("doctor:" + doctorId, "patient:" + patientId);
    keys.stream().sorted(Comparator.naturalOrder()).forEach(appointmentRepository::lock);
  }

  private PatientInfo requirePatient(UUID patientId) {
    try {
      return patientClient.getPatient(patientId).orElseThrow(
          () -> new ResourceNotFoundException("Patient not found with ID: " + patientId));
    } catch (StatusRuntimeException | CallNotPermittedException e) {
      log.error("Patient lookup failed: {}", e.toString());
      throw new ServiceUnavailableException(
          "Patient service is unavailable. Please try again shortly");
    }
  }

  DoctorInfo requireDoctor(UUID doctorId) {
    DoctorInfo doctor;
    try {
      doctor = doctorClient.getDoctor(doctorId).orElseThrow(
          () -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));
    } catch (DownstreamUnavailableException | CallNotPermittedException e) {
      log.error("Doctor lookup failed: {}", e.toString());
      throw new ServiceUnavailableException(
          "User service is unavailable. Please try again shortly");
    }
    if (!doctor.isActiveDoctor()) {
      throw new InvalidRequestException("doctorId",
          "The selected user is not an active doctor");
    }
    return doctor;
  }

  private Appointment find(UUID id) {
    return appointmentRepository.findById(id).orElseThrow(
        () -> new ResourceNotFoundException("Appointment not found with ID: " + id));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
