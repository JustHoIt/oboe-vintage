package com.oboe.backend.product.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProductImageResponse {

  private Long id;
  private String imageUrl;
  private Integer sortOrder;
  private boolean thumbnail;
}
