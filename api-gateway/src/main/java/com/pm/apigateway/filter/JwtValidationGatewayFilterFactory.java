package com.pm.apigateway.filter;

import com.pm.apigateway.exception.ApiErrorWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class JwtValidationGatewayFilterFactory extends
    AbstractGatewayFilterFactory<Object> {

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
          .toBodilessEntity()
          .map(response -> true)
          .onErrorResume(WebClientResponseException.Unauthorized.class,
              ex -> errorWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                  "Invalid or expired token").thenReturn(false))
          .onErrorResume(
              ex -> ex instanceof WebClientResponseException
                  || ex instanceof WebClientRequestException,
              ex -> errorWriter.write(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                  "Authentication service unavailable").thenReturn(false))
          .flatMap(valid -> valid ? chain.filter(exchange) : Mono.empty());
    };
  }
}
