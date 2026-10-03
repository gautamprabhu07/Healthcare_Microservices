package com.pm.billingservice.kafka;

import appointment.events.AppointmentEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import com.pm.billingservice.service.AppointmentEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class AppointmentEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(
      AppointmentEventConsumer.class);

  private final AppointmentEventHandler handler;

  public AppointmentEventConsumer(AppointmentEventHandler handler) {
    this.handler = handler;
  }

  @KafkaListener(topics = "appointment", groupId = "billing-service")
  public void consume(byte[] payload) throws InvalidProtocolBufferException {
    AppointmentEvent event = AppointmentEvent.parseFrom(payload);
    log.info("Received {} (eventId={}, appointmentId={})", event.getEventType(),
        event.getEventId(), event.getAppointmentId());
    handler.handle(event);
  }
}
