package com.oboe.backend.product.dto.request;

import com.oboe.backend.product.entity.Condition;
import com.oboe.backend.product.entity.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ProductCreateRequest {

  @NotBlank(message = "상품명은 필수입니다.")
  @Size(max = 200, message = "상품명은 200자를 초과할 수 없습니다.")
  private String name; // 상품명 (필수, 최대 200자)

  @NotBlank(message = "상품 설명은 필수입니다.")
  private String description; // 상품 설명 (필수)

  private Set<Long> categoryIds; // 카테고리 ID 목록 (선택사항)

  private String sku; // SKU 코드 (선택사항)

  @NotNull(message = "가격은 필수입니다.")
  @DecimalMin(value = "0.0", inclusive = false, message = "가격은 0보다 커야 합니다.")
  private BigDecimal price; // 상품 가격 (필수, 0초과)

  @NotNull(message = "재고 수량은 필수입니다.")
  @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다.")
  private Integer stockQuantity; // 재고 수량 (필수, 0이상)

  @NotNull(message = "상품 상태는 필수입니다.")
  private ProductStatus productStatus; // 상품 판매 상태 (필수)

  private String brand; // 브랜드명 (선택사항)

  private String yearOfRelease; // 출시 년도 (선택사항)

  private String size; // 사이즈 (선택사항)

  private String texture; // 소재 및 재질 (선택사항)

  private Condition condition; // 상품 상태 (선택사항)

  private List<ProductImageRequest> images; // 상품 이미지 목록 (선택사항)
}
