package com.pm.apigateway.filter;

import com.pm.apigateway.dto.ClaimsResponse;
import com.pm.apigateway.exception.ApiErrorWriter;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Validates the Bearer token with auth-service and forwards the verified
 * identity to the downstream service as X-User-* headers.
 */
@Component
public class JwtValidationGatewayFilterFactory extends
    AbstractGatewayFilterFactory<Object> {

  private static final Duration VALIDATE_TIMEOUT = Duration.ofSeconds(3);

  private final WebClient webClient;
  private final ApiErrorWriter errorWriter;

  public JwtValidationGatewayFilterFactory(WebClient.Builder webClientBuilder,
      @Value("${auth.service.url}") String authServiceUrl,
      ApiErrorWriter errorWriter) {
    this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
    this.errorWriter = errorWriter;
  }

  @Override
  public GatewayFilter apply(Object config) {
    return (exchange, chain) -> {
      String token =
          exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

      if (token == null || !token.startsWith("Bearer ")) {
        return errorWriter.write(exchange, HttpStatus.UNAUTHORIZED,
            "Missing or malformed Authorization header");
      }

      return webClient.get()
          .uri("/validate")
          .header(HttpHeaders.AUTHORIZATION, token)
          .retrieve()
          .bodyToMono(ClaimsResponse.class)
          .timeout(VALIDATE_TIMEOUT)
          .map(Optional::of)
          .switchIfEmpty(Mono.defer(() -> reject(exchange, HttpStatus.UNAUTHORIZED,
              "Invalid or expired token")))
          .onErrorResume(WebClientResponseException.Unauthorized.class,
              ex -> reject(exchange, HttpStatus.UNAUTHORIZED,
                  "Invalid or expired token"))
          .onErrorResume(
              ex -> ex instanceof WebClientResponseException
                  || ex instanceof WebClientRequestException
                  || ex instanceof TimeoutException,
              ex -> reject(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                  "Authentication service unavailable"))
          .flatMap(claims -> claims
              .map(c -> chain.filter(withIdentity(exchange, c)))
              .orElse(Mono.empty()));
    };
  }

  private Mono<Optional<ClaimsResponse>> reject(ServerWebExchange exchange,
      HttpStatus status, String message) {
    return errorWriter.write(exchange, status, message)
        .thenReturn(Optional.<ClaimsResponse>empty());
  }

  private ServerWebExchange withIdentity(ServerWebExchange exchange,
      ClaimsResponse claims) {
    ServerHttpRequest request = exchange.getRequest().mutate()
        .headers(headers -> {
          headers.set(GatewayHeaders.USER_ID, claims.userId());
          headers.set(GatewayHeaders.USER_EMAIL, claims.email());
          headers.set(GatewayHeaders.USER_ROLE, claims.role());
        })
        .build();
    return exchange.mutate().request(request).build();
  }
}
