package com.oboe.backend.message.service;

import com.oboe.backend.message.entity.MessageHistory;
import com.oboe.backend.message.repository.MessageHistoryRepository;
import com.oboe.backend.common.component.RedisComponent;
import com.oboe.backend.message.dto.SmsAuthRequestDto;
import com.oboe.backend.common.exception.CustomException;
import com.oboe.backend.common.exception.ErrorCode;
import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.user.repository.UserRepository;
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
@Transactional
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

  private static final char[] ALNUM = "ABCDEFGHJKLMNPQRSTUVWXYZ123456789".toCharArray(); // 헷갈리는 0,1,O,I 제외 예시
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final Duration SMS_CODE_EXPIRATION = Duration.ofMinutes(3); // 3분 만료

  public ResponseDto<String> sendMessage(SmsAuthRequestDto dto) {
    // 휴대폰 번호 유효성 검증
    if (dto.getPhoneNumber() == null || dto.getPhoneNumber().trim().isEmpty()) {
      throw new CustomException(ErrorCode.SMS_INVALID_PHONE_NUMBER, "휴대폰 번호를 입력해주세요.");
    }

    // 휴대폰 번호 형식 검증
    if (!dto.getPhoneNumber().matches("^01[016789]\\d{7,8}$")) {
      throw new CustomException(ErrorCode.SMS_INVALID_PHONE_NUMBER, "올바른 휴대폰 번호 형식이 아닙니다.");
    }

    // 이미 가입된 휴대폰 번호인지 확인 (비용 절약)
    if (userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
      log.warn("이미 가입된 휴대폰 번호로 SMS 발송 시도 - 휴대폰번호: {}", dto.getPhoneNumber());
      throw new CustomException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS, "이미 가입된 휴대폰 번호입니다.");
    }

    // 인증번호 재발송 제한 확인 (1분 내 재발송 방지)
    String rateLimitKey = "sms_rate_limit:" + dto.getPhoneNumber();
    if (redisComponent.hasKey(rateLimitKey)) {
      log.warn("인증번호 재발송 제한 - 휴대폰번호: {}", dto.getPhoneNumber());
      throw new CustomException(ErrorCode.SMS_QUOTA_EXCEEDED, "인증번호는 1분 후에 다시 요청해주세요.");
    }

    DefaultMessageService messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret,
        "https://api.solapi.com");

    String verificationCode = sixRandomCode();
    Message message = new Message();
    message.setFrom(fromNumber);
    message.setTo(dto.getPhoneNumber());
    message.setText("[Oboe-Vintage] 본인 인증번호는 " + verificationCode + "입니다. 정확히 입력해주세요.");

    try {
      messageService.send(message);
      log.info("SMS 발송 성공 - 수신번호: {}, 인증번호: {}", message.getTo(), verificationCode);

      // Redis에 인증번호 저장 (3분간 유효)
      String redisKey = "sms_auth:" + dto.getPhoneNumber();
      redisComponent.setExpiration(redisKey, verificationCode, SMS_CODE_EXPIRATION);
      log.info("인증번호 Redis 저장 완료 - 키: {}, 만료시간: 3분", redisKey);

      // 재발송 제한 설정 (1분간)
      redisComponent.setExpiration(rateLimitKey, "limited", Duration.ofMinutes(1));
      log.info("재발송 제한 설정 완료 - 휴대폰번호: {}, 제한시간: 1분", dto.getPhoneNumber());

      messageHistoryRecord(message, true, null);
      return ResponseDto.success("인증번호 발송에 성공했습니다.", "인증번호가 발송되었습니다.");

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

  public ResponseDto<String> verifyMessage(SmsAuthRequestDto dto) {
    // 휴대폰 번호와 인증번호 유효성 검증
    if (dto.getPhoneNumber() == null || dto.getPhoneNumber().trim().isEmpty()) {
      throw new CustomException(ErrorCode.SMS_INVALID_PHONE_NUMBER, "휴대폰 번호를 입력해주세요.");
    }
    
    if (dto.getVerificationCode() == null || dto.getVerificationCode().trim().isEmpty()) {
      throw new CustomException(ErrorCode.SMS_INVALID_VERIFICATION_CODE, "인증번호를 입력해주세요.");
    }

    // Redis에서 인증번호 조회
    String redisKey = "sms_auth:" + dto.getPhoneNumber();
    Object storedCode = redisComponent.get(redisKey);
    
    if (storedCode == null) {
      log.warn("인증번호 만료 또는 존재하지 않음 - 휴대폰번호: {}", dto.getPhoneNumber());
      throw new CustomException(ErrorCode.SMS_VERIFICATION_EXPIRED, "인증번호가 만료되었거나 존재하지 않습니다.");
    }
    
    // 인증번호 비교
    if (storedCode.toString().equals(dto.getVerificationCode())) {
      // 인증 성공 시 인증번호 삭제하고 인증 완료 상태 저장
      redisComponent.delete(redisKey);
      
      // 인증 완료 상태 저장 (10분간 유효)
      String verifiedKey = "sms_verified:" + dto.getPhoneNumber();
      redisComponent.setExpiration(verifiedKey, "verified", Duration.ofMinutes(10));
      
      log.info("SMS 인증 성공 - 휴대폰번호: {}", dto.getPhoneNumber());
      return ResponseDto.success("인증이 완료되었습니다.", "인증 성공");
    } else {
      log.warn("SMS 인증 실패 - 휴대폰번호: {}, 입력된 인증번호: {}, 저장된 인증번호: {}", 
               dto.getPhoneNumber(), dto.getVerificationCode(), storedCode);
      throw new CustomException(ErrorCode.SMS_VERIFICATION_FAILED, "인증번호가 일치하지 않습니다.");
    }
  }

  private String sixRandomCode() {
    StringBuilder sb = new StringBuilder(6);
    for (int i = 0; i < 6; i++) {
      sb.append(ALNUM[SECURE_RANDOM.nextInt(ALNUM.length)]);
    }
    return sb.toString();
  }

  public void messageHistoryRecord(Message message, boolean status, String e) {
    MessageHistory messageHistory = MessageHistory.from(message);
    messageHistory.setStatus(status);
    messageHistory.setFailureReason(e);

    messageHistoryRepository.save(messageHistory);
  }

}
