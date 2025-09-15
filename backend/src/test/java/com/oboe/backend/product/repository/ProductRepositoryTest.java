package com.oboe.backend.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.oboe.backend.config.JpaConfig;
import com.oboe.backend.product.entity.Condition;
import com.oboe.backend.product.entity.Product;
import com.oboe.backend.product.entity.ProductCategory;
import com.oboe.backend.product.entity.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
@DisplayName("ProductRepository 테스트")
class ProductRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private ProductCategoryRepository productCategoryRepository;

  private Product activeProduct;
  private Product inactiveProduct;
  private ProductCategory category;

  @BeforeEach
  void setUp() {
    // 테스트용 카테고리 생성
    category = ProductCategory.builder()
        .name("테스트 카테고리")
        .level(1)
        .sortOrder(1)
        .build();
    productCategoryRepository.save(category);

    // 활성 상품 생성
    activeProduct = Product.builder()
        .name("활성 상품")
        .description("활성 상품 설명")
        .price(new BigDecimal("100000"))
        .stockQuantity(10)
        .productStatus(ProductStatus.ACTIVE)
        .brand("테스트브랜드")
        .condition(Condition.EXCELLENT)
        .views(5)
        .build();
    activeProduct.getCategories().add(category);
    productRepository.save(activeProduct);

    // 비활성 상품 생성
    inactiveProduct = Product.builder()
        .name("비활성 상품")
        .description("비활성 상품 설명")
        .price(new BigDecimal("50000"))
        .stockQuantity(0)
        .productStatus(ProductStatus.INACTIVE)
        .brand("테스트브랜드")
        .views(2)
        .build();
    productRepository.save(inactiveProduct);

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @DisplayName("INACTIVE 상태 제외하고 전체 상품 조회")
  void findAllExcludingStatus() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Product> result = productRepository.findAllExcludingStatus(ProductStatus.INACTIVE,
        pageable);

    // then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getName()).isEqualTo("활성 상품");
    assertThat(result.getContent().get(0).getProductStatus()).isNotEqualTo(ProductStatus.INACTIVE);
  }

  @Test
  @DisplayName("ID로 조회 시 INACTIVE 제외")
  void findByIdExcludingInactive() {
    // when
    Optional<Product> activeResult = productRepository.findByIdExcludingInactive(
        activeProduct.getId());
    Optional<Product> inactiveResult = productRepository.findByIdExcludingInactive(
        inactiveProduct.getId());

    // then
    assertThat(activeResult).isPresent();
    assertThat(activeResult.get().getName()).isEqualTo("활성 상품");

    assertThat(inactiveResult).isEmpty(); // INACTIVE 상품은 조회되지 않음
  }

  @Test
  @DisplayName("상품 상태별 조회")
  void findByProductStatus() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Product> activeProducts = productRepository.findByProductStatus(ProductStatus.ACTIVE,
        pageable);
    Page<Product> inactiveProducts = productRepository.findByProductStatus(ProductStatus.INACTIVE,
        pageable);

    // then
    assertThat(activeProducts.getContent()).hasSize(1);
    assertThat(activeProducts.getContent().get(0).getProductStatus()).isEqualTo(
        ProductStatus.ACTIVE);

    assertThat(inactiveProducts.getContent()).hasSize(1);
    assertThat(inactiveProducts.getContent().get(0).getProductStatus()).isEqualTo(
        ProductStatus.INACTIVE);
  }

  @Test
  @DisplayName("브랜드별 조회 (INACTIVE 제외)")
  void findByBrandExcludingInactive() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Product> result = productRepository.findByBrandExcludingInactive("테스트브랜드", pageable);

    // then
    assertThat(result.getContent()).hasSize(1); // INACTIVE 제외하고 1개만 조회
    assertThat(result.getContent().get(0).getBrand()).isEqualTo("테스트브랜드");
    assertThat(result.getContent().get(0).getProductStatus()).isNotEqualTo(ProductStatus.INACTIVE);
  }

  @Test
  @DisplayName("카테고리별 조회 (INACTIVE 제외)")
  void findByCategoryIdExcludingInactive() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Product> result = productRepository.findByCategoryIdExcludingInactive(category.getId(),
        pageable);

    // then
    assertThat(result.getContent()).hasSize(1); // 카테고리가 설정된 활성 상품만 조회
    assertThat(result.getContent().get(0).getName()).isEqualTo("활성 상품");
  }

  @Test
  @DisplayName("조회수 증가")
  void incrementViews() {
    // given
    int initialViews = activeProduct.getViews();

    // when
    productRepository.incrementViews(activeProduct.getId());
    entityManager.flush();
    entityManager.clear();

    // then
    Product updatedProduct = productRepository.findById(activeProduct.getId()).orElse(null);
    assertThat(updatedProduct).isNotNull();
    assertThat(updatedProduct.getViews()).isEqualTo(initialViews + 1);
  }

  @Test
  @DisplayName("Soft Delete")
  void softDeleteById() {
    // given
    LocalDateTime deletedAt = LocalDateTime.now();

    // when
    productRepository.softDeleteById(activeProduct.getId(), deletedAt);
    entityManager.flush();
    entityManager.clear();

    // then
    Product deletedProduct = productRepository.findById(activeProduct.getId()).orElse(null);
    assertThat(deletedProduct).isNotNull();
    assertThat(deletedProduct.getProductStatus()).isEqualTo(ProductStatus.INACTIVE);
  }

  @Test
  @DisplayName("상품명으로 검색 (INACTIVE 제외)")
  void findByNameContainingExcludingInactive() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Product> result = productRepository.findByNameContainingExcludingInactive("활성", pageable);

    // then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getName()).contains("활성");
  }

  @Test
  @DisplayName("설명으로 검색 (INACTIVE 제외)")
  void findByDescriptionContainingExcludingInactive() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Product> result = productRepository.findByDescriptionContainingExcludingInactive("활성",
        pageable);

    // then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getDescription()).contains("활성");
  }

  @Test
  @DisplayName("키워드로 검색 (상품명, 설명, 브랜드)")
  void findByKeywordExcludingInactive() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Product> nameResult = productRepository.findByKeywordExcludingInactive("활성", pageable);
    Page<Product> brandResult = productRepository.findByKeywordExcludingInactive("테스트브랜드",
        pageable);

    // then
    assertThat(nameResult.getContent()).hasSize(1);
    assertThat(brandResult.getContent()).hasSize(1); // 활성 상품만 조회됨
  }

  @Test
  @DisplayName("조회수 Top N 상품 조회")
  void findTopByViewsExcludingInactive() {
    // given
    // 추가 상품 생성 (더 높은 조회수)
    Product highViewProduct = Product.builder()
        .name("인기 상품")
        .description("인기 상품 설명")
        .price(new BigDecimal("200000"))
        .stockQuantity(5)
        .productStatus(ProductStatus.ACTIVE)
        .views(10)
        .build();
    productRepository.save(highViewProduct);
    entityManager.flush();

    Pageable pageable = PageRequest.of(0, 2);

    // when
    List<Product> result = productRepository.findTopByViewsExcludingInactive(pageable);

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getViews()).isGreaterThanOrEqualTo(
        result.get(1).getViews()); // 조회수 내림차순
    assertThat(result.get(0).getName()).isEqualTo("인기 상품");
  }

  @Test
  @DisplayName("최신 상품 조회")
  void findLatestProductsExcludingInactive() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    List<Product> result = productRepository.findLatestProductsExcludingInactive(pageable);

    // then
    assertThat(result).hasSize(1); // INACTIVE 제외하고 1개
    assertThat(result.get(0).getName()).isEqualTo("활성 상품");
  }

  @Test
  @DisplayName("존재하지 않는 ID로 조회")
  void findByIdExcludingInactive_NotFound() {
    // when
    Optional<Product> result = productRepository.findByIdExcludingInactive(999L);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("존재하지 않는 브랜드로 조회")
  void findByBrandExcludingInactive_NotFound() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Product> result = productRepository.findByBrandExcludingInactive("존재하지않는브랜드", pageable);

    // then
    assertThat(result.getContent()).isEmpty();
  }

  @Test
  @DisplayName("빈 키워드로 검색")
  void findByKeywordExcludingInactive_EmptyKeyword() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Product> result = productRepository.findByKeywordExcludingInactive("", pageable);

    // then
    // 빈 키워드로도 검색 가능 (모든 상품이 빈 문자열을 포함)
    assertThat(result.getContent()).hasSize(1);
  }
}
