package com.oboe.backend.order.entity;

public enum OrderStatus {
  PENDING,           // 주문대기
  CONFIRMED,         // 주문확인
  PREPARING,         // 상품준비중
  SHIPPED,           // 배송중
  DELIVERED,         // 배송완료
  CANCELLED,         // 주문취소
  REFUNDED,          // 환불완료
  EXCHANGED          // 교환완료
}
