package net.java21.fourcut.gateway;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/** 테스트용 RS256 서명 키와 토큰. */
final class TestJwts {

  private final RSAKey key;

  TestJwts(String keyId) {
    try {
      key = new RSAKeyGenerator(2048).keyID(keyId).generate();
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
  }

  String jwks() {
    return new JWKSet(key.toPublicJWK()).toString();
  }

  String token(String subject, List<String> roles, Instant expiresAt) {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(subject)
            .claim("roles", roles)
            .issueTime(Date.from(expiresAt.minusSeconds(900)))
            .expirationTime(Date.from(expiresAt))
            .build();
    SignedJWT jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
    try {
      jwt.sign(new RSASSASigner(key));
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
    return jwt.serialize();
  }
}
