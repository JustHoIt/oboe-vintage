package com.oboe.backend.product.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProductCategory Entity 테스트")
class ProductCategoryTest {

  private ProductCategory parentCategory;
  private ProductCategory childCategory;
  private ProductCategory grandChildCategory;

  @BeforeEach
  void setUp() {
    // 최상위 카테고리 (level 1)
    parentCategory = ProductCategory.builder()
        .name("상의")
        .level(1)
        .sortOrder(1)
        .description("상의 카테고리")
        .children(new ArrayList<>())
        .build();

    // 중분류 카테고리 (level 2)
    childCategory = ProductCategory.builder()
        .name("셔츠")
        .level(2)
        .sortOrder(1)
        .parent(parentCategory)
        .description("셔츠 카테고리")
        .children(new ArrayList<>())
        .build();

    // 소분류 카테고리 (level 3)
    grandChildCategory = ProductCategory.builder()
        .name("드레스셔츠")
        .level(3)
        .sortOrder(1)
        .parent(childCategory)
        .description("드레스셔츠 카테고리")
        .children(new ArrayList<>())
        .build();
  }

  @Test
  @DisplayName("ProductCategory 기본 생성 테스트")
  void createBasicProductCategory() {
    // given & when - setUp에서 생성됨

    // then
    assertThat(parentCategory.getName()).isEqualTo("상의");
    assertThat(parentCategory.getLevel()).isEqualTo(1);
    assertThat(parentCategory.getSortOrder()).isEqualTo(1);
    assertThat(parentCategory.getDescription()).isEqualTo("상의 카테고리");
    assertThat(parentCategory.getParent()).isNull();
    assertThat(parentCategory.getChildren()).isNotNull();
    assertThat(parentCategory.getChildren()).isEmpty();
  }

  @Test
  @DisplayName("최상위 카테고리 확인 테스트")
  void isRootCategory() {
    // when & then
    assertThat(parentCategory.isRootCategory()).isTrue();
    assertThat(childCategory.isRootCategory()).isFalse();
    assertThat(grandChildCategory.isRootCategory()).isFalse();
  }

  @Test
  @DisplayName("자식 카테고리 존재 확인 테스트")
  void hasChildren() {
    // given
    parentCategory.getChildren().add(childCategory);
    childCategory.getChildren().add(grandChildCategory);

    // when & then
    assertThat(parentCategory.hasChildren()).isTrue();
    assertThat(childCategory.hasChildren()).isTrue();
    assertThat(grandChildCategory.hasChildren()).isFalse();
  }

  @Test
  @DisplayName("빈 자식 카테고리 리스트 테스트")
  void hasNoChildren() {
    // given & when - setUp에서 빈 children 리스트로 생성

    // then
    assertThat(parentCategory.hasChildren()).isFalse();
    assertThat(childCategory.hasChildren()).isFalse();
    assertThat(grandChildCategory.hasChildren()).isFalse();
  }

  @Test
  @DisplayName("카테고리 계층 구조 테스트")
  void categoryHierarchy() {
    // given
    parentCategory.getChildren().add(childCategory);
    childCategory.getChildren().add(grandChildCategory);

    // then - 부모-자식 관계 확인
    assertThat(childCategory.getParent()).isEqualTo(parentCategory);
    assertThat(grandChildCategory.getParent()).isEqualTo(childCategory);

    // 자식 리스트 확인
    assertThat(parentCategory.getChildren()).contains(childCategory);
    assertThat(childCategory.getChildren()).contains(grandChildCategory);

    // 레벨 확인
    assertThat(parentCategory.getLevel()).isEqualTo(1);
    assertThat(childCategory.getLevel()).isEqualTo(2);
    assertThat(grandChildCategory.getLevel()).isEqualTo(3);
  }

