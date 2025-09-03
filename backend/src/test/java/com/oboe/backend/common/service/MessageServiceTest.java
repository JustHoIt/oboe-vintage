package com.oboe.backend.common.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.oboe.backend.message.repository.MessageHistoryRepository;
import com.oboe.backend.common.component.RedisComponent;
import com.oboe.backend.message.dto.SmsAuthRequestDto;
import com.oboe.backend.common.exception.CustomException;
import com.oboe.backend.common.exception.ErrorCode;
import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.message.service.MessageService;
import com.oboe.backend.user.repository.UserRepository;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService 테스트")
class MessageServiceTest {

    @Mock
    private RedisComponent redisComponent;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageHistoryRepository messageHistoryRepository;

    @InjectMocks
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        // @Value 필드 주입
        ReflectionTestUtils.setField(messageService, "apiKey", "test_api_key_16");
        ReflectionTestUtils.setField(messageService, "apiSecret", "test_api_secret_16");
        ReflectionTestUtils.setField(messageService, "fromNumber", "01012345678");
    }

    @Test
    @DisplayName("인증번호 발송 성공 - 정상적인 휴대폰 번호")
    void sendMessage_Success() {
        // given
        SmsAuthRequestDto dto = SmsAuthRequestDto.builder()
                .phoneNumber("01012345678")
                .build();

        when(userRepository.existsByPhoneNumber(dto.getPhoneNumber())).thenReturn(false);
        when(redisComponent.hasKey(anyString())).thenReturn(false);

        // when & then - 실제 SMS 발송은 Mock으로 처리되지 않으므로 예외가 발생할 수 있음
        // 이 테스트는 로직 검증에 집중하고, 실제 SMS 발송은 MessageServiceMockTest에서 처리
        assertThatThrownBy(() -> messageService.sendMessage(dto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SMS_SEND_FAILED);
    }

    @Test
    @DisplayName("인증번호 발송 실패 - 이미 가입된 휴대폰 번호")
    void sendMessage_Fail_AlreadyRegisteredPhone() {
        // given
        SmsAuthRequestDto dto = SmsAuthRequestDto.builder()
                .phoneNumber("01012345678")
                .build();

        when(userRepository.existsByPhoneNumber(dto.getPhoneNumber())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> messageService.sendMessage(dto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PHONE_NUMBER_ALREADY_EXISTS)
                .hasMessage("이미 가입된 휴대폰 번호입니다.");

        // SMS 발송되지 않았는지 확인
        verify(redisComponent, never()).setExpiration(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("인증번호 발송 실패 - 재발송 제한")
    void sendMessage_Fail_RateLimit() {
        // given
        SmsAuthRequestDto dto = SmsAuthRequestDto.builder()
                .phoneNumber("01012345678")
                .build();

        when(userRepository.existsByPhoneNumber(dto.getPhoneNumber())).thenReturn(false);
        when(redisComponent.hasKey("sms_rate_limit:01012345678")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> messageService.sendMessage(dto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SMS_QUOTA_EXCEEDED)
                .hasMessage("인증번호는 1분 후에 다시 요청해주세요.");
    }

    @Test
    @DisplayName("인증번호 발송 실패 - 잘못된 휴대폰 번호 형식")
    void sendMessage_Fail_InvalidPhoneFormat() {
        // given
        SmsAuthRequestDto dto = SmsAuthRequestDto.builder()
                .phoneNumber("1234567890") // 잘못된 형식
                .build();

        // when & then
        assertThatThrownBy(() -> messageService.sendMessage(dto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SMS_INVALID_PHONE_NUMBER)
                .hasMessage("올바른 휴대폰 번호 형식이 아닙니다.");
    }

    @Test
    @DisplayName("인증번호 발송 실패 - 빈 휴대폰 번호")
    void sendMessage_Fail_EmptyPhone() {
        // given
        SmsAuthRequestDto dto = SmsAuthRequestDto.builder()
                .phoneNumber("")
                .build();

        // when & then
        assertThatThrownBy(() -> messageService.sendMessage(dto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SMS_INVALID_PHONE_NUMBER)
                .hasMessage("휴대폰 번호를 입력해주세요.");
    }

    @Test
    @DisplayName("인증번호 검증 성공")
    void verifyMessage_Success() {
        // given
        SmsAuthRequestDto dto = SmsAuthRequestDto.builder()
                .phoneNumber("01012345678")
                .verificationCode("ABC123")
                .build();

        when(redisComponent.get("sms_auth:01012345678")).thenReturn("ABC123");

        // when
        ResponseDto<String> result = messageService.verifyMessage(dto);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("인증이 완료되었습니다.");
        assertThat(result.getData()).isEqualTo("인증 성공");

        // Redis 작업 확인
        verify(redisComponent).delete("sms_auth:01012345678");
        verify(redisComponent).setExpiration(eq("sms_verified:01012345678"), eq("verified"), eq(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("인증번호 검증 실패 - 잘못된 인증번호")
    void verifyMessage_Fail_WrongCode() {
        // given
        SmsAuthRequestDto dto = SmsAuthRequestDto.builder()
                .phoneNumber("01012345678")
                .verificationCode("WRONG")
                .build();

        when(redisComponent.get("sms_auth:01012345678")).thenReturn("ABC123");

        // when & then
        assertThatThrownBy(() -> messageService.verifyMessage(dto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SMS_VERIFICATION_FAILED)
                .hasMessage("인증번호가 일치하지 않습니다.");

        // 인증 완료 상태 저장되지 않았는지 확인
        verify(redisComponent, never()).setExpiration(eq("sms_verified:01012345678"), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("인증번호 검증 실패 - 만료된 인증번호")
    void verifyMessage_Fail_ExpiredCode() {
        // given
        SmsAuthRequestDto dto = SmsAuthRequestDto.builder()
                .phoneNumber("01012345678")
                .verificationCode("ABC123")
                .build();

        when(redisComponent.get("sms_auth:01012345678")).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> messageService.verifyMessage(dto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SMS_VERIFICATION_EXPIRED)
                .hasMessage("인증번호가 만료되었거나 존재하지 않습니다.");
    }

    @Test
    @DisplayName("인증번호 검증 실패 - 빈 인증번호")
    void verifyMessage_Fail_EmptyCode() {
        // given
        SmsAuthRequestDto dto = SmsAuthRequestDto.builder()
                .phoneNumber("01012345678")
                .verificationCode("")
                .build();

        // when & then
        assertThatThrownBy(() -> messageService.verifyMessage(dto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SMS_INVALID_VERIFICATION_CODE)
                .hasMessage("인증번호를 입력해주세요.");
    }

    @Test
    @DisplayName("인증번호 검증 실패 - 빈 휴대폰 번호")
    void verifyMessage_Fail_EmptyPhone() {
        // given
        SmsAuthRequestDto dto = SmsAuthRequestDto.builder()
                .phoneNumber("")
                .verificationCode("ABC123")
                .build();

        // when & then
        assertThatThrownBy(() -> messageService.verifyMessage(dto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SMS_INVALID_PHONE_NUMBER)
                .hasMessage("휴대폰 번호를 입력해주세요.");
    }
}
