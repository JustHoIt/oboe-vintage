package com.oboe.backend.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SMS 인증 요청 DTO")
public class SmsAuthRequestDto {

    @Schema(description = "이메일", example = "user@example.com", required = false)
    private String email;

    @Schema(description = "휴대폰 번호", example = "01012345678", required = true)
    private String phoneNumber;

    @Schema(description = "인증번호", example = "ABC123", required = false)
    private String verificationCode;
}