  @Test
  @DisplayName("카테고리 레벨별 생성 테스트")
  void createCategoriesByLevel() {
    // given & when
    ProductCategory level1 = ProductCategory.builder()
        .name("대분류")
        .level(1)
        .sortOrder(1)
        .build();

    ProductCategory level2 = ProductCategory.builder()
        .name("중분류")
        .level(2)
        .sortOrder(1)
        .parent(level1)
        .build();

    ProductCategory level3 = ProductCategory.builder()
        .name("소분류")
        .level(3)
        .sortOrder(1)
        .parent(level2)
        .build();

    // then
    assertThat(level1.getLevel()).isEqualTo(1);
    assertThat(level2.getLevel()).isEqualTo(2);
    assertThat(level3.getLevel()).isEqualTo(3);

    assertThat(level1.isRootCategory()).isTrue();
    assertThat(level2.isRootCategory()).isFalse();
    assertThat(level3.isRootCategory()).isFalse();
  }

  @Test
  @DisplayName("정렬 순서 테스트")
  void testSortOrder() {
    // given & when
    ProductCategory first = ProductCategory.builder()
        .name("첫번째")
        .level(1)
        .sortOrder(1)
        .build();

    ProductCategory second = ProductCategory.builder()
        .name("두번째")
        .level(1)
        .sortOrder(2)
        .build();

    ProductCategory third = ProductCategory.builder()
        .name("세번째")
        .level(1)
        .sortOrder(3)
        .build();

    // then
    assertThat(first.getSortOrder()).isEqualTo(1);
    assertThat(second.getSortOrder()).isEqualTo(2);
    assertThat(third.getSortOrder()).isEqualTo(3);
  }

  @Test
  @DisplayName("기본 정렬 순서 값 테스트")
  void testDefaultSortOrder() {
    // given & when
    ProductCategory category = ProductCategory.builder()
        .name("기본 정렬")
        .level(1)
        .build();

    // then - 기본값은 0
    assertThat(category.getSortOrder()).isEqualTo(0);
  }

  @Test
  @DisplayName("기본 레벨 값 테스트")
  void testDefaultLevel() {
    // given & when
    ProductCategory category = ProductCategory.builder()
        .name("기본 레벨")
        .build();

    // then - 기본값은 1
    assertThat(category.getLevel()).isEqualTo(1);
  }

  @Test
  @DisplayName("카테고리 설명 필드 테스트")
  void testDescriptionField() {
    // given & when
    ProductCategory categoryWithDescription = ProductCategory.builder()
        .name("설명 테스트")
        .level(1)
        .description("이것은 테스트 카테고리입니다.")
        .build();

    ProductCategory categoryWithoutDescription = ProductCategory.builder()
        .name("설명 없음")
        .level(1)
        .build();

    // then
    assertThat(categoryWithDescription.getDescription()).isEqualTo("이것은 테스트 카테고리입니다.");
    assertThat(categoryWithoutDescription.getDescription()).isNull();
  }

  @Test
  @DisplayName("복잡한 카테고리 트리 구조 테스트")
  void complexCategoryTree() {
    // given - 상의 > 셔츠 > 드레스셔츠, 캐주얼셔츠
    ProductCategory dressShirt = ProductCategory.builder()
        .name("드레스셔츠")
        .level(3)
        .sortOrder(1)
        .parent(childCategory)
        .build();

    ProductCategory casualShirt = ProductCategory.builder()
        .name("캐주얼셔츠")
        .level(3)
        .sortOrder(2)
        .parent(childCategory)
        .build();

    // when - 관계 설정
    parentCategory.getChildren().add(childCategory);
    childCategory.getChildren().add(dressShirt);
    childCategory.getChildren().add(casualShirt);

    // then
    assertThat(parentCategory.getChildren()).hasSize(1);
    assertThat(childCategory.getChildren()).hasSize(2);
    assertThat(childCategory.getChildren()).contains(dressShirt, casualShirt);

    assertThat(dressShirt.getParent()).isEqualTo(childCategory);
    assertThat(casualShirt.getParent()).isEqualTo(childCategory);

    assertThat(dressShirt.getSortOrder()).isEqualTo(1);
    assertThat(casualShirt.getSortOrder()).isEqualTo(2);
  }

