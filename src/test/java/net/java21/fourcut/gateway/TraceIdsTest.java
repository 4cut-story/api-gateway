package net.java21.fourcut.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TraceIdsTest {

  @Test
  @DisplayName("32자리 소문자 16진수만 그대로 쓰고, 모두 0이거나 형식이 틀리면 새로 만든다")
  void normalize() {
    String valid = "4bf92f3577b34da6a3ce929d0e0e4736";
    assertThat(TraceIds.normalize(valid)).isEqualTo(valid);
    assertThat(TraceIds.normalize("00000000000000000000000000000000"))
        .matches("[0-9a-f]{32}")
        .isNotEqualTo("00000000000000000000000000000000");
    assertThat(TraceIds.normalize("4BF92F3577B34DA6A3CE929D0E0E4736")).matches("[0-9a-f]{32}");
    assertThat(TraceIds.normalize(null)).matches("[0-9a-f]{32}");
  }
}
