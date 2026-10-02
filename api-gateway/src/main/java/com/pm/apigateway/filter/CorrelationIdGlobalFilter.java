package com.pm.apigateway.filter;

import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reuses a well-formed incoming X-Request-Id or generates one, forwards it
 * downstream and returns it in the response.
 */
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(
      CorrelationIdGlobalFilter.class);
  private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._-]{1,100}");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String incoming = exchange.getRequest().getHeaders()
        .getFirst(GatewayHeaders.REQUEST_ID);
    String requestId = incoming != null && VALID_ID.matcher(incoming).matches()
        ? incoming : UUID.randomUUID().toString();

    ServerHttpRequest request = exchange.getRequest().mutate()
        .headers(headers -> headers.set(GatewayHeaders.REQUEST_ID, requestId))
        .build();
    exchange.getResponse().getHeaders().set(GatewayHeaders.REQUEST_ID, requestId);

    String method = request.getMethod().name();
    String path = request.getPath().value();

    return chain.filter(exchange.mutate().request(request).build())
        .doFinally(signal -> log.info("{} {} -> {} [requestId={}]", method, path,
            exchange.getResponse().getStatusCode(), requestId));
  }

  @Override
  public int getOrder() {
    return -200;
  }
}
