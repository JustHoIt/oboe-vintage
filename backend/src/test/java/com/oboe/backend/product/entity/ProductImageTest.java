package com.oboe.backend.product.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProductImage Entity 테스트")
class ProductImageTest {

  private Product product;
  private ProductImage productImage;

  @BeforeEach
  void setUp() {
    // 테스트용 Product 생성
    product = Product.builder()
        .name("이미지 테스트 상품")
        .description("이미지가 있는 테스트 상품입니다.")
        .price(new BigDecimal("150000"))
        .stockQuantity(1)
        .productStatus(ProductStatus.ACTIVE)
        .brand("테스트브랜드")
        .condition(Condition.EXCELLENT)
        .views(0)
        .build();

    // 기본 테스트용 ProductImage 생성
    productImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/images/product1.jpg")
        .sortOrder(1)
        .thumbnail(true)
        .build();
  }

  @Test
  @DisplayName("ProductImage 기본 생성 테스트")
  void createBasicProductImage() {
    // given & when - setUp에서 생성됨

    // then
    assertThat(productImage.getProduct()).isEqualTo(product);
    assertThat(productImage.getImageUrl()).isEqualTo("https://example.com/images/product1.jpg");
    assertThat(productImage.getSortOrder()).isEqualTo(1);
    assertThat(productImage.isThumbnail()).isTrue();
  }

  @Test
  @DisplayName("ProductImage 필수 필드만으로 생성 테스트")
  void createProductImageWithMinimalFields() {
    // given & when
    ProductImage minimalImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/minimal.jpg")
        .build();

    // then
    assertThat(minimalImage.getProduct()).isEqualTo(product);
    assertThat(minimalImage.getImageUrl()).isEqualTo("https://example.com/minimal.jpg");
    assertThat(minimalImage.getSortOrder()).isEqualTo(0); // 기본값
    assertThat(minimalImage.isThumbnail()).isFalse(); // 기본값
  }

  @Test
  @DisplayName("썸네일 이미지 설정 테스트")
  void testThumbnailImage() {
    // given & when
    ProductImage thumbnailImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/thumbnail.jpg")
        .sortOrder(0)
        .thumbnail(true)
        .build();

    ProductImage regularImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/regular.jpg")
        .sortOrder(1)
        .thumbnail(false)
        .build();

    // then
    assertThat(thumbnailImage.isThumbnail()).isTrue();
    assertThat(regularImage.isThumbnail()).isFalse();
  }

  @Test
  @DisplayName("이미지 정렬 순서 테스트")
  void testImageSortOrder() {
    // given & when
    ProductImage firstImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/first.jpg")
        .sortOrder(1)
        .thumbnail(true)
        .build();

    ProductImage secondImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/second.jpg")
        .sortOrder(2)
        .thumbnail(false)
        .build();

    ProductImage thirdImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/third.jpg")
        .sortOrder(3)
        .thumbnail(false)
        .build();

    // then
    assertThat(firstImage.getSortOrder()).isEqualTo(1);
    assertThat(secondImage.getSortOrder()).isEqualTo(2);
    assertThat(thirdImage.getSortOrder()).isEqualTo(3);

    // 정렬 순서 비교
    assertThat(firstImage.getSortOrder()).isLessThan(secondImage.getSortOrder());
    assertThat(secondImage.getSortOrder()).isLessThan(thirdImage.getSortOrder());
  }

  @Test
  @DisplayName("기본 정렬 순서 값 테스트")
  void testDefaultSortOrder() {
    // given & when
    ProductImage defaultOrderImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/default.jpg")
        .build();

    // then - 기본값은 0
    assertThat(defaultOrderImage.getSortOrder()).isEqualTo(0);
  }

