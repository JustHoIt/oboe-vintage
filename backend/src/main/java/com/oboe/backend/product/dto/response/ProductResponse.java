package com.oboe.backend.product.dto.response;

import com.oboe.backend.product.entity.Condition;
import com.oboe.backend.product.entity.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class ProductResponse {

  private Long id; // 상품 ID
  private String name; // 상품명
  private String description; // 상품 설명
  private Set<ProductCategoryResponse> categories; // 상품 카테고리 목록
  private String sku; // SKU 코드
  private BigDecimal price; // 상품 가격
  private Integer stockQuantity; // 재고 수량
  private List<ProductImageResponse> images; // 상품 이미지 목록
  private ProductStatus productStatus; // 상품 판매 상태
  private String brand; // 브랜드명
  private String yearOfRelease; // 출시 년도
  private String size; // 사이즈
  private String texture; // 소재 및 재질
  private Condition condition; // 상품 상태
  private Integer views; // 조회수
  private LocalDateTime createdAt; // 생성일시
  private LocalDateTime updatedAt; // 수정일시
}
