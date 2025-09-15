package com.oboe.backend.order.entity;

import com.oboe.backend.common.domain.BaseTimeEntity;
import com.oboe.backend.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OrderItem extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(nullable = false)
  private Integer quantity; // 주문수량

  @Column(nullable = false)
  private BigDecimal unitPrice; // 주문당시 단가

  @Column(nullable = false)
  private BigDecimal totalPrice; // 총 가격 (단가 * 수량)

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderItemStatus status; // 주문상품 상태

  // ==============================
  // 도메인 비즈니스 메서드들
  // ==============================

  /**
   * 총 가격 계산
   */
  public BigDecimal calculateTotalPrice() {
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }

  /**
   * 주문상품 상태 변경
   */
  public void changeStatus(OrderItemStatus newStatus) {
    this.status = newStatus;
  }

  /**
   * 주문상품 취소
   */
  public void cancel() {
    if (status == OrderItemStatus.DELIVERED) {
      throw new IllegalStateException("이미 배송완료된 상품은 취소할 수 없습니다.");
    }
    this.status = OrderItemStatus.CANCELLED;
  }

  /**
   * 주문상품 환불
   */
  public void refund() {
    if (status != OrderItemStatus.DELIVERED) {
      throw new IllegalStateException("배송완료된 상품만 환불 가능합니다.");
    }
    this.status = OrderItemStatus.REFUNDED;
  }

  /**
   * 주문상품 교환
   */
  public void exchange() {
    if (status != OrderItemStatus.DELIVERED) {
      throw new IllegalStateException("배송완료된 상품만 교환 가능합니다.");
    }
    this.status = OrderItemStatus.EXCHANGED;
  }

  /**
   * 주문상품 준비중으로 변경
   */
  public void markAsPreparing() {
    if (status != OrderItemStatus.ORDERED) {
      throw new IllegalStateException("주문된 상품만 준비중으로 변경 가능합니다.");
    }
    this.status = OrderItemStatus.PREPARING;
  }

  /**
   * 주문상품 배송중으로 변경
   */
  public void markAsShipped() {
    if (status != OrderItemStatus.PREPARING) {
      throw new IllegalStateException("준비중인 상품만 배송중으로 변경 가능합니다.");
    }
    this.status = OrderItemStatus.SHIPPED;
  }

  /**
   * 주문상품 배송완료로 변경
   */
  public void markAsDelivered() {
    if (status != OrderItemStatus.SHIPPED) {
      throw new IllegalStateException("배송중인 상품만 배송완료로 변경 가능합니다.");
    }
    this.status = OrderItemStatus.DELIVERED;
  }

  /**
   * 취소 가능 여부 확인
   */
  public boolean canCancel() {
    return status == OrderItemStatus.ORDERED || 
           status == OrderItemStatus.PREPARING;
  }

  /**
   * 환불 가능 여부 확인
   */
  public boolean canRefund() {
    return status == OrderItemStatus.DELIVERED;
  }

  /**
   * 교환 가능 여부 확인
   */
  public boolean canExchange() {
    return status == OrderItemStatus.DELIVERED;
  }

  /**
   * Order와의 양방향 관계 설정을 위한 메서드
   */
  public void setOrder(Order order) {
    this.order = order;
  }

  /**
   * 주문상품 생성 팩토리 메서드
   */
  public static OrderItem create(Product product, Integer quantity) {
    if (product.getStockQuantity() < quantity) {
      throw new IllegalArgumentException("재고가 부족합니다. 요청수량: " + quantity + ", 재고: " + product.getStockQuantity());
    }

    BigDecimal unitPrice = product.getPrice();
    BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

    return OrderItem.builder()
        .product(product)
        .quantity(quantity)
        .unitPrice(unitPrice)
        .totalPrice(totalPrice)
        .status(OrderItemStatus.ORDERED)
        .build();
  }
}
