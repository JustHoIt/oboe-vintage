package com.oboe.backend.common.util;

import static org.assertj.core.api.Assertions.*;

import com.oboe.backend.config.JwtConfig;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtUtil 테스트")
class JwtUtilTest {

  private JwtUtil jwtUtil;
  private User testUser;

  @BeforeEach
  void setUp() {
    JwtConfig jwtConfig = new JwtConfig();
    // JJWT 0.12.3에서는 최소 256비트(32바이트) 키가 필요
    jwtConfig.setSecret("test-secret-key-for-jwt-testing-only-must-be-at-least-32-characters-long");
    jwtConfig.setAccessTokenExpiration(3600000); // 1시간
    jwtConfig.setRefreshTokenExpiration(604800000); // 7일
    jwtConfig.setTokenPrefix("Bearer ");
    jwtConfig.setHeaderName("Authorization");
    
    jwtUtil = new JwtUtil(jwtConfig);

    testUser = User.builder()
        .id(1L)
        .email("test@example.com")
        .name("테스트사용자")
        .nickname("testuser")
        .phoneNumber("01012345678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .lastLoginAt(LocalDateTime.now())
        .build();
  }

  @Test
  @DisplayName("Access Token 생성 및 검증")
  void generateAndValidateAccessToken() {
    // when
    String accessToken = jwtUtil.generateAccessToken(testUser);

    // then
    assertThat(accessToken).isNotNull();
    assertThat(jwtUtil.isAccessToken(accessToken)).isTrue();
    assertThat(jwtUtil.isRefreshToken(accessToken)).isFalse();
    assertThat(jwtUtil.getEmailFromToken(accessToken)).isEqualTo("test@example.com");
    assertThat(jwtUtil.getUserIdFromToken(accessToken)).isEqualTo(1L);
    assertThat(jwtUtil.getRoleFromToken(accessToken)).isEqualTo("USER");
    assertThat(jwtUtil.validateToken(accessToken, "test@example.com")).isTrue();
  }

  @Test
  @DisplayName("Refresh Token 생성 및 검증")
  void generateAndValidateRefreshToken() {
    // when
    String refreshToken = jwtUtil.generateRefreshToken(testUser);

    // then
    assertThat(refreshToken).isNotNull();
    assertThat(jwtUtil.isRefreshToken(refreshToken)).isTrue();
    assertThat(jwtUtil.isAccessToken(refreshToken)).isFalse();
    assertThat(jwtUtil.getEmailFromToken(refreshToken)).isEqualTo("test@example.com");
    assertThat(jwtUtil.getUserIdFromToken(refreshToken)).isEqualTo(1L);
    assertThat(jwtUtil.validateToken(refreshToken, "test@example.com")).isTrue();
  }

  @Test
  @DisplayName("잘못된 이메일로 토큰 검증 실패")
  void validateTokenWithWrongEmail() {
    // given
    String accessToken = jwtUtil.generateAccessToken(testUser);

    // when & then
    assertThat(jwtUtil.validateToken(accessToken, "wrong@example.com")).isFalse();
  }

  @Test
  @DisplayName("Bearer 접두사 제거")
  void removeBearerPrefix() {
    // given
    String tokenWithBearer = "Bearer eyJhbGciOiJIUzUxMiJ9...";
    String tokenWithoutBearer = "eyJhbGciOiJIUzUxMiJ9...";

    // when & then
    assertThat(jwtUtil.removeBearerPrefix(tokenWithBearer)).isEqualTo(tokenWithoutBearer);
    assertThat(jwtUtil.removeBearerPrefix(tokenWithoutBearer)).isEqualTo(tokenWithoutBearer);
    assertThat(jwtUtil.removeBearerPrefix(null)).isNull();
  }

  @Test
  @DisplayName("토큰에서 사용자 정보 추출")
  void extractUserInfoFromToken() {
    // given
    String accessToken = jwtUtil.generateAccessToken(testUser);

    // when & then
    assertThat(jwtUtil.getEmailFromToken(accessToken)).isEqualTo("test@example.com");
    assertThat(jwtUtil.getUserIdFromToken(accessToken)).isEqualTo(1L);
    assertThat(jwtUtil.getRoleFromToken(accessToken)).isEqualTo("USER");
    assertThat(jwtUtil.getTokenTypeFromToken(accessToken)).isEqualTo("ACCESS");
  }

  @Test
  @DisplayName("토큰 만료 확인")
  void checkTokenExpiration() {
    // given
    String accessToken = jwtUtil.generateAccessToken(testUser);

    // when & then
    assertThat(jwtUtil.isTokenExpired(accessToken)).isFalse();
  }

  @Test
  @DisplayName("Access Token 만료 시간 반환")
  void getAccessTokenExpiration() {
    // when
    long expiration = jwtUtil.getAccessTokenExpiration();

    // then
    assertThat(expiration).isEqualTo(3600000); // 1시간
  }

  @Test
  @DisplayName("잘못된 토큰으로 검증 실패")
  void validateInvalidToken() {
    // given
    String invalidToken = "invalid.token.here";

    // when & then
    assertThatThrownBy(() -> jwtUtil.getEmailFromToken(invalidToken))
        .isInstanceOf(RuntimeException.class);
  }
}
