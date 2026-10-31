package net.java21.fourcut.gateway.error;

import org.springframework.http.HttpStatus;

/** 게이트웨이가 직접 내는 오류 코드 (docs api-conventions 5절). */
public enum ErrorCode {
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요해요."),
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "로그인이 만료됐어요. 다시 시도해 주세요."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "이 기능을 쓸 권한이 없어요."),
  NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없어요."),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식이에요."),
  PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "요청이 너무 커요."),
  UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 형식이에요."),
  MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않아요."),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류가 났어요. 잠시 뒤 다시 시도해 주세요."),
  UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "서버 응답에 문제가 있어요. 잠시 뒤 다시 시도해 주세요."),
  SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서비스를 잠시 쓸 수 없어요. 잠시 뒤 다시 시도해 주세요."),
  UPSTREAM_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "응답이 늦어지고 있어요. 잠시 뒤 다시 시도해 주세요.");

  private final HttpStatus status;
  private final String message;

  ErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }

  public HttpStatus status() {
    return status;
  }

  public String message() {
    return message;
  }
}
