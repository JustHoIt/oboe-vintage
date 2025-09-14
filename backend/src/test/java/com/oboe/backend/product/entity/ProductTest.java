package com.oboe.backend.product.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@DisplayName("Product Entity 테스트")
class ProductTest {

  private Product product;
  private ProductCategory category1;
  private ProductCategory category2;

  @BeforeEach
  void setUp() {
    // 테스트용 카테고리 생성
    category1 = ProductCategory.builder()
        .name("상의")
        .level(1)
        .sortOrder(1)
        .description("상의 카테고리")
        .build();
        
    category2 = ProductCategory.builder()
        .name("셔츠")
        .level(2)
        .sortOrder(1)
        .parent(category1)
        .description("셔츠 카테고리")
        .build();

    // 기본 테스트용 Product 생성
    product = Product.builder()
        .name("빈티지 데님 셔츠")
        .description("1980년대 빈티지 데님 셔츠입니다.")
        .price(new BigDecimal("150000"))
        .stockQuantity(1)
        .productStatus(ProductStatus.ACTIVE)
        .brand("리바이스")
        .yearOfRelease("1985")
        .size("L")
        .texture("100% 코튼")
        .condition(Condition.VERY_GOOD)
        .views(0)
        .build();
  }

  @Test
  @DisplayName("Product 기본 생성 테스트")
  void createProduct() {
    // given & when
    
    // then
    assertThat(product.getName()).isEqualTo("빈티지 데님 셔츠");
    assertThat(product.getDescription()).isEqualTo("1980년대 빈티지 데님 셔츠입니다.");
    assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("150000"));
    assertThat(product.getStockQuantity()).isEqualTo(1);
    assertThat(product.getProductStatus()).isEqualTo(ProductStatus.ACTIVE);
    assertThat(product.getBrand()).isEqualTo("리바이스");
    assertThat(product.getYearOfRelease()).isEqualTo("1985");
    assertThat(product.getSize()).isEqualTo("L");
    assertThat(product.getTexture()).isEqualTo("100% 코튼");
    assertThat(product.getCondition()).isEqualTo(Condition.VERY_GOOD);
    assertThat(product.getViews()).isEqualTo(0);
  }

  @Test
  @DisplayName("Product 필수 필드만으로 생성 테스트")
  void createProductWithMinimalFields() {
    // given & when
    Product minimalProduct = Product.builder()
        .name("최소 필드 상품")
        .description("최소 필드로 생성된 상품입니다.")
        .price(new BigDecimal("50000"))
        .stockQuantity(10)
        .productStatus(ProductStatus.ACTIVE)
        .views(0)
        .build();

    // then
    assertThat(minimalProduct.getName()).isEqualTo("최소 필드 상품");
    assertThat(minimalProduct.getDescription()).isEqualTo("최소 필드로 생성된 상품입니다.");
    assertThat(minimalProduct.getPrice()).isEqualByComparingTo(new BigDecimal("50000"));
    assertThat(minimalProduct.getStockQuantity()).isEqualTo(10);
    assertThat(minimalProduct.getProductStatus()).isEqualTo(ProductStatus.ACTIVE);
    assertThat(minimalProduct.getViews()).isEqualTo(0);
    
    // 선택 필드들은 null이어야 함
    assertThat(minimalProduct.getSku()).isNull();
    assertThat(minimalProduct.getBrand()).isNull();
    assertThat(minimalProduct.getYearOfRelease()).isNull();
    assertThat(minimalProduct.getSize()).isNull();
    assertThat(minimalProduct.getTexture()).isNull();
    assertThat(minimalProduct.getCondition()).isNull();
  }

  @Test
  @DisplayName("조회수 증가 테스트")
  void increaseViews() {
    // given
    int initialViews = product.getViews();
    
    // when
    product.increaseViews();
    
    // then
    assertThat(product.getViews()).isEqualTo(initialViews + 1);
  }

  @Test
  @DisplayName("조회수 여러 번 증가 테스트")
  void increaseViewsMultipleTimes() {
    // given
    int initialViews = product.getViews();
    
    // when
    product.increaseViews();
    product.increaseViews();
    product.increaseViews();
    
    // then
    assertThat(product.getViews()).isEqualTo(initialViews + 3);
  }

  @Test
  @DisplayName("조회수 초기값 테스트")
  void initialViewsValue() {
    // given & when
    Product newProduct = Product.builder()
        .name("새 상품")
        .description("새로운 상품")
        .price(new BigDecimal("10000"))
        .stockQuantity(1)
        .productStatus(ProductStatus.ACTIVE)
        .views(0)
        .build();

    // then
    assertThat(newProduct.getViews()).isEqualTo(0);
  }

  @Test
  @DisplayName("카테고리 연관관계 설정 테스트")
  void addCategories() {
    // given
    Set<ProductCategory> categories = new HashSet<>();
    categories.add(category1);
    categories.add(category2);
    
    // when
    Product productWithCategories = Product.builder()
        .name("카테고리 테스트 상품")
        .description("카테고리가 있는 상품")
        .price(new BigDecimal("100000"))
        .stockQuantity(1)
        .productStatus(ProductStatus.ACTIVE)
        .categories(categories)
        .views(0)
        .build();
    
    // then
    assertThat(productWithCategories.getCategories()).hasSize(2);
    assertThat(productWithCategories.getCategories()).contains(category1, category2);
  }

  @Test
  @DisplayName("중복 카테고리 방지 테스트")
  void preventDuplicateCategories() {
    // given
    Set<ProductCategory> categories = new HashSet<>();
    categories.add(category1);
    categories.add(category1); // 중복 추가
    categories.add(category2);
    
    // when
    Product productWithCategories = Product.builder()
        .name("중복 카테고리 테스트")
        .description("중복 카테고리 테스트")
        .price(new BigDecimal("100000"))
        .stockQuantity(1)
        .productStatus(ProductStatus.ACTIVE)
        .categories(categories)
        .views(0)
        .build();
    
    // then - Set이므로 중복 제거되어 2개만 있어야 함
    assertThat(productWithCategories.getCategories()).hasSize(2);
    assertThat(productWithCategories.getCategories()).contains(category1, category2);
  }

  @Test
  @DisplayName("빈 카테고리 Set 테스트")
  void emptyCategoriesSet() {
    // given & when - setUp의 product는 categories가 빈 Set
    
    // then
    assertThat(product.getCategories()).isNotNull();
    assertThat(product.getCategories()).isEmpty();
  }

  @Test
  @DisplayName("빈 ProductImages List 테스트")
  void emptyProductImagesList() {
    // given & when - setUp의 product는 productImages가 빈 List
    
    // then
    assertThat(product.getProductImages()).isNotNull();
    assertThat(product.getProductImages()).isEmpty();
  }

  @Test
  @DisplayName("BigDecimal 가격 정밀도 테스트")
  void testPricePrecision() {
    // given & when
    Product precisionProduct = Product.builder()
        .name("정밀도 테스트 상품")
        .description("가격 정밀도 테스트")
        .price(new BigDecimal("123456.78"))
        .stockQuantity(1)
        .productStatus(ProductStatus.ACTIVE)
        .views(0)
        .build();

    // then
    assertThat(precisionProduct.getPrice()).isEqualByComparingTo(new BigDecimal("123456.78"));
    assertThat(precisionProduct.getPrice().scale()).isEqualTo(2);
  }

  @Test
  @DisplayName("ProductStatus enum 모든 값 테스트")
  void testProductStatusEnum() {
    // given & when & then
    assertThat(ProductStatus.ACTIVE).isNotNull();
    assertThat(ProductStatus.SOLD_OUT).isNotNull();
    assertThat(ProductStatus.INACTIVE).isNotNull();
    assertThat(ProductStatus.TRADING).isNotNull();
    assertThat(ProductStatus.values()).hasSize(4);
  }

  @Test
  @DisplayName("Condition enum 모든 값 테스트")
  void testConditionEnum() {
    // given & when & then
    assertThat(Condition.NEW).isNotNull();
    assertThat(Condition.EXCELLENT).isNotNull();
    assertThat(Condition.VERY_GOOD).isNotNull();
    assertThat(Condition.GOOD).isNotNull();
    assertThat(Condition.FAIR).isNotNull();
    assertThat(Condition.values()).hasSize(5);
  }

  @Test
  @DisplayName("다양한 ProductStatus로 상품 생성 테스트")
  void createProductWithDifferentStatuses() {
    // given & when
    Product activeProduct = Product.builder()
        .name("판매중 상품")
        .description("판매중인 상품")
        .price(new BigDecimal("100000"))
        .stockQuantity(10)
        .productStatus(ProductStatus.ACTIVE)
        .views(0)
        .build();

    Product soldOutProduct = Product.builder()
        .name("품절 상품")
        .description("품절된 상품")
        .price(new BigDecimal("100000"))
        .stockQuantity(0)
        .productStatus(ProductStatus.SOLD_OUT)
        .views(0)
        .build();

    Product tradingProduct = Product.builder()
        .name("거래중 상품")
        .description("거래중인 상품")
        .price(new BigDecimal("100000"))
        .stockQuantity(1)
        .productStatus(ProductStatus.TRADING)
        .views(0)
        .build();

    // then
    assertThat(activeProduct.getProductStatus()).isEqualTo(ProductStatus.ACTIVE);
    assertThat(soldOutProduct.getProductStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    assertThat(tradingProduct.getProductStatus()).isEqualTo(ProductStatus.TRADING);
  }

  @Test
  @DisplayName("다양한 Condition으로 상품 생성 테스트")
  void createProductWithDifferentConditions() {
    // given & when
    Product newProduct = Product.builder()
        .name("새상품")
        .description("새상품 컨디션")
        .price(new BigDecimal("200000"))
        .stockQuantity(1)
        .productStatus(ProductStatus.ACTIVE)
        .condition(Condition.NEW)
        .views(0)
        .build();

    Product excellentProduct = Product.builder()
        .name("최상급 상품")
        .description("최상급 컨디션")
        .price(new BigDecimal("180000"))
        .stockQuantity(1)
        .productStatus(ProductStatus.ACTIVE)
        .condition(Condition.EXCELLENT)
        .views(0)
        .build();

    Product fairProduct = Product.builder()
        .name("중급 상품")
        .description("중급 컨디션")
        .price(new BigDecimal("80000"))
        .stockQuantity(1)
        .productStatus(ProductStatus.ACTIVE)
        .condition(Condition.FAIR)
        .views(0)
        .build();

    // then
    assertThat(newProduct.getCondition()).isEqualTo(Condition.NEW);
    assertThat(excellentProduct.getCondition()).isEqualTo(Condition.EXCELLENT);
    assertThat(fairProduct.getCondition()).isEqualTo(Condition.FAIR);
  }

  @Test
  @DisplayName("SKU 설정 테스트")
  void testSkuField() {
    // given & when
    Product productWithSku = Product.builder()
        .name("SKU 테스트 상품")
        .description("SKU가 있는 상품")
        .sku("VIN-DENIM-001")
        .price(new BigDecimal("150000"))
        .stockQuantity(1)
        .productStatus(ProductStatus.ACTIVE)
        .views(0)
        .build();

    // then
    assertThat(productWithSku.getSku()).isEqualTo("VIN-DENIM-001");
  }

  @Test
  @DisplayName("상품 상세 정보 필드 테스트")
  void testDetailFields() {
    // given & when - setUp의 product 사용
    
    // then
    assertThat(product.getBrand()).isEqualTo("리바이스");
    assertThat(product.getYearOfRelease()).isEqualTo("1985");
    assertThat(product.getSize()).isEqualTo("L");
    assertThat(product.getTexture()).isEqualTo("100% 코튼");
  }

  @Test
  @DisplayName("재고 0인 상품 생성 테스트")
  void createProductWithZeroStock() {
    // given & when
    Product zeroStockProduct = Product.builder()
        .name("재고없음 상품")
        .description("재고가 0인 상품")
        .price(new BigDecimal("50000"))
        .stockQuantity(0)
        .productStatus(ProductStatus.SOLD_OUT)
        .views(0)
        .build();

    // then
    assertThat(zeroStockProduct.getStockQuantity()).isEqualTo(0);
    assertThat(zeroStockProduct.getProductStatus()).isEqualTo(ProductStatus.SOLD_OUT);
  }

  @Test
  @DisplayName("높은 재고 수량 테스트")
  void createProductWithHighStock() {
    // given & when
    Product highStockProduct = Product.builder()
        .name("대량 재고 상품")
        .description("재고가 많은 상품")
        .price(new BigDecimal("10000"))
        .stockQuantity(999)
        .productStatus(ProductStatus.ACTIVE)
        .views(0)
        .build();

    // then
    assertThat(highStockProduct.getStockQuantity()).isEqualTo(999);
  }

  @Test
  @DisplayName("긴 상품명 테스트")
  void testLongProductName() {
    // given
    String longName = "이것은 매우 긴 상품명입니다. ".repeat(10); // 200자 근처
    
    // when
    Product longNameProduct = Product.builder()
        .name(longName)
        .description("긴 이름 테스트")
        .price(new BigDecimal("100000"))
        .stockQuantity(1)
        .productStatus(ProductStatus.ACTIVE)
        .views(0)
        .build();

    // then
    assertThat(longNameProduct.getName()).isEqualTo(longName);
    assertThat(longNameProduct.getName().length()).isLessThanOrEqualTo(200);
  }

  @Test
  @DisplayName("BaseTimeEntity 상속 확인 테스트")
  void testBaseTimeEntityInheritance() {
    // given & when - Product가 BaseTimeEntity를 상속받는지 확인
    
    // then - Product는 BaseTimeEntity의 메서드들을 가져야 함
    assertThat(product).isInstanceOf(com.oboe.backend.common.domain.BaseTimeEntity.class);
  }
}