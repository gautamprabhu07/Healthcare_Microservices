package com.pm.billingservice.config;

import com.pm.billingservice.kafka.BillingEventProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

  @Bean
  public NewTopic billingTopic() {
    return TopicBuilder.name(BillingEventProducer.TOPIC).partitions(1).replicas(1)
        .build();
  }

  /** Declared here too so the consumer never starts before the topic exists. */
  @Bean
  public NewTopic patientTopic() {
    return TopicBuilder.name("patient").partitions(1).replicas(1).build();
  }
}
