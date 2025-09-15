package com.oboe.backend.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PaymentInfo {

  @Column(length = 100)
  private String paymentId; // PG사 결제ID

  @Column(length = 100)
  private String transactionId; // 거래ID

  @Enumerated(EnumType.STRING)
  private PaymentStatus paymentStatus; // 결제상태

  private LocalDateTime paidAt; // 결제완료일시
  private LocalDateTime cancelledAt; // 결제취소일시
  private String cancelReason; // 취소사유

  /**
   * 결제완료 처리
   */
  public void markAsCompleted(String paymentId, String transactionId) {
    this.paymentId = paymentId;
    this.transactionId = transactionId;
    this.paymentStatus = PaymentStatus.COMPLETED;
    this.paidAt = LocalDateTime.now();
  }

  /**
   * 결제취소 처리
   */
  public void markAsCancelled(String cancelReason) {
    this.paymentStatus = PaymentStatus.CANCELLED;
    this.cancelledAt = LocalDateTime.now();
    this.cancelReason = cancelReason;
  }

  /**
   * 환불완료 처리
   */
  public void markAsRefunded() {
    this.paymentStatus = PaymentStatus.REFUNDED;
  }

  /**
   * 결제실패 처리
   */
  public void markAsFailed() {
    this.paymentStatus = PaymentStatus.FAILED;
  }
}