  @Test
  @DisplayName("다양한 이미지 URL 형식 테스트")
  void testVariousImageUrlFormats() {
    // given & when
    ProductImage httpImage = ProductImage.builder()
        .product(product)
        .imageUrl("http://example.com/image.jpg")
        .sortOrder(1)
        .build();

    ProductImage httpsImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://cdn.example.com/images/product/large/image.png")
        .sortOrder(2)
        .build();

    ProductImage s3Image = ProductImage.builder()
        .product(product)
        .imageUrl("https://my-bucket.s3.amazonaws.com/products/image.webp")
        .sortOrder(3)
        .build();

    // then
    assertThat(httpImage.getImageUrl()).startsWith("http://");
    assertThat(httpsImage.getImageUrl()).startsWith("https://");
    assertThat(s3Image.getImageUrl()).contains("s3.amazonaws.com");

    assertThat(httpImage.getImageUrl()).endsWith(".jpg");
    assertThat(httpsImage.getImageUrl()).endsWith(".png");
    assertThat(s3Image.getImageUrl()).endsWith(".webp");
  }

  @Test
  @DisplayName("긴 이미지 URL 테스트")
  void testLongImageUrl() {
    // given
    String longUrl = "https://very-long-domain-name-for-testing.example.com/very/long/path/to/images/with/many/subdirectories/and/a/very/long/filename/product-image-with-detailed-description.jpg";

    // when
    ProductImage longUrlImage = ProductImage.builder()
        .product(product)
        .imageUrl(longUrl)
        .sortOrder(1)
        .build();

    // then
    assertThat(longUrlImage.getImageUrl()).isEqualTo(longUrl);
    assertThat(longUrlImage.getImageUrl().length()).isLessThanOrEqualTo(512); // 제한사항 확인
  }

  @Test
  @DisplayName("Product와 ProductImage 연관관계 테스트")
  void testProductImageRelationship() {
    // given & when - setUp의 productImage 사용

    // then
    assertThat(productImage.getProduct()).isNotNull();
    assertThat(productImage.getProduct()).isEqualTo(product);
    assertThat(productImage.getProduct().getName()).isEqualTo("이미지 테스트 상품");
  }

  @Test
  @DisplayName("같은 상품의 여러 이미지 테스트")
  void testMultipleImagesForSameProduct() {
    // given & when
    ProductImage image1 = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/image1.jpg")
        .sortOrder(1)
        .thumbnail(true)
        .build();

    ProductImage image2 = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/image2.jpg")
        .sortOrder(2)
        .thumbnail(false)
        .build();

    ProductImage image3 = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/image3.jpg")
        .sortOrder(3)
        .thumbnail(false)
        .build();

    // then
    assertThat(image1.getProduct()).isEqualTo(product);
    assertThat(image2.getProduct()).isEqualTo(product);
    assertThat(image3.getProduct()).isEqualTo(product);

    // 모든 이미지가 같은 상품을 참조하는지 확인
    assertThat(image1.getProduct()).isEqualTo(image2.getProduct());
    assertThat(image2.getProduct()).isEqualTo(image3.getProduct());
  }

  @Test
  @DisplayName("다른 상품의 이미지 테스트")
  void testImagesForDifferentProducts() {
    // given
    Product anotherProduct = Product.builder()
        .name("다른 상품")
        .description("다른 테스트 상품")
        .price(new BigDecimal("200000"))
        .stockQuantity(1)
        .productStatus(ProductStatus.ACTIVE)
        .views(0)
        .build();

    // when
    ProductImage image1 = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/product1.jpg")
        .sortOrder(1)
        .build();

    ProductImage image2 = ProductImage.builder()
        .product(anotherProduct)
        .imageUrl("https://example.com/product2.jpg")
        .sortOrder(1)
        .build();

    // then
    assertThat(image1.getProduct()).isNotEqualTo(image2.getProduct());
    assertThat(image1.getProduct().getName()).isEqualTo("이미지 테스트 상품");
    assertThat(image2.getProduct().getName()).isEqualTo("다른 상품");
  }

  @Test
  @DisplayName("이미지 파일 확장자별 테스트")
  void testImageFileExtensions() {
    // given & when
    ProductImage jpgImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/image.jpg")
        .sortOrder(1)
        .build();

    ProductImage pngImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/image.png")
        .sortOrder(2)
        .build();

    ProductImage webpImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/image.webp")
        .sortOrder(3)
        .build();

    ProductImage gifImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/image.gif")
        .sortOrder(4)
        .build();

    // then
    assertThat(jpgImage.getImageUrl()).endsWith(".jpg");
    assertThat(pngImage.getImageUrl()).endsWith(".png");
    assertThat(webpImage.getImageUrl()).endsWith(".webp");
    assertThat(gifImage.getImageUrl()).endsWith(".gif");
  }

