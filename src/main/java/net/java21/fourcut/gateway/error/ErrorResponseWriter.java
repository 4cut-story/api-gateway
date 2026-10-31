package net.java21.fourcut.gateway.error;

import java.nio.charset.StandardCharsets;
import net.java21.fourcut.gateway.web.RequestHeaderWebFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** 공통 오류 형식 {@code {error: {code, message, traceId}}}으로 응답을 쓴다. */
public final class ErrorResponseWriter {

  private ErrorResponseWriter() {}

  public static Mono<Void> write(ServerWebExchange exchange, ErrorCode code) {
    ServerHttpResponse response = exchange.getResponse();
    if (response.isCommitted()) {
      return Mono.empty();
    }
    String traceId = RequestHeaderWebFilter.traceId(exchange);
    response.setStatusCode(code.status());
    HttpHeaders headers = response.getHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (code.status() == HttpStatus.UNAUTHORIZED) {
      headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    }
    String body =
        "{\"error\":{\"code\":\""
            + code.name()
            + "\",\"message\":\""
            + code.message()
            + "\",\"traceId\":\""
            + traceId
            + "\"}}";
    DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
    return response.writeWith(Mono.just(buffer));
  }
}
