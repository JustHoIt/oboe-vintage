package com.oboe.backend.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DeliveryInfo {

  @Column(nullable = false, length = 50)
  private String recipientName; // 수령인명

  @Column(nullable = false, length = 20)
  private String recipientPhone; // 수령인 연락처

  @Column(nullable = false, length = 200)
  private String roadAddress; // 도로명주소

  @Column(length = 200)
  private String detailAddress; // 상세주소

  @Column(nullable = false, length = 10)
  private String zipCode; // 우편번호

  private String deliveryMemo; // 배송 메모
  private String trackingNumber; // 운송장번호
  private LocalDateTime deliveredAt; // 배송완료일시

  /**
   * 배송완료 처리
   */
  public void markAsDelivered(String trackingNumber) {
    this.trackingNumber = trackingNumber;
    this.deliveredAt = LocalDateTime.now();
  }

  /**
   * 배송정보 업데이트
   */
  public void updateDeliveryInfo(String recipientName, String recipientPhone, 
                                String roadAddress, String detailAddress, String zipCode, 
                                String deliveryMemo) {
    if (recipientName != null && !recipientName.trim().isEmpty()) {
      this.recipientName = recipientName;
    }
    if (recipientPhone != null && !recipientPhone.trim().isEmpty()) {
      this.recipientPhone = recipientPhone;
    }
    if (roadAddress != null && !roadAddress.trim().isEmpty()) {
      this.roadAddress = roadAddress;
    }
    // detailAddress와 deliveryMemo는 null을 허용하므로 null 체크만 함
    this.detailAddress = detailAddress;
    if (zipCode != null && !zipCode.trim().isEmpty()) {
      this.zipCode = zipCode;
    }
    this.deliveryMemo = deliveryMemo;
  }
}
