package com.oboe.backend.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("휴대폰번호 정규화 유틸리티 테스트")
class PhoneNumberUtilTest {

  @Test
  @DisplayName("OAuth2 휴대폰번호 정규화 테스트")
  void testNormalizePhoneNumber_OAuth2() {
    // 카카오 형식: +82 10-1111-2222
    assertEquals("01011112222", PhoneNumberUtil.normalizePhoneNumber("+82 10-1111-2222"));

    // 네이버 형식: 010-1111-2222
    assertEquals("01011112222", PhoneNumberUtil.normalizePhoneNumber("010-1111-2222"));

    // 표준 형식: 01011112222
    assertEquals("01011112222", PhoneNumberUtil.normalizePhoneNumber("01011112222"));
  }

  @Test
  @DisplayName("OAuth2 중복 검사 시나리오 테스트")
  void testOAuth2DuplicateCheck() {
    // 카카오와 네이버에서 같은 번호를 다른 형식으로 받아와도
    // 같은 정규화된 번호로 변환되어 중복 검사가 정상 작동하는지 확인

    String kakaoPhone = "+82 10-1111-2222";
    String naverPhone = "010-1111-2222";

    String normalizedKakao = PhoneNumberUtil.normalizePhoneNumber(kakaoPhone);
    String normalizedNaver = PhoneNumberUtil.normalizePhoneNumber(naverPhone);

    // 두 형식이 같은 정규화된 번호로 변환되는지 확인
    assertEquals("01011112222", normalizedKakao);
    assertEquals("01011112222", normalizedNaver);
    assertEquals(normalizedKakao, normalizedNaver);
  }

  @Test
  @DisplayName("010 번호만 지원하는지 테스트")
  void testOnly010Numbers() {
    // 010 번호는 정상 처리
    assertEquals("01012345678", PhoneNumberUtil.normalizePhoneNumber("010-1234-5678"));
    assertEquals("01012345678", PhoneNumberUtil.normalizePhoneNumber("+82 10-1234-5678"));

    // 다른 통신사 번호는 예외 발생
    assertThrows(IllegalArgumentException.class, () ->
        PhoneNumberUtil.normalizePhoneNumber("016-1234-5678"));
    assertThrows(IllegalArgumentException.class, () ->
        PhoneNumberUtil.normalizePhoneNumber("017-1234-5678"));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "01011112222",      // 표준 형식
      "010-1111-2222",    // 네이버 형식
      "+82 10-1111-2222"  // 카카오 형식
  })
  @DisplayName("OAuth2 유효한 휴대폰번호 형식 검증")
  void testIsValidPhoneNumber_OAuth2Formats(String phoneNumber) {
    assertTrue(PhoneNumberUtil.isValidPhoneNumber(phoneNumber),
        "OAuth2 휴대폰번호가 유효해야 합니다: " + phoneNumber);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "010-123-456",     // 10자리 (너무 짧음)
      "010-1234-56789",  // 12자리 (너무 김)
      "016-1234-5678",   // 010이 아닌 번호
      "017-1234-5678",   // 010이 아닌 번호
      "abc-def-ghij",    // 문자 포함
      "0101234567a",     // 문자 포함
      "+1 123-456-7890", // 미국 번호
      "+82 20-1234-5678" // 010이 아닌 국제번호
  })
  @DisplayName("유효하지 않은 휴대폰번호 형식 검증")
  void testIsValidPhoneNumber_InvalidFormats(String phoneNumber) {
    assertFalse(PhoneNumberUtil.isValidPhoneNumber(phoneNumber),
        "휴대폰번호가 유효하지 않아야 합니다: " + phoneNumber);
  }

  @Test
  @DisplayName("null 및 빈 문자열 처리")
  void testNullAndEmpty() {
    assertNull(PhoneNumberUtil.normalizePhoneNumber(null));
    assertNull(PhoneNumberUtil.normalizePhoneNumber(""));
    assertNull(PhoneNumberUtil.normalizePhoneNumber("   "));
  }

  @Test
  @DisplayName("유효하지 않은 형식에 대한 예외 처리")
  void testInvalidFormatThrowsException() {
    assertThrows(IllegalArgumentException.class, () ->
        PhoneNumberUtil.normalizePhoneNumber("010-123-456"));

    assertThrows(IllegalArgumentException.class, () ->
        PhoneNumberUtil.normalizePhoneNumber("020-1234-5678"));

    assertThrows(IllegalArgumentException.class, () ->
        PhoneNumberUtil.normalizePhoneNumber("abc-def-ghij"));
  }

  @Test
  @DisplayName("안전한 정규화 테스트")
  void testSafeNormalizePhoneNumber() {
    // 유효한 번호
    assertEquals("01012345678", PhoneNumberUtil.safeNormalizePhoneNumber("010-1234-5678"));

    // 유효하지 않은 번호 (null 반환)
    assertNull(PhoneNumberUtil.safeNormalizePhoneNumber("010-123-456"));
    assertNull(PhoneNumberUtil.safeNormalizePhoneNumber("invalid"));
    assertNull(PhoneNumberUtil.safeNormalizePhoneNumber(null));
  }
}
