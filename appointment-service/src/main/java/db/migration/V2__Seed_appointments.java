package db.migration;

import java.sql.PreparedStatement;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Demo data: about two weeks of past appointments (completed, no-show,
 * cancelled) and the next two weeks of upcoming ones, for the seeded doctors
 * and the 15 seeded patients. A Java migration is used so the dates are
 * relative to the day the database is first created and the demo data never
 * looks stale. Appointments are on weekdays inside clinic hours (UTC).
 */
public class V2__Seed_appointments extends BaseJavaMigration {

  private record Person(String id, String name, String email) {
  }

  private static final List<Person> PATIENTS = List.of(
      new Person("123e4567-e89b-12d3-a456-426614174000", "John Doe", "john.doe@example.com"),
      new Person("123e4567-e89b-12d3-a456-426614174001", "Jane Smith", "jane.smith@example.com"),
      new Person("123e4567-e89b-12d3-a456-426614174002", "Alice Johnson", "alice.johnson@example.com"),
      new Person("123e4567-e89b-12d3-a456-426614174003", "Bob Brown", "bob.brown@example.com"),
      new Person("123e4567-e89b-12d3-a456-426614174004", "Emily Davis", "emily.davis@example.com"),
      new Person("223e4567-e89b-12d3-a456-426614174005", "Michael Green", "michael.green@example.com"),
      new Person("223e4567-e89b-12d3-a456-426614174006", "Sarah Taylor", "sarah.taylor@example.com"),
      new Person("223e4567-e89b-12d3-a456-426614174007", "David Wilson", "david.wilson@example.com"),
      new Person("223e4567-e89b-12d3-a456-426614174008", "Laura White", "laura.white@example.com"),
      new Person("223e4567-e89b-12d3-a456-426614174009", "James Harris", "james.harris@example.com"),
      new Person("223e4567-e89b-12d3-a456-426614174010", "Emma Moore", "emma.moore@example.com"),
      new Person("223e4567-e89b-12d3-a456-426614174011", "Ethan Martinez", "ethan.martinez@example.com"),
      new Person("223e4567-e89b-12d3-a456-426614174012", "Sophia Clark", "sophia.clark@example.com"),
      new Person("223e4567-e89b-12d3-a456-426614174013", "Daniel Lewis", "daniel.lewis@example.com"),
      new Person("223e4567-e89b-12d3-a456-426614174014", "Isabella Walker", "isabella.walker@example.com"));

  // Same ids as the users seeded by auth-service
  private static final List<Person> DOCTORS = List.of(
      new Person("a1000000-0000-4000-8000-000000000003", "John Smith", "dr.smith@clinic.com"),
      new Person("a1000000-0000-4000-8000-000000000004", "Priya Patel", "dr.patel@clinic.com"),
      new Person("a1000000-0000-4000-8000-000000000005", "Maria Garcia", "dr.garcia@clinic.com"));

  private static final UUID RECEPTIONIST_ID =
      UUID.fromString("a1000000-0000-4000-8000-000000000002");

  private static final List<LocalTime> SLOTS = List.of(LocalTime.of(9, 0),
      LocalTime.of(10, 30), LocalTime.of(13, 0), LocalTime.of(15, 30));

  private static final List<String> REASONS = List.of("Annual check-up",
      "Follow-up visit", "Blood pressure review", "Vaccination",
      "Chest pain consultation", "Skin rash", "Child wellness visit",
      "Lab results review");

  private static final String INSERT = "INSERT INTO appointment (id, patient_id, "
      + "patient_name, patient_email, doctor_id, doctor_name, start_time, end_time, "
      + "reason, notes, status, cancel_reason, created_by, created_at, updated_at) "
      + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  @Override
  public void migrate(Context context) throws Exception {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);

    try (PreparedStatement ps = context.getConnection().prepareStatement(INSERT)) {
      int sequence = 0;

      List<LocalDate> past = weekdays(today, -1, -1, 10);
      for (int w = 0; w < past.size(); w++) {
        for (int d = 0; d < DOCTORS.size(); d++) {
          int k = w * 3 + d;
          String status = k % 7 == 3 ? "NO_SHOW" : k % 9 == 4 ? "CANCELLED" : "COMPLETED";
          addRow(ps, ++sequence, past.get(w), w, d, (w * 3 + d) % 15, status);
        }
      }

      List<LocalDate> upcoming = weekdays(today, 1, 1, 8);
      for (int w = 0; w < upcoming.size(); w++) {
        for (int d = 0; d < DOCTORS.size(); d++) {
          int k = w * 3 + d;
          String status = k % 11 == 5 ? "CANCELLED" : "SCHEDULED";
          addRow(ps, ++sequence, upcoming.get(w), w + 3, d, ((w + 10) * 3 + d) % 15, status);
        }
      }
      ps.executeBatch();
    }
  }

  private static List<LocalDate> weekdays(LocalDate from, int firstOffset, int step,
      int count) {
    List<LocalDate> days = new ArrayList<>();
    LocalDate day = from.plusDays(firstOffset);
    while (days.size() < count) {
      if (day.getDayOfWeek() != DayOfWeek.SATURDAY
          && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
        days.add(day);
      }
      day = day.plusDays(step);
    }
    return days;
  }

  private void addRow(PreparedStatement ps, int sequence, LocalDate date, int slotSeed,
      int doctorIndex, int patientIndex, String status) throws Exception {
    Person doctor = DOCTORS.get(doctorIndex);
    Person patient = PATIENTS.get(patientIndex);

    Instant start = date.atTime(SLOTS.get((slotSeed + doctorIndex) % SLOTS.size()))
        .toInstant(ZoneOffset.UTC);
    Instant end = start.plus(Duration.ofMinutes(sequence % 4 == 0 ? 45 : 30));
    boolean past = start.isBefore(Instant.now());

    String notes = status.equals("COMPLETED") ? "Visit completed. No follow-up needed." : null;
    String cancelReason = status.equals("CANCELLED")
        ? (past ? "Patient requested cancellation" : "Rescheduled by patient") : null;
    Instant created = start.minus(Duration.ofDays(3));

    ps.setObject(1, UUID.nameUUIDFromBytes(("seed-appointment-" + sequence).getBytes()));
    ps.setObject(2, UUID.fromString(patient.id()));
    ps.setString(3, patient.name());
    ps.setString(4, patient.email());
    ps.setObject(5, UUID.fromString(doctor.id()));
    ps.setString(6, doctor.name());
    ps.setObject(7, OffsetDateTime.ofInstant(start, ZoneOffset.UTC));
    ps.setObject(8, OffsetDateTime.ofInstant(end, ZoneOffset.UTC));
    ps.setString(9, REASONS.get((sequence + doctorIndex) % REASONS.size()));
    ps.setString(10, notes);
    ps.setString(11, status);
    ps.setString(12, cancelReason);
    ps.setObject(13, RECEPTIONIST_ID);
    ps.setObject(14, OffsetDateTime.ofInstant(created, ZoneOffset.UTC));
    ps.setObject(15, OffsetDateTime.ofInstant(created, ZoneOffset.UTC));
    ps.addBatch();
  }
}
