package com.oboe.backend.cart.entity;

import com.oboe.backend.common.domain.BaseTimeEntity;
import com.oboe.backend.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Cart extends BaseTimeEntity {

  // 고유 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<CartItem> cartItems = new ArrayList<>();

  @Column(nullable = false)
  @Builder.Default
  private Integer totalItems = 0; // 총 상품 개수

  @Column(nullable = false)
  @Builder.Default
  private BigDecimal totalPrice = BigDecimal.ZERO; // 총 금액

  @Column(nullable = false)
  @Builder.Default
  private Boolean isActive = true; // 장바구니 활성화 여부

  // ==============================
  // 도메인 비즈니스 메서드들
  // ==============================

  /**
   * 장바구니에 상품 추가
   */
  public void addCartItem(CartItem cartItem) {
    // 이미 같은 상품이 있는지 확인
    CartItem existingItem = findCartItemByProduct(cartItem.getProduct().getId());
    
    if (existingItem != null) {
      // 기존 상품이 있으면 수량 증가
      existingItem.increaseQuantity(cartItem.getQuantity());
    } else {
      // 새로운 상품이면 추가
      cartItems.add(cartItem);
      cartItem.setCart(this);
    }
    
    recalculateTotals();
  }

  /**
   * 장바구니에서 상품 제거
   */
  public void removeCartItem(Long productId) {
    cartItems.removeIf(item -> item.getProduct().getId().equals(productId));
    recalculateTotals();
  }

  /**
   * 장바구니 상품 수량 변경
   */
  public void updateCartItemQuantity(Long productId, Integer newQuantity) {
    CartItem cartItem = findCartItemByProduct(productId);
    if (cartItem != null) {
      if (newQuantity <= 0) {
        removeCartItem(productId);
      } else {
        cartItem.setQuantity(newQuantity);
        recalculateTotals();
      }
    }
  }

  /**
   * 특정 상품의 장바구니 아이템 찾기
   */
  private CartItem findCartItemByProduct(Long productId) {
    return cartItems.stream()
        .filter(item -> item.getProduct().getId().equals(productId))
        .findFirst()
        .orElse(null);
  }

  /**
   * 총 개수와 총 금액 재계산
   */
  private void recalculateTotals() {
    this.totalItems = cartItems.stream()
        .mapToInt(CartItem::getQuantity)
        .sum();
    
    this.totalPrice = cartItems.stream()
        .map(CartItem::getTotalPrice)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * 장바구니 비우기
   */
  public void clear() {
    cartItems.clear();
    recalculateTotals();
  }

  /**
   * 장바구니 비활성화
   */
  public void deactivate() {
    this.isActive = false;
  }

  /**
   * 장바구니 활성화
   */
  public void activate() {
    this.isActive = true;
  }

  /**
   * 장바구니가 비어있는지 확인
   */
  public boolean isEmpty() {
    return cartItems.isEmpty();
  }

  /**
   * 장바구니에 상품이 있는지 확인
   */
  public boolean hasProduct(Long productId) {
    return findCartItemByProduct(productId) != null;
  }

  /**
   * 특정 상품의 수량 반환
   */
  public Integer getProductQuantity(Long productId) {
    CartItem cartItem = findCartItemByProduct(productId);
    return cartItem != null ? cartItem.getQuantity() : 0;
  }

  /**
   * 장바구니 아이템 수 반환
   */
  public int getItemCount() {
    return cartItems.size();
  }

  /**
   * 주문 가능 여부 확인 (장바구니가 비어있지 않고 활성화되어 있어야 함)
   */
  public boolean canPlaceOrder() {
    return isActive && !isEmpty();
  }
}
