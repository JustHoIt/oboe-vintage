package com.oboe.backend.order.entity;

public enum PaymentStatus {
  PENDING,           // 결제대기
  COMPLETED,         // 결제완료
  FAILED,            // 결제실패
  CANCELLED,         // 결제취소
  REFUNDED           // 환불완료
}
