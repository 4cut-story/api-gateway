package net.java21.fourcut.gateway.config;

import net.java21.fourcut.gateway.GatewayProperties;
import net.java21.fourcut.gateway.web.JsonAuthenticationEntryPoint;
import net.java21.fourcut.gateway.worker.WorkerTokenWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

/**
 * 보안 필터 체인 세 개.
 *
 * <ol>
 *   <li>GPU 워커 경로: 워커 토큰만 확인 (사용자 JWT 불가)
 *   <li>로그인·상태 확인 경로: 인증 없음
 *   <li>나머지: {@code /api/**}는 JWT 필수 (JWKS 로컬 검증)
 * </ol>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  @Order(1)
  SecurityWebFilterChain workerChain(ServerHttpSecurity http, GatewayProperties properties) {
    return http.securityMatcher(ServerWebExchangeMatchers.pathMatchers("/internal/gpu-jobs/**"))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .requestCache(ServerHttpSecurity.RequestCacheSpec::disable)
        .addFilterAt(new WorkerTokenWebFilter(properties), SecurityWebFiltersOrder.AUTHENTICATION)
        .authorizeExchange(auth -> auth.anyExchange().permitAll())
        .build();
  }

  @Bean
  @Order(2)
  SecurityWebFilterChain publicChain(ServerHttpSecurity http) {
    return http.securityMatcher(ServerWebExchangeMatchers.pathMatchers("/auth/**", "/actuator/**"))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .requestCache(ServerHttpSecurity.RequestCacheSpec::disable)
        .authorizeExchange(auth -> auth.anyExchange().permitAll())
        .build();
  }

  @Bean
  @Order(3)
  SecurityWebFilterChain apiChain(
      ServerHttpSecurity http, JsonAuthenticationEntryPoint entryPoint) {
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .requestCache(ServerHttpSecurity.RequestCacheSpec::disable)
        .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
        .authorizeExchange(
            auth -> auth.pathMatchers("/api/**").authenticated().anyExchange().permitAll())
        .oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(entryPoint).jwt(jwt -> {}))
        .build();
  }
}
