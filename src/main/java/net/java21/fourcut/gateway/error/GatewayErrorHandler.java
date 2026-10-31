package net.java21.fourcut.gateway.error;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 게이트웨이가 직접 내는 모든 오류를 공통 형식으로 바꾼다. Spring 기본 오류 본문(timestamp, path…)을 내보내지 않는다. 기본 핸들러(-1)보다 먼저 돈다.
 */
@Component
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    ErrorCode code = classify(ex);
    if (code.status().is5xxServerError()) {
      log.warn(
          "gateway error {} {} traceId={}: {}",
          code,
          exchange.getRequest().getPath(),
          exchange.getResponse().getHeaders().getFirst("X-Trace-Id"),
          ex.toString());
    }
    return ErrorResponseWriter.write(exchange, code);
  }

  static ErrorCode classify(Throwable ex) {
    if (ex instanceof AuthenticationServiceException) {
      // JWKS를 받아올 수 없으면 통과시키지 않는다 (fail-closed)
      return ErrorCode.SERVICE_UNAVAILABLE;
    }
    if (ex instanceof ResponseStatusException rse) {
      return fromStatus(HttpStatus.resolve(rse.getStatusCode().value()));
    }
    if (hasCause(ex, TimeoutException.class)) {
      return ErrorCode.UPSTREAM_TIMEOUT;
    }
    if (hasCause(ex, ConnectException.class) || hasCause(ex, UnknownHostException.class)) {
      return ErrorCode.SERVICE_UNAVAILABLE;
    }
    return ErrorCode.INTERNAL_ERROR;
  }

  private static ErrorCode fromStatus(HttpStatus status) {
    if (status == null) {
      return ErrorCode.INTERNAL_ERROR;
    }
    return switch (status) {
      case BAD_REQUEST -> ErrorCode.MALFORMED_REQUEST;
      case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
      case FORBIDDEN -> ErrorCode.FORBIDDEN;
      case NOT_FOUND -> ErrorCode.NOT_FOUND;
      case METHOD_NOT_ALLOWED -> ErrorCode.METHOD_NOT_ALLOWED;
      case PAYLOAD_TOO_LARGE, CONTENT_TOO_LARGE -> ErrorCode.PAYLOAD_TOO_LARGE;
      case UNSUPPORTED_MEDIA_TYPE -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
      case BAD_GATEWAY -> ErrorCode.UPSTREAM_ERROR;
      case SERVICE_UNAVAILABLE -> ErrorCode.SERVICE_UNAVAILABLE;
      case GATEWAY_TIMEOUT -> ErrorCode.UPSTREAM_TIMEOUT;
      default -> ErrorCode.INTERNAL_ERROR;
    };
  }

  private static boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
    for (Throwable t = ex; t != null; t = t.getCause()) {
      if (type.isInstance(t)) {
        return true;
      }
    }
    return false;
  }
}
