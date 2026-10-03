package com.pm.appointmentservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import patient.grpc.GetPatientRequest;
import patient.grpc.GetPatientResponse;
import patient.grpc.PatientServiceGrpc;

/** Looks patients up in patient-service over gRPC (port 9002). */
@Service
public class PatientClient {

  private static final Logger log = LoggerFactory.getLogger(PatientClient.class);

  private final ManagedChannel channel;
  private final PatientServiceGrpc.PatientServiceBlockingStub stub;
  private final long deadlineSeconds;

  public PatientClient(
      @Value("${patient.service.address:localhost}") String address,
      @Value("${patient.service.grpc.port:9002}") int port,
      @Value("${patient.service.deadline-seconds:3}") long deadlineSeconds) {
    this.deadlineSeconds = deadlineSeconds;

    log.info("Connecting to Patient Service gRPC at {}:{}", address, port);
    this.channel = ManagedChannelBuilder.forAddress(address, port)
        .usePlaintext().build();
    channel.getState(true);
    this.stub = PatientServiceGrpc.newBlockingStub(channel);
  }

  /**
   * Empty when the patient does not exist. gRPC failures are retried once and
   * then trip the circuit breaker; they surface as StatusRuntimeException.
   */
  @Retry(name = "patient")
  @CircuitBreaker(name = "patient")
  public Optional<PatientInfo> getPatient(UUID patientId) {
    GetPatientResponse response;
    try {
      response = stub
          .withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
          .getPatient(GetPatientRequest.newBuilder()
              .setPatientId(patientId.toString()).build());
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
        // The peer restarted: reconnect now instead of waiting out gRPC's backoff,
        // so the retry (and the next request) can succeed straight away.
        channel.resetConnectBackoff();
      }
      throw e;
    }

    if (!response.getExists()) {
      return Optional.empty();
    }
    return Optional.of(new PatientInfo(response.getPatientId(), response.getName(),
        response.getEmail()));
  }
}
