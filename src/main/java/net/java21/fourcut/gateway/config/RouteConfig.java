package net.java21.fourcut.gateway.config;

import net.java21.fourcut.gateway.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 경로 접두어 하나는 서비스 하나로만 보낸다 (docs architecture 6절). 경로는 바꾸지 않고 그대로 넘긴다. 표에 없는 경로(그 밖의 {@code
 * /internal/**} 포함)는 404.
 */
@Configuration
public class RouteConfig {

  @Bean
  RouteLocator routes(RouteLocatorBuilder builder, GatewayProperties properties) {
    return builder
        .routes()
        .route(
            "auth-service",
            r ->
                r.path(
                        "/auth/**",
                        "/api/v1/me",
                        "/api/v1/me/**",
                        "/api/v1/users",
                        "/api/v1/users/**")
                    .uri(properties.authUrl()))
        .route(
            "conversation-service",
            r ->
                r.path("/api/v1/conversations", "/api/v1/conversations/**", "/internal/gpu-jobs/**")
                    .uri(properties.conversationUrl()))
        .route(
            "content-service",
            r ->
                r.path(
                        "/api/v1/comics",
                        "/api/v1/comics/**",
                        "/api/v1/posts",
                        "/api/v1/posts/**",
                        "/api/v1/feed",
                        "/api/v1/feed/**",
                        "/api/v1/follows/**",
                        "/api/v1/comments/**")
                    .uri(properties.contentUrl()))
        .build();
  }
}
