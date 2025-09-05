package com.oboe.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토큰 응답 DTO")
public class TokenResponseDto {

  @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzUxMiJ9...")
  private String accessToken;

  @Schema(description = "JWT Refresh Token", example = "eyJhbGciOiJIUzUxMiJ9...")
  private String refreshToken;

  @Schema(description = "토큰 타입", example = "Bearer")
  private String tokenType = "Bearer";

  @Schema(description = "Access Token 만료 시간 (초)", example = "86400")
  private Long expiresIn;

  public static TokenResponseDto of(String accessToken, String refreshToken, Long expiresIn) {
    return TokenResponseDto.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(expiresIn)
        .build();
  }
}
