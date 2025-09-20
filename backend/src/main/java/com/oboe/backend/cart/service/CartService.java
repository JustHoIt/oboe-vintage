package com.oboe.backend.cart.service;

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
import com.oboe.backend.product.entity.Product;
import com.oboe.backend.product.repository.ProductRepository;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CartService {

  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final TokenProcessor tokenProcessor;

  /**
   * 사용자의 장바구니 조회
   */
  public CartDto getCartByUser(String authorization) {
    Long userId = getUserIdFromToken(authorization, "장바구니 조회");
    Cart cart = findOrCreateCart(userId);
    return convertToCartDto(cart);
  }

  /**
   * 장바구니에 상품 추가
   */
  @Transactional
  public CartItemDto addCartItem(String authorization, CartItemRequest request) {
    Long userId = getUserIdFromToken(authorization, "장바구니 상품 추가");

    // 사용자 장바구니 조회 또는 생성
    Cart cart = findOrCreateCart(userId);

    // 기존 장바구니 아이템 확인
    Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(
        cart.getId(), request.getProductId());

    // 최종 요청 수량 계산
    Integer finalQuantity = request.getQuantity();
    if (existingItem.isPresent()) {
      finalQuantity += existingItem.get().getQuantity();
    }

    // 상품 존재, 판매 상태, 재고 한번에 검증
    Product product = validateProductForCart(request.getProductId(), finalQuantity);

    CartItem cartItem;
    if (existingItem.isPresent()) {
      // 기존 아이템 수량 증가
      cartItem = existingItem.get();
      cartItem.increaseQuantity(request.getQuantity());
    } else {
      // 새 아이템 생성
      cartItem = CartItem.builder()
          .cart(cart)
          .product(product)
          .quantity(request.getQuantity())
          .unitPrice(product.getPrice())
          .totalPrice(product.getPrice().multiply(new java.math.BigDecimal(request.getQuantity())))
          .build();
    }

    cartItemRepository.save(cartItem);
    cart.addCartItem(cartItem);
    cartRepository.save(cart);

    log.info("상품 {}이(가) 장바구니에 추가되었습니다. 사용자 ID: {}, 수량: {}",
        product.getName(), userId, request.getQuantity());

    return convertToCartItemDto(cartItem);
  }

  /**
   * 장바구니 아이템 수량 변경
   */
  @Transactional
  public CartItemDto updateCartItemQuantity(String authorization, Long cartItemId,
      Integer newQuantity) {
    Long userId = getUserIdFromToken(authorization, "장바구니 아이템 수량 변경");
    CartItem cartItem = findCartItemById(cartItemId);
    validateCartOwnership(userId, cartItem.getCart().getId());

    // 상품 판매 상태 및 재고 검증
    validateProductForCart(cartItem.getProduct().getId(), newQuantity);

    cartItem.setQuantity(newQuantity);
    cartItemRepository.save(cartItem);

    // 장바구니 총액 재계산
    Cart cart = cartItem.getCart();
    cart.recalculateTotals();
    cartRepository.save(cart);

    log.info("장바구니 아이템 수량이 변경되었습니다. 사용자 ID: {}, 아이템 ID: {}, 새 수량: {}",
        userId, cartItemId, newQuantity);

    return convertToCartItemDto(cartItem);
  }

  /**
   * 장바구니에서 아이템 제거
   */
  @Transactional
  public void removeCartItem(String authorization, Long cartItemId) {
    Long userId = getUserIdFromToken(authorization, "장바구니 아이템 제거");
    CartItem cartItem = findCartItemById(cartItemId);
    validateCartOwnership(userId, cartItem.getCart().getId());

    Cart cart = cartItem.getCart();
    cart.removeCartItem(cartItem.getProduct().getId());

    cartItemRepository.delete(cartItem);
    cartRepository.save(cart);

    log.info("장바구니 아이템이 제거되었습니다. 사용자 ID: {}, 아이템 ID: {}", userId, cartItemId);
  }

  /**
   * 장바구니 비우기
   */
  @Transactional
  public void clearCart(String authorization) {
    Long userId = getUserIdFromToken(authorization, "장바구니 비우기");
    Cart cart = findOrCreateCart(userId);
    cart.clear();
    cartRepository.save(cart);

    log.info("장바구니가 비워졌습니다. 사용자 ID: {}", userId);
  }

  /**
   * 장바구니 요약 정보 조회
   */
  public CartSummaryResponse getCartSummary(String authorization) {
    Long userId = getUserIdFromToken(authorization, "장바구니 요약 조회");
    Cart cart = findOrCreateCart(userId);
    CartDto cartDto = convertToCartDto(cart);
    return CartSummaryResponse.from(cartDto);
  }

  /**
   * 장바구니 유효성 검증
   */
  public CartDto validateCart(String authorization) {
    Long userId = getUserIdFromToken(authorization, "장바구니 유효성 검증");
    Cart cart = findOrCreateCart(userId);

    // 각 아이템의 유효성 검증
    for (CartItem cartItem : cart.getCartItems()) {
      // 가격 변경 확인
      cartItem.updateUnitPriceFromProduct();

      // 재고 확인
      if (!cartItem.isStockAvailable()) {
        log.warn("재고 부족: 상품 ID {}, 요청 수량 {}, 사용 가능 수량 {}",
            cartItem.getProduct().getId(),
            cartItem.getQuantity(),
            cartItem.getProduct().getStockQuantity());
      }

      // 판매 상태 확인
      if (!cartItem.isProductAvailable()) {
        log.warn("판매 중지된 상품: 상품 ID {}", cartItem.getProduct().getId());
      }
    }

    cartRepository.save(cart);
    return convertToCartDto(cart);
  }

  // ==============================
  // 공통 구현 메소드
  // ==============================

  /**
   * 사용자 ID로 장바구니 조회, 없으면 새로 생성
   */
  private Cart findOrCreateCart(Long userId) {
    return cartRepository.findByUserId(userId)
        .orElseGet(() -> createNewCart(userId));
  }

  /**
   * 새로운 장바구니 생성
   */
  private Cart createNewCart(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자 ID: " + userId));

    Cart cart = Cart.builder()
        .user(user)
        .totalItems(0)
        .totalPrice(java.math.BigDecimal.ZERO)
        .build();

    return cartRepository.save(cart);
  }

  /**
   * Authorization 토큰에서 사용자 ID 추출
   */
  private Long getUserIdFromToken(String authorization, String context) {
    String email = tokenProcessor.extractEmailFromBearerToken(authorization, context);
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + email));

    return user.getId();
  }


  /**
   * 장바구니용 상품 종합 검증 (존재, 판매상태, 재고)
   */
  private Product validateProductForCart(Long productId, Integer requestedQuantity) {
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND, "상품 ID: " + productId));

    // 판매 상태 확인
    if (!product.getProductStatus().isAvailable()) {
      throw new CustomException(ErrorCode.CART_PRODUCT_UNAVAILABLE, "상품명: " + product.getName());
    }

    // 재고 확인
    if (product.getStockQuantity() == null) {
      throw new CustomException(ErrorCode.PRODUCT_INVALID_STOCK,
          "상품 재고 정보가 없습니다. 상품 ID: " + product.getId());
    }

    if (product.getStockQuantity() < requestedQuantity) {
      String errorMessage = String.format(
          "재고가 부족합니다. 상품명: %s, 현재 재고: %d개, 요청 수량: %d개",
          product.getName(),
          product.getStockQuantity(),
          requestedQuantity);

      log.warn("재고 부족 - {}", errorMessage);
      throw new CustomException(ErrorCode.CART_INSUFFICIENT_STOCK, errorMessage);
    }

    log.debug("상품 검증 통과 - 상품 ID: {}, 요청 수량: {}, 현재 재고: {}",
        product.getId(), requestedQuantity, product.getStockQuantity());

    return product;
  }

  /**
   * 장바구니 아이템 ID로 아이템 조회
   */
  private CartItem findCartItemById(Long cartItemId) {
    return cartItemRepository.findById(cartItemId)
        .orElseThrow(
            () -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND, "아이템 ID: " + cartItemId));
  }

  /**
   * 장바구니 소유권 검증 (사용자가 해당 장바구니의 소유자인지 확인)
   */
  private void validateCartOwnership(Long userId, Long cartId) {
    Cart cart = cartRepository.findById(cartId)
        .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND, "장바구니 ID: " + cartId));

    if (!cart.getUser().getId().equals(userId)) {
      throw new CustomException(ErrorCode.CART_ACCESS_DENIED);
    }
  }

  /**
   * Cart 엔티티를 CartDto로 변환
   */
  private CartDto convertToCartDto(Cart cart) {
    List<CartItemDto> itemDtos = cart.getCartItems().stream()
        .map(this::convertToCartItemDto)
        .toList();

    return CartDto.builder()
        .cartId(cart.getId())
        .totalItems(cart.getTotalItems())
        .totalPrice(cart.getTotalPrice())
        .itemCount(cart.getItemCount())
        .items(itemDtos)
        .build();
  }

  /**
   * CartItem 엔티티를 CartItemDto로 변환 (재고/가격 변경 경고 메시지 포함)
   */
  private CartItemDto convertToCartItemDto(CartItem cartItem) {
    String warningMessage = null;
    if (!cartItem.isStockAvailable()) {
      warningMessage = cartItem.getStockShortageMessage();
    } else if (cartItem.isPriceChanged()) {
      warningMessage = cartItem.getPriceChangeMessage();
    }

    return CartItemDto.builder()
        .cartItemId(cartItem.getId())
        .productId(cartItem.getProduct().getId())
        .productName(cartItem.getProduct().getName())
        .productImage(getProductImage(cartItem.getProduct()))
        .quantity(cartItem.getQuantity())
        .unitPrice(cartItem.getUnitPrice())
        .totalPrice(cartItem.getTotalPrice())
        .brand(cartItem.getProduct().getBrand())
        .condition(
            cartItem.getProduct().getCondition() != null ? cartItem.getProduct().getCondition()
                .name() : null)
        .isStockAvailable(cartItem.isStockAvailable())
        .isPriceChanged(cartItem.isPriceChanged())
        .warningMessage(warningMessage)
        .build();
  }

  /**
   * 상품의 대표 이미지 URL 추출 (첫 번째 이미지)
   */
  private String getProductImage(Product product) {
    return product.getProductImages().isEmpty() ? null
        : product.getProductImages().get(0).getImageUrl();
  }

}
