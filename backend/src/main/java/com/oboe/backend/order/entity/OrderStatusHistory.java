package com.oboe.backend.order.entity;

import com.oboe.backend.common.domain.BaseTimeEntity;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OrderStatusHistory extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus fromStatus; // 이전 상태

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus toStatus; // 변경된 상태

  @Column(length = 500)
  private String reason; // 상태변경 사유

  @Column(length = 1000)
  private String memo; // 관리자 메모

  // ==============================
  // 도메인 비즈니스 메서드들
  // ==============================

  /**
   * 상태변경 이력 생성
   */
  public static OrderStatusHistory create(Order order, OrderStatus fromStatus, 
                                        OrderStatus toStatus, String reason, String memo) {
    return OrderStatusHistory.builder()
        .order(order)
        .fromStatus(fromStatus)
        .toStatus(toStatus)
        .reason(reason)
        .memo(memo)
        .build();
  }

  /**
   * 메모 업데이트
   */
  public void updateMemo(String memo) {
    this.memo = memo;
  }

  /**
   * 사유 업데이트
   */
  public void updateReason(String reason) {
    this.reason = reason;
  }

  /**
   * 상태변경이 같은지 확인
   */
  public boolean isSameStatusChange(OrderStatus from, OrderStatus to) {
    return this.fromStatus == from && this.toStatus == to;
  }

  /**
   * 취소 관련 상태변경인지 확인
   */
  public boolean isCancellation() {
    return toStatus == OrderStatus.CANCELLED;
  }

  /**
   * 환불 관련 상태변경인지 확인
   */
  public boolean isRefund() {
    return toStatus == OrderStatus.REFUNDED;
  }

  /**
   * 배송 관련 상태변경인지 확인
   */
  public boolean isDeliveryRelated() {
    return toStatus == OrderStatus.SHIPPED || toStatus == OrderStatus.DELIVERED;
  }
}
