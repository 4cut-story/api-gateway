package net.java21.fourcut.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** 4cut 백엔드 입구. JWT 검증, 라우팅, 사용자 헤더 주입, GPU 워커 토큰 확인. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class GatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }
}
