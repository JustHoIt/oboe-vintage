package com.oboe.backend.message.service;

import com.oboe.backend.common.component.RedisComponent;
import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.common.exception.CustomException;
import com.oboe.backend.common.exception.ErrorCode;
import com.oboe.backend.common.util.PhoneNumberUtil;
import com.oboe.backend.message.dto.SmsAuthRequestDto;
import com.oboe.backend.message.entity.MessageHistory;
import com.oboe.backend.message.repository.MessageHistoryRepository;
import com.oboe.backend.user.dto.FindPasswordDto;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserStatus;
import com.oboe.backend.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.exception.NurigoEmptyResponseException;
import net.nurigo.sdk.message.exception.NurigoMessageNotReceivedException;
import net.nurigo.sdk.message.exception.NurigoUnknownException;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

  private final RedisComponent redisComponent;
  private final MessageHistoryRepository messageHistoryRepository;
  private final UserRepository userRepository;

  @Value("${coolsms.apikey}")
  private String apiKey;

  @Value("${coolsms.apisecret}")
  private String apiSecret;

  @Value("${coolsms.fromnumber}")
  private String fromNumber;

  @PostConstruct
  private void validateConfiguration() {
    if (apiKey == null || apiKey.isEmpty()) {
      throw new IllegalStateException("SMS API Key가 설정되지 않았습니다.");
    }
    if (apiSecret == null || apiSecret.isEmpty()) {
      throw new IllegalStateException("SMS API Secret이 설정되지 않았습니다.");
    }
    if (fromNumber == null || fromNumber.isEmpty()) {
      throw new IllegalStateException("SMS 발신번호가 설정되지 않았습니다.");
    }
    log.info("SMS 서비스 설정 검증 완료");
  }

  private static final char[] ALNUM = "ABCDEFGHJKLMNPQRSTUVWXYZ123456789".toCharArray(); // 헷갈리는 0,1,O,I 제외 예시
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final Duration SMS_CODE_EXPIRATION = Duration.ofMinutes(3); // 3분 만료

  // ✅회원가입 인증 문자 발송
  @Transactional
  public ResponseDto<String> sendMessage(SmsAuthRequestDto dto) {
    log.info("회원가입 SMS 발송 요청 - 휴대폰번호: {}", dto.getPhoneNumber());

    // 1. 휴대폰 번호 유효성 검증 및 정규화
    String normalizedPhoneNumber = validateAndNormalizePhoneNumber(dto.getPhoneNumber());

    // 2. 회원가입 관련 검증 (중복 가입 확인)
    validateSignUpEligibility(normalizedPhoneNumber);

    // 3. Rate limiting 검증
    validateRateLimit(normalizedPhoneNumber);

    // 4. SMS 발송
    String verificationCode = sixRandomCode();
    String messageText = "[Oboe-Vintage] 본인 인증번호는 " + verificationCode + "입니다. 정확히 입력해주세요.";
    sendSmsAndStoreCode(normalizedPhoneNumber, messageText, verificationCode);

    return ResponseDto.success("인증번호 발송에 성공했습니다.", "인증번호가 발송되었습니다.");
  }


  // ✅비밀번호 찾기 인증번호 발송
  @Transactional
  public ResponseDto<String> sendPasswordResetMessage(FindPasswordDto dto) {
    log.info("비밀번호 찾기 SMS 발송 요청 - 이메일: {}, 휴대폰번호: {}", dto.getEmail(), dto.getPhoneNumber());

    // 1. 사용자 검증 (이메일, 휴대폰번호로 일반 계정 조회)
    User user = validatePasswordResetEligibility(dto.getEmail(), dto.getPhoneNumber());

    // 2. 휴대폰 번호 유효성 검증 (이미 DB에 있는 번호이므로 정규화 불필요)
    validatePhoneNumber(dto.getPhoneNumber());

    // 3. Rate limiting 검증
    validateRateLimit(dto.getPhoneNumber());

    // 4. SMS 발송
    String verificationCode = sixRandomCode();
    String messageText = "[Oboe-Vintage] 비밀번호 찾기 인증번호는 " + verificationCode + "입니다. 정확히 입력해주세요.";
    sendSmsAndStoreCode(dto.getPhoneNumber(), messageText, verificationCode);

    return ResponseDto.success("인증번호 발송에 성공했습니다.", "인증번호가 발송되었습니다.");
  }

  // ✅인증번호 확인
  @Transactional(readOnly = true)
  public ResponseDto<String> verifyMessage(SmsAuthRequestDto dto) {
    log.info("SMS 인증 확인 요청 - 휴대폰번호: {}", dto.getPhoneNumber());

    // 1. 입력값 검증
    validatePhoneNumber(dto.getPhoneNumber());
    validateVerificationCode(dto.getVerificationCode());

    // 2. 인증번호 확인 및 처리
    verifyAndProcessCode(dto.getPhoneNumber(), dto.getVerificationCode());

    log.info("SMS 인증 성공 - 휴대폰번호: {}", dto.getPhoneNumber());
    return ResponseDto.success("인증이 완료되었습니다.", "인증 성공");
  }

  // ✅랜덤 6글자 생성
  private String sixRandomCode() {
    StringBuilder sb = new StringBuilder(6);
    for (int i = 0; i < 6; i++) {
      sb.append(ALNUM[SECURE_RANDOM.nextInt(ALNUM.length)]);
    }
    return sb.toString();
  }

  // 문자 발송시 DB에 문자 내역 저장
  public void messageHistoryRecord(Message message, boolean status, String e) {
    MessageHistory messageHistory = MessageHistory.from(message);
    messageHistory.setStatus(status);
    messageHistory.setFailureReason(e);

    messageHistoryRepository.save(messageHistory);
  }

  // ==============================
  // 공통 검증 메서드들
  // ==============================

  // 휴대폰 번호 유효성 검증 및 정규화
  private String validateAndNormalizePhoneNumber(String phoneNumber) {
    validatePhoneNumber(phoneNumber);

    try {
      String normalizedPhoneNumber = PhoneNumberUtil.normalizePhoneNumber(phoneNumber);
      log.info("SMS 발송 휴대폰번호 정규화: '{}' -> '{}'", phoneNumber, normalizedPhoneNumber);
      return normalizedPhoneNumber;
    } catch (IllegalArgumentException e) {
      log.warn("SMS 발송 휴대폰번호 정규화 실패: '{}'", phoneNumber, e);
      throw new CustomException(ErrorCode.SMS_INVALID_PHONE_NUMBER, "올바른 휴대폰 번호 형식이 아닙니다.");
    }
  }

  // 휴대폰 번호 기본 유효성 검증
  private void validatePhoneNumber(String phoneNumber) {
    if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
      throw new CustomException(ErrorCode.SMS_INVALID_PHONE_NUMBER, "휴대폰 번호를 입력해주세요.");
    }
  }

  // 인증번호 유효성 검증
  private void validateVerificationCode(String verificationCode) {
    if (verificationCode == null || verificationCode.trim().isEmpty()) {
      throw new CustomException(ErrorCode.SMS_INVALID_VERIFICATION_CODE, "인증번호를 입력해주세요.");
    }
  }

  // 회원가입 자격 검증 (중복 가입 확인)
  private void validateSignUpEligibility(String normalizedPhoneNumber) {
    if (userRepository.existsByPhoneNumber(normalizedPhoneNumber)) {
      log.warn("이미 가입된 휴대폰 번호로 SMS 발송 시도 - 휴대폰번호: {}", normalizedPhoneNumber);
      throw new CustomException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS, "이미 가입된 휴대폰 번호입니다.");
    }
  }

  // 비밀번호 재설정 자격 검증
  private User validatePasswordResetEligibility(String email, String phoneNumber) {
    // 이메일과 휴대폰번호로 사용자 검색 (일반 계정만)
    User user = userRepository.findByEmailAndPhoneNumberAndSocialProvider(email, phoneNumber)
        .orElseThrow(() -> {
          log.warn("비밀번호 찾기 실패 - 일치하는 사용자 없음: 이메일={}, 휴대폰번호={}", email, phoneNumber);
          return new CustomException(ErrorCode.USER_NOT_FOUND, "입력하신 정보와 일치하는 계정을 찾을 수 없습니다.");
        });

    // 일반 계정인지 확인
    if (user.getSocialProvider() != SocialProvider.LOCAL) {
      log.warn("비밀번호 재설정 실패 - 소셜 로그인 사용자: {}, 제공자: {}", user.getEmail(), user.getSocialProvider());
      throw new CustomException(ErrorCode.SOCIAL_LOGIN_NOT_SUPPORTED);
    }

    // 사용자 상태 확인
    if (user.getStatus() != UserStatus.ACTIVE) {
      log.warn("비밀번호 찾기 실패 - 비활성 사용자: {}, 상태: {}", user.getEmail(), user.getStatus());
      throw new CustomException(ErrorCode.FORBIDDEN, "비활성화된 계정입니다.");
    }

    return user;
  }

  // Rate limiting 검증
  private void validateRateLimit(String phoneNumber) {
    String rateLimitKey = "sms_rate_limit:" + phoneNumber;
    if (redisComponent.hasKey(rateLimitKey)) {
      log.warn("인증번호 재발송 제한 - 휴대폰번호: {}", phoneNumber);
      throw new CustomException(ErrorCode.SMS_QUOTA_EXCEEDED, "인증번호는 1분 후에 다시 요청해주세요.");
    }
  }

  // ==============================
  // 공통 처리 메서드들
  // ==============================

  // SMS 발송 및 Redis 저장
  private void sendSmsAndStoreCode(String phoneNumber, String messageText,
      String verificationCode) {
    DefaultMessageService messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret,
        "https://api.solapi.com");

    Message message = new Message();
    message.setFrom(fromNumber);
    message.setTo(phoneNumber);
    message.setText(messageText);

    try {
      messageService.send(message);
      log.info("SMS 발송 성공 - 수신번호: {}, 인증번호: {}", message.getTo(), verificationCode);

      // Redis에 인증번호 저장 (3분간 유효)
      String redisKey = "sms_auth:" + phoneNumber;
      redisComponent.setExpiration(redisKey, verificationCode, SMS_CODE_EXPIRATION);
      log.info("인증번호 Redis 저장 완료 - 키: {}, 만료시간: 3분", redisKey);

      // 재발송 제한 설정 (1분간)
      String rateLimitKey = "sms_rate_limit:" + phoneNumber;
      redisComponent.setExpiration(rateLimitKey, "limited", Duration.ofMinutes(1));
      log.info("재발송 제한 설정 완료 - 휴대폰번호: {}, 제한시간: 1분", phoneNumber);

      messageHistoryRecord(message, true, null);

    } catch (NurigoMessageNotReceivedException e) {
      log.error("SMS 발송 실패 - 메시지 수신 실패: {}", e.getMessage());
      messageHistoryRecord(message, false, e.getMessage());
      throw new CustomException(ErrorCode.SMS_SEND_FAILED, "SMS 발송에 실패했습니다. 다시 시도해주세요.");
    } catch (NurigoEmptyResponseException e) {
      log.error("SMS 발송 실패 - 빈 응답: {}", e.getMessage());
      messageHistoryRecord(message, false, e.getMessage());
      throw new CustomException(ErrorCode.SMS_SERVICE_UNAVAILABLE, "SMS 서비스에 일시적인 문제가 발생했습니다.");
    } catch (NurigoUnknownException e) {
      log.error("SMS 발송 실패 - 알 수 없는 오류: {}", e.getMessage());
      messageHistoryRecord(message, false, e.getMessage());
      throw new CustomException(ErrorCode.SMS_SEND_FAILED, "SMS 발송 중 오류가 발생했습니다.");
    } catch (Exception e) {
      log.error("SMS 발송 실패 - 예상치 못한 오류: {}", e.getMessage(), e);
      messageHistoryRecord(message, false, e.getMessage());
      throw new CustomException(ErrorCode.SMS_SEND_FAILED, "SMS 발송 중 예상치 못한 오류가 발생했습니다.");
    }
  }

  // 인증번호 확인 및 처리
  private void verifyAndProcessCode(String phoneNumber, String verificationCode) {
    // Redis에서 인증번호 조회
    String redisKey = "sms_auth:" + phoneNumber;
    Object storedCode = redisComponent.get(redisKey);

    if (storedCode == null) {
      log.warn("인증번호 만료 또는 존재하지 않음 - 휴대폰번호: {}", phoneNumber);
      throw new CustomException(ErrorCode.SMS_VERIFICATION_EXPIRED, "인증번호가 만료되었거나 존재하지 않습니다.");
    }

    // 인증번호 비교
    if (!storedCode.toString().equals(verificationCode)) {
      log.warn("SMS 인증 실패 - 휴대폰번호: {}, 입력된 인증번호: {}, 저장된 인증번호: {}",
          phoneNumber, verificationCode, storedCode);
      throw new CustomException(ErrorCode.SMS_VERIFICATION_FAILED, "인증번호가 일치하지 않습니다.");
    }

    // 인증 성공 시 인증번호 삭제하고 인증 완료 상태 저장
    redisComponent.delete(redisKey);

    // 인증 완료 상태 저장 (10분간 유효)
    String verifiedKey = "sms_verified:" + phoneNumber;
    redisComponent.setExpiration(verifiedKey, "verified", Duration.ofMinutes(10));
  }

}
