package com.oboe.backend.order.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DeliveryInfo Entity 테스트")
class DeliveryInfoTest {

  private DeliveryInfo deliveryInfo;

  @BeforeEach
  void setUp() {
    // 기본 테스트용 배송정보 생성
    deliveryInfo = DeliveryInfo.builder()
        .recipientName("홍길동")
        .recipientPhone("010-1234-5678")
        .roadAddress("서울시 강남구 테헤란로 123")
        .detailAddress("101호")
        .zipCode("06292")
        .deliveryMemo("문 앞에 놓아주세요")
        .build();
  }

  @Test
  @DisplayName("DeliveryInfo 기본 생성 테스트")
  void createDeliveryInfo() {
    // given & when

    // then
    assertThat(deliveryInfo.getRecipientName()).isEqualTo("홍길동");
    assertThat(deliveryInfo.getRecipientPhone()).isEqualTo("010-1234-5678");
    assertThat(deliveryInfo.getRoadAddress()).isEqualTo("서울시 강남구 테헤란로 123");
    assertThat(deliveryInfo.getDetailAddress()).isEqualTo("101호");
    assertThat(deliveryInfo.getZipCode()).isEqualTo("06292");
    assertThat(deliveryInfo.getDeliveryMemo()).isEqualTo("문 앞에 놓아주세요");
    assertThat(deliveryInfo.getTrackingNumber()).isNull();
    assertThat(deliveryInfo.getDeliveredAt()).isNull();
  }

  @Test
  @DisplayName("배송완료 처리 테스트")
  void markAsDelivered() {
    // given
    String trackingNumber = "1234567890";

    // when
    deliveryInfo.markAsDelivered(trackingNumber);

    // then
    assertThat(deliveryInfo.getTrackingNumber()).isEqualTo(trackingNumber);
    assertThat(deliveryInfo.getDeliveredAt()).isNotNull();
    assertThat(deliveryInfo.getDeliveredAt()).isBeforeOrEqualTo(LocalDateTime.now());
  }

  @Test
  @DisplayName("배송완료 처리 - null 운송장번호")
  void markAsDelivered_NullTrackingNumber() {
    // given
    String trackingNumber = null;

    // when
    deliveryInfo.markAsDelivered(trackingNumber);

    // then
    assertThat(deliveryInfo.getTrackingNumber()).isNull();
    assertThat(deliveryInfo.getDeliveredAt()).isNotNull();
  }

  @Test
  @DisplayName("배송완료 처리 - 빈 운송장번호")
  void markAsDelivered_EmptyTrackingNumber() {
    // given
    String trackingNumber = "";

    // when
    deliveryInfo.markAsDelivered(trackingNumber);

    // then
    assertThat(deliveryInfo.getTrackingNumber()).isEqualTo("");
    assertThat(deliveryInfo.getDeliveredAt()).isNotNull();
  }

  @Test
  @DisplayName("배송정보 업데이트 테스트")
  void updateDeliveryInfo() {
    // given
    String newRecipientName = "김수령";
    String newRecipientPhone = "010-9876-5432";
    String newRoadAddress = "서울시 서초구 강남대로 456";
    String newDetailAddress = "202호";
    String newZipCode = "06540";
    String newDeliveryMemo = "경비실에 맡겨주세요";

    // when
    deliveryInfo.updateDeliveryInfo(newRecipientName, newRecipientPhone, newRoadAddress,
                                   newDetailAddress, newZipCode, newDeliveryMemo);

    // then
    assertThat(deliveryInfo.getRecipientName()).isEqualTo(newRecipientName);
    assertThat(deliveryInfo.getRecipientPhone()).isEqualTo(newRecipientPhone);
    assertThat(deliveryInfo.getRoadAddress()).isEqualTo(newRoadAddress);
    assertThat(deliveryInfo.getDetailAddress()).isEqualTo(newDetailAddress);
    assertThat(deliveryInfo.getZipCode()).isEqualTo(newZipCode);
    assertThat(deliveryInfo.getDeliveryMemo()).isEqualTo(newDeliveryMemo);
  }

  @Test
  @DisplayName("배송정보 업데이트 - null 값 처리")
  void updateDeliveryInfo_NullValues() {
    // given
    String originalName = deliveryInfo.getRecipientName();
    String originalPhone = deliveryInfo.getRecipientPhone();
    String originalRoadAddress = deliveryInfo.getRoadAddress();
    String originalZipCode = deliveryInfo.getZipCode();

    // when
    deliveryInfo.updateDeliveryInfo(null, null, null, null, null, null);

    // then - null 값은 업데이트되지 않아야 함
    assertThat(deliveryInfo.getRecipientName()).isEqualTo(originalName);
    assertThat(deliveryInfo.getRecipientPhone()).isEqualTo(originalPhone);
    assertThat(deliveryInfo.getRoadAddress()).isEqualTo(originalRoadAddress);
    assertThat(deliveryInfo.getZipCode()).isEqualTo(originalZipCode);
  }

