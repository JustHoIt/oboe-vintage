package com.oboe.backend.cart.entity;

import static org.assertj.core.api.Assertions.assertThat;

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

@DisplayName("Cart Entity 테스트")
class CartTest {

  private User user;
  private Product product1;
  private Product product2;
  private Cart cart;

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
  @DisplayName("Cart 기본 생성 테스트")
  void createCart() {
    // given & when
    Cart newCart = Cart.builder()
        .user(user)
        .totalItems(0)
        .totalPrice(BigDecimal.ZERO)
        .build();

    // then
    assertThat(newCart.getUser()).isEqualTo(user);
    assertThat(newCart.getTotalItems()).isEqualTo(0);
    assertThat(newCart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(newCart.getCartItems()).isEmpty();
  }

  @Test
  @DisplayName("장바구니에 상품 추가 테스트")
  void addCartItem() {
    // given
    CartItem cartItem = CartItem.builder()
        .product(product1)
        .quantity(2)
        .unitPrice(product1.getPrice())
        .totalPrice(product1.getPrice().multiply(BigDecimal.valueOf(2)))
        .build();

    // when
    cart.addCartItem(cartItem);

    // then
    assertThat(cart.getCartItems()).hasSize(1);
    assertThat(cart.getCartItems()).contains(cartItem);
    assertThat(cartItem.getCart()).isEqualTo(cart);
    assertThat(cart.getTotalItems()).isEqualTo(2);
    assertThat(cart.getTotalPrice()).isEqualByComparingTo(new BigDecimal("300000")); // 150000 * 2
  }

  @Test
  @DisplayName("장바구니에 같은 상품 중복 추가 시 수량 증가 테스트")
  void addCartItem_DuplicateProduct() {
    // given
    CartItem firstItem = CartItem.builder()
        .product(product1)
        .quantity(1)
        .unitPrice(product1.getPrice())
        .totalPrice(product1.getPrice())
        .build();

    CartItem secondItem = CartItem.builder()
        .product(product1)
        .quantity(3)
        .unitPrice(product1.getPrice())
        .totalPrice(product1.getPrice().multiply(BigDecimal.valueOf(3)))
        .build();

    cart.addCartItem(firstItem);

    // when
    cart.addCartItem(secondItem);

    // then
    assertThat(cart.getCartItems()).hasSize(1); // 같은 상품이므로 아이템 개수는 1개
    assertThat(cart.getTotalItems()).isEqualTo(4); // 수량은 1 + 3 = 4개
    assertThat(cart.getTotalPrice()).isEqualByComparingTo(new BigDecimal("600000")); // 150000 * 4
    assertThat(cart.getCartItems().get(0).getQuantity()).isEqualTo(4);
  }

  @Test
  @DisplayName("장바구니에서 상품 제거 테스트")
  void removeCartItem() {
    // given
    CartItem cartItem = CartItem.builder()
        .product(product1)
        .quantity(2)
        .unitPrice(product1.getPrice())
        .totalPrice(product1.getPrice().multiply(BigDecimal.valueOf(2)))
        .build();
    cart.addCartItem(cartItem);

    // when
    cart.removeCartItem(product1.getId());

    // then
    assertThat(cart.getCartItems()).isEmpty();
    assertThat(cart.getTotalItems()).isEqualTo(0);
    assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("장바구니 상품 수량 변경 테스트")
  void updateCartItemQuantity() {
    // given
    CartItem cartItem = CartItem.builder()
        .product(product1)
        .quantity(2)
        .unitPrice(product1.getPrice())
        .totalPrice(product1.getPrice().multiply(BigDecimal.valueOf(2)))
        .build();
    cart.addCartItem(cartItem);

    // when
    cart.updateCartItemQuantity(product1.getId(), 5);

    // then
    assertThat(cart.getCartItems()).hasSize(1);
    assertThat(cart.getCartItems().get(0).getQuantity()).isEqualTo(5);
    assertThat(cart.getTotalItems()).isEqualTo(5);
    assertThat(cart.getTotalPrice()).isEqualByComparingTo(new BigDecimal("750000")); // 150000 * 5
  }

  @Test
  @DisplayName("장바구니 상품 수량을 0으로 변경 시 상품 제거 테스트")
  void updateCartItemQuantity_ZeroQuantity() {
    // given
    CartItem cartItem = CartItem.builder()
        .product(product1)
        .quantity(2)
        .unitPrice(product1.getPrice())
        .totalPrice(product1.getPrice().multiply(BigDecimal.valueOf(2)))
        .build();
    cart.addCartItem(cartItem);

    // when
    cart.updateCartItemQuantity(product1.getId(), 0);

    // then
    assertThat(cart.getCartItems()).isEmpty();
    assertThat(cart.getTotalItems()).isEqualTo(0);
    assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("존재하지 않는 상품 수량 변경 시 아무것도 변경되지 않음 테스트")
  void updateCartItemQuantity_NonExistentProduct() {
    // given
    Long nonExistentProductId = 999L;

    // when
    cart.updateCartItemQuantity(nonExistentProductId, 5);

    // then
    assertThat(cart.getCartItems()).isEmpty();
    assertThat(cart.getTotalItems()).isEqualTo(0);
    assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("장바구니 비우기 테스트")
  void clear() {
    // given
    CartItem cartItem1 = CartItem.builder()
        .product(product1)
        .quantity(2)
        .unitPrice(product1.getPrice())
        .totalPrice(product1.getPrice().multiply(BigDecimal.valueOf(2)))
        .build();

    CartItem cartItem2 = CartItem.builder()
        .product(product2)
        .quantity(1)
        .unitPrice(product2.getPrice())
        .totalPrice(product2.getPrice())
        .build();

    cart.addCartItem(cartItem1);
    cart.addCartItem(cartItem2);

    // when
    cart.clear();

    // then
    assertThat(cart.getCartItems()).isEmpty();
    assertThat(cart.getTotalItems()).isEqualTo(0);
    assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("장바구니 비우기 테스트")
  void clearCart() {
    // given - 상품 추가
    cart.addCartItem(createCartItem(product1, 2));
    assertThat(cart.getTotalItems()).isEqualTo(2);

    // when - 장바구니 비우기
    cart.clear();

    // then - 장바구니가 비워짐
    assertThat(cart.isEmpty()).isTrue();
    assertThat(cart.getTotalItems()).isEqualTo(0);
    assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("장바구니가 비어있는지 확인 테스트 - 비어있음")
  void isEmpty_True() {
    // given & when & then
    assertThat(cart.isEmpty()).isTrue();
  }

  @Test
  @DisplayName("장바구니가 비어있는지 확인 테스트 - 비어있지 않음")
  void isEmpty_False() {
    // given
    CartItem cartItem = CartItem.builder()
        .product(product1)
        .quantity(1)
        .unitPrice(product1.getPrice())
        .totalPrice(product1.getPrice())
        .build();
    cart.addCartItem(cartItem);

    // when & then
    assertThat(cart.isEmpty()).isFalse();
  }

  @Test
  @DisplayName("장바구니에 특정 상품이 있는지 확인 테스트 - 있음")
  void hasProduct_True() {
    // given
    CartItem cartItem = CartItem.builder()
        .product(product1)
        .quantity(1)
        .unitPrice(product1.getPrice())
        .totalPrice(product1.getPrice())
        .build();
    cart.addCartItem(cartItem);

    // when & then
    assertThat(cart.hasProduct(product1.getId())).isTrue();
  }

  @Test
  @DisplayName("장바구니에 특정 상품이 있는지 확인 테스트 - 없음")
  void hasProduct_False() {
    // given & when & then
    assertThat(cart.hasProduct(product1.getId())).isFalse();
  }

  @Test
  @DisplayName("특정 상품의 수량 반환 테스트 - 존재하는 상품")
  void getProductQuantity_ExistingProduct() {
    // given
    CartItem cartItem = CartItem.builder()
        .product(product1)
        .quantity(3)
        .unitPrice(product1.getPrice())
        .totalPrice(product1.getPrice().multiply(BigDecimal.valueOf(3)))
        .build();
    cart.addCartItem(cartItem);

    // when & then
    assertThat(cart.getProductQuantity(product1.getId())).isEqualTo(3);
  }

  @Test
  @DisplayName("특정 상품의 수량 반환 테스트 - 존재하지 않는 상품")
  void getProductQuantity_NonExistentProduct() {
    // given & when & then
    assertThat(cart.getProductQuantity(999L)).isEqualTo(0);
  }

  @Test
  @DisplayName("장바구니 아이템 수 반환 테스트")
  void getItemCount() {
    // given
    CartItem cartItem1 = CartItem.builder()
        .product(product1)
        .quantity(1)
        .unitPrice(product1.getPrice())
        .totalPrice(product1.getPrice())
        .build();

    CartItem cartItem2 = CartItem.builder()
        .product(product2)
        .quantity(1)
        .unitPrice(product2.getPrice())
        .totalPrice(product2.getPrice())
        .build();

    cart.addCartItem(cartItem1);
    cart.addCartItem(cartItem2);

    // when & then
    assertThat(cart.getItemCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("주문 가능 여부 확인 테스트 - 주문 가능")
  void canPlaceOrder_True() {
    // given
    CartItem cartItem = CartItem.builder()
        .product(product1)
        .quantity(1)
        .unitPrice(product1.getPrice())
        .totalPrice(product1.getPrice())
        .build();
    cart.addCartItem(cartItem);

    // when & then
    assertThat(cart.canPlaceOrder()).isTrue();
  }

  @Test
  @DisplayName("주문 가능 여부 확인 테스트 - 장바구니 비어있음")
  void canPlaceOrder_EmptyCart() {
    // given & when & then
    assertThat(cart.canPlaceOrder()).isFalse();
  }

  @Test
  @DisplayName("다양한 상품이 포함된 장바구니 총액 계산 테스트")
  void calculateTotalPrice_MultipleProducts() {
    // given
    CartItem cartItem1 = CartItem.builder()
        .product(product1)
        .quantity(2)
        .unitPrice(product1.getPrice())
        .totalPrice(product1.getPrice().multiply(BigDecimal.valueOf(2)))
        .build();

    CartItem cartItem2 = CartItem.builder()
        .product(product2)
        .quantity(1)
        .unitPrice(product2.getPrice())
        .totalPrice(product2.getPrice())
        .build();

    cart.addCartItem(cartItem1);
    cart.addCartItem(cartItem2);

    // when & then
    // (150000 * 2) + (200000 * 1) = 500000
    assertThat(cart.getTotalPrice()).isEqualByComparingTo(new BigDecimal("500000"));
    assertThat(cart.getTotalItems()).isEqualTo(3);
  }

  @Test
  @DisplayName("BaseTimeEntity 상속 확인 테스트")
  void testBaseTimeEntityInheritance() {
    // given & when & then
    assertThat(cart).isInstanceOf(com.oboe.backend.common.domain.BaseTimeEntity.class);
  }

  @Test
  @DisplayName("장바구니 초기 상태 확인 테스트")
  void testInitialState() {
    // given & when & then
    assertThat(cart.getUser()).isEqualTo(user);
    assertThat(cart.getTotalItems()).isEqualTo(0);
    assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(cart.getCartItems()).isEmpty();
    assertThat(cart.isEmpty()).isTrue();
    assertThat(cart.getItemCount()).isEqualTo(0);
    assertThat(cart.canPlaceOrder()).isFalse();
  }

  private CartItem createCartItem(Product product, int quantity) {
    return CartItem.builder()
        .cart(cart)
        .product(product)
        .quantity(quantity)
        .unitPrice(product.getPrice())
        .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
        .build();
  }
}
