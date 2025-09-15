package com.oboe.backend.product.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReviewCreateRequest {

  @NotNull(message = "평점은 필수입니다.")
  @Min(value = 1, message = "평점은 1점 이상이어야 합니다.")
  @Max(value = 5, message = "평점은 5점 이하여야 합니다.")
  private Integer rating;

  @NotBlank(message = "리뷰 제목은 필수입니다.")
  private String title;

  private String content; // 리뷰 내용 (선택사항)
}
