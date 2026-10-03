package com.pm.appointmentservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Looks doctors up in auth-service over its internal API (never routed by the gateway). */
@Service
public class DoctorClient {

  private final RestClient restClient;

  public DoctorClient(@Value("${auth.service.url}") String baseUrl,
      @Value("${auth.service.connect-timeout-ms:2000}") int connectTimeoutMs,
      @Value("${auth.service.read-timeout-ms:3000}") int readTimeoutMs) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(connectTimeoutMs);
    factory.setReadTimeout(readTimeoutMs);

    this.restClient = RestClient.builder().baseUrl(baseUrl)
        .requestFactory(factory).build();
  }

  /**
   * Empty when no such user exists. Connection problems and 5xx responses are
   * retried once and then trip the circuit breaker.
   */
  @Retry(name = "auth")
  @CircuitBreaker(name = "auth")
  public Optional<DoctorInfo> getDoctor(UUID userId) {
    try {
      return Optional.ofNullable(restClient.get()
          .uri("/internal/users/{id}", userId)
          .retrieve()
          .body(DoctorInfo.class));
    } catch (HttpClientErrorException.NotFound e) {
      return Optional.empty();
    } catch (RestClientException e) {
      throw new DownstreamUnavailableException("auth-service call failed", e);
    }
  }
}
