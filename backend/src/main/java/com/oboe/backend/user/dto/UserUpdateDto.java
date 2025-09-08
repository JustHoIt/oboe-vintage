package com.oboe.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 정보 수정 DTO (닉네임, 주소, 프로필 이미지) - 이메일과 비밀번호는 별도 API 사용")
public class UserUpdateDto {

    @Schema(description = "닉네임", example = "newNickname")
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2-20자리여야 합니다.")
    @Pattern(
        regexp = "^[가-힣a-zA-Z0-9_]+$",
        message = "닉네임은 한글, 영문, 숫자, 언더스코어만 사용 가능합니다."
    )
    private String nickname;

    @Schema(description = "도로명 주소", example = "서울시 강남구 테헤란로 123")
    private String roadAddress;

    @Schema(description = "상세 주소", example = "1층")
    private String detailAddress;

    @Schema(description = "우편번호", example = "02111")
    private String zipCode;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImg;
}
