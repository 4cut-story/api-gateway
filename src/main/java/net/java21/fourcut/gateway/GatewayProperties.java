package net.java21.fourcut.gateway;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 게이트웨이 설정. 값은 환경 변수(local .env, prod ConfigMap·Secret)에서 넣는다.
 *
 * @param authUrl auth-service 주소 ({@code SERVICES_AUTH_URL})
 * @param conversationUrl conversation-service 주소 ({@code SERVICES_CONVERSATION_URL})
 * @param contentUrl content-service 주소 ({@code SERVICES_CONTENT_URL})
 * @param workerTokens GPU 워커 토큰 (현재, 다음). 비어 있는 값은 무시한다
 */
@ConfigurationProperties("gateway")
public record GatewayProperties(
    URI authUrl, URI conversationUrl, URI contentUrl, List<String> workerTokens) {

  public GatewayProperties {
    Objects.requireNonNull(authUrl, "gateway.auth-url");
    Objects.requireNonNull(conversationUrl, "gateway.conversation-url");
    Objects.requireNonNull(contentUrl, "gateway.content-url");
    workerTokens =
        workerTokens == null
            ? List.of()
            : workerTokens.stream().filter(t -> t != null && !t.isBlank()).toList();
  }
}
