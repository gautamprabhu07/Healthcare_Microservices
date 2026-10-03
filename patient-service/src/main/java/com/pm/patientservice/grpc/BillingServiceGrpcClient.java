package com.pm.patientservice.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingServiceGrpcClient {

  private static final Logger log = LoggerFactory.getLogger(
      BillingServiceGrpcClient.class);
  private final ManagedChannel channel;
  private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;
  private final long deadlineSeconds;

  public BillingServiceGrpcClient(
      @Value("${billing.service.address:localhost}") String serverAddress,
      @Value("${billing.service.grpc.port:9001}") int serverPort,
      @Value("${billing.service.deadline-seconds:5}") long deadlineSeconds) {
    this.deadlineSeconds = deadlineSeconds;

    log.info("Connecting to Billing Service GRPC service at {}:{}",
        serverAddress, serverPort);

    this.channel = ManagedChannelBuilder.forAddress(serverAddress,
        serverPort).usePlaintext().build();

    // Connect eagerly so the first request doesn't pay the connection cost
    // and trip the call deadline.
    channel.getState(true);

    blockingStub = BillingServiceGrpc.newBlockingStub(channel);
  }

  /**
   * Each attempt has its own deadline. Failures are retried once, and repeated
   * failures open a circuit breaker so callers fail fast while billing is down
   * (see resilience4j.* in application.properties).
   */
  @Retry(name = "billing")
  @CircuitBreaker(name = "billing")
  public BillingResponse createBillingAccount(String patientId, String name,
      String email) {

    BillingRequest request = BillingRequest.newBuilder().setPatientId(patientId)
        .setName(name).setEmail(email).build();

    BillingResponse response;
    try {
      response = blockingStub
          .withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
          .createBillingAccount(request);
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
        // The peer restarted: reconnect now instead of waiting out gRPC's backoff.
        channel.resetConnectBackoff();
      }
      throw e;
    }
    log.info("Received response from billing service via GRPC: {}", response);
    return response;
  }
}
