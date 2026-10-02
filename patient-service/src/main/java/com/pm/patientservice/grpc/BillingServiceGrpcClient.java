package com.pm.patientservice.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingServiceGrpcClient {

  private static final Logger log = LoggerFactory.getLogger(
      BillingServiceGrpcClient.class);
  private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;
  private final long deadlineSeconds;

  public BillingServiceGrpcClient(
      @Value("${billing.service.address:localhost}") String serverAddress,
      @Value("${billing.service.grpc.port:9001}") int serverPort,
      @Value("${billing.service.deadline-seconds:10}") long deadlineSeconds) {
    this.deadlineSeconds = deadlineSeconds;

    log.info("Connecting to Billing Service GRPC service at {}:{}",
        serverAddress, serverPort);

    ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress,
        serverPort).usePlaintext().build();

    // Connect eagerly so the first request doesn't pay the connection cost
    // and trip the call deadline.
    channel.getState(true);

    blockingStub = BillingServiceGrpc.newBlockingStub(channel);
  }

  public BillingResponse createBillingAccount(String patientId, String name,
      String email) {

    BillingRequest request = BillingRequest.newBuilder().setPatientId(patientId)
        .setName(name).setEmail(email).build();

    BillingResponse response = blockingStub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
        .createBillingAccount(request);
    log.info("Received response from billing service via GRPC: {}", response);
    return response;
  }
}
