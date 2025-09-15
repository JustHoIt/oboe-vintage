package com.oboe.backend.product.repository;

import com.oboe.backend.product.entity.Product;
import com.oboe.backend.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

  /**
   * INACTIVE가 아닌 상품들만 조회
   *
   * @param excludeStatus 상품상태
   * @return
   */
  @Query("SELECT p FROM Product p WHERE p.productStatus != :excludeStatus")
  Page<Product> findAllExcludingStatus(@Param("excludeStatus") ProductStatus excludeStatus,
      Pageable pageable);

  /**
   * ID로 조회 (INACTIVE 제외)
   *
   * @param id 상품 ID
   * @return 상품 정보 (Optional)
   */
  @Query("SELECT p FROM Product p WHERE p.id = :id AND p.productStatus != 'INACTIVE'")
  Optional<Product> findByIdExcludingInactive(@Param("id") Long id);

  /**
   * 상품 상태별 조회
   *
   * @param productStatus 상품 상태
   * @param pageable 페이징 정보
   * @return 상품 목록 (Page)
   */
  Page<Product> findByProductStatus(ProductStatus productStatus, Pageable pageable);

  /**
   * 브랜드별 조회 (INACTIVE 제외)
   *
   * @param brand 브랜드명
   * @param pageable 페이징 정보
   * @return 상품 목록 (Page)
   */
  @Query("SELECT p FROM Product p WHERE p.brand = :brand AND p.productStatus != 'INACTIVE'")
  Page<Product> findByBrandExcludingInactive(@Param("brand") String brand, Pageable pageable);

  /**
   * 카테고리별 조회 (INACTIVE 제외)
   *
   * @param categoryId 카테고리 ID
   * @param pageable 페이징 정보
   * @return 상품 목록 (Page)
   */
  @Query("SELECT p FROM Product p JOIN p.categories c WHERE c.id = :categoryId AND p.productStatus != 'INACTIVE'")
  Page<Product> findByCategoryIdExcludingInactive(@Param("categoryId") Long categoryId,
      Pageable pageable);

  /**
   * 조회수 증가
   *
   * @param id 상품 ID
   */
  @Modifying
  @Query("UPDATE Product p SET p.views = p.views + 1 WHERE p.id = :id")
  void incrementViews(@Param("id") Long id);

  /**
   * Soft Delete - 상품 상태를 INACTIVE로 변경
   *
   * @param id 상품 ID
   * @param updatedAt 수정 시간
   */
  @Modifying
  @Query("UPDATE Product p SET p.productStatus = 'INACTIVE', p.updatedAt = :updatedAt WHERE p.id = :id")
  void softDeleteById(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

  /**
   * 상품명으로 검색 (INACTIVE 제외)
   *
   * @param keyword 검색 키워드
   * @param pageable 페이징 정보
   * @return 상품 목록 (Page)
   */
  @Query("SELECT p FROM Product p WHERE p.name LIKE %:keyword% AND p.productStatus != 'INACTIVE'")
  Page<Product> findByNameContainingExcludingInactive(@Param("keyword") String keyword,
      Pageable pageable);

  /**
   * 설명으로 검색 (INACTIVE 제외)
   *
   * @param keyword 검색 키워드
   * @param pageable 페이징 정보
   * @return 상품 목록 (Page)
   */
  @Query("SELECT p FROM Product p WHERE p.description LIKE %:keyword% AND p.productStatus != 'INACTIVE'")
  Page<Product> findByDescriptionContainingExcludingInactive(@Param("keyword") String keyword,
      Pageable pageable);

  /**
   * 상품명 또는 설명으로 검색 (INACTIVE 제외)
   *
   * @param keyword 검색 키워드
   * @param pageable 페이징 정보
   * @return 상품 목록 (Page)
   */
  @Query("SELECT p FROM Product p WHERE (p.name LIKE %:keyword% OR p.description LIKE %:keyword% OR p.brand LIKE %:keyword%) AND p.productStatus != 'INACTIVE'")
  Page<Product> findByKeywordExcludingInactive(@Param("keyword") String keyword, Pageable pageable);

  /**
   * 조회수 Top N 상품들 조회 (INACTIVE 제외)
   *
   * @param pageable 페이징 정보 (limit 설정)
   * @return 인기 상품 목록
   */
  @Query("SELECT p FROM Product p WHERE p.productStatus != 'INACTIVE' ORDER BY p.views DESC")
  List<Product> findTopByViewsExcludingInactive(Pageable pageable);

  /**
   * 최신 상품들 조회 (INACTIVE 제외)
   *
   * @param pageable 페이징 정보 (limit 설정)
   * @return 최신 상품 목록
   */
  @Query("SELECT p FROM Product p WHERE p.productStatus != 'INACTIVE' ORDER BY p.createdAt DESC")
  List<Product> findLatestProductsExcludingInactive(Pageable pageable);
}
