package com.oboe.backend.product.repository;

import com.oboe.backend.product.entity.ProductImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

  /**
   * 특정 상품의 이미지들 조회 (정렬 순서대로)
   *
   * @param productId 상품 ID
   * @return 상품 이미지 목록
   */
  List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);

  /**
   * 특정 상품의 썸네일 이미지 조회
   *
   * @param productId 상품 ID
   * @return 썸네일 이미지 (Optional)
   */
  Optional<ProductImage> findByProductIdAndThumbnailTrue(Long productId);

  /**
   * 특정 상품의 모든 이미지 삭제
   *
   * @param productId 상품 ID
   */
  @Modifying
  @Query("DELETE FROM ProductImage pi WHERE pi.product.id = :productId")
  void deleteByProductId(@Param("productId") Long productId);

  /**
   * 특정 상품의 이미지 개수 조회
   *
   * @param productId 상품 ID
   * @return 이미지 개수
   */
  long countByProductId(Long productId);

  /**
   * 특정 상품의 썸네일이 아닌 이미지들 조회
   *
   * @param productId 상품 ID
   * @return 일반 이미지 목록
   */
  List<ProductImage> findByProductIdAndThumbnailFalseOrderBySortOrderAsc(Long productId);

  /**
   * 정렬 순서별 이미지 조회
   *
   * @param productId 상품 ID
   * @param sortOrder 최소 정렬 순서
   * @return 이미지 목록
   */
  List<ProductImage> findByProductIdAndSortOrderGreaterThanEqualOrderBySortOrderAsc(Long productId,
      Integer sortOrder);
}
