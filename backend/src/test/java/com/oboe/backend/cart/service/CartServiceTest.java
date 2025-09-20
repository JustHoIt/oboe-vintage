package com.oboe.backend.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.oboe.backend.cart.dto.CartDto;
import com.oboe.backend.cart.dto.CartItemDto;
import com.oboe.backend.cart.dto.request.CartItemRequest;
import com.oboe.backend.cart.dto.response.CartSummaryResponse;
import com.oboe.backend.cart.entity.Cart;
import com.oboe.backend.cart.entity.CartItem;
import com.oboe.backend.cart.repository.CartItemRepository;
import com.oboe.backend.cart.repository.CartRepository;
import com.oboe.backend.common.exception.CustomException;
import com.oboe.backend.common.exception.ErrorCode;
import com.oboe.backend.common.service.TokenProcessor;
import com.oboe.backend.product.entity.Condition;
import com.oboe.backend.product.entity.Product;
import com.oboe.backend.product.entity.ProductStatus;
import com.oboe.backend.product.repository.ProductRepository;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import com.oboe.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService 테스트")
class CartServiceTest {

  @Mock
  private CartRepository cartRepository;

  @Mock
  private CartItemRepository cartItemRepository;

  @Mock
  private ProductRepository productRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private TokenProcessor tokenProcessor;

  @InjectMocks
  private CartService cartService;

  private User testUser;
  private Product testProduct;
  private Product outOfStockProduct;
  private Product inactiveProduct;
  private Cart testCart;
  private CartItem testCartItem;
  private CartItemRequest cartItemRequest;

