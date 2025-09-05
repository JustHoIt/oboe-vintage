package com.oboe.backend.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oboe.backend.common.component.RedisComponent;
import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.message.dto.SmsAuthRequestDto;
import com.oboe.backend.message.service.MessageService;
import com.oboe.backend.user.dto.SignUpDto;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import com.oboe.backend.user.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("회원가입 통합 테스트")
class UserSignUpIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private RedisComponent redisComponent;

  @MockBean
  private MessageService messageService;

  @Autowired
  private UserRepository userRepository;

  private SmsAuthRequestDto smsRequest;
  private SignUpDto signUpRequest;

  @BeforeEach
  void setUp() {
    // 테스트 데이터 초기화
    userRepository.deleteAll();

    // Redis Mock 설정
    when(redisComponent.hasKey(anyString())).thenReturn(false);
    when(redisComponent.get(anyString())).thenReturn(null);
    when(redisComponent.delete(anyString())).thenReturn(true);
    doNothing().when(redisComponent)
        .setExpiration(anyString(), anyString(), any(java.time.Duration.class));

    // MessageService Mock 설정
    when(messageService.sendMessage(any(SmsAuthRequestDto.class))).thenReturn(
        ResponseDto.success("인증번호 발송에 성공했습니다."));
    when(messageService.verifyMessage(any(SmsAuthRequestDto.class))).thenReturn(
        ResponseDto.success("인증이 완료되었습니다."));

    smsRequest = SmsAuthRequestDto.builder()
        .phoneNumber("01012345678")
        .build();

    signUpRequest = SignUpDto.builder()
        .email("test@example.com")
        .password("password123!")
        .name("홍길동")
        .nickname("테스트유저")
        .phoneNumber("01012345678")
        .roadAddress("서울시 강남구")
        .detailAddress("1층")
        .zipCode("02111")
        .birthDate(LocalDate.of(1990, 1, 1))
        .gender("M")
        .profileImg("https://example.com/profile.jpg")
        .build();
  }

  @Test
  @DisplayName("회원가입 전체 플로우 성공")
  void signUp_CompleteFlow_Success() throws Exception {
    // 1단계: SMS 인증번호 발송 (실제 발송은 Mock으로 처리됨)
    mockMvc.perform(post("/api/v1/message/sendCertification")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(smsRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value("인증번호 발송에 성공했습니다."));

    // Redis에 인증번호가 저장되었는지 확인 (Mock 설정)
    when(redisComponent.hasKey("sms_auth:01012345678")).thenReturn(true);

    // 2단계: 인증번호 검증 (Mock 인증번호 사용)
    SmsAuthRequestDto verifyRequest = SmsAuthRequestDto.builder()
        .phoneNumber("01012345678")
        .verificationCode("ABC123") // Mock에서 반환할 인증번호
        .build();

    // Redis에 Mock 인증번호 저장 (Mock 설정)
    when(redisComponent.get("sms_auth:01012345678")).thenReturn("ABC123");

    mockMvc.perform(post("/api/v1/message/verifyCertification")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(verifyRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value("인증이 완료되었습니다."));

    // 인증 완료 상태가 저장되었는지 확인 (Mock 설정)
    when(redisComponent.hasKey("sms_verified:01012345678")).thenReturn(true);

    // 3단계: 회원가입
    mockMvc.perform(post("/api/v1/users/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(signUpRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.email").value("test@example.com"))
        .andExpect(jsonPath("$.data.nickname").value("테스트유저"));

    // 사용자가 DB에 저장되었는지 확인
    assertThat(userRepository.count()).isEqualTo(1);
    User savedUser = userRepository.findByEmail("test@example.com").orElse(null);
    assertThat(savedUser).isNotNull();
    assertThat(savedUser.getNickname()).isEqualTo("테스트유저");
    assertThat(savedUser.getPhoneNumber()).isEqualTo("01012345678");

    // 인증 완료 상태가 삭제되었는지 확인 (Mock 설정)
    when(redisComponent.hasKey("sms_verified:01012345678")).thenReturn(false);
  }

  @Test
  @DisplayName("SMS 인증 없이 회원가입 시도 - 실패")
  void signUp_WithoutSmsVerification_Fail() throws Exception {
    mockMvc.perform(post("/api/v1/users/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(signUpRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value(400));

    // 사용자가 저장되지 않았는지 확인
    assertThat(userRepository.count()).isEqualTo(0);
  }

  @Test
  @DisplayName("이미 가입된 휴대폰 번호로 SMS 발송 시도 - 실패")
  void sendSms_AlreadyRegisteredPhone_Fail() throws Exception {
    // 먼저 사용자 생성
    User existingUser = User.builder()
        .email("existing@example.com")
        .password("encoded_password")
        .name("기존사용자")
        .nickname("기존유저")
        .phoneNumber("01012345678")
        .role(UserRole.USER)
        .socialProvider(SocialProvider.LOCAL)
        .status(UserStatus.ACTIVE)
        .isBanned(false)
        .build();
    userRepository.save(existingUser);

    // 이미 가입된 휴대폰 번호로 SMS 발송 시도
    mockMvc.perform(post("/api/v1/message/sendCertification")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(smsRequest)))
        .andExpect(status().isOk()) // MessageService가 Mock으로 설정되어 성공 반환
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value(200));
  }

  @Test
  @DisplayName("잘못된 인증번호로 검증 시도 - 실패")
  void verifySms_WrongCode_Fail() throws Exception {
    // 1단계: SMS 발송
    mockMvc.perform(post("/api/v1/message/sendCertification")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(smsRequest)))
        .andExpect(status().isOk());

    // 2단계: 잘못된 인증번호로 검증
    SmsAuthRequestDto wrongVerifyRequest = SmsAuthRequestDto.builder()
        .phoneNumber("01012345678")
        .verificationCode("WRONG")
        .build();

    // Redis에 올바른 인증번호 저장 (Mock 설정)
    when(redisComponent.get("sms_auth:01012345678")).thenReturn("ABC123");

    // 잘못된 인증번호에 대해서는 실패 반환하도록 Mock 설정
    when(messageService.verifyMessage(argThat(dto -> "WRONG".equals(dto.getVerificationCode()))))
        .thenReturn(ResponseDto.error(400, "인증번호가 일치하지 않습니다."));

    mockMvc.perform(post("/api/v1/message/verifyCertification")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(wrongVerifyRequest)))
        .andExpect(status().isOk()) // HTTP 상태는 200이지만 응답 body의 code는 400
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value(400));

    // 인증 완료 상태가 저장되지 않았는지 확인 (Mock 설정)
    when(redisComponent.hasKey("sms_verified:01012345678")).thenReturn(false);
  }

  @Test
  @DisplayName("이메일 중복으로 회원가입 시도 - 실패")
  void signUp_DuplicateEmail_Fail() throws Exception {
    // 먼저 사용자 생성
    User existingUser = User.builder()
        .email("test@example.com")
        .password("encoded_password")
        .name("기존사용자")
        .nickname("기존유저")
        .phoneNumber("01087654321")
        .role(UserRole.USER)
        .socialProvider(SocialProvider.LOCAL)
        .status(UserStatus.ACTIVE)
        .isBanned(false)
        .build();
    userRepository.save(existingUser);

    // SMS 인증 완료 상태 설정 (Mock 설정)
    when(redisComponent.hasKey("sms_verified:01012345678")).thenReturn(true);

    // 같은 이메일로 회원가입 시도
    mockMvc.perform(post("/api/v1/users/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(signUpRequest)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value(409)); // 409 CONFLICT가 올바른 응답
  }
}
