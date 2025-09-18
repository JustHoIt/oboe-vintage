package com.oboe.backend.product.entity;

public enum ProductStatus {
  ACTIVE, //판매중
  SOLD_OUT, //판매완료
  INACTIVE, //비활성
  TRADING; //거래중

  /**
   * 상품이 구매 가능한 상태인지 확인
   */
  public boolean isAvailable() {
    return this == ACTIVE;
  }
}
