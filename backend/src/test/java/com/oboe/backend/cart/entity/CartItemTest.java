package com.oboe.backend.cart.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oboe.backend.product.entity.Condition;
import com.oboe.backend.product.entity.Product;
import com.oboe.backend.product.entity.ProductStatus;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CartItem Entity 테스트")
class CartItemTest {

  private User user;
  private Product product1;
  private Product product2;
  private Cart cart;
  private CartItem cartItem;

  @BeforeEach
  void setUp() {
    // 테스트용 사용자 생성
    user = User.builder()
        .email("test@example.com")
        .password("password123")
        .name("홍길동")
        .nickname("hong123")
        .phoneNumber("010-1234-5678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .build();

    // 테스트용 상품 생성 (ID를 수동으로 설정)
    product1 = Product.builder()
        .name("빈티지 데님 셔츠")
        .description("1980년대 빈티지 데님 셔츠")
        .price(new BigDecimal("150000"))
        .stockQuantity(5)
        .productStatus(ProductStatus.ACTIVE)
        .brand("리바이스")
        .condition(Condition.VERY_GOOD)
        .build();
    product1 = setIdForTest(product1, 1L); // 테스트용 ID 설정

    product2 = Product.builder()
        .name("빈티지 청바지")
        .description("1990년대 빈티지 청바지")
        .price(new BigDecimal("200000"))
        .stockQuantity(3)
        .productStatus(ProductStatus.ACTIVE)
        .brand("리바이스")
        .condition(Condition.EXCELLENT)
        .build();
    product2 = setIdForTest(product2, 2L); // 테스트용 ID 설정

    // 기본 테스트용 장바구니 생성
    cart = Cart.builder()
        .user(user)
        .totalItems(0)
        .totalPrice(BigDecimal.ZERO)
        .isActive(true)
        .build();

    // 기본 테스트용 장바구니 아이템 생성
    cartItem = CartItem.builder()
        .product(product1)
        .quantity(2)
        .unitPrice(product1.getPrice())
        .totalPrice(product1.getPrice().multiply(BigDecimal.valueOf(2)))
        .build();
  }

  // 테스트용 ID 설정을 위한 헬퍼 메서드
  private Product setIdForTest(Product product, Long id) {
    try {
      java.lang.reflect.Field idField = Product.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(product, id);
      return product;
    } catch (Exception e) {
      throw new RuntimeException("Failed to set ID for test", e);
    }
  }

  @Test
  @DisplayName("CartItem 기본 생성 테스트")
  void createCartItem() {
    // given & when
    CartItem newCartItem = CartItem.builder()
        .product(product1)
        .quantity(3)
        .unitPrice(product1.getPrice())
        .totalPrice(product1.getPrice().multiply(BigDecimal.valueOf(3)))
        .build();

    // then
    assertThat(newCartItem.getProduct()).isEqualTo(product1);
    assertThat(newCartItem.getQuantity()).isEqualTo(3);
    assertThat(newCartItem.getUnitPrice()).isEqualByComparingTo(product1.getPrice());
    assertThat(newCartItem.getTotalPrice()).isEqualByComparingTo(new BigDecimal("450000")); // 150000 * 3
  }

  @Test
  @DisplayName("수량 증가 테스트")
  void increaseQuantity() {
    // given
    Integer additionalQuantity = 3;

    // when
    cartItem.increaseQuantity(additionalQuantity);

    // then
    assertThat(cartItem.getQuantity()).isEqualTo(5); // 2 + 3
    assertThat(cartItem.getTotalPrice()).isEqualByComparingTo(new BigDecimal("750000")); // 150000 * 5
  }

  @Test
  @DisplayName("수량 증가 테스트 - 잘못된 수량")
  void increaseQuantity_InvalidQuantity() {
    // given & when & then
    assertThatThrownBy(() -> cartItem.increaseQuantity(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("추가할 수량은 0보다 커야 합니다");

    assertThatThrownBy(() -> cartItem.increaseQuantity(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("추가할 수량은 0보다 커야 합니다");

    assertThatThrownBy(() -> cartItem.increaseQuantity(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("추가할 수량은 0보다 커야 합니다");
  }

  @Test
  @DisplayName("수량 감소 테스트")
  void decreaseQuantity() {
    // given
    Integer decreaseQuantity = 1;

    // when
    cartItem.decreaseQuantity(decreaseQuantity);

    // then
    assertThat(cartItem.getQuantity()).isEqualTo(1); // 2 - 1
    assertThat(cartItem.getTotalPrice()).isEqualByComparingTo(new BigDecimal("150000")); // 150000 * 1
  }

  @Test
  @DisplayName("수량 감소 테스트 - 잘못된 수량")
  void decreaseQuantity_InvalidQuantity() {
    // given & when & then
    assertThatThrownBy(() -> cartItem.decreaseQuantity(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("감소할 수량은 0보다 커야 합니다");

    assertThatThrownBy(() -> cartItem.decreaseQuantity(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("감소할 수량은 0보다 커야 합니다");

    assertThatThrownBy(() -> cartItem.decreaseQuantity(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("감소할 수량은 0보다 커야 합니다");
  }

  @Test
  @DisplayName("수량 감소 테스트 - 수량 부족")
  void decreaseQuantity_InsufficientQuantity() {
    // given
    Integer decreaseQuantity = 5; // 현재 수량은 2

    // when & then
    assertThatThrownBy(() -> cartItem.decreaseQuantity(decreaseQuantity))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("수량이 부족합니다");
  }

  @Test
  @DisplayName("수량 설정 테스트")
  void setQuantity() {
    // given
    Integer newQuantity = 5;

    // when
    cartItem.setQuantity(newQuantity);

    // then
    assertThat(cartItem.getQuantity()).isEqualTo(5);
    assertThat(cartItem.getTotalPrice()).isEqualByComparingTo(new BigDecimal("750000")); // 150000 * 5
  }

  @Test
  @DisplayName("수량 설정 테스트 - 잘못된 수량")
  void setQuantity_InvalidQuantity() {
    // given & when & then
    assertThatThrownBy(() -> cartItem.setQuantity(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("수량은 0보다 커야 합니다");

    assertThatThrownBy(() -> cartItem.setQuantity(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("수량은 0보다 커야 합니다");

    assertThatThrownBy(() -> cartItem.setQuantity(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("수량은 0보다 커야 합니다");
  }

  @Test
  @DisplayName("단위 가격 설정 테스트")
  void setUnitPrice() {
    // given
    BigDecimal newUnitPrice = new BigDecimal("180000");

    // when
    cartItem.setUnitPrice(newUnitPrice);

    // then
    assertThat(cartItem.getUnitPrice()).isEqualByComparingTo(newUnitPrice);
    assertThat(cartItem.getTotalPrice()).isEqualByComparingTo(new BigDecimal("360000")); // 180000 * 2
  }

  @Test
  @DisplayName("단위 가격 설정 테스트 - 잘못된 가격")
  void setUnitPrice_InvalidPrice() {
    // given & when & then
    assertThatThrownBy(() -> cartItem.setUnitPrice(BigDecimal.valueOf(-1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("단위 가격은 0 이상이어야 합니다");

    assertThatThrownBy(() -> cartItem.setUnitPrice(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("단위 가격은 0 이상이어야 합니다");
  }

  @Test
  @DisplayName("상품 가격으로 단위 가격 업데이트 테스트")
  void updateUnitPriceFromProduct() {
    // given
    cartItem.setUnitPrice(new BigDecimal("100000")); // 다른 가격으로 설정

    // when
    cartItem.updateUnitPriceFromProduct();

    // then
    assertThat(cartItem.getUnitPrice()).isEqualByComparingTo(product1.getPrice()); // 150000
    assertThat(cartItem.getTotalPrice()).isEqualByComparingTo(new BigDecimal("300000")); // 150000 * 2
  }

  @Test
  @DisplayName("장바구니 연결 설정 테스트")
  void setCart() {
    // given & when
    cartItem.setCart(cart);

    // then
    assertThat(cartItem.getCart()).isEqualTo(cart);
  }

  @Test
  @DisplayName("상품 재고 확인 테스트 - 재고 충분")
  void isStockAvailable_True() {
    // given
    cartItem.setQuantity(3); // 재고는 5개

    // when & then
    assertThat(cartItem.isStockAvailable()).isTrue();
  }

  @Test
  @DisplayName("상품 재고 확인 테스트 - 재고 부족")
  void isStockAvailable_False() {
    // given
    cartItem.setQuantity(10); // 재고는 5개

    // when & then
    assertThat(cartItem.isStockAvailable()).isFalse();
  }

  @Test
  @DisplayName("상품 재고 확인 테스트 - 정확히 재고 수량만큼")
  void isStockAvailable_ExactStock() {
    // given
    cartItem.setQuantity(5); // 재고와 정확히 같은 수량

    // when & then
    assertThat(cartItem.isStockAvailable()).isTrue();
  }

  @Test
  @DisplayName("상품이 판매 중인지 확인 테스트 - 판매 중")
  void isProductAvailable_True() {
    // given & when & then
    assertThat(cartItem.isProductAvailable()).isTrue();
  }

  @Test
  @DisplayName("상품이 판매 중인지 확인 테스트 - 판매 중지")
  void isProductAvailable_False() {
    // given
    product1.changeStatus(ProductStatus.INACTIVE);

    // when & then
    assertThat(cartItem.isProductAvailable()).isFalse();
  }

  @Test
  @DisplayName("장바구니 아이템이 유효한지 확인 테스트 - 유효함")
  void isValid_True() {
    // given & when & then
    assertThat(cartItem.isValid()).isTrue();
  }

  @Test
  @DisplayName("장바구니 아이템이 유효한지 확인 테스트 - 재고 부족")
  void isValid_False_InsufficientStock() {
    // given
    cartItem.setQuantity(10); // 재고 부족

    // when & then
    assertThat(cartItem.isValid()).isFalse();
  }

  @Test
  @DisplayName("장바구니 아이템이 유효한지 확인 테스트 - 상품 판매 중지")
  void isValid_False_ProductUnavailable() {
    // given
    product1.changeStatus(ProductStatus.INACTIVE);

    // when & then
    assertThat(cartItem.isValid()).isFalse();
  }

  @Test
  @DisplayName("현재 상품 가격과 장바구니 가격이 다른지 확인 테스트 - 가격 동일")
  void isPriceChanged_False() {
    // given & when & then
    assertThat(cartItem.isPriceChanged()).isFalse();
  }

  @Test
  @DisplayName("현재 상품 가격과 장바구니 가격이 다른지 확인 테스트 - 가격 상승")
  void isPriceChanged_True_PriceIncreased() {
    // given
    cartItem.setUnitPrice(new BigDecimal("100000")); // 원래 가격보다 낮게 설정
    // product1의 가격은 150000

    // when & then
    assertThat(cartItem.isPriceChanged()).isTrue();
  }

  @Test
  @DisplayName("현재 상품 가격과 장바구니 가격이 다른지 확인 테스트 - 가격 하락")
  void isPriceChanged_True_PriceDecreased() {
    // given
    cartItem.setUnitPrice(new BigDecimal("200000")); // 원래 가격보다 높게 설정
    // product1의 가격은 150000

    // when & then
    assertThat(cartItem.isPriceChanged()).isTrue();
  }

  @Test
  @DisplayName("가격 변경으로 인한 메시지 생성 테스트 - 가격 상승")
  void getPriceChangeMessage_PriceIncreased() {
    // given
    cartItem.setUnitPrice(new BigDecimal("100000")); // 원래 가격보다 낮게 설정

    // when
    String message = cartItem.getPriceChangeMessage();

    // then
    assertThat(message).contains("상품 가격이 100000원에서 150000원으로 인상되었습니다");
  }

  @Test
  @DisplayName("가격 변경으로 인한 메시지 생성 테스트 - 가격 하락")
  void getPriceChangeMessage_PriceDecreased() {
    // given
    cartItem.setUnitPrice(new BigDecimal("200000")); // 원래 가격보다 높게 설정

    // when
    String message = cartItem.getPriceChangeMessage();

    // then
    assertThat(message).contains("상품 가격이 200000원에서 150000원으로 인하되었습니다");
  }

  @Test
  @DisplayName("가격 변경으로 인한 메시지 생성 테스트 - 가격 변경 없음")
  void getPriceChangeMessage_NoChange() {
    // given & when
    String message = cartItem.getPriceChangeMessage();

    // then
    assertThat(message).isNull();
  }

  @Test
  @DisplayName("재고 부족 메시지 생성 테스트 - 재고 충분")
  void getStockShortageMessage_StockSufficient() {
    // given & when
    String message = cartItem.getStockShortageMessage();

    // then
    assertThat(message).isNull();
  }

  @Test
  @DisplayName("재고 부족 메시지 생성 테스트 - 재고 부족")
  void getStockShortageMessage_StockInsufficient() {
    // given
    cartItem.setQuantity(10); // 재고는 5개

    // when
    String message = cartItem.getStockShortageMessage();

    // then
    assertThat(message).contains("재고가 부족합니다");
    assertThat(message).contains("현재 재고: 5개");
    assertThat(message).contains("요청 수량: 10개");
  }

  @Test
  @DisplayName("BaseTimeEntity 상속 확인 테스트")
  void testBaseTimeEntityInheritance() {
    // given & when & then
    assertThat(cartItem).isInstanceOf(com.oboe.backend.common.domain.BaseTimeEntity.class);
  }

  @Test
  @DisplayName("다양한 상품 상태에서의 유효성 확인 테스트")
  void testValidityWithDifferentProductStatuses() {
    // given & when & then
    // ACTIVE 상태 - 유효함
    assertThat(cartItem.isValid()).isTrue();

    // SOLD_OUT 상태 - 판매 불가
    product1.changeStatus(ProductStatus.SOLD_OUT);
    assertThat(cartItem.isValid()).isFalse();

    // INACTIVE 상태 - 판매 불가
    product1.changeStatus(ProductStatus.INACTIVE);
    assertThat(cartItem.isValid()).isFalse();

    // TRADING 상태 - 판매 불가
    product1.changeStatus(ProductStatus.TRADING);
    assertThat(cartItem.isValid()).isFalse();
  }

  @Test
  @DisplayName("총 가격 자동 계산 테스트")
  void testTotalPriceCalculation() {
    // given
    cartItem.setUnitPrice(new BigDecimal("100000"));
    cartItem.setQuantity(5);

    // when & then
    assertThat(cartItem.getTotalPrice()).isEqualByComparingTo(new BigDecimal("500000")); // 100000 * 5

    // 수량 변경 시 자동 재계산
    cartItem.setQuantity(3);
    assertThat(cartItem.getTotalPrice()).isEqualByComparingTo(new BigDecimal("300000")); // 100000 * 3

    // 단위 가격 변경 시 자동 재계산
    cartItem.setUnitPrice(new BigDecimal("150000"));
    assertThat(cartItem.getTotalPrice()).isEqualByComparingTo(new BigDecimal("450000")); // 150000 * 3
  }
}
