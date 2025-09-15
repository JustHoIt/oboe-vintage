package com.oboe.backend.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProductImageRequest {

  @NotBlank(message = "이미지 URL은 필수입니다.")
  @Size(max = 512, message = "이미지 URL은 512자를 초과할 수 없습니다.")
  private String imageUrl;

  private Integer sortOrder;

  private boolean thumbnail;
}
