package com.pm.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Removes any identity headers sent by the client on EVERY route, so the only
 * X-User-* headers a downstream service ever sees are the ones the gateway
 * sets after validating the token.
 */
@Component
public class IdentityHeaderStripFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest().mutate()
        .headers(headers -> {
          headers.remove(GatewayHeaders.USER_ID);
          headers.remove(GatewayHeaders.USER_EMAIL);
          headers.remove(GatewayHeaders.USER_ROLE);
        })
        .build();

    return chain.filter(exchange.mutate().request(request).build());
  }

  @Override
  public int getOrder() {
    return -100;
  }
}