  @BeforeEach
  void setUp() {
    // 테스트용 사용자 생성
    testUser = User.builder()
        .email("test@example.com")
        .password("password123")
        .name("홍길동")
        .nickname("hong123")
        .phoneNumber("010-1234-5678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .build();
    setIdForTest(testUser, 1L);

    // 테스트용 상품 생성 (재고 충분)
    testProduct = Product.builder()
        .name("빈티지 데님 셔츠")
        .description("1980년대 빈티지 데님 셔츠")
        .price(new BigDecimal("150000"))
        .stockQuantity(10)
        .productStatus(ProductStatus.ACTIVE)
        .brand("리바이스")
        .condition(Condition.VERY_GOOD)
        .build();
    setIdForTest(testProduct, 1L);

    // 재고 부족 상품
    outOfStockProduct = Product.builder()
        .name("품절 상품")
        .description("재고가 부족한 상품")
        .price(new BigDecimal("100000"))
        .stockQuantity(2)
        .productStatus(ProductStatus.ACTIVE)
        .brand("테스트")
        .condition(Condition.GOOD)
        .build();
    setIdForTest(outOfStockProduct, 2L);

    // 비활성 상품
    inactiveProduct = Product.builder()
        .name("비활성 상품")
        .description("판매 중지된 상품")
        .price(new BigDecimal("200000"))
        .stockQuantity(5)
        .productStatus(ProductStatus.INACTIVE)
        .brand("테스트")
        .condition(Condition.EXCELLENT)
        .build();
    setIdForTest(inactiveProduct, 3L);

    // 테스트용 장바구니 생성
    testCart = Cart.builder()
        .user(testUser)
        .totalItems(0)
        .totalPrice(BigDecimal.ZERO)
        .build();
    setIdForTest(testCart, 1L);

    // 테스트용 장바구니 아이템 생성
    testCartItem = CartItem.builder()
        .cart(testCart)
        .product(testProduct)
        .quantity(2)
        .unitPrice(testProduct.getPrice())
        .totalPrice(testProduct.getPrice().multiply(BigDecimal.valueOf(2)))
        .build();
    setIdForTest(testCartItem, 1L);

    // 테스트용 요청 객체 생성
    cartItemRequest = CartItemRequest.builder()
        .productId(1L)
        .quantity(3)
        .build();
  }

  // 테스트용 ID 설정을 위한 헬퍼 메서드
  private void setIdForTest(Object entity, Long id) {
    try {
      java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(entity, id);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set ID for test", e);
    }
  }

  @Nested
  @DisplayName("장바구니 조회 테스트")
  class GetCartTests {

    @Test
    @DisplayName("사용자 장바구니 조회 성공")
    void getCartByUser_Success() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 조회"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartRepository.findByUserId(1L)).willReturn(Optional.of(testCart));

      // when
      CartDto result = cartService.getCartByUser(authorization);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getCartId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("장바구니가 없을 때 새 장바구니 생성")
    void getCartByUser_CreateNewCart() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 조회"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartRepository.findByUserId(1L)).willReturn(Optional.empty());
      given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
      given(cartRepository.save(any(Cart.class))).willReturn(testCart);

      // when
      CartDto result = cartService.getCartByUser(authorization);

      // then
      assertThat(result).isNotNull();
      then(cartRepository).should().save(any(Cart.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 장바구니 조회 실패")
    void getCartByUser_UserNotFound() {
      // given
      String authorization = "Bearer valid-token";
      String email = "nonexistent@example.com";

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 조회"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> cartService.getCartByUser(authorization))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("상품 추가 테스트")
  class AddCartItemTests {

    @Test
    @DisplayName("새 상품을 장바구니에 추가 성공")
    void addCartItem_NewProduct_Success() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 상품 추가"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartRepository.findByUserId(1L)).willReturn(Optional.of(testCart));
      given(cartItemRepository.findByCartIdAndProductId(1L, 1L)).willReturn(Optional.empty());
      given(productRepository.findById(1L)).willReturn(Optional.of(testProduct));
      given(cartItemRepository.save(any(CartItem.class))).willReturn(testCartItem);
      given(cartRepository.save(any(Cart.class))).willReturn(testCart);

      // when
      CartItemDto result = cartService.addCartItem(authorization, cartItemRequest);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getProductId()).isEqualTo(1L);
      assertThat(result.getQuantity()).isEqualTo(3);
      then(cartItemRepository).should().save(any(CartItem.class));
      then(cartRepository).should().save(any(Cart.class));
    }

    @Test
    @DisplayName("기존 상품의 수량 증가 성공")
    void addCartItem_ExistingProduct_Success() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";
      CartItem existingItem = CartItem.builder()
          .cart(testCart)
          .product(testProduct)
          .quantity(2)
          .unitPrice(testProduct.getPrice())
          .totalPrice(testProduct.getPrice().multiply(BigDecimal.valueOf(2)))
          .build();

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 상품 추가"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartRepository.findByUserId(1L)).willReturn(Optional.of(testCart));
      given(cartItemRepository.findByCartIdAndProductId(1L, 1L)).willReturn(
          Optional.of(existingItem));
      given(productRepository.findById(1L)).willReturn(Optional.of(testProduct));
      given(cartItemRepository.save(any(CartItem.class))).willReturn(existingItem);
      given(cartRepository.save(any(Cart.class))).willReturn(testCart);

      // when
      CartItemDto result = cartService.addCartItem(authorization, cartItemRequest);

      // then
      assertThat(result).isNotNull();
      then(cartItemRepository).should().save(any(CartItem.class));
      then(cartRepository).should().save(any(Cart.class));
    }

    @Test
    @DisplayName("재고 부족으로 상품 추가 실패")
    void addCartItem_InsufficientStock_Failure() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";
      CartItemRequest request = CartItemRequest.builder()
          .productId(2L)
          .quantity(5) // 재고는 2개만 있음
          .build();

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 상품 추가"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartRepository.findByUserId(1L)).willReturn(Optional.of(testCart));
      given(cartItemRepository.findByCartIdAndProductId(1L, 2L)).willReturn(Optional.empty());
      given(productRepository.findById(2L)).willReturn(Optional.of(outOfStockProduct));

      // when & then
      assertThatThrownBy(() -> cartService.addCartItem(authorization, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_INSUFFICIENT_STOCK);

      then(cartItemRepository).should(never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("기존 상품 + 추가 수량으로 재고 부족 실패")
    void addCartItem_ExistingProductInsufficientStock_Failure() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";
      CartItem existingItem = CartItem.builder()
          .cart(testCart)
          .product(outOfStockProduct)
          .quantity(1) // 기존 1개
          .unitPrice(outOfStockProduct.getPrice())
          .totalPrice(outOfStockProduct.getPrice())
          .build();

      CartItemRequest request = CartItemRequest.builder()
          .productId(2L)
          .quantity(2) // 추가 2개 (총 3개가 되어 재고 2개 초과)
          .build();

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 상품 추가"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartRepository.findByUserId(1L)).willReturn(Optional.of(testCart));
      given(cartItemRepository.findByCartIdAndProductId(1L, 2L)).willReturn(
          Optional.of(existingItem));
      given(productRepository.findById(2L)).willReturn(Optional.of(outOfStockProduct));

      // when & then
      assertThatThrownBy(() -> cartService.addCartItem(authorization, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("판매 중지된 상품 추가 실패")
    void addCartItem_InactiveProduct_Failure() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";
      CartItemRequest request = CartItemRequest.builder()
          .productId(3L)
          .quantity(1)
          .build();

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 상품 추가"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartRepository.findByUserId(1L)).willReturn(Optional.of(testCart));
      given(cartItemRepository.findByCartIdAndProductId(1L, 3L)).willReturn(Optional.empty());
      given(productRepository.findById(3L)).willReturn(Optional.of(inactiveProduct));

      // when & then
      assertThatThrownBy(() -> cartService.addCartItem(authorization, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_PRODUCT_UNAVAILABLE);
    }

    @Test
    @DisplayName("존재하지 않는 상품 추가 실패")
    void addCartItem_ProductNotFound_Failure() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";
      CartItemRequest request = CartItemRequest.builder()
          .productId(999L)
          .quantity(1)
          .build();

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 상품 추가"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartRepository.findByUserId(1L)).willReturn(Optional.of(testCart));
      given(cartItemRepository.findByCartIdAndProductId(1L, 999L)).willReturn(Optional.empty());
      given(productRepository.findById(999L)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> cartService.addCartItem(authorization, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("수량 변경 테스트")
  class UpdateQuantityTests {

    @Test
    @DisplayName("장바구니 아이템 수량 변경 성공")
    void updateCartItemQuantity_Success() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";
      Integer newQuantity = 5;

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 아이템 수량 변경"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartItemRepository.findById(1L)).willReturn(Optional.of(testCartItem));
      given(cartRepository.findById(1L)).willReturn(Optional.of(testCart));
      given(productRepository.findById(1L)).willReturn(Optional.of(testProduct));
      given(cartItemRepository.save(any(CartItem.class))).willReturn(testCartItem);
      given(cartRepository.save(any(Cart.class))).willReturn(testCart);

      // when
      CartItemDto result = cartService.updateCartItemQuantity(authorization, 1L, newQuantity);

      // then
      assertThat(result).isNotNull();
      then(cartItemRepository).should().save(any(CartItem.class));
      then(cartRepository).should().save(any(Cart.class));
    }

    @Test
    @DisplayName("재고 부족으로 수량 변경 실패")
    void updateCartItemQuantity_InsufficientStock_Failure() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";
      Integer newQuantity = 15; // 재고는 10개
      CartItem cartItemWithOutOfStockProduct = CartItem.builder()
          .cart(testCart)
          .product(testProduct)
          .quantity(2)
          .unitPrice(testProduct.getPrice())
          .totalPrice(testProduct.getPrice().multiply(BigDecimal.valueOf(2)))
          .build();

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 아이템 수량 변경"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartItemRepository.findById(1L)).willReturn(Optional.of(cartItemWithOutOfStockProduct));
      given(cartRepository.findById(1L)).willReturn(Optional.of(testCart));
      given(productRepository.findById(1L)).willReturn(Optional.of(testProduct));

      // when & then
      assertThatThrownBy(() -> cartService.updateCartItemQuantity(authorization, 1L, newQuantity))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_INSUFFICIENT_STOCK);

      then(cartItemRepository).should(never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("존재하지 않는 장바구니 아이템 수량 변경 실패")
    void updateCartItemQuantity_CartItemNotFound_Failure() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 아이템 수량 변경"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartItemRepository.findById(999L)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> cartService.updateCartItemQuantity(authorization, 999L, 5))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사용자의 장바구니 아이템 수량 변경 실패")
    void updateCartItemQuantity_AccessDenied_Failure() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";
      User otherUser = User.builder()
          .email("other@example.com")
          .build();
      setIdForTest(otherUser, 2L);

      Cart otherUserCart = Cart.builder()
          .user(otherUser)
          .totalItems(0)
          .totalPrice(BigDecimal.ZERO)
          .build();
      setIdForTest(otherUserCart, 2L);

      CartItem otherUserCartItem = CartItem.builder()
          .cart(otherUserCart)
          .product(testProduct)
          .quantity(1)
          .unitPrice(testProduct.getPrice())
          .totalPrice(testProduct.getPrice())
          .build();

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 아이템 수량 변경"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartItemRepository.findById(1L)).willReturn(Optional.of(otherUserCartItem));
      given(cartRepository.findById(2L)).willReturn(Optional.of(otherUserCart));

      // when & then
      assertThatThrownBy(() -> cartService.updateCartItemQuantity(authorization, 1L, 5))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ACCESS_DENIED);
    }
  }

  @Nested
  @DisplayName("아이템 제거 테스트")
  class RemoveCartItemTests {

    @Test
    @DisplayName("장바구니 아이템 제거 성공")
    void removeCartItem_Success() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 아이템 제거"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartItemRepository.findById(1L)).willReturn(Optional.of(testCartItem));
      given(cartRepository.findById(1L)).willReturn(Optional.of(testCart));
      given(cartRepository.save(any(Cart.class))).willReturn(testCart);

      // when
      cartService.removeCartItem(authorization, 1L);

      // then
      then(cartItemRepository).should().delete(testCartItem);
      then(cartRepository).should().save(any(Cart.class));
    }
  }

  @Nested
  @DisplayName("장바구니 비우기 테스트")
  class ClearCartTests {

    @Test
    @DisplayName("장바구니 비우기 성공")
    void clearCart_Success() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 비우기"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartRepository.findByUserId(1L)).willReturn(Optional.of(testCart));
      given(cartRepository.save(any(Cart.class))).willReturn(testCart);

      // when
      cartService.clearCart(authorization);

      // then
      then(cartRepository).should().save(any(Cart.class));
    }
  }

  @Nested
  @DisplayName("장바구니 요약 테스트")
  class GetCartSummaryTests {

    @Test
    @DisplayName("장바구니 요약 조회 성공")
    void getCartSummary_Success() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 요약 조회"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartRepository.findByUserId(1L)).willReturn(Optional.of(testCart));

      // when
      CartSummaryResponse result = cartService.getCartSummary(authorization);

      // then
      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("장바구니 유효성 검증 테스트")
  class ValidateCartTests {

    @Test
    @DisplayName("장바구니 유효성 검증 성공")
    void validateCart_Success() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";
      List<CartItem> cartItems = new ArrayList<>();
      cartItems.add(testCartItem);
      testCart = Cart.builder()
          .user(testUser)
          .totalItems(2)
          .totalPrice(new BigDecimal("300000"))
          .cartItems(cartItems)
          .build();

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 유효성 검증"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartRepository.findByUserId(1L)).willReturn(Optional.of(testCart));
      given(cartRepository.save(any(Cart.class))).willReturn(testCart);

      // when
      CartDto result = cartService.validateCart(authorization);

      // then
      assertThat(result).isNotNull();
      then(cartRepository).should().save(any(Cart.class));
    }
  }

  @Nested
  @DisplayName("재고 없는 상품 테스트")
  class NullStockTests {

    @Test
    @DisplayName("재고 정보가 null인 상품 추가 실패")
    void addCartItem_NullStock_Failure() {
      // given
      Product nullStockProduct = Product.builder()
          .name("재고 정보 없는 상품")
          .price(new BigDecimal("100000"))
          .stockQuantity(null) // 재고 정보 없음
          .productStatus(ProductStatus.ACTIVE)
          .build();
      setIdForTest(nullStockProduct, 4L);

      String authorization = "Bearer valid-token";
      String email = "test@example.com";
      CartItemRequest request = CartItemRequest.builder()
          .productId(4L)
          .quantity(1)
          .build();

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 상품 추가"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartRepository.findByUserId(1L)).willReturn(Optional.of(testCart));
      given(cartItemRepository.findByCartIdAndProductId(1L, 4L)).willReturn(Optional.empty());
      given(productRepository.findById(4L)).willReturn(Optional.of(nullStockProduct));

      // when & then
      assertThatThrownBy(() -> cartService.addCartItem(authorization, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_INVALID_STOCK);
    }
  }

  @Nested
  @DisplayName("추가 엣지 케이스 테스트")
  class AdditionalEdgeCaseTests {

    @Test
    @DisplayName("수량 0으로 변경 시 예외 발생 확인")
    void updateCartItemQuantity_ZeroQuantity_ThrowsException() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 아이템 수량 변경"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartItemRepository.findById(1L)).willReturn(Optional.of(testCartItem));
      given(cartRepository.findById(1L)).willReturn(Optional.of(testCart));
      given(productRepository.findById(1L)).willReturn(Optional.of(testProduct));

      // when & then
      assertThatThrownBy(() -> cartService.updateCartItemQuantity(authorization, 1L, 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("수량은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("매우 큰 수량으로 상품 추가 시 재고 부족 확인")
    void addCartItem_VeryLargeQuantity_InsufficientStock() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";
      CartItemRequest request = CartItemRequest.builder()
          .productId(1L)
          .quantity(1000) // 매우 큰 수량
          .build();

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 상품 추가"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartRepository.findByUserId(1L)).willReturn(Optional.of(testCart));
      given(cartItemRepository.findByCartIdAndProductId(1L, 1L)).willReturn(Optional.empty());
      given(productRepository.findById(1L)).willReturn(Optional.of(testProduct));

      // when & then
      assertThatThrownBy(() -> cartService.addCartItem(authorization, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("활성 장바구니가 없을 때 새 장바구니 생성")
    void addCartItem_NoActiveCart_CreatesNewCart() {
      // given
      String authorization = "Bearer valid-token";
      String email = "test@example.com";
      CartItemRequest request = CartItemRequest.builder()
          .productId(1L)
          .quantity(2)
          .build();

      given(tokenProcessor.extractEmailFromBearerToken(authorization, "장바구니 상품 추가"))
          .willReturn(email);
      given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
      given(cartRepository.findByUserId(1L)).willReturn(Optional.empty()); // 활성 장바구니 없음
      given(cartItemRepository.findByCartIdAndProductId(anyLong(), eq(1L))).willReturn(
          Optional.empty());
      given(productRepository.findById(1L)).willReturn(Optional.of(testProduct));
      given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
      given(cartRepository.save(any(Cart.class))).willReturn(testCart);
      given(cartItemRepository.save(any(CartItem.class))).willReturn(testCartItem);

      // when
      CartItemDto result = cartService.addCartItem(authorization, request);

      // then - 새 장바구니가 생성되고 상품이 추가되어야 함
      assertThat(result).isNotNull();
      then(cartRepository).should(times(2)).save(any(Cart.class)); // 장바구니 생성 + 아이템 추가 후 저장
    }
  }
}
