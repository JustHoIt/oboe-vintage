package com.oboe.backend.order.entity.payment;

public enum PaymentStatus {
  ABORTED, //실패
  CANCELED, //취소
  DONE, //종료
  EXPIRED, //만료됨
  IN_PROGRESS, //진행중
  PARTIAL_CANCELED, //부분 취소
  READY, //준비
  WAITING_FOR_DEPOSIT //입금 대기
}
