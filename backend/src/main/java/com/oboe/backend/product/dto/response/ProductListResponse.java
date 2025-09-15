package com.oboe.backend.product.dto.response;

import com.oboe.backend.product.entity.Condition;
import com.oboe.backend.product.entity.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProductListResponse {

  private Long id;
  private String name;
  private BigDecimal price;
  private ProductStatus productStatus;
  private String brand;
  private Condition condition;
  private Integer views;
  private String thumbnailImage; // 썸네일 이미지 URL
  private LocalDateTime createdAt;
}
