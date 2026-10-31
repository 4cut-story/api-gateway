package net.java21.fourcut.gateway.worker;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import net.java21.fourcut.gateway.GatewayProperties;
import net.java21.fourcut.gateway.error.ErrorCode;
import net.java21.fourcut.gateway.error.ErrorResponseWriter;
import net.java21.fourcut.gateway.web.HeaderNames;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * GPU 워커 경로({@code /internal/gpu-jobs/**})만 지키는 필터. 사용자 JWT는 받지 않고 워커 공유 토큰만 받는다. 토큰 교체를 위해 두 개(현재,
 * 다음)를 함께 받고, 상수 시간으로 비교한다. {@code X-Worker-Id}도 있어야 한다.
 */
public class WorkerTokenWebFilter implements WebFilter {

  private static final String BEARER = "Bearer ";

  private final List<byte[]> tokens;

  public WorkerTokenWebFilter(GatewayProperties properties) {
    this.tokens =
        properties.workerTokens().stream().map(t -> t.getBytes(StandardCharsets.UTF_8)).toList();
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    HttpHeaders headers = exchange.getRequest().getHeaders();
    String workerId = headers.getFirst(HeaderNames.WORKER_ID);
    if (workerId == null
        || workerId.isBlank()
        || !matches(headers.getFirst(HttpHeaders.AUTHORIZATION))) {
      return ErrorResponseWriter.write(exchange, ErrorCode.UNAUTHORIZED);
    }
    return chain.filter(exchange);
  }

  boolean matches(String authorization) {
    if (authorization == null || !authorization.startsWith(BEARER)) {
      return false;
    }
    byte[] presented = authorization.substring(BEARER.length()).getBytes(StandardCharsets.UTF_8);
    boolean ok = false;
    for (byte[] token : tokens) {
      ok |= MessageDigest.isEqual(token, presented);
    }
    return ok;
  }
}
