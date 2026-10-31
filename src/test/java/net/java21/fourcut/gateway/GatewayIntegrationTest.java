package net.java21.fourcut.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
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
class GatewayIntegrationTest {

  private static final String USER = "0193a1c2-7e5b-7c3d-9a2f-5b8e4d1c6a70";
  private static final String WORKER_TOKEN = "worker-secret-token";
  private static final String NEXT_WORKER_TOKEN = "next-worker-token";

  private static final FakeBackend BACKEND;
  private static final TestJwts JWTS = new TestJwts("k1");

  static {
    try {
      BACKEND = new FakeBackend();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Autowired private WebTestClient client;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("SERVICES_AUTH_URL", BACKEND::url);
    registry.add("SERVICES_CONVERSATION_URL", BACKEND::url);
    registry.add("SERVICES_CONTENT_URL", BACKEND::url);
    registry.add("GPU_WORKER_TOKEN", () -> WORKER_TOKEN);
    registry.add("GPU_WORKER_TOKEN_NEXT", () -> NEXT_WORKER_TOKEN);
  }

  @AfterAll
  static void stop() {
    BACKEND.close();
  }

  @BeforeEach
  void reset() {
    BACKEND.reset();
    BACKEND.jwksStatus(200);
    BACKEND.jwks(JWTS.jwks());
  }

  private String validToken() {
    return JWTS.token(USER, List.of("ROLE_MEMBER", "ROLE_ADMIN"), Instant.now().plusSeconds(900));
  }

  @Test
  @DisplayName("JWT가 맞으면 하위 서비스로 보내고 검증된 사용자 헤더를 붙인다")
  void forwardsWithVerifiedUserHeaders() {
    client
        .get()
        .uri("/api/v1/feed")
        .header("Authorization", "Bearer " + validToken())
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .exists("X-Trace-Id");

    assertThat(BACKEND.lastPath()).isEqualTo("/api/v1/feed");
    assertThat(BACKEND.lastHeaders())
        .containsEntry("x-user-id", USER)
        .containsEntry("x-user-roles", "ROLE_MEMBER,ROLE_ADMIN")
        .doesNotContainKey("authorization");
  }

  @Test
  @DisplayName("외부에서 넣은 X-User-Id·X-User-Roles는 지우고 검증된 값으로 다시 붙인다 (보안 필수)")
  void stripsForgedUserHeaders() {
    client
        .get()
        .uri("/api/v1/conversations/abc")
        .header("Authorization", "Bearer " + validToken())
        .header("X-User-Id", "attacker")
        .header("x-user-roles", "ROLE_ADMIN")
        .exchange()
        .expectStatus()
        .isOk();

    assertThat(BACKEND.lastHeaders()).containsEntry("x-user-id", USER);
    assertThat(BACKEND.lastHeaders().get("x-user-roles")).isEqualTo("ROLE_MEMBER,ROLE_ADMIN");
  }

  @Test
  @DisplayName("인증이 없는 /auth 경로에서도 외부 사용자 헤더를 지운다")
  void stripsForgedHeadersOnPublicPath() {
    client
        .post()
        .uri("/auth/token")
        .header("X-User-Id", "attacker")
        .header("X-User-Roles", "ROLE_ADMIN")
        .exchange()
        .expectStatus()
        .isOk();

    assertThat(BACKEND.lastHeaders()).doesNotContainKeys("x-user-id", "x-user-roles");
  }

  @Test
  @DisplayName("토큰이 없으면 401 UNAUTHORIZED, 공통 오류 형식과 WWW-Authenticate")
  void missingTokenIsUnauthorized() {
    client
        .get()
        .uri("/api/v1/me")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .valueEquals("WWW-Authenticate", "Bearer")
        .expectBody()
        .jsonPath("$.error.code")
        .isEqualTo("UNAUTHORIZED")
        .jsonPath("$.error.traceId")
        .isNotEmpty();
    assertThat(BACKEND.lastPath()).isNull();
  }

  @Test
  @DisplayName("위조된 토큰은 401 UNAUTHORIZED")
  void forgedTokenIsUnauthorized() {
    String forged =
        new TestJwts("k1").token(USER, List.of("ROLE_ADMIN"), Instant.now().plusSeconds(60));
    client
        .get()
        .uri("/api/v1/me")
        .header("Authorization", "Bearer " + forged)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody()
        .jsonPath("$.error.code")
        .isEqualTo("UNAUTHORIZED");
  }

  @Test
  @DisplayName("만료된 토큰은 401 TOKEN_EXPIRED")
  void expiredTokenIsTokenExpired() {
    String expired = JWTS.token(USER, List.of("ROLE_MEMBER"), Instant.now().minusSeconds(600));
    client
        .get()
        .uri("/api/v1/me")
        .header("Authorization", "Bearer " + expired)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody()
        .jsonPath("$.error.code")
        .isEqualTo("TOKEN_EXPIRED");
  }

  @Test
  @DisplayName("올바른 추적 ID는 그대로, 틀린 형식은 새로 만들어 하위 서비스와 응답에 붙인다")
  void traceIdIsKeptOrReplaced() {
    String trace = "4bf92f3577b34da6a3ce929d0e0e4736";
    client
        .get()
        .uri("/api/v1/feed")
        .header("Authorization", "Bearer " + validToken())
        .header("X-Trace-Id", trace)
        .exchange()
        .expectHeader()
        .valueEquals("X-Trace-Id", trace);
    assertThat(BACKEND.lastHeaders()).containsEntry("x-trace-id", trace);

    client
        .get()
        .uri("/api/v1/feed")
        .header("Authorization", "Bearer " + validToken())
        .header("X-Trace-Id", "not-a-trace")
        .exchange()
        .expectHeader()
        .valueMatches("X-Trace-Id", "^[0-9a-f]{32}$");
    assertThat(BACKEND.lastHeaders().get("x-trace-id")).matches("^[0-9a-f]{32}$");
  }

  @Test
  @DisplayName("GPU 워커 경로는 워커 토큰과 X-Worker-Id가 맞을 때만 conversation-service로 보낸다")
  void workerPathAcceptsWorkerToken() {
    client
        .post()
        .uri("/internal/gpu-jobs/claim")
        .header("Authorization", "Bearer " + WORKER_TOKEN)
        .header("X-Worker-Id", "gpu-pc-1")
        .exchange()
        .expectStatus()
        .isOk();
    assertThat(BACKEND.lastPath()).isEqualTo("/internal/gpu-jobs/claim");
    assertThat(BACKEND.lastHeaders())
        .containsEntry("x-worker-id", "gpu-pc-1")
        .doesNotContainKey("authorization");

    client
        .post()
        .uri("/internal/gpu-jobs/claim")
        .header("Authorization", "Bearer " + NEXT_WORKER_TOKEN)
        .header("X-Worker-Id", "gpu-pc-1")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  @DisplayName("GPU 워커 경로에 사용자 JWT·틀린 토큰·워커 ID 없음은 401")
  void workerPathRejectsOthers() {
    client
        .post()
        .uri("/internal/gpu-jobs/claim")
        .header("Authorization", "Bearer " + validToken())
        .header("X-Worker-Id", "gpu-pc-1")
        .exchange()
        .expectStatus()
        .isUnauthorized();
    client
        .post()
        .uri("/internal/gpu-jobs/claim")
        .header("Authorization", "Bearer " + WORKER_TOKEN)
        .exchange()
        .expectStatus()
        .isUnauthorized();
    client
        .post()
        .uri("/internal/gpu-jobs/claim")
        .header("X-Worker-Id", "gpu-pc-1")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody()
        .jsonPath("$.error.code")
        .isEqualTo("UNAUTHORIZED");
    assertThat(BACKEND.lastPath()).isNull();
  }

  @Test
  @DisplayName("그 밖의 /internal 경로와 정의되지 않은 경로는 404 NOT_FOUND")
  void otherInternalPathsAreNotFound() {
    client
        .post()
        .uri("/internal/comics")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.error.code")
        .isEqualTo("NOT_FOUND");
    client.get().uri("/nothing").exchange().expectStatus().isNotFound();
    assertThat(BACKEND.lastPath()).isNull();
  }

  @Test
  @DisplayName("경로 접두어마다 정해진 서비스로 경로를 바꾸지 않고 보낸다")
  void routesByPrefix() {
    for (String path :
        List.of(
            "/api/v1/users/x",
            "/api/v1/comics",
            "/api/v1/posts/p1/comments",
            "/api/v1/follows/u1",
            "/api/v1/comments/c1")) {
      client
          .get()
          .uri(path)
          .header("Authorization", "Bearer " + validToken())
          .exchange()
          .expectStatus()
          .isOk();
      assertThat(BACKEND.lastPath()).isEqualTo(path);
    }
  }
}
