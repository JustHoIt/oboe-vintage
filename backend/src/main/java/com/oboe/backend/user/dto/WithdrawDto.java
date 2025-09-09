package com.oboe.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawDto {

  @NotBlank(message = "비밀번호는 필수입니다.")
  @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
  private String password;

  @NotBlank(message = "탈퇴 사유는 필수입니다.")
  @Size(max = 500, message = "탈퇴 사유는 500자 이하여야 합니다.")
  private String reason;
}
