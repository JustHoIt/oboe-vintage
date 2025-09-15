package com.oboe.backend.order.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OrderStatusHistory Entity 테스트")
class OrderStatusHistoryTest {

  private User user;
  private Order order;

  @BeforeEach
  void setUp() {
    // 테스트용 사용자 생성
    user = User.builder()
        .email("test@example.com")
        .password("password123")
        .name("홍길동")
        .nickname("hong123")
        .phoneNumber("010-1234-5678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .build();

    // 테스트용 주문 생성
    order = Order.builder()
        .orderNumber("ORD-20241201-001")
        .user(user)
        .status(OrderStatus.PENDING)
        .paymentMethod(PaymentMethod.CARD)
        .totalAmount(new java.math.BigDecimal("100000"))
        .finalAmount(new java.math.BigDecimal("100000"))
        .build();
  }

  @Test
  @DisplayName("OrderStatusHistory 기본 생성 테스트")
  void createOrderStatusHistory() {
    // given & when
    OrderStatusHistory history = OrderStatusHistory.builder()
        .order(order)
        .fromStatus(OrderStatus.PENDING)
        .toStatus(OrderStatus.CONFIRMED)
        .reason("주문 확인 완료")
        .memo("관리자 확인")
        .build();

    // then
    assertThat(history.getOrder()).isEqualTo(order);
    assertThat(history.getFromStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(history.getToStatus()).isEqualTo(OrderStatus.CONFIRMED);
    assertThat(history.getReason()).isEqualTo("주문 확인 완료");
    assertThat(history.getMemo()).isEqualTo("관리자 확인");
  }

  @Test
  @DisplayName("OrderStatusHistory 팩토리 메서드로 생성 테스트")
  void createOrderStatusHistoryWithFactory() {
    // given
    OrderStatus fromStatus = OrderStatus.PENDING;
    OrderStatus toStatus = OrderStatus.CONFIRMED;
    String reason = "주문 확인 완료";
    String memo = "관리자 확인";

    // when
    OrderStatusHistory history = OrderStatusHistory.create(order, fromStatus, toStatus, reason, memo);

    // then
    assertThat(history.getOrder()).isEqualTo(order);
    assertThat(history.getFromStatus()).isEqualTo(fromStatus);
    assertThat(history.getToStatus()).isEqualTo(toStatus);
    assertThat(history.getReason()).isEqualTo(reason);
    assertThat(history.getMemo()).isEqualTo(memo);
  }

  @Test
  @DisplayName("메모 업데이트 테스트")
  void updateMemo() {
    // given
    OrderStatusHistory history = OrderStatusHistory.builder()
        .order(order)
        .fromStatus(OrderStatus.PENDING)
        .toStatus(OrderStatus.CONFIRMED)
        .reason("주문 확인 완료")
        .memo("기존 메모")
        .build();

    String newMemo = "업데이트된 메모";

    // when
    history.updateMemo(newMemo);

    // then
    assertThat(history.getMemo()).isEqualTo(newMemo);
  }

  @Test
  @DisplayName("사유 업데이트 테스트")
  void updateReason() {
    // given
    OrderStatusHistory history = OrderStatusHistory.builder()
        .order(order)
        .fromStatus(OrderStatus.PENDING)
        .toStatus(OrderStatus.CONFIRMED)
        .reason("기존 사유")
        .memo("관리자 메모")
        .build();

    String newReason = "업데이트된 사유";

    // when
    history.updateReason(newReason);

    // then
    assertThat(history.getReason()).isEqualTo(newReason);
  }

  @Test
  @DisplayName("상태변경이 같은지 확인 테스트")
  void isSameStatusChange() {
    // given
    OrderStatusHistory history = OrderStatusHistory.builder()
        .order(order)
        .fromStatus(OrderStatus.PENDING)
        .toStatus(OrderStatus.CONFIRMED)
        .reason("주문 확인 완료")
        .build();

    // when & then
    assertThat(history.isSameStatusChange(OrderStatus.PENDING, OrderStatus.CONFIRMED)).isTrue();
    assertThat(history.isSameStatusChange(OrderStatus.CONFIRMED, OrderStatus.PREPARING)).isFalse();
    assertThat(history.isSameStatusChange(OrderStatus.PREPARING, OrderStatus.CONFIRMED)).isFalse();
  }

  @Test
  @DisplayName("취소 관련 상태변경 확인 테스트")
  void isCancellation() {
    // given
    OrderStatusHistory cancellationHistory = OrderStatusHistory.builder()
        .order(order)
        .fromStatus(OrderStatus.CONFIRMED)
        .toStatus(OrderStatus.CANCELLED)
        .reason("고객 요청")
        .build();

    OrderStatusHistory normalHistory = OrderStatusHistory.builder()
        .order(order)
        .fromStatus(OrderStatus.PENDING)
        .toStatus(OrderStatus.CONFIRMED)
        .reason("주문 확인")
        .build();

    // when & then
    assertThat(cancellationHistory.isCancellation()).isTrue();
    assertThat(normalHistory.isCancellation()).isFalse();
  }

  @Test
  @DisplayName("환불 관련 상태변경 확인 테스트")
  void isRefund() {
    // given
    OrderStatusHistory refundHistory = OrderStatusHistory.builder()
        .order(order)
        .fromStatus(OrderStatus.DELIVERED)
        .toStatus(OrderStatus.REFUNDED)
        .reason("고객 요청")
        .build();

    OrderStatusHistory normalHistory = OrderStatusHistory.builder()
        .order(order)
        .fromStatus(OrderStatus.PENDING)
        .toStatus(OrderStatus.CONFIRMED)
        .reason("주문 확인")
        .build();

    // when & then
    assertThat(refundHistory.isRefund()).isTrue();
    assertThat(normalHistory.isRefund()).isFalse();
  }

  @Test
  @DisplayName("배송 관련 상태변경 확인 테스트")
  void isDeliveryRelated() {
    // given
    OrderStatusHistory shippedHistory = OrderStatusHistory.builder()
        .order(order)
        .fromStatus(OrderStatus.PREPARING)
        .toStatus(OrderStatus.SHIPPED)
        .reason("배송 시작")
        .build();

    OrderStatusHistory deliveredHistory = OrderStatusHistory.builder()
        .order(order)
        .fromStatus(OrderStatus.SHIPPED)
        .toStatus(OrderStatus.DELIVERED)
        .reason("배송 완료")
        .build();

    OrderStatusHistory normalHistory = OrderStatusHistory.builder()
        .order(order)
        .fromStatus(OrderStatus.PENDING)
        .toStatus(OrderStatus.CONFIRMED)
        .reason("주문 확인")
        .build();

    // when & then
    assertThat(shippedHistory.isDeliveryRelated()).isTrue();
    assertThat(deliveredHistory.isDeliveryRelated()).isTrue();
    assertThat(normalHistory.isDeliveryRelated()).isFalse();
  }

  @Test
  @DisplayName("모든 OrderStatus enum 값 테스트")
  void testAllOrderStatusValues() {
    // given & when & then
    assertThat(OrderStatus.PENDING).isNotNull();
    assertThat(OrderStatus.CONFIRMED).isNotNull();
    assertThat(OrderStatus.PREPARING).isNotNull();
    assertThat(OrderStatus.SHIPPED).isNotNull();
    assertThat(OrderStatus.DELIVERED).isNotNull();
    assertThat(OrderStatus.CANCELLED).isNotNull();
    assertThat(OrderStatus.REFUNDED).isNotNull();
    assertThat(OrderStatus.EXCHANGED).isNotNull();
    assertThat(OrderStatus.values()).hasSize(8);
  }

  @Test
  @DisplayName("다양한 상태변경 시나리오 테스트")
  void testVariousStatusChangeScenarios() {
    // given & when
    OrderStatusHistory pendingToConfirmed = OrderStatusHistory.create(
        order, OrderStatus.PENDING, OrderStatus.CONFIRMED, "주문 확인", "관리자 확인");

    OrderStatusHistory confirmedToPreparing = OrderStatusHistory.create(
        order, OrderStatus.CONFIRMED, OrderStatus.PREPARING, "상품 준비 시작", "재고 확인 완료");

    OrderStatusHistory preparingToShipped = OrderStatusHistory.create(
        order, OrderStatus.PREPARING, OrderStatus.SHIPPED, "배송 시작", "택배사 접수 완료");

    OrderStatusHistory shippedToDelivered = OrderStatusHistory.create(
        order, OrderStatus.SHIPPED, OrderStatus.DELIVERED, "배송 완료", "고객 수령 확인");

    OrderStatusHistory deliveredToRefunded = OrderStatusHistory.create(
        order, OrderStatus.DELIVERED, OrderStatus.REFUNDED, "환불 요청", "고객 불만");

    // then
    assertThat(pendingToConfirmed.getFromStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(pendingToConfirmed.getToStatus()).isEqualTo(OrderStatus.CONFIRMED);

    assertThat(confirmedToPreparing.getFromStatus()).isEqualTo(OrderStatus.CONFIRMED);
    assertThat(confirmedToPreparing.getToStatus()).isEqualTo(OrderStatus.PREPARING);

    assertThat(preparingToShipped.getFromStatus()).isEqualTo(OrderStatus.PREPARING);
    assertThat(preparingToShipped.getToStatus()).isEqualTo(OrderStatus.SHIPPED);
    assertThat(preparingToShipped.isDeliveryRelated()).isTrue();

    assertThat(shippedToDelivered.getFromStatus()).isEqualTo(OrderStatus.SHIPPED);
    assertThat(shippedToDelivered.getToStatus()).isEqualTo(OrderStatus.DELIVERED);
    assertThat(shippedToDelivered.isDeliveryRelated()).isTrue();

    assertThat(deliveredToRefunded.getFromStatus()).isEqualTo(OrderStatus.DELIVERED);
    assertThat(deliveredToRefunded.getToStatus()).isEqualTo(OrderStatus.REFUNDED);
    assertThat(deliveredToRefunded.isRefund()).isTrue();
  }

  @Test
  @DisplayName("긴 사유와 메모 테스트")
  void testLongReasonAndMemo() {
    // given
    String longReason = "이것은 매우 긴 사유입니다. ".repeat(20); // 500자 근처
    String longMemo = "이것은 매우 긴 관리자 메모입니다. ".repeat(20); // 1000자 근처

    // when
    OrderStatusHistory history = OrderStatusHistory.builder()
        .order(order)
        .fromStatus(OrderStatus.PENDING)
        .toStatus(OrderStatus.CONFIRMED)
        .reason(longReason)
        .memo(longMemo)
        .build();

    // then
    assertThat(history.getReason()).isEqualTo(longReason);
    assertThat(history.getMemo()).isEqualTo(longMemo);
    assertThat(history.getReason().length()).isLessThanOrEqualTo(500);
    assertThat(history.getMemo().length()).isLessThanOrEqualTo(1000);
  }

  @Test
  @DisplayName("null 값 처리 테스트")
  void testNullValues() {
    // given & when
    OrderStatusHistory history = OrderStatusHistory.builder()
        .order(order)
        .fromStatus(OrderStatus.PENDING)
        .toStatus(OrderStatus.CONFIRMED)
        .reason(null)
        .memo(null)
        .build();

    // then
    assertThat(history.getReason()).isNull();
    assertThat(history.getMemo()).isNull();
  }

  @Test
  @DisplayName("빈 문자열 처리 테스트")
  void testEmptyStrings() {
    // given
    OrderStatusHistory history = OrderStatusHistory.builder()
        .order(order)
        .fromStatus(OrderStatus.PENDING)
        .toStatus(OrderStatus.CONFIRMED)
        .reason("")
        .memo("")
        .build();

    // when
    history.updateReason("");
    history.updateMemo("");

    // then
    assertThat(history.getReason()).isEqualTo("");
    assertThat(history.getMemo()).isEqualTo("");
  }

  @Test
  @DisplayName("BaseTimeEntity 상속 확인 테스트")
  void testBaseTimeEntityInheritance() {
    // given
    OrderStatusHistory history = OrderStatusHistory.builder()
        .order(order)
        .fromStatus(OrderStatus.PENDING)
        .toStatus(OrderStatus.CONFIRMED)
        .reason("주문 확인")
        .build();

    // when & then
    assertThat(history).isInstanceOf(com.oboe.backend.common.domain.BaseTimeEntity.class);
  }
}
