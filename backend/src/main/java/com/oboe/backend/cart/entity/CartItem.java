package com.oboe.backend.cart.entity;

import com.oboe.backend.common.domain.BaseTimeEntity;
import com.oboe.backend.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CartItem extends BaseTimeEntity {

  // 고유 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cart_id", nullable = false)
  private Cart cart;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(nullable = false)
  @Builder.Default
  private Integer quantity = 1; // 상품 수량

  @Column(nullable = false)
  private BigDecimal unitPrice; // 단위 가격 (장바구니 추가 시점의 가격)

  @Column(nullable = false)
  private BigDecimal totalPrice; // 총 가격 (단위가격 × 수량)

  // ==============================
  // 도메인 비즈니스 메서드들
  // ==============================

  /**
   * 수량 증가
   */
  public void increaseQuantity(Integer additionalQuantity) {
    if (additionalQuantity == null || additionalQuantity <= 0) {
      throw new IllegalArgumentException("추가할 수량은 0보다 커야 합니다.");
    }

    this.quantity += additionalQuantity;
    calculateTotalPrice();
  }

  /**
   * 수량 감소
   */
  public void decreaseQuantity(Integer decreaseQuantity) {
    if (decreaseQuantity == null || decreaseQuantity <= 0) {
      throw new IllegalArgumentException("감소할 수량은 0보다 커야 합니다.");
    }

    if (this.quantity <= decreaseQuantity) {
      throw new IllegalArgumentException("수량이 부족합니다. 현재 수량: " + this.quantity);
    }

    this.quantity -= decreaseQuantity;
    calculateTotalPrice();
  }

  /**
   * 수량 설정
   */
  public void setQuantity(Integer quantity) {
    if (quantity == null || quantity <= 0) {
      throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
    }

    this.quantity = quantity;
    calculateTotalPrice();
  }

  /**
   * 총 가격 계산
   */
  private void calculateTotalPrice() {
    if (unitPrice != null && quantity != null) {
      this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
  }

  /**
   * 단위 가격 설정
   */
  public void setUnitPrice(BigDecimal unitPrice) {
    if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("단위 가격은 0 이상이어야 합니다.");
    }

    this.unitPrice = unitPrice;
    calculateTotalPrice();
  }

  /**
   * 상품 가격으로 단위 가격 업데이트
   */
  public void updateUnitPriceFromProduct() {
    if (product != null && product.getPrice() != null) {
      this.unitPrice = product.getPrice();
      calculateTotalPrice();
    }
  }

  /**
   * 장바구니 연결 설정
   */
  public void setCart(Cart cart) {
    this.cart = cart;
  }

  /**
   * 상품 재고 확인
   */
  public boolean isStockAvailable() {
    return product != null && product.getStockQuantity() != null &&
        product.getStockQuantity() >= quantity;
  }

  /**
   * 상품이 판매 중인지 확인
   */
  public boolean isProductAvailable() {
    return product != null && product.getProductStatus() != null &&
        product.getProductStatus().isAvailable();
  }

  /**
   * 장바구니 아이템이 유효한지 확인 (재고, 판매상태 등)
   */
  public boolean isValid() {
    return isStockAvailable() && isProductAvailable();
  }

  /**
   * 현재 상품 가격과 장바구니 가격이 다른지 확인
   */
  public boolean isPriceChanged() {
    if (product == null || product.getPrice() == null || unitPrice == null) {
      return false;
    }

    return !product.getPrice().equals(unitPrice);
  }

  /**
   * 가격 변경으로 인한 메시지 생성
   */
  public String getPriceChangeMessage() {
    if (!isPriceChanged()) {
      return null;
    }

    BigDecimal currentPrice = product.getPrice();
    if (currentPrice.compareTo(unitPrice) > 0) {
      return String.format("상품 가격이 %.0f원에서 %.0f원으로 인상되었습니다.",
          unitPrice, currentPrice);
    } else {
      return String.format("상품 가격이 %.0f원에서 %.0f원으로 인하되었습니다.",
          unitPrice, currentPrice);
    }
  }

  /**
   * 재고 부족 메시지 생성
   */
  public String getStockShortageMessage() {
    if (product == null || product.getStockQuantity() == null) {
      return "재고 정보를 확인할 수 없습니다.";
    }

    if (product.getStockQuantity() < quantity) {
      return String.format("재고가 부족합니다. 현재 재고: %d개, 요청 수량: %d개",
          product.getStockQuantity(), quantity);
    }

    return null;
  }
}
