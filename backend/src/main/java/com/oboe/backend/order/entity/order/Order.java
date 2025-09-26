package com.oboe.backend.order.entity.order;

import com.oboe.backend.common.domain.BaseTimeEntity;
import com.oboe.backend.order.entity.DeliveryInfo;
import com.oboe.backend.order.entity.payment.PaymentInfo;
import com.oboe.backend.order.entity.payment.PaymentMethod;
import com.oboe.backend.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Order extends BaseTimeEntity {

  // 고유 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String orderNumber; // 주문번호 (예: ORD-20241201-001)

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status; // 주문상태

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentMethod paymentMethod; // 결제수단

  @Column(nullable = false)
  private BigDecimal totalAmount; // 총 주문금액

  @Column(nullable = false)
  @Builder.Default
  private BigDecimal deliveryFee = BigDecimal.ZERO; // 배송비

  @Column(nullable = false)
  @Builder.Default
  private BigDecimal discountAmount = BigDecimal.ZERO; // 할인금액

  @Column(nullable = false)
  private BigDecimal finalAmount; // 최종 결제금액

  // 배송 정보
  @Embedded
  private DeliveryInfo deliveryInfo;

  // 결제 정보
  @Embedded
  private PaymentInfo paymentInfo;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<OrderItem> orderItems = new ArrayList<>();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<OrderStatusHistory> statusHistory = new ArrayList<>();

  // ==============================
  // 도메인 비즈니스 메서드들
  // ==============================

  /**
   * 주문번호 생성
   */
  public static String generateOrderNumber() {
    String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
    return String.format("ORD-%s-%s", date, time);
  }

  /**
   * 총 주문금액 계산
   */
  public BigDecimal calculateTotalAmount() {
    return orderItems.stream()
        .map(OrderItem::getTotalPrice)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * 최종 결제금액 계산 (총액 - 할인 + 배송비)
   */
  public BigDecimal calculateFinalAmount() {
    return totalAmount.subtract(discountAmount).add(deliveryFee);
  }

  /**
   * 주문상품 추가
   */
  public void addOrderItem(OrderItem orderItem) {
    orderItems.add(orderItem);
    orderItem.setOrder(this);
    recalculateAmounts();
  }

  /**
   * 주문상품 제거
   */
  public void removeOrderItem(OrderItem orderItem) {
    orderItems.remove(orderItem);
    orderItem.setOrder(null);
    recalculateAmounts();
  }

  /**
   * 금액 재계산
   */
  private void recalculateAmounts() {
    this.totalAmount = calculateTotalAmount();
    this.finalAmount = calculateFinalAmount();
  }

  /**
   * 주문상태 변경
   */
  public void changeStatus(OrderStatus newStatus, String reason) {
    OrderStatus oldStatus = this.status;
    this.status = newStatus;
    
    // 상태변경 이력 추가
    OrderStatusHistory history = OrderStatusHistory.builder()
        .order(this)
        .fromStatus(oldStatus)
        .toStatus(newStatus)
        .reason(reason)
        .build();
    statusHistory.add(history);
  }

  /**
   * 주문상태 자동 계산 (주문상품들의 상태를 기반으로)
   */
  public void calculateStatus() {
    if (orderItems.isEmpty()) {
      this.status = OrderStatus.PENDING;
      return;
    }

    // 모든 상품이 배송완료면 주문도 배송완료
    if (orderItems.stream().allMatch(item -> 
        item.getStatus() == OrderItemStatus.DELIVERED)) {
      this.status = OrderStatus.DELIVERED;
      return;
    }

    // 하나라도 배송중이면 주문은 배송중
    if (orderItems.stream().anyMatch(item -> 
        item.getStatus() == OrderItemStatus.SHIPPED)) {
      this.status = OrderStatus.SHIPPED;
      return;
    }

    // 하나라도 준비중이면 주문은 준비중
    if (orderItems.stream().anyMatch(item -> 
        item.getStatus() == OrderItemStatus.PREPARING)) {
      this.status = OrderStatus.PREPARING;
      return;
    }

    // 모든 상품이 취소되면 주문도 취소
    if (orderItems.stream().allMatch(item -> 
        item.getStatus() == OrderItemStatus.CANCELLED)) {
      this.status = OrderStatus.CANCELLED;
      return;
    }

    this.status = OrderStatus.CONFIRMED;
  }

  /**
   * 주문취소 가능 여부 확인
   */
  public boolean canCancel() {
    return status == OrderStatus.PENDING || 
           status == OrderStatus.CONFIRMED || 
           status == OrderStatus.PREPARING;
  }

  /**
   * 환불 가능 여부 확인
   */
  public boolean canRefund() {
    return status == OrderStatus.DELIVERED || 
           status == OrderStatus.SHIPPED;
  }

  /**
   * 주문 취소 처리
   */
  public void cancel(String reason) {
    if (!canCancel()) {
      throw new IllegalStateException("주문을 취소할 수 없는 상태입니다. 현재 상태: " + status);
    }
    
    // 모든 주문상품 취소
    orderItems.forEach(item -> item.cancel());
    
    // 주문상태 변경
    changeStatus(OrderStatus.CANCELLED, reason);
    
    // 결제 취소
    if (paymentInfo != null) {
      paymentInfo.markAsCancelled(reason);
    }
  }

  /**
   * 배송정보 업데이트
   */
  public void updateDeliveryInfo(String recipientName, String recipientPhone, 
                                String roadAddress, String detailAddress, String zipCode, 
                                String deliveryMemo) {
    if (deliveryInfo == null) {
      deliveryInfo = DeliveryInfo.builder().build();
    }
    deliveryInfo.updateDeliveryInfo(recipientName, recipientPhone, roadAddress, 
                                   detailAddress, zipCode, deliveryMemo);
  }

  /**
   * 배송완료 처리
   */
  public void markAsDelivered(String trackingNumber) {
    if (deliveryInfo != null) {
      deliveryInfo.markAsDelivered(trackingNumber);
    }
    changeStatus(OrderStatus.DELIVERED, "배송완료");
  }

  /**
   * 할인금액 설정
   */
  public void setDiscountAmount(BigDecimal discountAmount) {
    this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
    this.finalAmount = calculateFinalAmount();
  }

  /**
   * 배송비 설정
   */
  public void setDeliveryFee(BigDecimal deliveryFee) {
    this.deliveryFee = deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
    this.finalAmount = calculateFinalAmount();
  }

  /**
   * 총 주문금액 설정
   */
  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    this.finalAmount = calculateFinalAmount();
  }
}
