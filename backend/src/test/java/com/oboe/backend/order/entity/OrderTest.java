package com.oboe.backend.order.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oboe.backend.order.entity.order.Order;
import com.oboe.backend.order.entity.order.OrderItem;
import com.oboe.backend.order.entity.order.OrderItemStatus;
import com.oboe.backend.order.entity.order.OrderStatus;
import com.oboe.backend.order.entity.payment.PaymentMethod;
import com.oboe.backend.product.entity.Condition;
import com.oboe.backend.product.entity.Product;
import com.oboe.backend.product.entity.ProductStatus;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Order Entity 테스트")
class OrderTest {

  private User user;
  private Product product1;
  private Product product2;
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

    // 테스트용 상품 생성
    product1 = Product.builder()
        .name("빈티지 데님 셔츠")
        .description("1980년대 빈티지 데님 셔츠")
        .price(new BigDecimal("150000"))
        .stockQuantity(5)
        .productStatus(ProductStatus.ACTIVE)
        .brand("리바이스")
        .condition(Condition.VERY_GOOD)
        .build();

    product2 = Product.builder()
        .name("빈티지 청바지")
        .description("1990년대 빈티지 청바지")
        .price(new BigDecimal("200000"))
        .stockQuantity(3)
        .productStatus(ProductStatus.ACTIVE)
        .brand("리바이스")
        .condition(Condition.EXCELLENT)
        .build();