  @Test
  @DisplayName("배송정보 업데이트 - 빈 문자열 처리")
  void updateDeliveryInfo_EmptyStrings() {
    // given
    String originalName = deliveryInfo.getRecipientName();
    String originalPhone = deliveryInfo.getRecipientPhone();
    String originalRoadAddress = deliveryInfo.getRoadAddress();
    String originalZipCode = deliveryInfo.getZipCode();

    // when
    deliveryInfo.updateDeliveryInfo("", "", "", "", "", "");

    // then - 빈 문자열은 업데이트되지 않아야 함
    assertThat(deliveryInfo.getRecipientName()).isEqualTo(originalName);
    assertThat(deliveryInfo.getRecipientPhone()).isEqualTo(originalPhone);
    assertThat(deliveryInfo.getRoadAddress()).isEqualTo(originalRoadAddress);
    assertThat(deliveryInfo.getZipCode()).isEqualTo(originalZipCode);
  }

  @Test
  @DisplayName("배송정보 업데이트 - 공백 문자열 처리")
  void updateDeliveryInfo_WhitespaceStrings() {
    // given
    String originalName = deliveryInfo.getRecipientName();
    String originalPhone = deliveryInfo.getRecipientPhone();
    String originalRoadAddress = deliveryInfo.getRoadAddress();
    String originalZipCode = deliveryInfo.getZipCode();

    // when
    deliveryInfo.updateDeliveryInfo("   ", "   ", "   ", "   ", "   ", "   ");

    // then - 공백 문자열은 업데이트되지 않아야 함
    assertThat(deliveryInfo.getRecipientName()).isEqualTo(originalName);
    assertThat(deliveryInfo.getRecipientPhone()).isEqualTo(originalPhone);
    assertThat(deliveryInfo.getRoadAddress()).isEqualTo(originalRoadAddress);
    assertThat(deliveryInfo.getZipCode()).isEqualTo(originalZipCode);
  }

  @Test
  @DisplayName("배송정보 업데이트 - detailAddress와 deliveryMemo는 null 허용")
  void updateDeliveryInfo_NullableFields() {
    // given
    String newDetailAddress = null;
    String newDeliveryMemo = null;

    // when
    deliveryInfo.updateDeliveryInfo("김수령", "010-9876-5432", "서울시 서초구 강남대로 456",
                                   newDetailAddress, "06540", newDeliveryMemo);

    // then
    assertThat(deliveryInfo.getRecipientName()).isEqualTo("김수령");
    assertThat(deliveryInfo.getRecipientPhone()).isEqualTo("010-9876-5432");
    assertThat(deliveryInfo.getRoadAddress()).isEqualTo("서울시 서초구 강남대로 456");
    // detailAddress는 null로 업데이트되어야 함 (기존 값이 있더라도)
    assertThat(deliveryInfo.getDetailAddress()).isNull();
    assertThat(deliveryInfo.getZipCode()).isEqualTo("06540");
    // deliveryMemo는 null로 업데이트되어야 함 (기존 값이 있더라도)
    assertThat(deliveryInfo.getDeliveryMemo()).isNull();
  }

  @Test
  @DisplayName("배송정보 업데이트 - 부분 업데이트")
  void updateDeliveryInfo_PartialUpdate() {
    // given
    String originalName = deliveryInfo.getRecipientName();
    String originalPhone = deliveryInfo.getRecipientPhone();
    String newRoadAddress = "서울시 서초구 강남대로 456";
    String newZipCode = "06540";

    // when
    deliveryInfo.updateDeliveryInfo(null, null, newRoadAddress, null, newZipCode, null);

    // then
    assertThat(deliveryInfo.getRecipientName()).isEqualTo(originalName);
    assertThat(deliveryInfo.getRecipientPhone()).isEqualTo(originalPhone);
    assertThat(deliveryInfo.getRoadAddress()).isEqualTo(newRoadAddress);
    assertThat(deliveryInfo.getZipCode()).isEqualTo(newZipCode);
  }

