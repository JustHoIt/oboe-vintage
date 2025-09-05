package com.oboe.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {

  @Value("${jwt.secret}")
  private String secret;
  private long accessTokenExpiration = 86400000; // 24시간 (밀리초)
  private long refreshTokenExpiration = 604800000; // 7일 (밀리초)
  private String tokenPrefix = "Bearer ";
  private String headerName = "Authorization";
}
