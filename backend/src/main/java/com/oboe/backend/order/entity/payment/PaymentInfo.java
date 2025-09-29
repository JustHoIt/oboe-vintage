package com.oboe.backend.order.entity.payment;

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

  // TossPayment API 필수 필드들
  @Column(length = 200, unique = true)
  private String paymentKey; // Toss Payments에서 제공하는 결제에 대한 식별 값(필수)

  @Column(length = 100, unique = true)
  private String orderId; // 주문 고유 식별자 (6-64자)

  @Column(length = 100)
  private String orderName; // 주문명

  @Enumerated(value = EnumType.STRING)
  @Column(nullable = false)
  private PaymentMethod paymentMethod; // 결제수단 (카드, 계좌이체, 가상계좌 등)

  @Enumerated(value = EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus tossPaymentStatus;  // 결제 상태

  @Column(length = 100)
  private String customerKey; // 고객 식별자

  private long totalAmount; // 총 결제 금액

  // 기존 필드들
  @Column(length = 100)
  private String paymentId; // PG사 결제ID (레거시 호환)

  @Column(length = 100)
  private String transactionId; // 거래ID (레거시 호환)
  private String receiptUrl; // 영수증 URL
  private String successUrl; // 결제 성공 리디렉션 URL
  private String failUrl; // 결제 실패 리디렉션 URL

  // 카드 정보 (카드 결제 시)
  @Column(length = 50)
  private String cardCompany; // 카드사

  @Column(length = 20)
  private String cardNumber; // 마스킹된 카드번호

  @Column(length = 10)
  private String installmentPlanMonths; // 할부개월

  // 기존 시간 필드들
  private LocalDateTime paidAt; // 결제완료일시
  private LocalDateTime approvedAt; // 결제 승인 시간
  private LocalDateTime cancelledAt; // 결제취소일시

  private String cancelReason; // 취소사유

  /**
   * TossPayment 결제 승인 처리
   */
  public void approvePayment(String paymentKey, String orderId, String orderName, 
                           PaymentMethod paymentMethod, String customerKey, String receiptUrl, long totalAmount) {
    this.paymentKey = paymentKey;
    this.orderId = orderId;
    this.orderName = orderName;
    this.paymentMethod = paymentMethod;
    this.customerKey = customerKey;
    this.receiptUrl = receiptUrl;
    this.totalAmount = totalAmount;
    this.tossPaymentStatus = PaymentStatus.DONE;
    this.approvedAt = LocalDateTime.now();
    this.paidAt = LocalDateTime.now();
  }

  /**
   * 카드 결제 정보 설정
   */
  public void setCardInfo(String cardCompany, String cardNumber, String installmentPlanMonths) {
    this.cardCompany = cardCompany;
    this.cardNumber = cardNumber;
    this.installmentPlanMonths = installmentPlanMonths;
  }

  /**
   * 결제 URL 설정
   */
  public void setPaymentUrls(String successUrl, String failUrl) {
    this.successUrl = successUrl;
    this.failUrl = failUrl;
  }

  /**
   * 결제완료 처리 (레거시 호환)
   */
  public void markAsCompleted(String paymentId, String transactionId) {
    this.paymentId = paymentId;
    this.transactionId = transactionId;
    this.tossPaymentStatus = PaymentStatus.DONE;
    this.paidAt = LocalDateTime.now();
  }

  /**
   * 결제취소 처리
   */
  public void markAsCancelled(String cancelReason) {
    this.tossPaymentStatus = PaymentStatus.CANCELED;
    this.cancelledAt = LocalDateTime.now();
    this.cancelReason = cancelReason;
  }

  /**
   * 환불완료 처리
   */
  public void markAsRefunded() {
    this.tossPaymentStatus = PaymentStatus.CANCELED; // TossPayment에서는 환불도 CANCELED 상태
  }

  /**
   * 결제실패 처리
   */
  public void markAsFailed() {
    this.tossPaymentStatus = PaymentStatus.ABORTED;
  }

  /**
   * TossPayment 결제 상태 확인
   */
  public boolean isTossPayment() {
    return paymentKey != null && !paymentKey.trim().isEmpty();
  }

  /**
   * 카드 결제 여부 확인
   */
  public boolean isCardPayment() {
    return PaymentMethod.카드.equals(paymentMethod);
  }

  /**
   * 결제 승인 완료 여부 확인
   */
  public boolean isApproved() {
    return approvedAt != null && tossPaymentStatus == PaymentStatus.DONE;
  }
}
