package com.pm.patientservice.grpc;

import com.pm.patientservice.repository.PatientRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import patient.grpc.GetPatientRequest;
import patient.grpc.GetPatientResponse;
import patient.grpc.PatientServiceGrpc;

/**
 * gRPC lookup used by other services (e.g. appointment-service) to check that
 * a patient exists. Listens on grpc.server.port (9002), internal network only.
 */
@GrpcService
public class PatientGrpcService extends PatientServiceGrpc.PatientServiceImplBase {

  private static final Logger log = LoggerFactory.getLogger(
      PatientGrpcService.class);

  private final PatientRepository patientRepository;

  public PatientGrpcService(PatientRepository patientRepository) {
    this.patientRepository = patientRepository;
  }

  /**
   * Runs the real lookup once after startup so the first caller does not pay for
   * cold Hibernate/protobuf classes (which exceeds the callers' short deadlines).
   */
  @EventListener(ApplicationReadyEvent.class)
  void warmUp() {
    try {
      getPatient(GetPatientRequest.newBuilder()
          .setPatientId(new UUID(0L, 0L).toString()).build(), new StreamObserver<>() {
            @Override
            public void onNext(GetPatientResponse value) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
            }
          });
    } catch (RuntimeException e) {
      log.warn("GetPatient warm-up failed: {}", e.toString());
    }
  }

  @Override
  public void getPatient(GetPatientRequest request,
      StreamObserver<GetPatientResponse> responseObserver) {

    UUID id;
    try {
      id = UUID.fromString(request.getPatientId());
    } catch (IllegalArgumentException e) {
      responseObserver.onError(Status.INVALID_ARGUMENT
          .withDescription("patientId must be a valid UUID").asRuntimeException());
      return;
    }

    GetPatientResponse response = patientRepository.findById(id)
        .map(patient -> GetPatientResponse.newBuilder()
            .setExists(true)
            .setPatientId(patient.getId().toString())
            .setName(patient.getName())
            .setEmail(patient.getEmail())
            .build())
        .orElseGet(() -> GetPatientResponse.newBuilder()
            .setExists(false)
            .setPatientId(request.getPatientId())
            .build());

    log.info("GetPatient patientId={} exists={}", request.getPatientId(),
        response.getExists());
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
