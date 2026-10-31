# api-gateway

백엔드 입구. JWT 검증(`jwk-set-uri`), 라우팅, 외부 `X-User-Id`·`X-User-Roles` 제거 후 주입, traceId 생성. Spring Cloud Gateway.

- 기술: Java 21, Spring Boot 4, Spring Cloud Gateway, Maven Wrapper, `common/parent-pom`. 테스트는 `@WebFluxTest`.
- 설계와 스펙: [docs](https://github.com/4cut-story/docs)
