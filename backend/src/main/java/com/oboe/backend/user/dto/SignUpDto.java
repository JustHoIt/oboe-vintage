package com.oboe.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원가입 요청 DTO")
public class SignUpDto {

  @NotBlank(message = "이메일은 필수입니다.")
  @Email(message = "올바른 이메일 형식이 아닙니다.")
  @Schema(description = "사용자 이메일", example = "user@example.com", required = true)
  private String email;

  @NotBlank(message = "비밀번호는 필수입니다.")
  @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
  @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
      message = "비밀번호는 8-20자 영문, 숫자, 특수문자를 포함해야 합니다.")
  @Schema(description = "사용자 비밀번호", example = "password123!", required = true)
  private String password;

  @NotBlank(message = "이름은 필수입니다.")
  @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하여야 합니다.")
  @Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "이름은 한글, 영문, 공백만 입력 가능합니다.")
  @Schema(description = "사용자 이름", example = "홍길동", required = true)
  private String name;

  @NotBlank(message = "닉네임은 필수입니다.")
  @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
  @Pattern(regexp = "^[가-힣a-zA-Z0-9_]+$", message = "닉네임은 한글, 영문, 숫자, 언더스코어만 입력 가능합니다.")
  @Schema(description = "사용자 닉네임", example = "즐거운사자", required = true)
  private String nickname;

  @NotBlank(message = "휴대폰번호는 필수입니다.")
  @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
  @Schema(description = "사용자 휴대폰번호", example = "01012345678", required = true)
  private String phoneNumber;

  @Schema(description = "사용자 주소", example = "서울시 강남구 테헤란로 123")
  private String roadAddress;

  @Schema(description = "사용자 상세 주소", example = "2층 A201호")
  private String detailAddress;

  @Schema(description = "우편 번호", example = "02111")
  private String zipCode;

  @Schema(description = "사용자 생일", example = "1997-12-12")
  private LocalDate birthDate;

  @Pattern(regexp = "^(M|F|U)$", message = "성별은 'M', 'F', 'U'만 입력 가능합니다.")
  @Schema(description = "사용자 성별", example = "M", allowableValues = {"M", "F", "U"})
  private String gender;

  @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
  private String profileImg;
}