    // 기본 테스트용 주문 생성
    order = Order.builder()
        .orderNumber("ORD-20241201-001")
        .user(user)
        .status(OrderStatus.PENDING)
        .paymentMethod(PaymentMethod.카드)
        .totalAmount(new BigDecimal("350000"))
        .deliveryFee(new BigDecimal("3000"))
        .discountAmount(new BigDecimal("10000"))
        .finalAmount(new BigDecimal("343000"))
        .build();
  }

  @Test
  @DisplayName("Order 기본 생성 테스트")
  void createOrder() {
    // given & when

    // then
    assertThat(order.getOrderNumber()).isEqualTo("ORD-20241201-001");
    assertThat(order.getUser()).isEqualTo(user);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.카드);
    assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("350000"));
    assertThat(order.getDeliveryFee()).isEqualByComparingTo(new BigDecimal("3000"));
    assertThat(order.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("10000"));
    assertThat(order.getFinalAmount()).isEqualByComparingTo(new BigDecimal("343000"));
  }

  @Test
  @DisplayName("주문번호 생성 테스트")
  void generateOrderNumber() {
    // given & when
    String orderNumber = Order.generateOrderNumber();

    // then
    assertThat(orderNumber).startsWith("ORD-");
    assertThat(orderNumber).matches("ORD-\\d{8}-\\d{6}");
    // ORD-YYYYMMDD-HHMMSS 형식이므로 총 19자리
    assertThat(orderNumber).hasSize(19);
  }

  @Test
  @DisplayName("총 주문금액 계산 테스트")
  void calculateTotalAmount() {
    // given
    OrderItem orderItem1 = OrderItem.create(product1, 2);
    OrderItem orderItem2 = OrderItem.create(product2, 1);
    order.addOrderItem(orderItem1);
    order.addOrderItem(orderItem2);

    // when
    BigDecimal totalAmount = order.calculateTotalAmount();

    // then
    // (150000 * 2) + (200000 * 1) = 500000
    assertThat(totalAmount).isEqualByComparingTo(new BigDecimal("500000"));
  }

  @Test
  @DisplayName("최종 결제금액 계산 테스트")
  void calculateFinalAmount() {
    // given
    order.setTotalAmount(new BigDecimal("100000"));
    order.setDeliveryFee(new BigDecimal("3000"));
    order.setDiscountAmount(new BigDecimal("5000"));

    // when
    BigDecimal finalAmount = order.calculateFinalAmount();

    // then
    // 100000 - 5000 + 3000 = 98000
    assertThat(finalAmount).isEqualByComparingTo(new BigDecimal("98000"));
  }

  @Test
  @DisplayName("주문상품 추가 테스트")
  void addOrderItem() {
    // given
    OrderItem orderItem = OrderItem.create(product1, 2);

    // when
    order.addOrderItem(orderItem);

    // then
    assertThat(order.getOrderItems()).hasSize(1);
    assertThat(order.getOrderItems()).contains(orderItem);
    assertThat(orderItem.getOrder()).isEqualTo(order);
  }

  @Test
  @DisplayName("주문상품 제거 테스트")
  void removeOrderItem() {
    // given
    OrderItem orderItem = OrderItem.create(product1, 2);
    order.addOrderItem(orderItem);

    // when
    order.removeOrderItem(orderItem);

    // then
    assertThat(order.getOrderItems()).isEmpty();
    assertThat(orderItem.getOrder()).isNull();
  }

  @Test
  @DisplayName("주문상태 변경 테스트")
  void changeStatus() {
    // given
    String reason = "주문 확인 완료";

    // when
    order.changeStatus(OrderStatus.CONFIRMED, reason);

    // then
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    assertThat(order.getStatusHistory()).hasSize(1);
    assertThat(order.getStatusHistory().get(0).getFromStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(order.getStatusHistory().get(0).getToStatus()).isEqualTo(OrderStatus.CONFIRMED);
    assertThat(order.getStatusHistory().get(0).getReason()).isEqualTo(reason);
  }

  @Test
  @DisplayName("주문상태 자동 계산 - 모든 상품 배송완료")
  void calculateStatus_AllDelivered() {
    // given
    OrderItem orderItem1 = OrderItem.create(product1, 1);
    OrderItem orderItem2 = OrderItem.create(product2, 1);
    order.addOrderItem(orderItem1);
    order.addOrderItem(orderItem2);
    
    // 상태 전환: ORDERED -> PREPARING -> SHIPPED -> DELIVERED
    orderItem1.markAsPreparing();
    orderItem1.markAsShipped();
    orderItem1.markAsDelivered();
    
    orderItem2.markAsPreparing();
    orderItem2.markAsShipped();
    orderItem2.markAsDelivered();

    // when
    order.calculateStatus();

    // then
    assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
  }

  @Test
  @DisplayName("주문상태 자동 계산 - 일부 상품 배송중")
  void calculateStatus_SomeShipped() {
    // given
    OrderItem orderItem1 = OrderItem.create(product1, 1);
    OrderItem orderItem2 = OrderItem.create(product2, 1);
    order.addOrderItem(orderItem1);
    order.addOrderItem(orderItem2);
    
    // 상태 전환: ORDERED -> PREPARING -> SHIPPED
    orderItem1.markAsPreparing();
    orderItem1.markAsShipped();
    orderItem2.markAsPreparing();

    // when
    order.calculateStatus();

    // then
    assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
  }

  @Test
  @DisplayName("주문상태 자동 계산 - 모든 상품 준비중")
  void calculateStatus_AllPreparing() {
    // given
    OrderItem orderItem1 = OrderItem.create(product1, 1);
    OrderItem orderItem2 = OrderItem.create(product2, 1);
    order.addOrderItem(orderItem1);
    order.addOrderItem(orderItem2);
    
    orderItem1.markAsPreparing();
    orderItem2.markAsPreparing();

    // when
    order.calculateStatus();

    // then
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
  }

  @Test
  @DisplayName("주문상태 자동 계산 - 모든 상품 취소")
  void calculateStatus_AllCancelled() {
    // given
    OrderItem orderItem1 = OrderItem.create(product1, 1);
    OrderItem orderItem2 = OrderItem.create(product2, 1);
    order.addOrderItem(orderItem1);
    order.addOrderItem(orderItem2);
    
    orderItem1.cancel();
    orderItem2.cancel();

    // when
    order.calculateStatus();

    // then
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
  }

  @Test
  @DisplayName("주문취소 가능 여부 확인 - 취소 가능한 상태")
  void canCancel_True() {
    // given
    order.changeStatus(OrderStatus.CONFIRMED, "주문 확인");

    // when & then
    assertThat(order.canCancel()).isTrue();
  }

  @Test
  @DisplayName("주문취소 가능 여부 확인 - 취소 불가능한 상태")
  void canCancel_False() {
    // given
    order.changeStatus(OrderStatus.DELIVERED, "배송완료");

    // when & then
    assertThat(order.canCancel()).isFalse();
  }

  @Test
  @DisplayName("환불 가능 여부 확인 - 환불 가능한 상태")
  void canRefund_True() {
    // given
    order.changeStatus(OrderStatus.DELIVERED, "배송완료");

    // when & then
    assertThat(order.canRefund()).isTrue();
  }

  @Test
  @DisplayName("환불 가능 여부 확인 - 환불 불가능한 상태")
  void canRefund_False() {
    // given
    order.changeStatus(OrderStatus.PENDING, "주문대기");

    // when & then
    assertThat(order.canRefund()).isFalse();
  }

  @Test
  @DisplayName("주문 취소 처리 테스트")
  void cancel() {
    // given
    OrderItem orderItem = OrderItem.create(product1, 2);
    order.addOrderItem(orderItem);
    order.changeStatus(OrderStatus.CONFIRMED, "주문 확인");
    String cancelReason = "고객 요청";

    // when
    order.cancel(cancelReason);

    // then
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.CANCELLED);
  }

  @Test
  @DisplayName("주문 취소 처리 - 취소 불가능한 상태에서 예외 발생")
  void cancel_ThrowsException() {
    // given
    order.changeStatus(OrderStatus.DELIVERED, "배송완료");
    String cancelReason = "고객 요청";

    // when & then
    assertThatThrownBy(() -> order.cancel(cancelReason))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("주문을 취소할 수 없는 상태입니다");
  }

  @Test
  @DisplayName("배송정보 업데이트 테스트")
  void updateDeliveryInfo() {
    // given
    String recipientName = "김수령";
    String recipientPhone = "010-9876-5432";
    String roadAddress = "서울시 강남구 테헤란로 123";
    String detailAddress = "101호";
    String zipCode = "06292";
    String deliveryMemo = "문 앞에 놓아주세요";

    // when
    order.updateDeliveryInfo(recipientName, recipientPhone, roadAddress, 
                           detailAddress, zipCode, deliveryMemo);

    // then
    assertThat(order.getDeliveryInfo()).isNotNull();
    assertThat(order.getDeliveryInfo().getRecipientName()).isEqualTo(recipientName);
    assertThat(order.getDeliveryInfo().getRecipientPhone()).isEqualTo(recipientPhone);
    assertThat(order.getDeliveryInfo().getRoadAddress()).isEqualTo(roadAddress);
    assertThat(order.getDeliveryInfo().getDetailAddress()).isEqualTo(detailAddress);
    assertThat(order.getDeliveryInfo().getZipCode()).isEqualTo(zipCode);
    assertThat(order.getDeliveryInfo().getDeliveryMemo()).isEqualTo(deliveryMemo);
  }

  @Test
  @DisplayName("배송완료 처리 테스트")
  void markAsDelivered() {
    // given
    String trackingNumber = "1234567890";
    // 배송정보 먼저 설정
    order.updateDeliveryInfo("홍길동", "010-1234-5678", "서울시 강남구 테헤란로 123", 
                           "101호", "06292", "문 앞에 놓아주세요");

    // when
    order.markAsDelivered(trackingNumber);

    // then
    assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    assertThat(order.getDeliveryInfo().getTrackingNumber()).isEqualTo(trackingNumber);
    assertThat(order.getDeliveryInfo().getDeliveredAt()).isNotNull();
  }

  @Test
  @DisplayName("할인금액 설정 테스트")
  void setDiscountAmount() {
    // given
    BigDecimal discountAmount = new BigDecimal("15000");

    // when
    order.setDiscountAmount(discountAmount);

    // then
    assertThat(order.getDiscountAmount()).isEqualByComparingTo(discountAmount);
    assertThat(order.getFinalAmount()).isEqualByComparingTo(new BigDecimal("338000")); // 350000 - 15000 + 3000
  }

  @Test
  @DisplayName("배송비 설정 테스트")
  void setDeliveryFee() {
    // given
    BigDecimal deliveryFee = new BigDecimal("5000");

    // when
    order.setDeliveryFee(deliveryFee);

    // then
    assertThat(order.getDeliveryFee()).isEqualByComparingTo(deliveryFee);
    assertThat(order.getFinalAmount()).isEqualByComparingTo(new BigDecimal("345000")); // 350000 - 10000 + 5000
  }

  @Test
  @DisplayName("빈 주문상품 목록으로 총 금액 계산")
  void calculateTotalAmount_EmptyOrderItems() {
    // given & when
    BigDecimal totalAmount = order.calculateTotalAmount();

    // then
    assertThat(totalAmount).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("다양한 결제수단 테스트")
  void testPaymentMethods() {
    // given & when
    Order cardOrder = Order.builder()
        .orderNumber("ORD-001")
        .user(user)
        .status(OrderStatus.PENDING)
        .paymentMethod(PaymentMethod.카드)
        .totalAmount(new BigDecimal("100000"))
        .finalAmount(new BigDecimal("100000"))
        .build();

    Order mobilePayOrder = Order.builder()
        .orderNumber("ORD-002")
        .user(user)
        .status(OrderStatus.PENDING)
        .paymentMethod(PaymentMethod.휴대폰)
        .totalAmount(new BigDecimal("100000"))
        .finalAmount(new BigDecimal("100000"))
        .build();

    // then
    assertThat(cardOrder.getPaymentMethod()).isEqualTo(PaymentMethod.카드);
    assertThat(mobilePayOrder.getPaymentMethod()).isEqualTo(PaymentMethod.휴대폰);
  }

  @Test
  @DisplayName("BaseTimeEntity 상속 확인 테스트")
  void testBaseTimeEntityInheritance() {
    // given & when

    // then
    assertThat(order).isInstanceOf(com.oboe.backend.common.domain.BaseTimeEntity.class);
  }
}
