package net.java21.fourcut.gateway.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.server.ResponseStatusException;

class GatewayErrorHandlerTest {

  @Test
  @DisplayName("예외를 공통 오류 코드로 바꾼다")
  void classify() {
    assertThat(GatewayErrorHandler.classify(new AuthenticationServiceException("jwks")))
        .isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
    assertThat(GatewayErrorHandler.classify(new RuntimeException(new TimeoutException())))
        .isEqualTo(ErrorCode.UPSTREAM_TIMEOUT);
    assertThat(GatewayErrorHandler.classify(new RuntimeException(new ConnectException())))
        .isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
    assertThat(GatewayErrorHandler.classify(new IllegalStateException()))
        .isEqualTo(ErrorCode.INTERNAL_ERROR);
    assertThat(status(HttpStatus.BAD_REQUEST)).isEqualTo(ErrorCode.MALFORMED_REQUEST);
    assertThat(status(HttpStatus.UNAUTHORIZED)).isEqualTo(ErrorCode.UNAUTHORIZED);
    assertThat(status(HttpStatus.FORBIDDEN)).isEqualTo(ErrorCode.FORBIDDEN);
    assertThat(status(HttpStatus.METHOD_NOT_ALLOWED)).isEqualTo(ErrorCode.METHOD_NOT_ALLOWED);
    assertThat(status(HttpStatus.CONTENT_TOO_LARGE)).isEqualTo(ErrorCode.PAYLOAD_TOO_LARGE);
    assertThat(status(HttpStatus.UNSUPPORTED_MEDIA_TYPE))
        .isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    assertThat(status(HttpStatus.BAD_GATEWAY)).isEqualTo(ErrorCode.UPSTREAM_ERROR);
    assertThat(status(HttpStatus.SERVICE_UNAVAILABLE)).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
    assertThat(status(HttpStatus.GATEWAY_TIMEOUT)).isEqualTo(ErrorCode.UPSTREAM_TIMEOUT);
    assertThat(status(HttpStatus.I_AM_A_TEAPOT)).isEqualTo(ErrorCode.INTERNAL_ERROR);
  }

  private static ErrorCode status(HttpStatus status) {
    return GatewayErrorHandler.classify(new ResponseStatusException(status));
  }
}