  @Test
  @DisplayName("긴 배송정보 테스트")
  void testLongDeliveryInfo() {
    // given
    String longName = "이것은매우긴수령인명입니다".repeat(2); // 50자 근처
    String longPhone = "010-1234-5678";
    String longRoadAddress = "이것은매우긴도로명주소입니다".repeat(10); // 200자 근처
    String longDetailAddress = "이것은매우긴상세주소입니다".repeat(10); // 200자 근처
    String longZipCode = "12345";
    String longDeliveryMemo = "이것은매우긴배송메모입니다".repeat(20); // 200자 근처

    // when
    DeliveryInfo longDeliveryInfo = DeliveryInfo.builder()
        .recipientName(longName)
        .recipientPhone(longPhone)
        .roadAddress(longRoadAddress)
        .detailAddress(longDetailAddress)
        .zipCode(longZipCode)
        .deliveryMemo(longDeliveryMemo)
        .build();

    // then
    assertThat(longDeliveryInfo.getRecipientName()).isEqualTo(longName);
    assertThat(longDeliveryInfo.getRecipientPhone()).isEqualTo(longPhone);
    assertThat(longDeliveryInfo.getRoadAddress()).isEqualTo(longRoadAddress);
    assertThat(longDeliveryInfo.getDetailAddress()).isEqualTo(longDetailAddress);
    assertThat(longDeliveryInfo.getZipCode()).isEqualTo(longZipCode);
    assertThat(longDeliveryInfo.getDeliveryMemo()).isEqualTo(longDeliveryMemo);

    // 길이 제한 확인
    assertThat(longDeliveryInfo.getRecipientName().length()).isLessThanOrEqualTo(50);
    assertThat(longDeliveryInfo.getRecipientPhone().length()).isLessThanOrEqualTo(20);
    assertThat(longDeliveryInfo.getRoadAddress().length()).isLessThanOrEqualTo(200);
    assertThat(longDeliveryInfo.getDetailAddress().length()).isLessThanOrEqualTo(200);
    assertThat(longDeliveryInfo.getZipCode().length()).isLessThanOrEqualTo(10);
  }

  @Test
  @DisplayName("특수문자가 포함된 배송정보 테스트")
  void testSpecialCharactersInDeliveryInfo() {
    // given
    String specialName = "홍길동(별명:홍대감)";
    String specialPhone = "010-1234-5678";
    String specialRoadAddress = "서울시 강남구 테헤란로 123 (구:역삼동)";
    String specialDetailAddress = "101호 (엘리베이터 옆)";
    String specialZipCode = "06292";
    String specialDeliveryMemo = "문 앞에 놓아주세요! (경비실 X)";

    // when
    DeliveryInfo specialDeliveryInfo = DeliveryInfo.builder()
        .recipientName(specialName)
        .recipientPhone(specialPhone)
        .roadAddress(specialRoadAddress)
        .detailAddress(specialDetailAddress)
        .zipCode(specialZipCode)
        .deliveryMemo(specialDeliveryMemo)
        .build();

    // then
    assertThat(specialDeliveryInfo.getRecipientName()).isEqualTo(specialName);
    assertThat(specialDeliveryInfo.getRecipientPhone()).isEqualTo(specialPhone);
    assertThat(specialDeliveryInfo.getRoadAddress()).isEqualTo(specialRoadAddress);
    assertThat(specialDeliveryInfo.getDetailAddress()).isEqualTo(specialDetailAddress);
    assertThat(specialDeliveryInfo.getZipCode()).isEqualTo(specialZipCode);
    assertThat(specialDeliveryInfo.getDeliveryMemo()).isEqualTo(specialDeliveryMemo);
  }

  @Test
  @DisplayName("배송완료 처리 후 배송정보 업데이트 테스트")
  void updateDeliveryInfoAfterDelivery() {
    // given
    deliveryInfo.markAsDelivered("1234567890");
    LocalDateTime deliveredAt = deliveryInfo.getDeliveredAt();

    // when
    deliveryInfo.updateDeliveryInfo("김수령", "010-9876-5432", "서울시 서초구 강남대로 456",
                                   "202호", "06540", "경비실에 맡겨주세요");

    // then
    assertThat(deliveryInfo.getRecipientName()).isEqualTo("김수령");
    assertThat(deliveryInfo.getTrackingNumber()).isEqualTo("1234567890");
    assertThat(deliveryInfo.getDeliveredAt()).isEqualTo(deliveredAt); // 배송완료 시간은 변경되지 않음
  }

  @Test
  @DisplayName("빈 DeliveryInfo 생성 테스트")
  void createEmptyDeliveryInfo() {
    // given & when
    DeliveryInfo emptyDeliveryInfo = DeliveryInfo.builder().build();

    // then
    assertThat(emptyDeliveryInfo.getRecipientName()).isNull();
    assertThat(emptyDeliveryInfo.getRecipientPhone()).isNull();
    assertThat(emptyDeliveryInfo.getRoadAddress()).isNull();
    assertThat(emptyDeliveryInfo.getDetailAddress()).isNull();
    assertThat(emptyDeliveryInfo.getZipCode()).isNull();
    assertThat(emptyDeliveryInfo.getDeliveryMemo()).isNull();
    assertThat(emptyDeliveryInfo.getTrackingNumber()).isNull();
    assertThat(emptyDeliveryInfo.getDeliveredAt()).isNull();
  }
}
