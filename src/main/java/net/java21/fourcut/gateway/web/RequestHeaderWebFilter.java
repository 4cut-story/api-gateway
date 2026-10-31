package net.java21.fourcut.gateway.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 모든 요청에서 가장 먼저 돈다. 외부에서 들어온 사용자 헤더를 무조건 지우고(위조 방지), 추적 ID를 정해 요청·응답에 붙인다. 인증 실패처럼 라우팅 전에 끝나는 요청도
 * 같은 추적 ID를 쓰도록 보안 필터보다 앞에 둔다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestHeaderWebFilter implements WebFilter {

  /** 요청 속성 이름. 오류 응답 본문에 같은 추적 ID를 넣을 때 쓴다. */
  public static final String TRACE_ID_ATTRIBUTE =
      RequestHeaderWebFilter.class.getName() + ".traceId";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String traceId =
        TraceIds.normalize(exchange.getRequest().getHeaders().getFirst(HeaderNames.TRACE_ID));
    ServerHttpRequest request =
        exchange
            .getRequest()
            .mutate()
            .headers(
                headers -> {
                  headers.remove(HeaderNames.USER_ID);
                  headers.remove(HeaderNames.USER_ROLES);
                  headers.set(HeaderNames.TRACE_ID, traceId);
                })
            .build();
    exchange.getAttributes().put(TRACE_ID_ATTRIBUTE, traceId);
    exchange.getResponse().getHeaders().set(HeaderNames.TRACE_ID, traceId);
    return chain.filter(exchange.mutate().request(request).build());
  }

  /** 요청에 정해 둔 추적 ID. 필터를 거치지 않은 경우에는 새로 만든다. */
  public static String traceId(ServerWebExchange exchange) {
    Object value = exchange.getAttribute(TRACE_ID_ATTRIBUTE);
    return value instanceof String s ? s : TraceIds.generate();
  }
}
