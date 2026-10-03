package com.pm.appointmentservice.repository;

import com.pm.appointmentservice.model.Appointment;
import com.pm.appointmentservice.model.AppointmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository
    extends JpaRepository<Appointment, UUID>, JpaSpecificationExecutor<Appointment> {

  /** Used when nothing needs to be excluded from an overlap check. */
  UUID NO_APPOINTMENT = new UUID(0L, 0L);

  /** True when the doctor has another SCHEDULED appointment overlapping [start, end). */
  @Query("select count(a) > 0 from Appointment a "
      + "where a.doctorId = :doctorId and a.status = :status "
      + "and a.startTime < :end and a.endTime > :start and a.id <> :excludeId")
  boolean doctorHasOverlap(@Param("doctorId") UUID doctorId,
      @Param("status") AppointmentStatus status, @Param("start") Instant start,
      @Param("end") Instant end, @Param("excludeId") UUID excludeId);

  /** True when the patient has another SCHEDULED appointment overlapping [start, end). */
  @Query("select count(a) > 0 from Appointment a "
      + "where a.patientId = :patientId and a.status = :status "
      + "and a.startTime < :end and a.endTime > :start and a.id <> :excludeId")
  boolean patientHasOverlap(@Param("patientId") UUID patientId,
      @Param("status") AppointmentStatus status, @Param("start") Instant start,
      @Param("end") Instant end, @Param("excludeId") UUID excludeId);

  List<Appointment> findByDoctorIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
      UUID doctorId, AppointmentStatus status, Instant before, Instant after);

  /** SCHEDULED appointments of a patient that start after the given instant. */
  List<Appointment> findByPatientIdAndStatusAndStartTimeAfter(UUID patientId,
      AppointmentStatus status, Instant after);

  /** Refreshes the cached patient details on every appointment of the patient. */
  @Modifying
  @Query("update Appointment a set a.patientName = :name, a.patientEmail = :email "
      + "where a.patientId = :patientId")
  int updatePatientDetails(@Param("patientId") UUID patientId,
      @Param("name") String name, @Param("email") String email);

  /**
   * Transaction-scoped advisory lock (PostgreSQL). Serialises concurrent bookings
   * for the same doctor or patient so two requests cannot both pass the overlap check.
   */
  @Query(value = "select cast(pg_advisory_xact_lock(hashtext(:key)) as varchar)",
      nativeQuery = true)
  String lock(@Param("key") String key);
}
