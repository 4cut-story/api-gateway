package net.java21.fourcut.gateway.web;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

/** W3C trace-id 형식(32자리 소문자 16진수) 추적 ID. */
public final class TraceIds {

  private static final Pattern VALID = Pattern.compile("^[0-9a-f]{32}$");
  private static final SecureRandom RANDOM = new SecureRandom();

  private TraceIds() {}

  /** 형식이 맞으면 그대로, 아니면 새로 만든다. 모두 0인 값은 W3C 규칙상 무효다. */
  public static String normalize(String candidate) {
    if (candidate != null
        && VALID.matcher(candidate).matches()
        && !"00000000000000000000000000000000".equals(candidate)) {
      return candidate;
    }
    return generate();
  }

  public static String generate() {
    byte[] bytes = new byte[16];
    RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }
}
