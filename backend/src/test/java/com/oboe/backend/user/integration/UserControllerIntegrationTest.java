package com.oboe.backend.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oboe.backend.user.dto.LoginDto;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.oboe.backend.common.component.RedisComponent;
import com.oboe.backend.message.service.MessageService;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
@DisplayName("User Controller 통합 테스트")
class UserControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @MockBean
  private RedisComponent redisComponent;

  @MockBean
  private MessageService messageService;

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
  @DisplayName("회원가입 통합 테스트")
  void signUp_IntegrationTest() throws Exception {
    // given
    SignUpDto signUpDto = SignUpDto.builder()
        .email("integration@example.com")
        .password("password123!")
        .name("통합테스트")
        .nickname("integrationtest")
        .phoneNumber("01012345678")
        .roadAddress("서울시 강남구")
        .detailAddress("1층")
        .zipCode("02111")
        .birthDate(LocalDate.of(1990, 1, 1))
        .gender("남")
        .build();

    // Mock 설정
    when(redisComponent.hasKey("sms_verified:01012345678")).thenReturn(true);

    // when & then
    mockMvc.perform(post("/api/v1/users/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(signUpDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.email").value("integration@example.com"))
        .andExpect(jsonPath("$.data.nickname").value("integrationtest"));

    // 데이터베이스에 저장되었는지 확인
    assertThat(userRepository.findByEmail("integration@example.com")).isPresent();
  }

  @Test
  @DisplayName("로그인 통합 테스트")
  void login_IntegrationTest() throws Exception {
    // given
    User user = User.builder()
        .email("login@example.com")
        .password(passwordEncoder.encode("password123!"))
        .name("로그인테스트")
        .nickname("logintest")
        .phoneNumber("01012345678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .build();
    userRepository.save(user);

    LoginDto loginDto = LoginDto.builder()
        .email("login@example.com")
        .password("password123!")
        .build();

    // when & then
    mockMvc.perform(post("/api/v1/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.email").value("login@example.com"))
        .andExpect(jsonPath("$.data.accessToken").exists())
        .andExpect(jsonPath("$.data.refreshToken").exists());
  }

  @Test
  @DisplayName("이메일 중복 검사 통합 테스트")
  void signUp_Fail_DuplicateEmail() throws Exception {
    // given
    User existingUser = User.builder()
        .email("duplicate@example.com")
        .password(passwordEncoder.encode("password123!"))
        .name("기존사용자")
        .nickname("existinguser")
        .phoneNumber("01012345678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .build();
    userRepository.save(existingUser);

    SignUpDto signUpDto = SignUpDto.builder()
        .email("duplicate@example.com")
        .password("password123!")
        .name("중복테스트")
        .nickname("duplicatetest")
        .phoneNumber("01087654321")
        .build();

    // when & then
    mockMvc.perform(post("/api/v1/users/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(signUpDto)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  @DisplayName("닉네임 중복 검사 통합 테스트")
  void signUp_Fail_DuplicateNickname() throws Exception {
    // given
    User existingUser = User.builder()
        .email("existing@example.com")
        .password(passwordEncoder.encode("password123!"))
        .name("기존사용자")
        .nickname("duplicatenickname")
        .phoneNumber("01012345678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .build();
    userRepository.save(existingUser);

    SignUpDto signUpDto = SignUpDto.builder()
        .email("new@example.com")
        .password("password123!")
        .name("새사용자")
        .nickname("duplicatenickname")
        .phoneNumber("01087654321")
        .build();

    // when & then
    mockMvc.perform(post("/api/v1/users/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(signUpDto)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false));
  }
}
