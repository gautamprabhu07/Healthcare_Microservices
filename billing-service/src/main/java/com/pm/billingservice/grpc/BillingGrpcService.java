package com.pm.billingservice.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc.BillingServiceImplBase;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.service.BillingAccountService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class BillingGrpcService extends BillingServiceImplBase {

  private static final Logger log = LoggerFactory.getLogger(
      BillingGrpcService.class);

  private final BillingAccountService accountService;

  public BillingGrpcService(BillingAccountService accountService) {
    this.accountService = accountService;
  }

  /** Idempotent: calling it again for the same patient returns the same account. */
  @Override
  public void createBillingAccount(BillingRequest billingRequest,
      StreamObserver<BillingResponse> responseObserver) {

    log.info("createBillingAccount request received {}", billingRequest);

    UUID patientId;
    try {
      patientId = UUID.fromString(billingRequest.getPatientId());
    } catch (IllegalArgumentException e) {
      responseObserver.onError(Status.INVALID_ARGUMENT
          .withDescription("patientId must be a valid UUID").asRuntimeException());
      return;
    }

    BillingAccount account = accountService.createOrGet(patientId,
        billingRequest.getName(), billingRequest.getEmail());

    responseObserver.onNext(BillingResponse.newBuilder()
        .setAccountId(account.getId().toString())
        .setStatus(account.getStatus().name())
        .build());
    responseObserver.onCompleted();
  }
}
