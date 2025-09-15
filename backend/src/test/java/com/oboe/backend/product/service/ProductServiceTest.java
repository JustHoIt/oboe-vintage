package com.oboe.backend.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oboe.backend.common.exception.CustomException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;

import com.oboe.backend.product.dto.request.ProductCreateRequest;
import com.oboe.backend.product.dto.request.ProductImageRequest;
import com.oboe.backend.product.dto.request.ProductSearchRequest;
import com.oboe.backend.product.dto.request.ProductUpdateRequest;
import com.oboe.backend.product.dto.response.ProductListResponse;
import com.oboe.backend.product.dto.response.ProductResponse;
import com.oboe.backend.product.entity.Condition;
import com.oboe.backend.product.entity.Product;
import com.oboe.backend.product.entity.ProductCategory;
import com.oboe.backend.product.entity.ProductStatus;
import com.oboe.backend.product.repository.ProductCategoryRepository;
import com.oboe.backend.product.repository.ProductImageRepository;
import com.oboe.backend.product.repository.ProductRepository;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 테스트")
class ProductServiceTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private ProductCategoryRepository productCategoryRepository;

  @Mock
  private ProductImageRepository productImageRepository;

  @InjectMocks
  private ProductService productService;

  private Product testProduct;
  private ProductCategory testCategory;
  private ProductCreateRequest createRequest;
  private ProductUpdateRequest updateRequest;
  private Authentication adminAuthentication;
  private User adminUser;

  @BeforeEach
  void setUp() {
    // 테스트용 ADMIN 사용자
    adminUser = User.builder()
        .id(1L)
        .email("admin@example.com")
        .password("password")
        .name("관리자")
        .nickname("admin")
        .phoneNumber("010-0000-0000")
        .role(UserRole.ADMIN)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .build();

    // 테스트용 Authentication 객체
    adminAuthentication = new UsernamePasswordAuthenticationToken(
        adminUser, 
        null, 
        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
    );

    // 테스트용 카테고리
    testCategory = ProductCategory.builder()
        .id(1L)
        .name("상의")
        .level(1)
        .sortOrder(1)
        .build();

    // 테스트용 상품
    testProduct = Product.builder()
        .id(1L)
        .name("테스트 상품")
        .description("테스트 상품 설명")
        .price(new BigDecimal("100000"))
        .stockQuantity(10)
        .productStatus(ProductStatus.ACTIVE)
        .brand("테스트브랜드")
        .condition(Condition.EXCELLENT)
        .views(0)
        .build();

    // 상품 생성 요청 DTO
    createRequest = ProductCreateRequest.builder()
        .name("새로운 상품")
        .description("새로운 상품 설명")
        .categoryIds(Set.of(1L))
        .price(new BigDecimal("150000"))
        .stockQuantity(5)
        .productStatus(ProductStatus.ACTIVE)
        .brand("새브랜드")
        .condition(Condition.NEW)
        .images(List.of(ProductImageRequest.builder()
            .imageUrl("https://example.com/image.jpg")
            .sortOrder(1)
            .thumbnail(true)
            .build()))
        .build();

    // 상품 수정 요청 DTO
    updateRequest = ProductUpdateRequest.builder()
        .name("수정된 상품")
        .description("수정된 설명")
        .price(new BigDecimal("200000"))
        .build();
  }

  @Test
  @DisplayName("상품 생성 성공")
  void createProduct_Success() {
    // given
    given(productCategoryRepository.findById(1L)).willReturn(Optional.of(testCategory));
    given(productRepository.save(any(Product.class))).willReturn(testProduct);
    given(productImageRepository.saveAll(anyList())).willReturn(Collections.emptyList());

    // when
    ProductResponse result = productService.createProduct(createRequest, adminAuthentication);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo(testProduct.getName());

    verify(productCategoryRepository).findById(1L);
    verify(productRepository).save(any(Product.class));
    verify(productImageRepository).saveAll(anyList());
  }

  @Test
  @DisplayName("존재하지 않는 카테고리로 상품 생성 시 예외 발생")
  void createProduct_CategoryNotFound_ThrowsException() {
    // given
    given(productCategoryRepository.findById(1L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> productService.createProduct(createRequest, adminAuthentication))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining("존재하지 않는 카테고리입니다: 1");
  }

  @Test
  @DisplayName("상품 수정 성공")
  void updateProduct_Success() {
    // given
    given(productRepository.findById(1L)).willReturn(Optional.of(testProduct));
    given(productRepository.save(any(Product.class))).willReturn(testProduct);

    // when
    ProductResponse result = productService.updateProduct(1L, updateRequest, adminAuthentication);

    // then
    assertThat(result).isNotNull();
    verify(productRepository).findById(1L);
    verify(productRepository).save(testProduct);
  }

  @Test
  @DisplayName("존재하지 않는 상품 수정 시 예외 발생")
  void updateProduct_ProductNotFound_ThrowsException() {
    // given
    given(productRepository.findById(1L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> productService.updateProduct(1L, updateRequest, adminAuthentication))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining("존재하지 않는 상품입니다: 1");
  }

  @Test
  @DisplayName("상품 삭제 성공 (Soft Delete)")
  void deleteProduct_Success() {
    // given
    given(productRepository.findById(1L)).willReturn(Optional.of(testProduct));

    // when
    productService.deleteProduct(1L, adminAuthentication);

    // then
    verify(productRepository).findById(1L);
    verify(productRepository).softDeleteById(eq(1L), any(LocalDateTime.class));
  }

  @Test
  @DisplayName("상품 목록 조회 - 전체 조회")
  void getProducts_AllProducts_Success() {
    // given
    ProductSearchRequest searchRequest = ProductSearchRequest.builder()
        .page(0)
        .size(20)
        .build();

    Page<Product> productPage = new PageImpl<>(List.of(testProduct));
    given(productRepository.findAllExcludingStatus(eq(ProductStatus.INACTIVE), any(Pageable.class)))
        .willReturn(productPage);

    // when
    Page<ProductListResponse> result = productService.getProducts(searchRequest);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getName()).isEqualTo(testProduct.getName());

    verify(productRepository).findAllExcludingStatus(eq(ProductStatus.INACTIVE),
        any(Pageable.class));
  }

  @Test
  @DisplayName("상품 목록 조회 - 키워드 검색")
  void getProducts_WithKeyword_Success() {
    // given
    ProductSearchRequest searchRequest = ProductSearchRequest.builder()
        .keyword("테스트")
        .page(0)
        .size(20)
        .build();

    Page<Product> productPage = new PageImpl<>(List.of(testProduct));
    given(productRepository.searchByKeyword(eq("테스트"), any(Pageable.class)))
        .willReturn(productPage);

    // when
    Page<ProductListResponse> result = productService.getProducts(searchRequest);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);

    verify(productRepository).searchByKeyword(eq("테스트"), any(Pageable.class));
  }

  @Test
  @DisplayName("상품 목록 조회 - 필터 조건")
  void getProducts_WithFilters_Success() {
    // given
    ProductSearchRequest searchRequest = ProductSearchRequest.builder()
        .status(ProductStatus.ACTIVE)
        .sortBy("latest")
        .page(0)
        .size(20)
        .build();

    Page<Product> productPage = new PageImpl<>(List.of(testProduct));
    given(productRepository.searchByFilters(eq(searchRequest), any(Pageable.class)))
        .willReturn(productPage);

    // when
    Page<ProductListResponse> result = productService.getProducts(searchRequest);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);

    verify(productRepository).searchByFilters(eq(searchRequest), any(Pageable.class));
  }

  @Test
  @DisplayName("상품 목록 조회 - 키워드 + 필터")
  void getProducts_WithKeywordAndFilters_Success() {
    // given
    ProductSearchRequest searchRequest = ProductSearchRequest.builder()
        .keyword("테스트")
        .status(ProductStatus.ACTIVE)
        .sortBy("views")
        .page(0)
        .size(20)
        .build();

    Page<Product> productPage = new PageImpl<>(List.of(testProduct));
    given(productRepository.searchProducts(eq(searchRequest), any(Pageable.class)))
        .willReturn(productPage);

    // when
    Page<ProductListResponse> result = productService.getProducts(searchRequest);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);

    verify(productRepository).searchProducts(eq(searchRequest), any(Pageable.class));
  }

  @Test
  @DisplayName("상품 상세 조회 성공")
  void getProduct_Success() {
    // given
    given(productRepository.findByIdExcludingInactive(1L)).willReturn(Optional.of(testProduct));

    // when
    ProductResponse result = productService.getProductDetail(1L);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testProduct.getId());
    assertThat(result.getName()).isEqualTo(testProduct.getName());

    verify(productRepository).findByIdExcludingInactive(1L);
    verify(productRepository).incrementViews(1L);
  }

  @Test
  @DisplayName("존재하지 않는 상품 상세 조회 시 예외 발생")
  void getProduct_ProductNotFound_ThrowsException() {
    // given
    given(productRepository.findByIdExcludingInactive(1L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> productService.getProductDetail(1L))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining("상품을 찾을 수 없습니다: 1");
  }

  @Test
  @DisplayName("인기 상품 목록 조회")
  void getPopularProducts_Success() {
    // given
    List<Product> products = List.of(testProduct);
    given(productRepository.findTopByViewsExcludingInactive(any(Pageable.class)))
        .willReturn(products);

    // when
    List<ProductListResponse> result = productService.getPopularProducts(10);

    // then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo(testProduct.getName());

    verify(productRepository).findTopByViewsExcludingInactive(any(Pageable.class));
  }

  @Test
  @DisplayName("최신 상품 목록 조회")
  void getLatestProducts_Success() {
    // given
    List<Product> products = List.of(testProduct);
    given(productRepository.findLatestProductsExcludingInactive(any(Pageable.class)))
        .willReturn(products);

    // when
    List<ProductListResponse> result = productService.getLatestProducts(10);

    // then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo(testProduct.getName());

    verify(productRepository).findLatestProductsExcludingInactive(any(Pageable.class));
  }

  @Test
  @DisplayName("페이지 기본값 설정 테스트")
  void getProducts_DefaultPagination() {
    // given
    ProductSearchRequest searchRequest = ProductSearchRequest.builder().build();

    Page<Product> productPage = new PageImpl<>(List.of(testProduct));
    given(productRepository.findAllExcludingStatus(eq(ProductStatus.INACTIVE), any(Pageable.class)))
        .willReturn(productPage);

    // when
    Page<ProductListResponse> result = productService.getProducts(searchRequest);

    // then
    assertThat(result).isNotNull();

    // 기본값 확인을 위한 ArgumentCaptor 사용
    verify(productRepository).findAllExcludingStatus(eq(ProductStatus.INACTIVE),
        argThat(pageable -> {
          return pageable.getPageNumber() == 0 && pageable.getPageSize() == 20;
        }));
  }

  @Test
  @DisplayName("상품 이미지 없이 생성")
  void createProduct_WithoutImages_Success() {
    // given
    ProductCreateRequest requestWithoutImages = ProductCreateRequest.builder()
        .name("이미지 없는 상품")
        .description("이미지 없는 상품 설명")
        .price(new BigDecimal("100000"))
        .stockQuantity(1)
        .productStatus(ProductStatus.ACTIVE)
        .build();

    given(productRepository.save(any(Product.class))).willReturn(testProduct);

    // when
    ProductResponse result = productService.createProduct(requestWithoutImages, adminAuthentication);

    // then
    assertThat(result).isNotNull();
    verify(productRepository).save(any(Product.class));
    verify(productImageRepository, never()).saveAll(anyList());
  }

  @Test
  @DisplayName("ADMIN 권한이 없는 사용자의 상품 생성 시 예외 발생")
  void createProduct_NonAdminUser_ThrowsException() {
    // given
    User normalUser = User.builder()
        .id(2L)
        .email("user@example.com")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .build();

    Authentication userAuthentication = new UsernamePasswordAuthenticationToken(
        normalUser, 
        null, 
        List.of(new SimpleGrantedAuthority("ROLE_USER"))
    );

    // when & then
    assertThatThrownBy(() -> productService.createProduct(createRequest, userAuthentication))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining("접근 권한이 없습니다");
  }

  @Test
  @DisplayName("인증되지 않은 사용자의 상품 생성 시 예외 발생")
  void createProduct_UnauthenticatedUser_ThrowsException() {
    // when & then
    assertThatThrownBy(() -> productService.createProduct(createRequest, null))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining("접근 권한이 없습니다");
  }

  @Test
  @DisplayName("ADMIN 권한이 없는 사용자의 상품 수정 시 예외 발생")
  void updateProduct_NonAdminUser_ThrowsException() {
    // given
    User normalUser = User.builder()
        .id(2L)
        .email("user@example.com")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .build();

    Authentication userAuthentication = new UsernamePasswordAuthenticationToken(
        normalUser, 
        null, 
        List.of(new SimpleGrantedAuthority("ROLE_USER"))
    );

    // when & then
    assertThatThrownBy(() -> productService.updateProduct(1L, updateRequest, userAuthentication))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining("접근 권한이 없습니다");
  }

  @Test
  @DisplayName("ADMIN 권한이 없는 사용자의 상품 삭제 시 예외 발생")
  void deleteProduct_NonAdminUser_ThrowsException() {
    // given
    User normalUser = User.builder()
        .id(2L)
        .email("user@example.com")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .build();

    Authentication userAuthentication = new UsernamePasswordAuthenticationToken(
        normalUser, 
        null, 
        List.of(new SimpleGrantedAuthority("ROLE_USER"))
    );

    // when & then
    assertThatThrownBy(() -> productService.deleteProduct(1L, userAuthentication))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining("접근 권한이 없습니다");
  }

  @Test
  @DisplayName("음수 가격으로 상품 생성 시 예외 발생")
  void createProduct_NegativePrice_ThrowsException() {
    // given
    ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
        .name("테스트 상품")
        .price(BigDecimal.valueOf(-1000))
        .stockQuantity(10)
        .productStatus(ProductStatus.ACTIVE)
        .build();

    // when & then
    assertThatThrownBy(() -> productService.createProduct(invalidRequest, adminAuthentication))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining("상품 가격은 0 이상이어야 합니다");
  }

  @Test
  @DisplayName("음수 재고로 상품 생성 시 예외 발생")
  void createProduct_NegativeStock_ThrowsException() {
    // given
    ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
        .name("테스트 상품")
        .price(BigDecimal.valueOf(10000))
        .stockQuantity(-5)
        .productStatus(ProductStatus.ACTIVE)
        .build();

    // when & then
    assertThatThrownBy(() -> productService.createProduct(invalidRequest, adminAuthentication))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining("재고 수량은 0 이상이어야 합니다");
  }

  @Test
  @DisplayName("이미 비활성화된 상품 삭제 시 예외 발생")
  void deleteProduct_AlreadyInactive_ThrowsException() {
    // given
    Product inactiveProduct = Product.builder()
        .id(1L)
        .name("비활성 상품")
        .productStatus(ProductStatus.INACTIVE)
        .build();

    given(productRepository.findById(1L)).willReturn(Optional.of(inactiveProduct));

    // when & then
    assertThatThrownBy(() -> productService.deleteProduct(1L, adminAuthentication))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining("이미 비활성화된 상품입니다");
  }
}
