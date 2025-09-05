package com.oboe.backend.common.util;

import com.oboe.backend.config.JwtConfig;
import com.oboe.backend.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

  private final JwtConfig jwtConfig;

  private Key getSigningKey() {
    String secret = jwtConfig.getSecret();
    if (secret == null || secret.trim().isEmpty()) {
      throw new IllegalStateException("JWT secret이 설정되지 않았습니다.");
    }
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * Access Token 생성
   */
  public String generateAccessToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", user.getId());
    claims.put("email", user.getEmail());
    claims.put("role", user.getRole().name());
    claims.put("status", user.getStatus().name());
    claims.put("socialProvider", user.getSocialProvider().name());
    claims.put("tokenType", "ACCESS");

    return createToken(claims, user.getEmail(), jwtConfig.getAccessTokenExpiration());
  }

  /**
   * Refresh Token 생성
   */
  public String generateRefreshToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", user.getId());
    claims.put("email", user.getEmail());
    claims.put("tokenType", "REFRESH");

    return createToken(claims, user.getEmail(), jwtConfig.getRefreshTokenExpiration());
  }

  /**
   * JWT 토큰 생성
   */
  private String createToken(Map<String, Object> claims, String subject, long expiration) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expiration);

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(subject)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(getSigningKey(), SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * 토큰에서 사용자 이메일 추출
   */
  public String getEmailFromToken(String token) {
    return getClaimFromToken(token, Claims::getSubject);
  }

  /**
   * 토큰에서 사용자 ID 추출
   */
  public Long getUserIdFromToken(String token) {
    Claims claims = getAllClaimsFromToken(token);
    return claims.get("userId", Long.class);
  }

  /**
   * 토큰에서 사용자 역할 추출
   */
  public String getRoleFromToken(String token) {
    Claims claims = getAllClaimsFromToken(token);
    return claims.get("role", String.class);
  }

  /**
   * 토큰에서 토큰 타입 추출
   */
  public String getTokenTypeFromToken(String token) {
    Claims claims = getAllClaimsFromToken(token);
    return claims.get("tokenType", String.class);
  }

  /**
   * 토큰 만료일 추출
   */
  public Date getExpirationDateFromToken(String token) {
    return getClaimFromToken(token, Claims::getExpiration);
  }

  /**
   * 토큰이 만료되었는지 확인
   */
  public Boolean isTokenExpired(String token) {
    try {
      Date expiration = getExpirationDateFromToken(token);
      return expiration.before(new Date());
    } catch (Exception e) {
      log.warn("토큰 만료 확인 중 오류 발생: {}", e.getMessage());
      return true;
    }
  }

  /**
   * 토큰 유효성 검증
   */
  public Boolean validateToken(String token, String email) {
    try {
      String tokenEmail = getEmailFromToken(token);
      return (tokenEmail.equals(email) && !isTokenExpired(token));
    } catch (Exception e) {
      log.warn("토큰 유효성 검증 중 오류 발생: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Access Token인지 확인
   */
  public Boolean isAccessToken(String token) {
    try {
      String tokenType = getTokenTypeFromToken(token);
      return "ACCESS".equals(tokenType);
    } catch (Exception e) {
      log.warn("토큰 타입 확인 중 오류 발생: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Refresh Token인지 확인
   */
  public Boolean isRefreshToken(String token) {
    try {
      String tokenType = getTokenTypeFromToken(token);
      return "REFRESH".equals(tokenType);
    } catch (Exception e) {
      log.warn("토큰 타입 확인 중 오류 발생: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 토큰에서 특정 클레임 추출
   */
  private <T> T getClaimFromToken(String token,
      java.util.function.Function<Claims, T> claimsResolver) {
    Claims claims = getAllClaimsFromToken(token);
    return claimsResolver.apply(claims);
  }

  /**
   * 토큰에서 모든 클레임 추출
   */
  private Claims getAllClaimsFromToken(String token) {
    try {
      return Jwts.parser()
          .setSigningKey(getSigningKey())
          .build()
          .parseClaimsJws(token)
          .getBody();
    } catch (Exception e) {
      log.warn("토큰 파싱 중 오류 발생: {}", e.getMessage());
      throw new RuntimeException("유효하지 않은 토큰입니다.", e);
    }
  }

  /**
   * 토큰에서 Bearer 접두사 제거
   */
  public String removeBearerPrefix(String token) {
    if (token != null && token.startsWith(jwtConfig.getTokenPrefix())) {
      return token.substring(jwtConfig.getTokenPrefix().length());
    }
    return token;
  }

  /**
   * 토큰 만료 시간까지 남은 시간 (초)
   */
  public long getRemainingTime(String token) {
    try {
      Date expiration = getExpirationDateFromToken(token);
      long remainingTime = (expiration.getTime() - System.currentTimeMillis()) / 1000;
      return Math.max(0, remainingTime);
    } catch (Exception e) {
      log.warn("토큰 남은 시간 계산 중 오류 발생: {}", e.getMessage());
      return 0;
    }
  }

  /**
   * Access Token 만료 시간 반환 (초)
   */
  public long getAccessTokenExpiration() {
    return jwtConfig.getAccessTokenExpiration();
  }
}
