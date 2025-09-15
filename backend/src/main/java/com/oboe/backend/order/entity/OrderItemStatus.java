package com.oboe.backend.order.entity;

public enum OrderItemStatus {
  ORDERED,           // 주문됨
  PREPARING,         // 준비중
  SHIPPED,           // 배송중
  DELIVERED,         // 배송완료
  CANCELLED,         // 취소됨
  REFUNDED,          // 환불됨
  EXCHANGED          // 교환됨
}
