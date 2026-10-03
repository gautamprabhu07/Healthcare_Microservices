package com.pm.billingservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import java.time.Instant;

/** A Kafka event that has already been handled (keyed by the event's eventId). */
@Entity
public class ProcessedEvent {

  @Id
  @Column(length = 64)
  private String eventId;

  @Column(nullable = false)
  private Instant processedAt;

  protected ProcessedEvent() {
  }

  public ProcessedEvent(String eventId) {
    this.eventId = eventId;
  }

  @PrePersist
  void onCreate() {
    processedAt = Instant.now();
  }

  public String getEventId() {
    return eventId;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }
}
