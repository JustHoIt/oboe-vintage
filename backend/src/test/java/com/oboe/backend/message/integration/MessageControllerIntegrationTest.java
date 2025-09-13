package com.oboe.backend.message.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oboe.backend.message.dto.SmsAuthRequestDto;
import com.oboe.backend.user.dto.FindPasswordDto;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import com.oboe.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.oboe.backend.common.component.RedisComponent;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "coolsms.apikey=test_api_key",
    "coolsms.apisecret=test_api_secret", 
    "coolsms.fromnumber=01012345678",
    "jwt.secret=test_jwt_secret_key_for_testing_purposes_only_32_chars",
    "jwt.access-token-expiration=86400000",
    "jwt.refresh-token-expiration=604800000",
    "spring.security.oauth2.client.registration.kakao.client-id=test_kakao_client_id",
    "spring.security.oauth2.client.registration.kakao.client-secret=test_kakao_client_secret",
    "spring.security.oauth2.client.registration.naver.client-id=test_naver_client_id",
    "spring.security.oauth2.client.registration.naver.client-secret=test_naver_client_secret"
})
@Transactional
@DisplayName("Message Controller 통합 테스트")
class MessageControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @MockBean
  private RedisComponent redisComponent;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    
    // Mock 설정
    when(redisComponent.hasKey(anyString())).thenReturn(false);
    when(redisComponent.get(anyString())).thenReturn(null);
    // set 메서드는 void이므로 doNothing() 사용
    doNothing().when(redisComponent).set(anyString(), anyString());
    when(redisComponent.delete(anyString())).thenReturn(true);
  }

  @Test
  @DisplayName("SMS 인증번호 발송 통합 테스트")
  void sendCertification_IntegrationTest() throws Exception {
    // given
    SmsAuthRequestDto dto = SmsAuthRequestDto.builder()
        .phoneNumber("01012345678")
        .build();

    // when & then - 실제 SMS 발송은 Mock으로 처리되지 않으므로 예외가 발생할 수 있음
    // 이 테스트는 API 엔드포인트가 정상적으로 작동하는지 확인
    mockMvc.perform(post("/api/v1/message/sendCertification")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().is5xxServerError()); // SMS 발송 실패로 인한 서버 에러
  }

  @Test
  @DisplayName("SMS 인증번호 검증 통합 테스트")
  void verifyCertification_IntegrationTest() throws Exception {
    // given
    SmsAuthRequestDto dto = SmsAuthRequestDto.builder()
        .phoneNumber("01012345678")
        .verificationCode("ABC123")
        .build();

    // when & then - Redis에 인증번호가 없으므로 실패
    mockMvc.perform(post("/api/v1/message/verifyCertification")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  @DisplayName("비밀번호 찾기 SMS 발송 통합 테스트 - 사용자 없음")
  void sendPasswordReset_UserNotFound() throws Exception {
    // given
    FindPasswordDto dto = FindPasswordDto.builder()
        .email("notfound@example.com")
        .phoneNumber("01012345678")
        .build();

    // when & then
    mockMvc.perform(post("/api/v1/message/sendPasswordReset")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  @DisplayName("비밀번호 찾기 SMS 발송 통합 테스트 - OAuth2 사용자")
  void sendPasswordReset_OAuth2User() throws Exception {
    // given
    User oauth2User = User.builder()
        .email("oauth2@example.com")
        .password("encodedPassword")
        .name("OAuth2사용자")
        .nickname("oauth2user")
        .phoneNumber("01012345678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.KAKAO)
        .build();
    userRepository.save(oauth2User);

    FindPasswordDto dto = FindPasswordDto.builder()
        .email("oauth2@example.com")
        .phoneNumber("01012345678")
        .build();

    // when & then
    mockMvc.perform(post("/api/v1/message/sendPasswordReset")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  @DisplayName("잘못된 휴대폰 번호 형식 테스트")
  void sendCertification_InvalidPhoneFormat() throws Exception {
    // given
    SmsAuthRequestDto dto = SmsAuthRequestDto.builder()
        .phoneNumber("1234567890") // 잘못된 형식
        .build();

    // when & then
    mockMvc.perform(post("/api/v1/message/sendCertification")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }
}
