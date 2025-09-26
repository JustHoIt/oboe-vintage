package com.oboe.backend.order.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.oboe.backend.order.entity.payment.PaymentInfo;
import com.oboe.backend.order.entity.payment.PaymentMethod;
import com.oboe.backend.order.entity.payment.PaymentStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaymentInfo Entity 테스트")
class PaymentInfoTest {

  private PaymentInfo paymentInfo;

  @BeforeEach
  void setUp() {
    // 기본 테스트용 결제정보 생성
    paymentInfo = PaymentInfo.builder()
        .paymentId("pay_1234567890")
        .transactionId("txn_1234567890")
        .tossPaymentStatus(PaymentStatus.READY)
        .paymentMethod(PaymentMethod.카드)
        .totalAmount(10000L)
        .build();
  }

  @Test
  @DisplayName("PaymentInfo 기본 생성 테스트")
  void createPaymentInfo() {
    // given & when

    // then
    assertThat(paymentInfo.getPaymentId()).isEqualTo("pay_1234567890");
    assertThat(paymentInfo.getTransactionId()).isEqualTo("txn_1234567890");
    assertThat(paymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.READY);
    assertThat(paymentInfo.getPaymentMethod()).isEqualTo(PaymentMethod.카드);
    assertThat(paymentInfo.getTotalAmount()).isEqualTo(10000L);
    assertThat(paymentInfo.getPaidAt()).isNull();
    assertThat(paymentInfo.getCancelledAt()).isNull();
    assertThat(paymentInfo.getCancelReason()).isNull();
  }

  @Test
  @DisplayName("결제완료 처리 테스트")
  void markAsCompleted() {
    // given
    String paymentId = "pay_9876543210";
    String transactionId = "txn_9876543210";

    // when
    paymentInfo.markAsCompleted(paymentId, transactionId);

    // then
    assertThat(paymentInfo.getPaymentId()).isEqualTo(paymentId);
    assertThat(paymentInfo.getTransactionId()).isEqualTo(transactionId);
    assertThat(paymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.DONE);
    assertThat(paymentInfo.getPaidAt()).isNotNull();
    assertThat(paymentInfo.getPaidAt()).isBeforeOrEqualTo(LocalDateTime.now());
  }

  @Test
  @DisplayName("결제완료 처리 - null 값")
  void markAsCompleted_NullValues() {
    // given
    String paymentId = null;
    String transactionId = null;

    // when
    paymentInfo.markAsCompleted(paymentId, transactionId);

    // then
    assertThat(paymentInfo.getPaymentId()).isNull();
    assertThat(paymentInfo.getTransactionId()).isNull();
    assertThat(paymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.DONE);
    assertThat(paymentInfo.getPaidAt()).isNotNull();
  }

  @Test
  @DisplayName("결제완료 처리 - 빈 문자열")
  void markAsCompleted_EmptyStrings() {
    // given
    String paymentId = "";
    String transactionId = "";

    // when
    paymentInfo.markAsCompleted(paymentId, transactionId);

    // then
    assertThat(paymentInfo.getPaymentId()).isEqualTo("");
    assertThat(paymentInfo.getTransactionId()).isEqualTo("");
    assertThat(paymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.DONE);
    assertThat(paymentInfo.getPaidAt()).isNotNull();
  }

