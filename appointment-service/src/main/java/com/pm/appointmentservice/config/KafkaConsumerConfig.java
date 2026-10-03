package com.pm.appointmentservice.config;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Consumer failure policy: retry 3 times, one second apart, then publish the
 * message to "<topic>.DLT" and move on so one bad message cannot block the
 * partition. Messages that can never succeed (unreadable or malformed) skip the
 * retries and go straight to the dead-letter topic.
 */
@Configuration
public class KafkaConsumerConfig {

  @Bean
  public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, byte[]> template) {
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
        template, (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));

    DefaultErrorHandler handler = new DefaultErrorHandler(recoverer,
        new FixedBackOff(1000L, 3));
    handler.addNotRetryableExceptions(InvalidProtocolBufferException.class,
        IllegalArgumentException.class);
    return handler;
  }

  /** Declared here too so the consumer never starts before the topic exists. */
  @Bean
  public NewTopic patientTopic() {
    return TopicBuilder.name("patient").partitions(1).replicas(1).build();
  }

  @Bean
  public NewTopic patientDltTopic() {
    return TopicBuilder.name("patient.DLT").partitions(1).replicas(1).build();
  }
}
