package com.oboe.backend.order.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaymentInfo 임베디드 클래스 테스트")
class PaymentInfoTest {

  private PaymentInfo paymentInfo;

  @BeforeEach
  void setUp() {
    // 기본 테스트용 결제정보 생성
    paymentInfo = PaymentInfo.builder()
        .paymentId("pay_1234567890")
        .transactionId("txn_1234567890")
        .paymentStatus(PaymentStatus.PENDING)
        .build();
  }

  @Test
  @DisplayName("PaymentInfo 기본 생성 테스트")
  void createPaymentInfo() {
    // given & when

    // then
    assertThat(paymentInfo.getPaymentId()).isEqualTo("pay_1234567890");
    assertThat(paymentInfo.getTransactionId()).isEqualTo("txn_1234567890");
    assertThat(paymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
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
    assertThat(paymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
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
    assertThat(paymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
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
    assertThat(paymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
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
    assertThat(paymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
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
    assertThat(paymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
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
    assertThat(paymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
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
    assertThat(paymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
  }

  @Test
  @DisplayName("환불완료 처리 - 결제완료되지 않은 상태")
  void markAsRefunded_NotCompleted() {
    // given
    paymentInfo = PaymentInfo.builder()
        .paymentStatus(PaymentStatus.PENDING)
        .build();

    // when
    paymentInfo.markAsRefunded();

    // then
    assertThat(paymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
  }

  @Test
  @DisplayName("결제실패 처리 테스트")
  void markAsFailed() {
    // given
    paymentInfo = PaymentInfo.builder()
        .paymentStatus(PaymentStatus.PENDING)
        .build();

    // when
    paymentInfo.markAsFailed();

    // then
    assertThat(paymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
  }

  @Test
  @DisplayName("결제상태 변경 플로우 테스트")
  void testPaymentStatusFlow() {
    // given
    PaymentInfo flowPaymentInfo = PaymentInfo.builder()
        .paymentStatus(PaymentStatus.PENDING)
        .build();

    // when & then
    // PENDING -> COMPLETED
    flowPaymentInfo.markAsCompleted("pay_123", "txn_123");
    assertThat(flowPaymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
    assertThat(flowPaymentInfo.getPaidAt()).isNotNull();

    // COMPLETED -> CANCELLED
    flowPaymentInfo.markAsCancelled("고객 요청");
    assertThat(flowPaymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
    assertThat(flowPaymentInfo.getCancelledAt()).isNotNull();
    assertThat(flowPaymentInfo.getCancelReason()).isEqualTo("고객 요청");
  }

  @Test
  @DisplayName("환불 플로우 테스트")
  void testRefundFlow() {
    // given
    PaymentInfo refundPaymentInfo = PaymentInfo.builder()
        .paymentStatus(PaymentStatus.PENDING)
        .build();

    // when & then
    // PENDING -> COMPLETED
    refundPaymentInfo.markAsCompleted("pay_123", "txn_123");
    assertThat(refundPaymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);

    // COMPLETED -> REFUNDED
    refundPaymentInfo.markAsRefunded();
    assertThat(refundPaymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
  }

  @Test
  @DisplayName("실패 플로우 테스트")
  void testFailureFlow() {
    // given
    PaymentInfo failurePaymentInfo = PaymentInfo.builder()
        .paymentStatus(PaymentStatus.PENDING)
        .build();

    // when & then
    // PENDING -> FAILED
    failurePaymentInfo.markAsFailed();
    assertThat(failurePaymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
  }

  @Test
  @DisplayName("모든 PaymentStatus enum 값 테스트")
  void testAllPaymentStatusValues() {
    // given & when & then
    assertThat(PaymentStatus.PENDING).isNotNull();
    assertThat(PaymentStatus.COMPLETED).isNotNull();
    assertThat(PaymentStatus.FAILED).isNotNull();
    assertThat(PaymentStatus.CANCELLED).isNotNull();
    assertThat(PaymentStatus.REFUNDED).isNotNull();
    assertThat(PaymentStatus.values()).hasSize(5);
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
        .paymentStatus(PaymentStatus.PENDING)
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
        .paymentStatus(PaymentStatus.PENDING)
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
    assertThat(emptyPaymentInfo.getPaymentStatus()).isNull();
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
    assertThat(paymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
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
    assertThat(paymentInfo.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
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
        .paymentStatus(PaymentStatus.PENDING)
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
}
