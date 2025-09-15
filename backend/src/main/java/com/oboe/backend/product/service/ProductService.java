package com.oboe.backend.product.service;

import com.oboe.backend.common.exception.CustomException;
import com.oboe.backend.common.exception.ErrorCode;
import com.oboe.backend.product.dto.request.ProductCreateRequest;
import com.oboe.backend.product.dto.request.ProductImageRequest;
import com.oboe.backend.product.dto.request.ProductSearchRequest;
import com.oboe.backend.product.dto.request.ProductUpdateRequest;
import com.oboe.backend.product.dto.response.ProductCategoryResponse;
import com.oboe.backend.product.dto.response.ProductImageResponse;
import com.oboe.backend.product.dto.response.ProductListResponse;
import com.oboe.backend.product.dto.response.ProductResponse;
import com.oboe.backend.product.entity.Product;
import com.oboe.backend.product.entity.ProductCategory;
import com.oboe.backend.product.entity.ProductImage;
import com.oboe.backend.product.entity.ProductStatus;
import com.oboe.backend.product.repository.ProductCategoryRepository;
import com.oboe.backend.product.repository.ProductImageRepository;
import com.oboe.backend.product.repository.ProductRepository;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductCategoryRepository productCategoryRepository;
  private final ProductImageRepository productImageRepository;

  /**
   * 상품 생성 (ADMIN 권한 필요)
   */
  @Transactional
  public ProductResponse createProduct(ProductCreateRequest request,
      Authentication authentication) {
    log.info("상품 생성 요청: {}", request.getName());

    // ADMIN 권한 검증
    validateAdminUser(authentication);

    // 비즈니스 로직 검증
    validateProductRequest(request.getPrice(), request.getStockQuantity());

    // 카테고리 조회 및 검증
    Set<ProductCategory> categories = validateAndGetCategories(request.getCategoryIds());

    // 상품 엔티티 생성
    Product product = createProductBuilder(request, categories);

    Product savedProduct = productRepository.save(product);

    // 상품 이미지 생성 및 저장
    if (request.getImages() != null && !request.getImages().isEmpty()) {
      validateProductImages(request.getImages());
      List<ProductImage> images = createProductImages(savedProduct, request.getImages());
      productImageRepository.saveAll(images);
      savedProduct.getProductImages().addAll(images);
    }

    log.info("상품 생성 완료: ID={}, 이름={}", savedProduct.getId(), savedProduct.getName());
    return convertToProductResponse(savedProduct);
  }

  private Product createProductBuilder(ProductCreateRequest request,
      Set<ProductCategory> categories) {
    return Product.builder()
        .name(request.getName())
        .description(request.getDescription())
        .categories(categories)
        .sku(request.getSku())
        .price(request.getPrice())
        .stockQuantity(request.getStockQuantity())
        .productStatus(request.getProductStatus())
        .brand(request.getBrand())
        .yearOfRelease(request.getYearOfRelease())
        .size(request.getSize())
        .texture(request.getTexture())
        .condition(request.getCondition())
        .views(0)
        .build();
  }

  /**
   * 상품 수정 (ADMIN 권한 필요)
   */
  @Transactional
  public ProductResponse updateProduct(Long productId, ProductUpdateRequest request,
      Authentication authentication) {
    log.info("상품 수정 요청: ID={}", productId);

    // ADMIN 권한 검증
    validateAdminUser(authentication);

    // Product 존재 검증
    Product product = getProduct(productId);

    // 비즈니스 로직 검증
    validateProductRequest(request.getPrice(), request.getStockQuantity());

    // 상품 정보 업데이트
    updateProductFields(product, request);

    // 카테고리 업데이트
    if (request.getCategoryIds() != null) {
      Set<ProductCategory> categories = validateAndGetCategories(request.getCategoryIds());
      product.updateCategories(categories);
    }

    // 이미지 업데이트
    if (request.getImages() != null) {
      validateProductImages(request.getImages());

      // 기존 이미지 삭제
      productImageRepository.deleteByProductId(productId);

      // 새 이미지 추가
      if (!request.getImages().isEmpty()) {
        List<ProductImage> images = createProductImages(product, request.getImages());
        productImageRepository.saveAll(images);
      }
    }

    Product savedProduct = productRepository.save(product);
    log.info("상품 수정 완료: ID={}, 이름={}", savedProduct.getId(), savedProduct.getName());

    return convertToProductResponse(savedProduct);
  }

  private void updateProductFields(Product product, ProductUpdateRequest request) {
    product.updateProductInfo(
        request.getName(),
        request.getDescription(),
        request.getSku(),
        request.getPrice(),
        request.getStockQuantity(),
        request.getProductStatus(),
        request.getBrand(),
        request.getYearOfRelease(),
        request.getSize(),
        request.getTexture(),
        request.getCondition()
    );
  }

  /**
   * 상품 삭제 (Soft Delete, ADMIN 권한 필요)
   */
  @Transactional
  public void deleteProduct(Long productId, Authentication authentication) {
    log.info("상품 삭제 요청: ID={}", productId);

    // ADMIN 권한 검증
    validateAdminUser(authentication);

    // Product 존재 검증
    Product product = getProduct(productId);

    // 삭제 가능 여부 검증
    validateProductDeletion(product);

    // Soft Delete - 상품 상태를 INACTIVE로 변경
    product.changeStatus(ProductStatus.INACTIVE);
    productRepository.softDeleteById(productId, LocalDateTime.now());

    log.info("상품 삭제 완료: ID={}, 이름={}", productId, product.getName());
  }

  /**
   * 상품 목록 조회 (복합 검색)
   */
  public Page<ProductListResponse> getProducts(ProductSearchRequest searchRequest) {
    log.info("상품 목록 조회: 검색어={}, 상태={}, 정렬={}",
        searchRequest.getKeyword(), searchRequest.getStatus(), searchRequest.getSortBy());

    // 페이지 설정
    int page = searchRequest.getPage() != null ? searchRequest.getPage() : 0;
    int size = searchRequest.getSize() != null ? searchRequest.getSize() : 20;
    Pageable pageable = PageRequest.of(page, size);

    Page<Product> products;

    /*
     * 검색 조건에 따른 분기 처리
     * 1. 검색어 + 필터: 복합 검색
     * 2. 검색어만: 키워드 검색
     * 3. 필터만: 조건 필터링
     * 4. 조건 없음: 전체 조회
     */
    if (hasSearchKeyword(searchRequest) && hasFilters(searchRequest)) {
      products = productRepository.searchProducts(searchRequest, pageable);
    } else if (hasSearchKeyword(searchRequest)) {
      products = productRepository.searchByKeyword(searchRequest.getKeyword(), pageable);
    } else if (hasFilters(searchRequest)) {
      products = productRepository.searchByFilters(searchRequest, pageable);
    } else {
      products = productRepository.findAllExcludingStatus(ProductStatus.INACTIVE, pageable);
    }

    return products.map(this::convertToProductListResponse);
  }

  /**
   * 상품 상세 조회
   */
  @Transactional
  public ProductResponse getProductDetail(Long productId) {
    log.info("상품 상세 조회: ID={}", productId);

    Product product = productRepository.findByIdExcludingInactive(productId)
        .orElseThrow(
            () -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND, "상품을 찾을 수 없습니다: " + productId));

    /*
     * 조회수 증가 처리
     * - 실패 시에도 상품 조회는 정상적으로 진행
     * - 에러 로깅으로 모니터링 가능
     */
    try {
      productRepository.incrementViews(productId);
      log.debug("조회수 증가 완료: ID={}", productId);
    } catch (Exception e) {
      log.warn("조회수 증가 실패: ID={}, 오류={}", productId, e.getMessage());
    }

    return convertToProductResponse(product);
  }

  /**
   * 인기 상품 목록 조회 (조회수 기준)
   */
  public List<ProductListResponse> getPopularProducts(int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    List<Product> products = productRepository.findTopByViewsExcludingInactive(pageable);
    return products.stream()
        .map(this::convertToProductListResponse)
        .collect(Collectors.toList());
  }

  /**
   * 최신 상품 목록 조회
   */
  public List<ProductListResponse> getLatestProducts(int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    List<Product> products = productRepository.findLatestProductsExcludingInactive(pageable);
    return products.stream()
        .map(this::convertToProductListResponse)
        .collect(Collectors.toList());
  }

  // ===== Private Helper Methods =====

  /**
   * ADMIN 권한 사용자 검증
   */
  private void validateAdminUser(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new CustomException(ErrorCode.FORBIDDEN, "접근 권한이 없습니다.");
    }

    User user = (User) authentication.getPrincipal();
    if (user == null || !UserRole.ADMIN.equals(user.getRole())) {
      throw new CustomException(ErrorCode.FORBIDDEN, "접근 권한이 없습니다.");
    }

    log.info("ADMIN 사용자 검증 완료: {}", user.getEmail());
  }

  /**
   * Product 존재 검증 (비활성 상품 포함)
   */
  private Product getProduct(Long productId) {
    if (productId == null || productId <= 0) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "올바르지 않은 상품 ID입니다: " + productId);
    }

    return productRepository.findById(productId)
        .orElseThrow(
            () -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND, "존재하지 않는 상품입니다: " + productId));
  }

  /**
   * 상품 생성 요청 검증
   */
  private void validateProductRequest(BigDecimal price, Integer quantity) {

    // 가격 검증
    if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
      throw new CustomException(ErrorCode.PRODUCT_INVALID_PRICE, "상품 가격은 0 이상이어야 합니다.");
    }

    // 재고 검증
    if (quantity != null && quantity < 0) {
      throw new CustomException(ErrorCode.PRODUCT_INVALID_STOCK, "재고 수량은 0 이상이어야 합니다.");
    }
  }

  /**
   * 상품 삭제 가능 여부 검증
   */
  private void validateProductDeletion(Product product) {
    if (ProductStatus.INACTIVE.equals(product.getProductStatus())) {
      throw new CustomException(ErrorCode.PRODUCT_INACTIVE, "이미 비활성화된 상품입니다.");
    }
  }

  /**
   * 카테고리 조회 및 검증
   */
  private Set<ProductCategory> validateAndGetCategories(Set<Long> categoryIds) {
    if (categoryIds == null || categoryIds.isEmpty()) {
      return new HashSet<>();
    }

    Set<ProductCategory> categories = new HashSet<>();
    for (Long categoryId : categoryIds) {
      ProductCategory category = productCategoryRepository.findById(categoryId)
          .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND,
              "존재하지 않는 카테고리입니다: " + categoryId));
      categories.add(category);
    }
    return categories;
  }

  /**
   * 상품 이미지 검증
   */
  private void validateProductImages(List<ProductImageRequest> images) {
    if (images == null) {
      return;
    }

    /*
     * 상품 이미지 유효성 검증
     * 1. 최대 개수 제한 (10개)
     * 2. 썸네일 이미지 개수 제한 (1개)
     * 3. 이미지 URL 필수 체크
     */
    final int MAX_IMAGES = 10;
    if (images.size() > MAX_IMAGES) {
      throw new CustomException(ErrorCode.PRODUCT_IMAGE_LIMIT_EXCEEDED,
          String.format("상품 이미지는 최대 %d개까지 등록할 수 있습니다.", MAX_IMAGES));
    }

    long thumbnailCount = images.stream()
        .mapToInt(img -> img.isThumbnail() ? 1 : 0)
        .sum();

    if (thumbnailCount > 1) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "썸네일 이미지는 1개만 설정할 수 있습니다.");
    }

    for (ProductImageRequest image : images) {
      if (image.getImageUrl() == null || image.getImageUrl().trim().isEmpty()) {
        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이미지 URL은 필수입니다.");
      }
    }
  }

  /**
   * 상품 이미지 생성
   */
  private List<ProductImage> createProductImages(Product product,
      List<ProductImageRequest> imageRequests) {
    return imageRequests.stream()
        .map(imageRequest -> ProductImage.builder()
            .product(product)
            .imageUrl(imageRequest.getImageUrl())
            .sortOrder(imageRequest.getSortOrder() != null ? imageRequest.getSortOrder() : 0)
            .thumbnail(imageRequest.isThumbnail())
            .build())
        .collect(Collectors.toList());
  }

  private boolean hasSearchKeyword(ProductSearchRequest request) {
    return request.getKeyword() != null && !request.getKeyword().trim().isEmpty();
  }

  private boolean hasFilters(ProductSearchRequest request) {
    return request.getStatus() != null ||
        request.getCategoryId() != null ||
        request.getBrand() != null ||
        request.getCondition() != null ||
        request.getSortBy() != null;
  }

  private ProductResponse convertToProductResponse(Product product) {
    return ProductResponse.builder()
        .id(product.getId())
        .name(product.getName())
        .description(product.getDescription())
        .categories(product.getCategories().stream()
            .map(this::convertToCategoryResponse)
            .collect(Collectors.toSet()))
        .sku(product.getSku())
        .price(product.getPrice())
        .stockQuantity(product.getStockQuantity())
        .images(product.getProductImages().stream()
            .map(this::convertToImageResponse)
            .collect(Collectors.toList()))
        .productStatus(product.getProductStatus())
        .brand(product.getBrand())
        .yearOfRelease(product.getYearOfRelease())
        .size(product.getSize())
        .texture(product.getTexture())
        .condition(product.getCondition())
        .views(product.getViews())
        .createdAt(product.getCreatedAt())
        .updatedAt(product.getUpdatedAt())
        .build();
  }

  private ProductListResponse convertToProductListResponse(Product product) {
    String thumbnailImage = product.getProductImages().stream()
        .filter(ProductImage::isThumbnail)
        .findFirst()
        .map(ProductImage::getImageUrl)
        .orElse(product.getProductImages().stream()
            .findFirst()
            .map(ProductImage::getImageUrl)
            .orElse(null));

    return ProductListResponse.builder()
        .id(product.getId())
        .name(product.getName())
        .price(product.getPrice())
        .productStatus(product.getProductStatus())
        .brand(product.getBrand())
        .condition(product.getCondition())
        .views(product.getViews())
        .thumbnailImage(thumbnailImage)
        .createdAt(product.getCreatedAt())
        .build();
  }

  private ProductCategoryResponse convertToCategoryResponse(ProductCategory category) {
    return ProductCategoryResponse.builder()
        .id(category.getId())
        .name(category.getName())
        .level(category.getLevel())
        .description(category.getDescription())
        .build();
  }

  private ProductImageResponse convertToImageResponse(ProductImage image) {
    return ProductImageResponse.builder()
        .id(image.getId())
        .imageUrl(image.getImageUrl())
        .sortOrder(image.getSortOrder())
        .thumbnail(image.isThumbnail())
        .build();
  }
}
