package net.java21.fourcut.gateway.config;

import net.java21.fourcut.gateway.GatewayProperties;
import net.java21.fourcut.gateway.web.JsonAuthenticationEntryPoint;
import net.java21.fourcut.gateway.worker.WorkerTokenWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
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
 *   <li>나머지: {@code /api/**}는 JWT 필수 (JWKS 로컬 검증). 단 공개 만화 보기용 GET({@link #ANONYMOUS_GET})은 토큰 없이도
 *       통과한다. 토큰이 있으면 검증하고 사용자 헤더를 붙인다.
 * </ol>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  /**
   * 로그인 없이 볼 수 있는 경로 (첫 화면 포털, 사용자 페이지 {@code /@닉네임}, 게시물 상세). 공개 범위는 하위 서비스가 보는 사람 기준으로 적용한다 (사용자
   * 헤더가 없으면 전체 공개만).
   */
  static final String[] ANONYMOUS_GET = {
    "/api/v1/feed",
    "/api/v1/posts",
    "/api/v1/posts/*",
    "/api/v1/posts/*/image",
    "/api/v1/posts/*/comments",
    "/api/v1/follows/*/stats",
    "/api/v1/users",
    "/api/v1/users/*"
  };

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
            auth ->
                auth.pathMatchers(HttpMethod.GET, ANONYMOUS_GET)
                    .permitAll()
                    .pathMatchers("/api/**")
                    .authenticated()
                    .anyExchange()
                    .permitAll())
        .oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(entryPoint).jwt(jwt -> {}))
        .build();
  }
}
