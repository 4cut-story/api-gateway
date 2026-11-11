# api-gateway

**백엔드의 입구. JWT를 확인하고 요청을 각 서비스로 나눠 보내요.**

[← 4cut story 소개](https://github.com/4cut-story) · [데모](https://4cut.java21.net) · [아키텍처](https://github.com/4cut-story/.github/blob/main/docs/architecture.md)

## 하는 일

- auth-service가 공개한 공개키(JWKS)로 JWT를 직접 확인해요. 매 요청마다 auth-service를 부르지 않아요.
- 밖에서 들어온 `X-User-Id`·`X-User-Roles` 헤더는 지우고, 확인한 토큰의 값으로 다시 넣어요.
- 요청마다 traceId를 만들어 붙여요.
- GPU PC 워커가 일을 가져가는 `/internal/gpu-jobs/**` 경로는 JWT 대신 워커 토큰으로 확인해요. 토큰을 바꾸는 동안에는 새 토큰과 옛 토큰을 함께 받아요.

## 시스템 안에서의 위치

```
web ─────────┐                 ┌─> auth-service          /auth/**, /api/v1/me, /api/v1/users
             ├─> api-gateway ──┼─> conversation-service  /api/v1/conversations, characters, sounds, rss-feeds
GPU PC 워커 ─┘                 └─> content-service       /api/v1/comics, posts, feed, follows, comments
```

클러스터 안에서만 열려 있고, 외부 요청은 web을 거쳐서 들어와요. GPU PC 워커만 HTTPS로 `/internal/gpu-jobs/**`에 닿아요.

## 기술

Java 21 · Spring Boot 4 · Spring Cloud Gateway (WebFlux) · Spring Security OAuth2 Resource Server

## 실행

```bash
SERVICES_AUTH_URL=http://localhost:8081 \
SERVICES_CONVERSATION_URL=http://localhost:8082 \
SERVICES_CONTENT_URL=http://localhost:8083 \
GPU_WORKER_TOKEN=<워커 토큰> \
./mvnw spring-boot:run
```

기본 포트는 8080이에요.

## 검사

```bash
./mvnw verify          # 테스트, 정적 분석, 커버리지 80%
./mvnw spotless:apply  # 코드 스타일 고치기
```

## 자세한 문서

| 문서 | 내용 |
| --- | --- |
| [아키텍처](https://github.com/4cut-story/.github/blob/main/docs/architecture.md) | 서비스별 역할, 통신 방식, 로그인과 GPU 작업 흐름 |
| [API 규칙](https://github.com/4cut-story/docs/blob/main/docs/api-conventions.md) | URL, 응답·오류 형식, 헤더, 서비스 간 호출 |
| [common](https://github.com/4cut-story/common) | OpenAPI 계약, 빌드와 CI 기준 |
