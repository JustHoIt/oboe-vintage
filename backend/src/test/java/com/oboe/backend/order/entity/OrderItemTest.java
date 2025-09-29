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
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OrderItem Entity 테스트")
class OrderItemTest {

  private Product product;
  private Order order;

  @BeforeEach
  void setUp() {
    // 테스트용 상품 생성
    product = Product.builder()
        .name("빈티지 데님 셔츠")
        .description("1980년대 빈티지 데님 셔츠")
        .price(new BigDecimal("150000"))
        .stockQuantity(10)
        .productStatus(ProductStatus.ACTIVE)
        .brand("리바이스")
        .condition(Condition.VERY_GOOD)
        .build();

    // 테스트용 주문 생성
    order = Order.builder()
        .orderNumber("ORD-20241201-001")
        .status(OrderStatus.PENDING)
        .paymentMethod(PaymentMethod.카드)
        .totalAmount(new BigDecimal("300000"))
        .finalAmount(new BigDecimal("300000"))
        .build();
  }

  @Test
  @DisplayName("OrderItem 기본 생성 테스트")
  void createOrderItem() {
    // given & when
    OrderItem orderItem = OrderItem.builder()
        .order(order)
        .product(product)
        .quantity(2)
        .unitPrice(new BigDecimal("150000"))
        .totalPrice(new BigDecimal("300000"))
        .status(OrderItemStatus.ORDERED)
        .build();

    // then
    assertThat(orderItem.getOrder()).isEqualTo(order);
    assertThat(orderItem.getProduct()).isEqualTo(product);
    assertThat(orderItem.getQuantity()).isEqualTo(2);
    assertThat(orderItem.getUnitPrice()).isEqualByComparingTo(new BigDecimal("150000"));
    assertThat(orderItem.getTotalPrice()).isEqualByComparingTo(new BigDecimal("300000"));
    assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.ORDERED);
  }

  @Test
  @DisplayName("OrderItem 팩토리 메서드로 생성 테스트")
  void createOrderItemWithFactory() {
    // given
    Integer quantity = 3;

    // when
    OrderItem orderItem = OrderItem.create(product, quantity);

    // then
    assertThat(orderItem.getProduct()).isEqualTo(product);
    assertThat(orderItem.getQuantity()).isEqualTo(quantity);
    assertThat(orderItem.getUnitPrice()).isEqualByComparingTo(product.getPrice());
    assertThat(orderItem.getTotalPrice()).isEqualByComparingTo(new BigDecimal("450000")); // 150000 * 3
    assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.ORDERED);
  }

  @Test
  @DisplayName("OrderItem 팩토리 메서드 - 재고 부족 시 예외 발생")
  void createOrderItem_InsufficientStock() {
    // given
    Integer quantity = 15; // 재고는 10개

    // when & then
    assertThatThrownBy(() -> OrderItem.create(product, quantity))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("재고가 부족합니다");
  }

  @Test
  @DisplayName("총 가격 계산 테스트")
  void calculateTotalPrice() {
    // given
    OrderItem orderItem = OrderItem.builder()
        .product(product)
        .quantity(5)
        .unitPrice(new BigDecimal("150000"))
        .status(OrderItemStatus.ORDERED)
        .build();

    // when
    BigDecimal totalPrice = orderItem.calculateTotalPrice();

    // then
    assertThat(totalPrice).isEqualByComparingTo(new BigDecimal("750000")); // 150000 * 5
  }

  @Test
  @DisplayName("주문상품 상태 변경 테스트")
  void changeStatus() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);

    // when
    orderItem.changeStatus(OrderItemStatus.PREPARING);

    // then
    assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.PREPARING);
  }

  @Test
  @DisplayName("주문상품 취소 테스트")
  void cancel() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);
    orderItem.changeStatus(OrderItemStatus.PREPARING);

    // when
    orderItem.cancel();

    // then
    assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.CANCELLED);
  }

  @Test
  @DisplayName("주문상품 취소 - 배송완료된 상품은 취소 불가")
  void cancel_DeliveredItem_ThrowsException() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);
    // 상태 전환: ORDERED -> PREPARING -> SHIPPED -> DELIVERED
    orderItem.markAsPreparing();
    orderItem.markAsShipped();
    orderItem.markAsDelivered();

    // when & then
    assertThatThrownBy(() -> orderItem.cancel())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("이미 배송완료된 상품은 취소할 수 없습니다");
  }

  @Test
  @DisplayName("주문상품 환불 테스트")
  void refund() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);
    // 상태 전환: ORDERED -> PREPARING -> SHIPPED -> DELIVERED
    orderItem.markAsPreparing();
    orderItem.markAsShipped();
    orderItem.markAsDelivered();

    // when
    orderItem.refund();

    // then
    assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.REFUNDED);
  }

  @Test
  @DisplayName("주문상품 환불 - 배송완료되지 않은 상품은 환불 불가")
  void refund_NotDelivered_ThrowsException() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);
    orderItem.changeStatus(OrderItemStatus.PREPARING);

    // when & then
    assertThatThrownBy(() -> orderItem.refund())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("배송완료된 상품만 환불 가능합니다");
  }

  @Test
  @DisplayName("주문상품 교환 테스트")
  void exchange() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);
    // 상태 전환: ORDERED -> PREPARING -> SHIPPED -> DELIVERED
    orderItem.markAsPreparing();
    orderItem.markAsShipped();
    orderItem.markAsDelivered();

    // when
    orderItem.exchange();

    // then
    assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.EXCHANGED);
  }

  @Test
  @DisplayName("주문상품 교환 - 배송완료되지 않은 상품은 교환 불가")
  void exchange_NotDelivered_ThrowsException() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);
    orderItem.changeStatus(OrderItemStatus.PREPARING);

    // when & then
    assertThatThrownBy(() -> orderItem.exchange())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("배송완료된 상품만 교환 가능합니다");
  }

  @Test
  @DisplayName("주문상품 준비중으로 변경 테스트")
  void markAsPreparing() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);

    // when
    orderItem.markAsPreparing();

    // then
    assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.PREPARING);
  }

  @Test
  @DisplayName("주문상품 준비중으로 변경 - 주문되지 않은 상품은 준비중으로 변경 불가")
  void markAsPreparing_NotOrdered_ThrowsException() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);
    orderItem.changeStatus(OrderItemStatus.PREPARING);

    // when & then
    assertThatThrownBy(() -> orderItem.markAsPreparing())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("주문된 상품만 준비중으로 변경 가능합니다");
  }

  @Test
  @DisplayName("주문상품 배송중으로 변경 테스트")
  void markAsShipped() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);
    orderItem.markAsPreparing();

    // when
    orderItem.markAsShipped();

    // then
    assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.SHIPPED);
  }

  @Test
  @DisplayName("주문상품 배송중으로 변경 - 준비중이 아닌 상품은 배송중으로 변경 불가")
  void markAsShipped_NotPreparing_ThrowsException() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);

    // when & then
    assertThatThrownBy(() -> orderItem.markAsShipped())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("준비중인 상품만 배송중으로 변경 가능합니다");
  }

  @Test
  @DisplayName("주문상품 배송완료로 변경 테스트")
  void markAsDelivered() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);
    // 상태 전환: ORDERED -> PREPARING -> SHIPPED
    orderItem.markAsPreparing();
    orderItem.markAsShipped();

    // when
    orderItem.markAsDelivered();

    // then
    assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.DELIVERED);
  }

  @Test
  @DisplayName("주문상품 배송완료로 변경 - 배송중이 아닌 상품은 배송완료로 변경 불가")
  void markAsDelivered_NotShipped_ThrowsException() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);
    orderItem.markAsPreparing();

    // when & then
    assertThatThrownBy(() -> orderItem.markAsDelivered())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("배송중인 상품만 배송완료로 변경 가능합니다");
  }

  @Test
  @DisplayName("취소 가능 여부 확인 - 취소 가능한 상태")
  void canCancel_True() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);
    orderItem.markAsPreparing();

    // when & then
    assertThat(orderItem.canCancel()).isTrue();
  }

  @Test
  @DisplayName("취소 가능 여부 확인 - 취소 불가능한 상태")
  void canCancel_False() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);
    // 상태 전환: ORDERED -> PREPARING -> SHIPPED -> DELIVERED
    orderItem.markAsPreparing();
    orderItem.markAsShipped();
    orderItem.markAsDelivered();

    // when & then
    assertThat(orderItem.canCancel()).isFalse();
  }

  @Test
  @DisplayName("환불 가능 여부 확인 - 환불 가능한 상태")
  void canRefund_True() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);
    // 상태 전환: ORDERED -> PREPARING -> SHIPPED -> DELIVERED
    orderItem.markAsPreparing();
    orderItem.markAsShipped();
    orderItem.markAsDelivered();

    // when & then
    assertThat(orderItem.canRefund()).isTrue();
  }

  @Test
  @DisplayName("환불 가능 여부 확인 - 환불 불가능한 상태")
  void canRefund_False() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);
    orderItem.markAsPreparing();

    // when & then
    assertThat(orderItem.canRefund()).isFalse();
  }

  @Test
  @DisplayName("교환 가능 여부 확인 - 교환 가능한 상태")
  void canExchange_True() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);
    // 상태 전환: ORDERED -> PREPARING -> SHIPPED -> DELIVERED
    orderItem.markAsPreparing();
    orderItem.markAsShipped();
    orderItem.markAsDelivered();

    // when & then
    assertThat(orderItem.canExchange()).isTrue();
  }

  @Test
  @DisplayName("교환 가능 여부 확인 - 교환 불가능한 상태")
  void canExchange_False() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);
    orderItem.markAsPreparing();

    // when & then
    assertThat(orderItem.canExchange()).isFalse();
  }

  @Test
  @DisplayName("Order와의 양방향 관계 설정 테스트")
  void setOrder() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);

    // when
    orderItem.setOrder(order);

    // then
    assertThat(orderItem.getOrder()).isEqualTo(order);
  }

  @Test
  @DisplayName("BigDecimal 가격 정밀도 테스트")
  void testPricePrecision() {
    // given
    Product precisionProduct = Product.builder()
        .name("정밀도 테스트 상품")
        .description("가격 정밀도 테스트")
        .price(new BigDecimal("123456.78"))
        .stockQuantity(10)
        .productStatus(ProductStatus.ACTIVE)
        .build();

    // when
    OrderItem orderItem = OrderItem.create(precisionProduct, 2);

    // then
    assertThat(orderItem.getUnitPrice()).isEqualByComparingTo(new BigDecimal("123456.78"));
    assertThat(orderItem.getTotalPrice()).isEqualByComparingTo(new BigDecimal("246913.56"));
  }

  @Test
  @DisplayName("OrderItemStatus enum 모든 값 테스트")
  void testOrderItemStatusEnum() {
    // given & when & then
    assertThat(OrderItemStatus.ORDERED).isNotNull();
    assertThat(OrderItemStatus.PREPARING).isNotNull();
    assertThat(OrderItemStatus.SHIPPED).isNotNull();
    assertThat(OrderItemStatus.DELIVERED).isNotNull();
    assertThat(OrderItemStatus.CANCELLED).isNotNull();
    assertThat(OrderItemStatus.REFUNDED).isNotNull();
    assertThat(OrderItemStatus.EXCHANGED).isNotNull();
    assertThat(OrderItemStatus.values()).hasSize(7);
  }

  @Test
  @DisplayName("상태 변경 플로우 테스트")
  void testStatusChangeFlow() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);

    // when & then
    // ORDERED -> PREPARING
    orderItem.markAsPreparing();
    assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.PREPARING);

    // PREPARING -> SHIPPED
    orderItem.markAsShipped();
    assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.SHIPPED);

    // SHIPPED -> DELIVERED
    orderItem.markAsDelivered();
    assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.DELIVERED);

    // DELIVERED -> REFUNDED
    orderItem.refund();
    assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.REFUNDED);
  }

  @Test
  @DisplayName("BaseTimeEntity 상속 확인 테스트")
  void testBaseTimeEntityInheritance() {
    // given
    OrderItem orderItem = OrderItem.create(product, 2);

    // when & then
    assertThat(orderItem).isInstanceOf(com.oboe.backend.common.domain.BaseTimeEntity.class);
  }
}