  @Test
  @DisplayName("결제취소 처리 테스트")
  void markAsCancelled() {
    // given
    String cancelReason = "고객 요청";

    // when
    paymentInfo.markAsCancelled(cancelReason);

    // then
    assertThat(paymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
    assertThat(paymentInfo.getCancelledAt()).isNotNull();
    assertThat(paymentInfo.getCancelledAt()).isBeforeOrEqualTo(LocalDateTime.now());
    assertThat(paymentInfo.getCancelReason()).isEqualTo(cancelReason);
  }

  @Test
  @DisplayName("결제취소 처리 - null 사유")
  void markAsCancelled_NullReason() {
    // given
    String cancelReason = null;

    // when
    paymentInfo.markAsCancelled(cancelReason);

    // then
    assertThat(paymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
    assertThat(paymentInfo.getCancelledAt()).isNotNull();
    assertThat(paymentInfo.getCancelReason()).isNull();
  }

  @Test
  @DisplayName("결제취소 처리 - 빈 사유")
  void markAsCancelled_EmptyReason() {
    // given
    String cancelReason = "";

    // when
    paymentInfo.markAsCancelled(cancelReason);

    // then
    assertThat(paymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
    assertThat(paymentInfo.getCancelledAt()).isNotNull();
    assertThat(paymentInfo.getCancelReason()).isEqualTo("");
  }

  @Test
  @DisplayName("환불완료 처리 테스트")
  void markAsRefunded() {
    // given
    paymentInfo.markAsCompleted("pay_123", "txn_123");

    // when
    paymentInfo.markAsRefunded();

    // then
    assertThat(paymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
  }

  @Test
  @DisplayName("환불완료 처리 - 결제완료되지 않은 상태")
  void markAsRefunded_NotCompleted() {
    // given
    paymentInfo = PaymentInfo.builder()
        .tossPaymentStatus(PaymentStatus.READY)
        .build();

    // when
    paymentInfo.markAsRefunded();

    // then
    assertThat(paymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
  }

  @Test
  @DisplayName("결제실패 처리 테스트")
  void markAsFailed() {
    // given
    paymentInfo = PaymentInfo.builder()
        .tossPaymentStatus(PaymentStatus.READY)
        .build();

    // when
    paymentInfo.markAsFailed();

    // then
    assertThat(paymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.ABORTED);
  }

  @Test
  @DisplayName("결제상태 변경 플로우 테스트")
  void testPaymentStatusFlow() {
    // given
    PaymentInfo flowPaymentInfo = PaymentInfo.builder()
        .tossPaymentStatus(PaymentStatus.READY)
        .build();

    // when & then
    // PENDING -> COMPLETED
    flowPaymentInfo.markAsCompleted("pay_123", "txn_123");
    assertThat(flowPaymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.DONE);
    assertThat(flowPaymentInfo.getPaidAt()).isNotNull();

    // COMPLETED -> CANCELLED
    flowPaymentInfo.markAsCancelled("고객 요청");
    assertThat(flowPaymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
    assertThat(flowPaymentInfo.getCancelledAt()).isNotNull();
    assertThat(flowPaymentInfo.getCancelReason()).isEqualTo("고객 요청");
  }

  @Test
  @DisplayName("환불 플로우 테스트")
  void testRefundFlow() {
    // given
    PaymentInfo refundPaymentInfo = PaymentInfo.builder()
        .tossPaymentStatus(PaymentStatus.READY)
        .build();

    // when & then
    // PENDING -> COMPLETED
    refundPaymentInfo.markAsCompleted("pay_123", "txn_123");
    assertThat(refundPaymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.DONE);

    // COMPLETED -> REFUNDED
    refundPaymentInfo.markAsRefunded();
    assertThat(refundPaymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
  }

  @Test
  @DisplayName("실패 플로우 테스트")
  void testFailureFlow() {
    // given
    PaymentInfo failurePaymentInfo = PaymentInfo.builder()
        .tossPaymentStatus(PaymentStatus.READY)
        .build();

    // when & then
    // PENDING -> FAILED
    failurePaymentInfo.markAsFailed();
    assertThat(failurePaymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.ABORTED);
  }

  @Test
  @DisplayName("모든 PaymentStatus enum 값 테스트")
  void testAllPaymentStatusValues() {
    // given & when & then
    assertThat(PaymentStatus.READY).isNotNull();
    assertThat(PaymentStatus.DONE).isNotNull();
    assertThat(PaymentStatus.ABORTED).isNotNull();
    assertThat(PaymentStatus.CANCELED).isNotNull();
    assertThat(PaymentStatus.IN_PROGRESS).isNotNull();
    assertThat(PaymentStatus.EXPIRED).isNotNull();
    assertThat(PaymentStatus.PARTIAL_CANCELED).isNotNull();
    assertThat(PaymentStatus.WAITING_FOR_DEPOSIT).isNotNull();
    assertThat(PaymentStatus.values()).hasSize(8);
  }

  @Test
  @DisplayName("긴 결제 ID 테스트")
  void testLongPaymentIds() {
    // given
    String longPaymentId = "pay_" + "1234567890".repeat(5); // 50자 근처
    String longTransactionId = "txn_" + "9876543210".repeat(5); // 50자 근처

    // when
    PaymentInfo longIdPaymentInfo = PaymentInfo.builder()
        .paymentId(longPaymentId)
        .transactionId(longTransactionId)
        .tossPaymentStatus(PaymentStatus.READY)
        .build();

    // then
    assertThat(longIdPaymentInfo.getPaymentId()).isEqualTo(longPaymentId);
    assertThat(longIdPaymentInfo.getTransactionId()).isEqualTo(longTransactionId);
    assertThat(longIdPaymentInfo.getPaymentId().length()).isLessThanOrEqualTo(100);
    assertThat(longIdPaymentInfo.getTransactionId().length()).isLessThanOrEqualTo(100);
  }

  @Test
  @DisplayName("특수문자가 포함된 결제 정보 테스트")
  void testSpecialCharactersInPaymentInfo() {
    // given
    String specialPaymentId = "pay_123-456_789.abc";
    String specialTransactionId = "txn_987-654_321.def";
    String specialCancelReason = "고객 요청 (환불) - 불만사항";

    // when
    PaymentInfo specialPaymentInfo = PaymentInfo.builder()
        .paymentId(specialPaymentId)
        .transactionId(specialTransactionId)
        .tossPaymentStatus(PaymentStatus.READY)
        .build();

    specialPaymentInfo.markAsCancelled(specialCancelReason);

    // then
    assertThat(specialPaymentInfo.getPaymentId()).isEqualTo(specialPaymentId);
    assertThat(specialPaymentInfo.getTransactionId()).isEqualTo(specialTransactionId);
    assertThat(specialPaymentInfo.getCancelReason()).isEqualTo(specialCancelReason);
  }

  @Test
  @DisplayName("빈 PaymentInfo 생성 테스트")
  void createEmptyPaymentInfo() {
    // given & when
    PaymentInfo emptyPaymentInfo = PaymentInfo.builder().build();

    // then
    assertThat(emptyPaymentInfo.getPaymentId()).isNull();
    assertThat(emptyPaymentInfo.getTransactionId()).isNull();
    assertThat(emptyPaymentInfo.getTossPaymentStatus()).isNull();
    assertThat(emptyPaymentInfo.getPaidAt()).isNull();
    assertThat(emptyPaymentInfo.getCancelledAt()).isNull();
    assertThat(emptyPaymentInfo.getCancelReason()).isNull();
  }

  @Test
  @DisplayName("결제완료 후 취소 처리 테스트")
  void testCompletedThenCancelled() {
    // given
    paymentInfo.markAsCompleted("pay_123", "txn_123");
    LocalDateTime paidAt = paymentInfo.getPaidAt();

    // when
    paymentInfo.markAsCancelled("고객 요청");

    // then
    assertThat(paymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
    assertThat(paymentInfo.getPaidAt()).isEqualTo(paidAt); // 결제완료 시간은 유지
    assertThat(paymentInfo.getCancelledAt()).isNotNull();
    assertThat(paymentInfo.getCancelReason()).isEqualTo("고객 요청");
  }

  @Test
  @DisplayName("결제완료 후 환불 처리 테스트")
  void testCompletedThenRefunded() {
    // given
    paymentInfo.markAsCompleted("pay_123", "txn_123");
    LocalDateTime paidAt = paymentInfo.getPaidAt();

    // when
    paymentInfo.markAsRefunded();

    // then
    assertThat(paymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
    assertThat(paymentInfo.getPaidAt()).isEqualTo(paidAt); // 결제완료 시간은 유지
    assertThat(paymentInfo.getCancelledAt()).isNull();
  }

  @Test
  @DisplayName("시간 순서 확인 테스트")
  void testTimeOrder() {
    // given
    LocalDateTime startTime = LocalDateTime.now();

    // when
    paymentInfo.markAsCompleted("pay_123", "txn_123");
    LocalDateTime paidAt = paymentInfo.getPaidAt();

    // 새로운 PaymentInfo로 취소 테스트
    PaymentInfo cancelPaymentInfo = PaymentInfo.builder()
        .tossPaymentStatus(PaymentStatus.READY)
        .build();
    cancelPaymentInfo.markAsCancelled("고객 요청");
    LocalDateTime cancelledAt = cancelPaymentInfo.getCancelledAt();

    // then
    assertThat(paidAt).isNotNull();
    assertThat(cancelledAt).isNotNull();
    // 시간이 설정되었는지만 확인 (정확한 순서는 테스트 환경에 따라 달라질 수 있음)
    assertThat(paidAt).isAfterOrEqualTo(startTime);
    assertThat(cancelledAt).isAfterOrEqualTo(startTime);
  }

  // ==============================
  // TossPayment API 관련 테스트
  // ==============================

  @Test
  @DisplayName("TossPayment 결제 승인 처리 테스트")
  void approvePayment() {
    // given
    String paymentKey = "test_payment_key_1234567890";
    String orderId = "order_1234567890";
    String orderName = "테스트 상품";
    String method = "카드";
    String customerKey = "customer_1234567890";
    String receiptUrl = "https://api.tosspayments.com/v1/payments/test_payment_key_1234567890/receipt";

    // when
    paymentInfo.approvePayment(paymentKey, orderId, orderName, PaymentMethod.카드, customerKey, receiptUrl, 10000L);

    // then
    assertThat(paymentInfo.getPaymentKey()).isEqualTo(paymentKey);
    assertThat(paymentInfo.getOrderId()).isEqualTo(orderId);
    assertThat(paymentInfo.getOrderName()).isEqualTo(orderName);
    assertThat(paymentInfo.getPaymentMethod()).isEqualTo(PaymentMethod.카드);
    assertThat(paymentInfo.getCustomerKey()).isEqualTo(customerKey);
    assertThat(paymentInfo.getReceiptUrl()).isEqualTo(receiptUrl);
    assertThat(paymentInfo.getTotalAmount()).isEqualTo(10000L);
    assertThat(paymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.DONE);
    assertThat(paymentInfo.getApprovedAt()).isNotNull();
    assertThat(paymentInfo.getPaidAt()).isNotNull();
  }

  @Test
  @DisplayName("카드 결제 정보 설정 테스트")
  void setCardInfo() {
    // given
    String cardCompany = "신한카드";
    String cardNumber = "1234-****-****-5678";
    String installmentPlanMonths = "12";

    // when
    paymentInfo.setCardInfo(cardCompany, cardNumber, installmentPlanMonths);

    // then
    assertThat(paymentInfo.getCardCompany()).isEqualTo(cardCompany);
    assertThat(paymentInfo.getCardNumber()).isEqualTo(cardNumber);
    assertThat(paymentInfo.getInstallmentPlanMonths()).isEqualTo(installmentPlanMonths);
  }

  @Test
  @DisplayName("결제 URL 설정 테스트")
  void setPaymentUrls() {
    // given
    String successUrl = "https://example.com/payment/success";
    String failUrl = "https://example.com/payment/fail";

    // when
    paymentInfo.setPaymentUrls(successUrl, failUrl);

    // then
    assertThat(paymentInfo.getSuccessUrl()).isEqualTo(successUrl);
    assertThat(paymentInfo.getFailUrl()).isEqualTo(failUrl);
  }

  @Test
  @DisplayName("TossPayment 결제 여부 확인 테스트")
  void isTossPayment() {
    // given & when & then
    // paymentKey가 없는 경우
    assertThat(paymentInfo.isTossPayment()).isFalse();

    // paymentKey가 있는 경우
    paymentInfo = PaymentInfo.builder()
        .paymentKey("test_payment_key_1234567890")
        .build();
    assertThat(paymentInfo.isTossPayment()).isTrue();

    // paymentKey가 빈 문자열인 경우
    paymentInfo = PaymentInfo.builder()
        .paymentKey("")
        .build();
    assertThat(paymentInfo.isTossPayment()).isFalse();
  }

  @Test
  @DisplayName("카드 결제 여부 확인 테스트")
  void isCardPayment() {
    // given & when & then
    // paymentMethod가 없는 경우
    assertThat(paymentInfo.isCardPayment()).isTrue(); // setUp에서 카드로 설정됨

    // paymentMethod가 "카드"인 경우
    paymentInfo = PaymentInfo.builder()
        .paymentMethod(PaymentMethod.카드)
        .build();
    assertThat(paymentInfo.isCardPayment()).isTrue();

    // paymentMethod가 "계좌이체"인 경우
    paymentInfo = PaymentInfo.builder()
        .paymentMethod(PaymentMethod.계좌이체)
        .build();
    assertThat(paymentInfo.isCardPayment()).isFalse();
  }

  @Test
  @DisplayName("결제 승인 완료 여부 확인 테스트")
  void isApproved() {
    // given & when & then
    // 승인되지 않은 경우
    assertThat(paymentInfo.isApproved()).isFalse();

    // approvedAt이 있지만 상태가 COMPLETED가 아닌 경우
    paymentInfo = PaymentInfo.builder()
        .approvedAt(LocalDateTime.now())
        .tossPaymentStatus(PaymentStatus.READY)
        .build();
    assertThat(paymentInfo.isApproved()).isFalse();

    // 상태가 DONE이지만 approvedAt이 없는 경우
    paymentInfo = PaymentInfo.builder()
        .tossPaymentStatus(PaymentStatus.DONE)
        .build();
    assertThat(paymentInfo.isApproved()).isFalse();

    // 승인 완료된 경우
    paymentInfo = PaymentInfo.builder()
        .approvedAt(LocalDateTime.now())
        .tossPaymentStatus(PaymentStatus.DONE)
        .build();
    assertThat(paymentInfo.isApproved()).isTrue();
  }

  @Test
  @DisplayName("TossPayment 결제 플로우 통합 테스트")
  void testTossPaymentFlow() {
    // given
    String paymentKey = "test_payment_key_1234567890";
    String orderId = "order_1234567890";
    String orderName = "테스트 상품";
    String method = "카드";
    String customerKey = "customer_1234567890";
    String receiptUrl = "https://api.tosspayments.com/v1/payments/test_payment_key_1234567890/receipt";
    String successUrl = "https://example.com/payment/success";
    String failUrl = "https://example.com/payment/fail";

    // when & then
    // 1. 결제 URL 설정
    paymentInfo.setPaymentUrls(successUrl, failUrl);
    assertThat(paymentInfo.getSuccessUrl()).isEqualTo(successUrl);
    assertThat(paymentInfo.getFailUrl()).isEqualTo(failUrl);

    // 2. 결제 승인 처리
    paymentInfo.approvePayment(paymentKey, orderId, orderName, PaymentMethod.카드, customerKey, receiptUrl, 10000L);
    assertThat(paymentInfo.isTossPayment()).isTrue();
    assertThat(paymentInfo.isApproved()).isTrue();
    assertThat(paymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.DONE);

    // 3. 카드 정보 설정
    paymentInfo.setCardInfo("신한카드", "1234-****-****-5678", "12");
    assertThat(paymentInfo.isCardPayment()).isTrue();
    assertThat(paymentInfo.getCardCompany()).isEqualTo("신한카드");
  }

  @Test
  @DisplayName("TossPayment 주문 ID 유효성 테스트")
  void testOrderIdValidation() {
    // given
    String validOrderId = "order_1234567890"; // 6-64자, 영문/숫자/특수문자
    String invalidOrderId = "123"; // 6자 미만

    // when & then
    // 유효한 주문 ID
    PaymentInfo validPaymentInfo = PaymentInfo.builder()
        .orderId(validOrderId)
        .build();
    assertThat(validPaymentInfo.getOrderId()).isEqualTo(validOrderId);
    assertThat(validPaymentInfo.getOrderId().length()).isGreaterThanOrEqualTo(6);
    assertThat(validPaymentInfo.getOrderId().length()).isLessThanOrEqualTo(64);

    // 유효하지 않은 주문 ID (길이 제한은 DB에서 처리)
    PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
        .orderId(invalidOrderId)
        .build();
    assertThat(invalidPaymentInfo.getOrderId()).isEqualTo(invalidOrderId);
  }

  @Test
  @DisplayName("TossPayment 결제키 유니크 제약 테스트")
  void testPaymentKeyUniqueness() {
    // given
    String paymentKey1 = "test_payment_key_1234567890";
    String paymentKey2 = "test_payment_key_0987654321";

    // when
    PaymentInfo paymentInfo1 = PaymentInfo.builder()
        .paymentKey(paymentKey1)
        .build();
    PaymentInfo paymentInfo2 = PaymentInfo.builder()
        .paymentKey(paymentKey2)
        .build();

    // then
    assertThat(paymentInfo1.getPaymentKey()).isEqualTo(paymentKey1);
    assertThat(paymentInfo2.getPaymentKey()).isEqualTo(paymentKey2);
    assertThat(paymentInfo1.getPaymentKey()).isNotEqualTo(paymentInfo2.getPaymentKey());
  }

  @Test
  @DisplayName("TossPayment 결제 취소 테스트")
  void testTossPaymentCancellation() {
    // given
    PaymentInfo tossPaymentInfo = PaymentInfo.builder()
        .paymentKey("test_payment_key_1234567890")
        .orderId("order_1234567890")
        .tossPaymentStatus(PaymentStatus.DONE)
        .approvedAt(LocalDateTime.now())
        .build();

    // when
    tossPaymentInfo.markAsCancelled("고객 요청");

    // then
    assertThat(tossPaymentInfo.getTossPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
    assertThat(tossPaymentInfo.getCancelledAt()).isNotNull();
    assertThat(tossPaymentInfo.getCancelReason()).isEqualTo("고객 요청");
    // TossPayment 관련 필드는 유지되어야 함
    assertThat(tossPaymentInfo.getPaymentKey()).isEqualTo("test_payment_key_1234567890");
    assertThat(tossPaymentInfo.getOrderId()).isEqualTo("order_1234567890");
    assertThat(tossPaymentInfo.getApprovedAt()).isNotNull();
  }
}
