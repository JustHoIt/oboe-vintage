package com.oboe.backend.product.repository;

import com.oboe.backend.product.dto.request.ProductSearchRequest;
import com.oboe.backend.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {

  /**
   * 복합 검색 조건으로 상품 검색 - 검색어 (상품명, 설명, 브랜드) - 상품 상태 필터 - 카테고리 필터 - 브랜드 필터 - 컨디션 필터 - 정렬 조건 (최신순,
   * 오래된순, 조회순)
   */
  Page<Product> searchProducts(ProductSearchRequest searchRequest, Pageable pageable);

  /**
   * 키워드로만 검색 (상품명, 설명, 브랜드에서 검색)
   */
  Page<Product> searchByKeyword(String keyword, Pageable pageable);

  /**
   * 필터 조건으로만 검색 (키워드 없이)
   */
  Page<Product> searchByFilters(ProductSearchRequest searchRequest, Pageable pageable);
}
