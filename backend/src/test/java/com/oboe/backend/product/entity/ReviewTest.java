package com.oboe.backend.product.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Review Entity 테스트")
class ReviewTest {

  private Product product;
  private User user;
  private Review review;

  @BeforeEach
  void setUp() {
    // 테스트용 Product 생성
    product = Product.builder()
        .name("리뷰 테스트 상품")
        .description("리뷰를 위한 테스트 상품입니다.")
        .price(new BigDecimal("100000"))
        .stockQuantity(1)
        .productStatus(ProductStatus.SOLD_OUT)
        .brand("테스트브랜드")
        .condition(Condition.EXCELLENT)
        .views(0)
        .build();

    // 테스트용 User 생성
    user = User.builder()
        .email("reviewer@example.com")
        .password("password123")
        .name("리뷰어")
        .nickname("reviewer123")
        .phoneNumber("010-1234-5678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .build();

    // 기본 테스트용 Review 생성
    review = Review.builder()
        .product(product)
        .user(user)
        .rating(5)
        .title("훌륭한 상품입니다")
        .content("품질이 정말 좋네요. 배송도 빠르고 포장도 꼼꼼했습니다. 추천합니다!")
        .build();
  }

  @Test
  @DisplayName("Review 기본 생성 테스트")
  void createBasicReview() {
    // given & when - setUp에서 생성됨

    // then
    assertThat(review.getProduct()).isEqualTo(product);
    assertThat(review.getUser()).isEqualTo(user);
    assertThat(review.getRating()).isEqualTo(5);
    assertThat(review.getTitle()).isEqualTo("훌륭한 상품입니다");
    assertThat(review.getContent()).isEqualTo("품질이 정말 좋네요. 배송도 빠르고 포장도 꼼꼼했습니다. 추천합니다!");
  }

  @Test
  @DisplayName("Review 필수 필드만으로 생성 테스트")
  void createReviewWithMinimalFields() {
    // given & when
    Review minimalReview = Review.builder()
        .product(product)
        .user(user)
        .rating(3)
        .title("보통입니다")
        .build();

    // then
    assertThat(minimalReview.getProduct()).isEqualTo(product);
    assertThat(minimalReview.getUser()).isEqualTo(user);
    assertThat(minimalReview.getRating()).isEqualTo(3);
    assertThat(minimalReview.getTitle()).isEqualTo("보통입니다");
    assertThat(minimalReview.getContent()).isNull(); // content는 선택 필드
  }

  @Test
  @DisplayName("다양한 평점으로 리뷰 생성 테스트")
  void createReviewsWithDifferentRatings() {
    // given & when
    Review rating1 = Review.builder()
        .product(product)
        .user(user)
        .rating(1)
        .title("별로입니다")
        .content("기대했던 것보다 품질이 떨어집니다.")
        .build();

    Review rating3 = Review.builder()
        .product(product)
        .user(user)
        .rating(3)
        .title("보통입니다")
        .content("나쁘지 않지만 특별하지도 않습니다.")
        .build();

    Review rating5 = Review.builder()
        .product(product)
        .user(user)
        .rating(5)
        .title("최고입니다")
        .content("정말 만족스럽습니다. 다릇 옷들고 구매할 의향이 있습니다.")
        .build();

    // then
    assertThat(rating1.getRating()).isEqualTo(1);
    assertThat(rating3.getRating()).isEqualTo(3);
    assertThat(rating5.getRating()).isEqualTo(5);
  }

  @Test
  @DisplayName("평점 유효 범위 테스트")
  void testValidRatingRange() {
    // given & when - 유효한 범위의 평점들
    for (int rating = 1; rating <= 5; rating++) {
      Review validReview = Review.builder()
          .product(product)
          .user(user)
          .rating(rating)
          .title("평점 " + rating + "점")
          .content("평점 테스트")
          .build();

      // then
      assertThat(validReview.getRating()).isEqualTo(rating);
      assertThat(validReview.getRating()).isBetween(1, 5);
    }
  }

  @Test
  @DisplayName("긴 리뷰 제목 테스트")
  void testLongReviewTitle() {
    // given
    String longTitle = "이것은 매우 긴 리뷰 제목입니다. ".repeat(10);

    // when
    Review longTitleReview = Review.builder()
        .product(product)
        .user(user)
        .rating(4)
        .title(longTitle)
        .content("긴 제목 테스트")
        .build();

    // then
    assertThat(longTitleReview.getTitle()).isEqualTo(longTitle);
  }

  @Test
  @DisplayName("긴 리뷰 내용 테스트")
  void testLongReviewContent() {
    // given
    String longContent = "이 상품에 대한 매우 자세한 리뷰입니다. ".repeat(50);

    // when
    Review longContentReview = Review.builder()
        .product(product)
        .user(user)
        .rating(4)
        .title("자세한 리뷰")
        .content(longContent)
        .build();

    // then
    assertThat(longContentReview.getContent()).isEqualTo(longContent);
  }

  @Test
  @DisplayName("빈 리뷰 내용 테스트")
  void testEmptyReviewContent() {
    // given & when
    Review emptyContentReview = Review.builder()
        .product(product)
        .user(user)
        .rating(3)
        .title("내용 없는 리뷰")
        .content("")
        .build();

    // then
    assertThat(emptyContentReview.getContent()).isEqualTo("");
    assertThat(emptyContentReview.getTitle()).isEqualTo("내용 없는 리뷰");
  }

  @Test
  @DisplayName("Product와 User 연관관계 테스트")
  void testProductUserRelationship() {
    // given & when - setUp의 review 사용

    // then
    assertThat(review.getProduct()).isNotNull();
    assertThat(review.getUser()).isNotNull();
    assertThat(review.getProduct().getName()).isEqualTo("리뷰 테스트 상품");
    assertThat(review.getUser().getName()).isEqualTo("리뷰어");
    assertThat(review.getUser().getNickname()).isEqualTo("reviewer123");
  }

  @Test
  @DisplayName("다른 사용자가 같은 상품에 리뷰 작성 테스트")
  void testDifferentUsersSameProduct() {
    // given
    User anotherUser = User.builder()
        .email("another@example.com")
        .password("password123")
        .name("다른리뷰어")
        .nickname("another123")
        .phoneNumber("010-9876-5432")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .build();

    // when
    Review anotherReview = Review.builder()
        .product(product) // 같은 상품
        .user(anotherUser) // 다른 사용자
        .rating(4)
        .title("다른 사용자의 리뷰")
        .content("저도 이 상품 좋다고 생각합니다.")
        .build();

    // then
    assertThat(review.getProduct()).isEqualTo(anotherReview.getProduct());
    assertThat(review.getUser()).isNotEqualTo(anotherReview.getUser());
    assertThat(review.getRating()).isNotEqualTo(anotherReview.getRating());
  }

  @Test
  @DisplayName("같은 사용자가 다른 상품에 리뷰 작성 테스트")
  void testSameUserDifferentProducts() {
    // given
    Product anotherProduct = Product.builder()
        .name("다른 테스트 상품")
        .description("또 다른 테스트 상품입니다.")
        .price(new BigDecimal("200000"))
        .stockQuantity(1)
        .productStatus(ProductStatus.ACTIVE)
        .condition(Condition.GOOD)
        .views(0)
        .build();

    // when
    Review anotherReview = Review.builder()
        .product(anotherProduct) // 다른 상품
        .user(user) // 같은 사용자
        .rating(3)
        .title("다른 상품 리뷰")
        .content("이 상품은 보통입니다.")
        .build();

    // then
    assertThat(review.getUser()).isEqualTo(anotherReview.getUser());
    assertThat(review.getProduct()).isNotEqualTo(anotherReview.getProduct());
    assertThat(review.getTitle()).isNotEqualTo(anotherReview.getTitle());
  }

  @Test
  @DisplayName("리뷰 평점별 분류 테스트")
  void testReviewsByRating() {
    // given & when
    Review excellentReview = Review.builder()
        .product(product)
        .user(user)
        .rating(5)
        .title("최고")
        .content("완벽합니다")
        .build();

    Review goodReview = Review.builder()
        .product(product)
        .user(user)
        .rating(4)
        .title("좋음")
        .content("만족합니다")
        .build();

    Review averageReview = Review.builder()
        .product(product)
        .user(user)
        .rating(3)
        .title("보통")
        .content("그럭저럭")
        .build();

    Review poorReview = Review.builder()
        .product(product)
        .user(user)
        .rating(2)
        .title("별로")
        .content("아쉽습니다")
        .build();

    Review badReview = Review.builder()
        .product(product)
        .user(user)
        .rating(1)
        .title("나쁨")
        .content("실망스럽습니다")
        .build();

    // then
    assertThat(excellentReview.getRating()).isEqualTo(5);
    assertThat(goodReview.getRating()).isEqualTo(4);
    assertThat(averageReview.getRating()).isEqualTo(3);
    assertThat(poorReview.getRating()).isEqualTo(2);
    assertThat(badReview.getRating()).isEqualTo(1);

    // 모든 평점이 유효 범위 내에 있는지 확인
    assertThat(excellentReview.getRating()).isBetween(1, 5);
    assertThat(goodReview.getRating()).isBetween(1, 5);
    assertThat(averageReview.getRating()).isBetween(1, 5);
    assertThat(poorReview.getRating()).isBetween(1, 5);
    assertThat(badReview.getRating()).isBetween(1, 5);
  }

  @Test
  @DisplayName("특수 문자가 포함된 리뷰 테스트")
  void testReviewWithSpecialCharacters() {
    // given & when
    Review specialCharReview = Review.builder()
        .product(product)
        .user(user)
        .rating(5)
        .title("★★★★★ 최고의 상품! @#$%")
        .content("정말 좋습니다! 100% 만족해요. 가격도 합리적이고... 👍😊")
        .build();

    // then
    assertThat(specialCharReview.getTitle()).contains("★★★★★");
    assertThat(specialCharReview.getTitle()).contains("@#$%");
    assertThat(specialCharReview.getContent()).contains("100%");
    assertThat(specialCharReview.getContent()).contains("👍😊");
  }

  @Test
  @DisplayName("BaseTimeEntity 상속 확인 테스트")
  void testBaseTimeEntityInheritance() {
    // given & when - Review가 BaseTimeEntity를 상속받는지 확인

    // then
    assertThat(review).isInstanceOf(com.oboe.backend.common.domain.BaseTimeEntity.class);
  }

  @Test
  @DisplayName("리뷰 필드 null 체크 테스트")
  void testNullFields() {
    // given & when
    Review reviewWithNullContent = Review.builder()
        .product(product)
        .user(user)
        .rating(4)
        .title("내용 없음")
        .content(null)
        .build();

    // then
    assertThat(reviewWithNullContent.getProduct()).isNotNull();
    assertThat(reviewWithNullContent.getUser()).isNotNull();
    assertThat(reviewWithNullContent.getTitle()).isNotNull();
    assertThat(reviewWithNullContent.getContent()).isNull(); // content는 nullable
    assertThat(reviewWithNullContent.getRating()).isNotZero();
  }
}
