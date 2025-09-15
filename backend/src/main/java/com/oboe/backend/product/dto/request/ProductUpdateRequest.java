package com.oboe.backend.product.dto.request;

import com.oboe.backend.product.entity.Condition;
import com.oboe.backend.product.entity.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProductUpdateRequest {

  @Size(max = 200, message = "상품명은 200자를 초과할 수 없습니다.")
  private String name;

  private String description;

  private Set<Long> categoryIds; // 카테고리 ID 목록

  private String sku; // 상품 코드

  @DecimalMin(value = "0.0", inclusive = false, message = "가격은 0보다 커야 합니다.")
  private BigDecimal price;

  @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다.")
  private Integer stockQuantity;

  private ProductStatus productStatus;

  private String brand; // 브랜드명

  private String yearOfRelease; // 출시 년도

  private String size; // 사이즈

  private String texture; // 소재 및 재질

  private Condition condition; // 상품 컨디션

  private List<ProductImageRequest> images; // 상품 이미지들
}
