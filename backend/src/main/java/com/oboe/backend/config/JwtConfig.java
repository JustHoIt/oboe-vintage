package com.oboe.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
@Slf4j
public class JwtConfig {

  @Value("${jwt.secret}")
  private String secret;
  private long accessTokenExpiration = 86400000; // 24시간 (밀리초)
  private long refreshTokenExpiration = 604800000; // 7일 (밀리초)
  private String tokenPrefix = "Bearer ";
  private String headerName = "Authorization";

  @PostConstruct
  private void validateConfiguration() {
    if (secret == null || secret.isEmpty()) {
      throw new IllegalStateException("JWT Secret이 설정되지 않았습니다.");
    }
    if (secret.length() < 32) {
      throw new IllegalStateException("JWT Secret은 최소 32자 이상이어야 합니다.");
    }
    log.info("JWT 설정 검증 완료");
  }
}
