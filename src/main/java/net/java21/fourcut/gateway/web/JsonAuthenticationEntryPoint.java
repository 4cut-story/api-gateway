package net.java21.fourcut.gateway.web;

import net.java21.fourcut.gateway.error.ErrorCode;
import net.java21.fourcut.gateway.error.ErrorResponseWriter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** JWT가 없거나 무효면 401. 만료만 {@code TOKEN_EXPIRED}로 구분하고 다른 사유는 알려 주지 않는다. */
@Component
public class JsonAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

  @Override
  public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
    return ErrorResponseWriter.write(
        exchange, expired(ex) ? ErrorCode.TOKEN_EXPIRED : ErrorCode.UNAUTHORIZED);
  }

  static boolean expired(Throwable ex) {
    for (Throwable t = ex; t != null; t = t.getCause()) {
      String message = t.getMessage();
      if (message != null && message.contains("expired")) {
        return true;
      }
    }
    return false;
  }
}