  @Test
  @DisplayName("썸네일과 일반 이미지 구분 테스트")
  void testThumbnailVsRegularImages() {
    // given & when
    ProductImage thumbnailImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/thumbnail.jpg")
        .sortOrder(0)
        .thumbnail(true)
        .build();

    ProductImage regularImage1 = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/regular1.jpg")
        .sortOrder(1)
        .thumbnail(false)
        .build();

    ProductImage regularImage2 = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/regular2.jpg")
        .sortOrder(2)
        .thumbnail(false)
        .build();

    // then
    assertThat(thumbnailImage.isThumbnail()).isTrue();
    assertThat(regularImage1.isThumbnail()).isFalse();
    assertThat(regularImage2.isThumbnail()).isFalse();

    // 썸네일이 첫 번째 순서인지 확인
    assertThat(thumbnailImage.getSortOrder()).isLessThan(regularImage1.getSortOrder());
    assertThat(thumbnailImage.getSortOrder()).isLessThan(regularImage2.getSortOrder());
  }

  @Test
  @DisplayName("음수 정렬 순서 테스트")
  void testNegativeSortOrder() {
    // given & when
    ProductImage negativeOrderImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/negative.jpg")
        .sortOrder(-1)
        .build();

    // then
    assertThat(negativeOrderImage.getSortOrder()).isEqualTo(-1);
    assertThat(negativeOrderImage.getSortOrder()).isLessThan(0);
  }

  @Test
  @DisplayName("큰 정렬 순서 값 테스트")
  void testLargeSortOrder() {
    // given & when
    ProductImage largeOrderImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/large.jpg")
        .sortOrder(999)
        .build();

    // then
    assertThat(largeOrderImage.getSortOrder()).isEqualTo(999);
    assertThat(largeOrderImage.getSortOrder()).isGreaterThan(100);
  }

  @Test
  @DisplayName("BaseTimeEntity 상속 확인 테스트")
  void testBaseTimeEntityInheritance() {
    // given & when - ProductImage가 BaseTimeEntity를 상속받는지 확인

    // then
    assertThat(productImage).isInstanceOf(com.oboe.backend.common.domain.BaseTimeEntity.class);
  }

  @Test
  @DisplayName("이미지 URL 필드 길이 제한 테스트")
  void testImageUrlLengthLimit() {
    // given
    String url = "https://example.com/image.jpg";

    // when
    ProductImage image = ProductImage.builder()
        .product(product)
        .imageUrl(url)
        .sortOrder(1)
        .build();

    // then
    assertThat(image.getImageUrl()).isEqualTo(url);
    assertThat(image.getImageUrl().length()).isLessThanOrEqualTo(512);
  }

  @Test
  @DisplayName("이미지 정렬 순서 동일값 테스트")
  void testSameSortOrder() {
    // given & when - 같은 정렬 순서를 가진 이미지들
    ProductImage image1 = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/image1.jpg")
        .sortOrder(1)
        .build();

    ProductImage image2 = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/image2.jpg")
        .sortOrder(1) // 같은 순서
        .build();

    // then
    assertThat(image1.getSortOrder()).isEqualTo(image2.getSortOrder());
    assertThat(image1.getImageUrl()).isNotEqualTo(image2.getImageUrl());
  }

  @Test
  @DisplayName("이미지 필수 필드 테스트")
  void testRequiredFields() {
    // given & when - 필수 필드만으로 생성
    ProductImage requiredOnlyImage = ProductImage.builder()
        .product(product)
        .imageUrl("https://example.com/required.jpg")
        .build();

    // then
    assertThat(requiredOnlyImage.getProduct()).isNotNull();
    assertThat(requiredOnlyImage.getImageUrl()).isNotNull();
    assertThat(requiredOnlyImage.getImageUrl()).isNotEmpty();
  }
}
