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
@DisplayName("MessageService Mock 테스트 (실제 SMS 발송 없이)")
class MessageServiceMockTest {

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
        ReflectionTestUtils.setField(messageService, "apiKey", "test_api_key");
        ReflectionTestUtils.setField(messageService, "apiSecret", "test_api_secret");
        ReflectionTestUtils.setField(messageService, "fromNumber", "01012345678");
    }

    @Test
    @DisplayName("인증번호 검증 로직 테스트")
    void verifyMessage_LogicTest() {
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
    @DisplayName("인증번호 검증 실패 - 잘못된 코드")
    void verifyMessage_WrongCode_Test() {
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
    void verifyMessage_ExpiredCode_Test() {
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
    void verifyMessage_EmptyCode_Test() {
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
    void verifyMessage_EmptyPhone_Test() {
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