  @Test
  @DisplayName("여러 최상위 카테고리 테스트")
  void multipleRootCategories() {
    // given & when
    ProductCategory tops = ProductCategory.builder()
        .name("상의")
        .level(1)
        .sortOrder(1)
        .build();

    ProductCategory bottoms = ProductCategory.builder()
        .name("하의")
        .level(1)
        .sortOrder(2)
        .build();

    ProductCategory shoes = ProductCategory.builder()
        .name("신발")
        .level(1)
        .sortOrder(3)
        .build();

    // then
    assertThat(tops.isRootCategory()).isTrue();
    assertThat(bottoms.isRootCategory()).isTrue();
    assertThat(shoes.isRootCategory()).isTrue();

    assertThat(tops.getLevel()).isEqualTo(1);
    assertThat(bottoms.getLevel()).isEqualTo(1);
    assertThat(shoes.getLevel()).isEqualTo(1);
  }

  @Test
  @DisplayName("카테고리 이름 길이 테스트")
  void testCategoryNameLength() {
    // given
    String shortName = "짧은이름";
    String longName = "이것은 매우 긴 카테고리 이름입니다. 100자를 넘지 않도록 주의해야 합니다.";

    // when
    ProductCategory shortCategory = ProductCategory.builder()
        .name(shortName)
        .level(1)
        .build();

    ProductCategory longCategory = ProductCategory.builder()
        .name(longName)
        .level(1)
        .build();

    // then
    assertThat(shortCategory.getName()).isEqualTo(shortName);
    assertThat(longCategory.getName()).isEqualTo(longName);
    assertThat(longCategory.getName().length()).isLessThanOrEqualTo(100);
  }

  @Test
  @DisplayName("같은 부모를 가진 자식 카테고리들 테스트")
  void siblingsCategories() {
    // given
    ProductCategory shirt = ProductCategory.builder()
        .name("셔츠")
        .level(2)
        .sortOrder(1)
        .parent(parentCategory)
        .build();

    ProductCategory tshirt = ProductCategory.builder()
        .name("티셔츠")
        .level(2)
        .sortOrder(2)
        .parent(parentCategory)
        .build();

    ProductCategory outer = ProductCategory.builder()
        .name("아우터")
        .level(2)
        .sortOrder(3)
        .parent(parentCategory)
        .build();

    // when
    parentCategory.getChildren().add(shirt);
    parentCategory.getChildren().add(tshirt);
    parentCategory.getChildren().add(outer);

    // then
    assertThat(parentCategory.getChildren()).hasSize(3);
    assertThat(parentCategory.getChildren()).contains(shirt, tshirt, outer);

    // 모든 자식들이 같은 부모를 가지는지 확인
    assertThat(shirt.getParent()).isEqualTo(parentCategory);
    assertThat(tshirt.getParent()).isEqualTo(parentCategory);
    assertThat(outer.getParent()).isEqualTo(parentCategory);

    // 모든 자식들이 같은 레벨인지 확인
    assertThat(shirt.getLevel()).isEqualTo(2);
    assertThat(tshirt.getLevel()).isEqualTo(2);
    assertThat(outer.getLevel()).isEqualTo(2);
  }

  @Test
  @DisplayName("BaseTimeEntity 상속 확인 테스트")
  void testBaseTimeEntityInheritance() {
    // given & when - ProductCategory가 BaseTimeEntity를 상속받는지 확인

    // then
    assertThat(parentCategory).isInstanceOf(com.oboe.backend.common.domain.BaseTimeEntity.class);
  }

  @Test
  @DisplayName("카테고리 필수 필드 테스트")
  void testRequiredFields() {
    // given & when
    ProductCategory minimalCategory = ProductCategory.builder()
        .name("필수필드만")
        .build();

    // then
    assertThat(minimalCategory.getName()).isEqualTo("필수필드만");
    assertThat(minimalCategory.getLevel()).isEqualTo(1); // 기본값
    assertThat(minimalCategory.getSortOrder()).isEqualTo(0); // 기본값
    assertThat(minimalCategory.getParent()).isNull();
    assertThat(minimalCategory.getDescription()).isNull();
  }
}
