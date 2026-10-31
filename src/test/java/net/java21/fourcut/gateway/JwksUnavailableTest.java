package net.java21.fourcut.gateway;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class JwksUnavailableTest {

  @Autowired private WebTestClient client;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    // 아무도 듣지 않는 주소: JWKS도, 하위 서비스도 받을 수 없다
    registry.add("SERVICES_AUTH_URL", () -> "http://127.0.0.1:1");
    registry.add("SERVICES_CONVERSATION_URL", () -> "http://127.0.0.1:1");
    registry.add("SERVICES_CONTENT_URL", () -> "http://127.0.0.1:1");
  }

  @Test
  @DisplayName("JWKS를 받아올 수 없으면 통과시키지 않고 503 SERVICE_UNAVAILABLE (fail-closed)")
  void jwksUnavailableIsServiceUnavailable() {
    String token = new TestJwts("k1").token("u", List.of(), Instant.now().plusSeconds(60));
    client
        .get()
        .uri("/api/v1/me")
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus()
        .isEqualTo(503)
        .expectBody()
        .jsonPath("$.error.code")
        .isEqualTo("SERVICE_UNAVAILABLE");
  }

  @Test
  @DisplayName("하위 서비스에 연결할 수 없으면 503 SERVICE_UNAVAILABLE")
  void upstreamDownIsServiceUnavailable() {
    client
        .post()
        .uri("/auth/token")
        .exchange()
        .expectStatus()
        .isEqualTo(503)
        .expectBody()
        .jsonPath("$.error.code")
        .isEqualTo("SERVICE_UNAVAILABLE");
  }
}
