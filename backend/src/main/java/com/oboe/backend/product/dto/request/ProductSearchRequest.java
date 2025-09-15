package com.oboe.backend.product.dto.request;

import com.oboe.backend.product.entity.ProductStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProductSearchRequest {

  private String keyword; // 검색어 (상품명, 브랜드, 설명에서 검색)

  private ProductStatus status; // 상품 상태 필터

  private String sortBy; // 정렬 기준: "latest"(최신순), "oldest"(오래된순), "views"(조회순)

  private Long categoryId; // 카테고리 필터

  private String brand; // 브랜드 필터

  private String condition; // 컨디션 필터

  private Integer page; // 페이지 번호 (0부터 시작)

  private Integer size; // 페이지 크기 (기본 20)
}
