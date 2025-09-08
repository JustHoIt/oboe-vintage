package com.oboe.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FindIdResponseDto {

  private String email;
  private String maskedEmail; // 이메일 마스킹 처리 (예: ab***@example.com)

  public static FindIdResponseDto from(String email) {
    String maskedEmail = maskEmail(email);
    return FindIdResponseDto.builder()
        .email(email)
        .maskedEmail(maskedEmail)
        .build();
  }

  private static String maskEmail(String email) {
    if (email == null || email.length() < 3) {
      return email;
    }
    
    String[] parts = email.split("@");
    if (parts.length != 2) {
      return email;
    }
    
    String localPart = parts[0];
    String domain = parts[1];
    
    if (localPart.length() <= 2) {
      return localPart + "***@" + domain;
    }
    
    return localPart.substring(0, 2) + "***@" + domain;
  }
}
