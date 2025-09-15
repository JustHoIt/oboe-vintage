package com.oboe.backend.product.repository;

import com.oboe.backend.product.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

  /**
   * 최상위 카테고리들 조회 (parent가 null인 카테고리들)
   *
   * @return 최상위 카테고리 목록
   */
  List<ProductCategory> findByParentIsNullOrderBySortOrderAsc();

  /**
   * 특정 부모 카테고리의 자식 카테고리들 조회
   *
   * @param parentId 부모 카테고리 ID
   * @return 자식 카테고리 목록
   */
  List<ProductCategory> findByParentIdOrderBySortOrderAsc(Long parentId);

  /**
   * 레벨별 카테고리 조회
   *
   * @param level 카테고리 레벨
   * @return 해당 레벨의 카테고리 목록
   */
  List<ProductCategory> findByLevelOrderBySortOrderAsc(Integer level);

  /**
   * 카테고리명으로 검색
   *
   * @param name 검색할 카테고리명
   * @return 검색 결과 카테고리 목록
   */
  List<ProductCategory> findByNameContainingIgnoreCase(String name);

  /**
   * 특정 카테고리와 그 하위 카테고리들 조회
   *
   * @param parentId 부모 카테고리 ID
   * @return 부모와 하위 카테고리 목록
   */
  @Query("SELECT c FROM ProductCategory c WHERE c.parent.id = :parentId OR c.id = :parentId ORDER BY c.level, c.sortOrder")
  List<ProductCategory> findCategoryWithChildren(@Param("parentId") Long parentId);

  /**
   * 카테고리 계층 구조 조회 (특정 카테고리부터 루트까지)
   *
   * @param categoryId 시작 카테고리 ID
   * @return 계층 구조 카테고리 목록
   */
  @Query(value = "WITH RECURSIVE category_hierarchy AS (" +
      "  SELECT id, name, parent_id, level, sort_order, description " +
      "  FROM product_categories WHERE id = :categoryId " +
      "  UNION ALL " +
      "  SELECT c.id, c.name, c.parent_id, c.level, c.sort_order, c.description " +
      "  FROM product_categories c " +
      "  JOIN category_hierarchy ch ON c.id = ch.parent_id" +
      ") SELECT * FROM category_hierarchy ORDER BY level DESC",
      nativeQuery = true)
  List<ProductCategory> findCategoryHierarchy(@Param("categoryId") Long categoryId);
}
