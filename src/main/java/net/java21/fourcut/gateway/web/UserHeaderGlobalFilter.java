package net.java21.fourcut.gateway.web;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** 하위 서비스로 보내기 직전에 검증된 사용자 정보를 헤더로 붙인다. JWT는 게이트웨이까지만 가므로 {@code Authorization}은 지운다. */
@Component
public class UserHeaderGlobalFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return exchange
        .getPrincipal()
        .filter(JwtAuthenticationToken.class::isInstance)
        .map(JwtAuthenticationToken.class::cast)
        .map(token -> withUser(exchange.getRequest(), token.getToken()))
        .defaultIfEmpty(withoutUser(exchange.getRequest()))
        .flatMap(request -> chain.filter(exchange.mutate().request(request).build()));
  }

  private static ServerHttpRequest withUser(ServerHttpRequest request, Jwt jwt) {
    return request
        .mutate()
        .headers(
            headers -> {
              headers.remove(HttpHeaders.AUTHORIZATION);
              headers.set(HeaderNames.USER_ID, jwt.getSubject());
              headers.set(HeaderNames.USER_ROLES, roles(jwt));
            })
        .build();
  }

  private static ServerHttpRequest withoutUser(ServerHttpRequest request) {
    return request.mutate().headers(headers -> headers.remove(HttpHeaders.AUTHORIZATION)).build();
  }

  static String roles(Jwt jwt) {
    Object claim = jwt.getClaims().get("roles");
    Collection<?> values = claim instanceof Collection<?> c ? c : List.of();
    return values.stream().map(String::valueOf).collect(Collectors.joining(","));
  }

  @Override
  public int getOrder() {
    return HIGHEST_PRECEDENCE;
  }
}
