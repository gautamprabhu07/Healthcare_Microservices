package com.pm.billingservice.service;

import com.pm.billingservice.model.ProcessedEvent;
import com.pm.billingservice.repository.ProcessedEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records event ids so a redelivered Kafka event is handled only once. It must
 * run inside the same transaction as the event's business change, so the two
 * commit (or roll back) together.
 */
@Component
public class EventIdempotency {

  private final ProcessedEventRepository repository;

  public EventIdempotency(ProcessedEventRepository repository) {
    this.repository = repository;
  }

  /**
   * Returns true the first time an event id is seen (and records it), false for
   * a duplicate. Events without an id cannot be de-duplicated here, so they are
   * always processed; their handlers are naturally idempotent.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public boolean firstTime(String eventId) {
    if (eventId == null || eventId.isBlank()) {
      return true;
    }
    if (repository.existsById(eventId)) {
      return false;
    }
    repository.saveAndFlush(new ProcessedEvent(eventId));
    return true;
  }
}
