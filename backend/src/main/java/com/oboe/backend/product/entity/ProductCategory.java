package com.oboe.backend.product.entity;

import com.oboe.backend.common.domain.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "product_categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class ProductCategory extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  // 부모 카테고리 (null이면 최상위)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private ProductCategory parent;

  // 자식 카테고리들
  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
  @Builder.Default
  private List<ProductCategory> children = new ArrayList<>();

  /**
   * * ex)
   *   1. 상의 (level=1)
   *    ├── 1-1. 셔츠 (level=2)
   *    │   ├── 1-1-1. 드레스셔츠 (level=3)
   *    │   ├── 1-1-2. 캐주얼셔츠 (level=3)
   *    │   └── 1-1-3. 빈티지셔츠 (level=3)
   *    ├── 1-2. 티셔츠 (level=2)
   *    │   ├── 1-2-1. 반팔티 (level=3)
   *    │   └── 1-2-2. 긴팔티 (level=3)
   *    └── 1-3. 아우터 (level=2)
   *        ├── 1-3-1. 자켓 (level=3)
   *        └── 1-3-2. 코트 (level=3)
   *   2. 하의 (level=1)
   *    ├── 2-1. 바지 (level=2)
   *    └── 2-2. 스커트 (level=2)
   *   3. 신발 (level=1)
   *    ├── 3-1. 운동화 (level=2)
   *    ├── 3-2. 구두 (level=2)
   *    └── 3-3. 부츠 (level=2)
   *   4. 모자 (level=1)
   *   5. 악세서리 (level=1)
   */

  @Column(nullable = false)
  @Builder.Default
  private Integer level = 1; // 1:대분류, 2:중분류, 3:소분류

  @Column(nullable = false)
  @Builder.Default
  private Integer sortOrder = 0; // 같은 레벨에서의 정렬 순서

  private String description; // 카테고리 설명

  /**
   * 최상위 카테고리 여부 확인
   *
   * @return 최상위 카테고리면 true
   */
  public boolean isRootCategory() {
    return parent == null;
  }

  /**
   * 하위 카테고리 존재 여부 확인
   *
   * @return 하위 카테고리가 있으면 true
   */
  public boolean hasChildren() {
    return !children.isEmpty();
  }
}