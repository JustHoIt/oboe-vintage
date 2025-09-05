package com.oboe.backend.user.dto;

import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 응답 DTO")
public class LoginResponseDto {

  @Schema(description = "사용자 ID", example = "1")
  private Long id;

  @Schema(description = "사용자 이메일", example = "user@example.com")
  private String email;

  @Schema(description = "사용자 이름", example = "홍길동")
  private String name;

  @Schema(description = "사용자 닉네임", example = "즐거운사자")
  private String nickname;

  @Schema(description = "사용자 역할", example = "USER")
  private UserRole role;

  @Schema(description = "사용자 상태", example = "ACTIVE")
  private UserStatus status;

  @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
  private String profileImg;

  @Schema(description = "마지막 로그인 시간", example = "2024-01-01T12:00:00")
  private String lastLoginAt;

  @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzUxMiJ9...")
  private String accessToken;

  @Schema(description = "JWT Refresh Token", example = "eyJhbGciOiJIUzUxMiJ9...")
  private String refreshToken;

  @Schema(description = "토큰 타입", example = "Bearer")
  private String tokenType = "Bearer";

  @Schema(description = "Access Token 만료 시간 (초)", example = "86400")
  private Long expiresIn;

  public static LoginResponseDto from(User user) {
    return LoginResponseDto.builder()
        .id(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .nickname(user.getNickname())
        .role(user.getRole())
        .status(user.getStatus())
        .profileImg(user.getProfileImg())
        .lastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null)
        .build();
  }

  public static LoginResponseDto from(User user, String accessToken, String refreshToken, Long expiresIn) {
    return LoginResponseDto.builder()
        .id(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .nickname(user.getNickname())
        .role(user.getRole())
        .status(user.getStatus())
        .profileImg(user.getProfileImg())
        .lastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null)
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(expiresIn)
        .build();
  }
}
