package com.oboe.backend.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 휴대폰번호 정규화 유틸리티 다양한 형식의 휴대폰번호를 표준 형식(01012345678)으로 변환
 */
@Slf4j
@Component
public class PhoneNumberUtil {

  // 한국 휴대폰번호 패턴 (010만 지원)
  private static final Pattern KOREAN_MOBILE_PATTERN = Pattern.compile("^010\\d{8}$");
  // 네이버 형식: 010-1111-2222
  private static final Pattern HYPHEN_PATTERN = Pattern.compile("^010-\\d{4}-\\d{4}$");

  /**
   * 휴대폰번호를 표준 형식(01012345678)으로 정규화
   *
   * @param phoneNumber 원본 휴대폰번호
   * @return 정규화된 휴대폰번호 (01012345678 형식)
   * @throws IllegalArgumentException 유효하지 않은 휴대폰번호인 경우
   */
  public static String normalizePhoneNumber(String phoneNumber) {
    if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
      log.warn("휴대폰번호가 null이거나 빈 문자열입니다.");
      return null;
    }

    // 공백 제거
    String normalized = phoneNumber.trim();
    log.debug("원본 휴대폰번호: '{}'", phoneNumber);

    // 이미 표준 형식인 경우
    if (KOREAN_MOBILE_PATTERN.matcher(normalized).matches()) {
      log.debug("이미 표준 형식입니다: {}", normalized);
      return normalized;
    }

    // 국제번호 +82 형식 처리
    if (normalized.startsWith("+82")) {
      normalized = handleInternationalFormat(normalized);
    }
    // 하이픈이 포함된 형식 처리
    else if (normalized.contains("-")) {
      normalized = handleHyphenFormat(normalized);
    }
    // 공백이 포함된 형식 처리
    else if (normalized.contains(" ")) {
      normalized = handleSpaceFormat(normalized);
    }

    // 최종 검증
    if (normalized != null && KOREAN_MOBILE_PATTERN.matcher(normalized).matches()) {
      log.debug("정규화 성공: '{}' -> '{}'", phoneNumber, normalized);
      return normalized;
    }

    log.warn("유효하지 않은 휴대폰번호 형식: '{}'", phoneNumber);
    throw new IllegalArgumentException("유효하지 않은 휴대폰번호 형식입니다: " + phoneNumber);
  }

  /**
   * 국제번호 +82 형식 처리 +82 10-3805-3128 -> 01038053128 +82-10-1234-5678 -> 01012345678
   */
  private static String handleInternationalFormat(String phoneNumber) {
    log.debug("국제번호 형식 처리: {}", phoneNumber);

    // +82 제거 (공백이나 하이픈이 있을 수 있음)
    String withoutCountryCode = phoneNumber.substring(3).trim();

    // +82- 형식인 경우 첫 번째 하이픈도 제거
    if (withoutCountryCode.startsWith("-")) {
      withoutCountryCode = withoutCountryCode.substring(1).trim();
    }

    // 0을 추가하여 010 형식으로 만들기
    if (withoutCountryCode.startsWith("1")) {
      withoutCountryCode = "0" + withoutCountryCode;
    }

    // 하이픈과 공백 제거
    return withoutCountryCode.replaceAll("[\\s-]", "");
  }

  /**
   * 하이픈이 포함된 형식 처리 010-3805-3128 -> 01038053128
   */
  private static String handleHyphenFormat(String phoneNumber) {
    log.debug("하이픈 형식 처리: {}", phoneNumber);

    if (HYPHEN_PATTERN.matcher(phoneNumber).matches()) {
      return phoneNumber.replaceAll("-", "");
    }

    // 일반적인 하이픈 패턴 처리
    return phoneNumber.replaceAll("-", "");
  }

  /**
   * 공백이 포함된 형식 처리 010 3805 3128 -> 01038053128
   */
  private static String handleSpaceFormat(String phoneNumber) {
    log.debug("공백 형식 처리: {}", phoneNumber);
    return phoneNumber.replaceAll("\\s+", "");
  }

  /**
   * 휴대폰번호 유효성 검증
   *
   * @param phoneNumber 검증할 휴대폰번호
   * @return 유효한 휴대폰번호인지 여부
   */
  public static boolean isValidPhoneNumber(String phoneNumber) {
    if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
      return false;
    }

    try {
      String normalized = normalizePhoneNumber(phoneNumber);
      return normalized != null && KOREAN_MOBILE_PATTERN.matcher(normalized).matches();
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * 휴대폰번호를 안전하게 정규화 (예외 발생 시 null 반환)
   *
   * @param phoneNumber 원본 휴대폰번호
   * @return 정규화된 휴대폰번호 또는 null
   */
  public static String safeNormalizePhoneNumber(String phoneNumber) {
    try {
      return normalizePhoneNumber(phoneNumber);
    } catch (Exception e) {
      log.warn("휴대폰번호 정규화 실패: {}", phoneNumber, e);
      return null;
    }
  }
}
