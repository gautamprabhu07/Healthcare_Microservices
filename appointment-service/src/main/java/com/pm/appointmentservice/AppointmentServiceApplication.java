package com.pm.appointmentservice;

import com.pm.appointmentservice.config.ClinicProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ClinicProperties.class)
public class AppointmentServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AppointmentServiceApplication.class, args);
  }
}
