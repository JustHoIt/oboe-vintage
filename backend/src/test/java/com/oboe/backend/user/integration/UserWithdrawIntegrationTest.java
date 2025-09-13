package com.oboe.backend.user.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oboe.backend.user.dto.WithdrawDto;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import com.oboe.backend.user.repository.UserRepository;
import com.oboe.backend.common.util.JwtUtil;
import com.oboe.backend.common.service.FileUploadService;
import com.oboe.backend.message.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
    "jwt.secret=test_jwt_secret_key_for_testing_purposes_only_must_be_at_least_64_characters_long_for_HS512_algorithm",
    "jwt.access-token-expiration=86400000",
    "jwt.refresh-token-expiration=604800000"
})
@DisplayName("회원탈퇴 통합 테스트")
class UserWithdrawIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtUtil jwtUtil;

  @MockBean
  private FileUploadService fileUploadService;

  @MockBean
  private MessageService messageService;

  private User testUser;
  private String accessToken;

  @BeforeEach
  void setUp() {
    // 테스트 사용자 생성
    testUser = User.builder()
        .email("withdraw@example.com")
        .password(passwordEncoder.encode("password123!"))
        .name("탈퇴테스트")
        .nickname("withdrawtest")
        .phoneNumber("01012345678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .roadAddress("서울시 강남구")
        .detailAddress("1층")
        .zipCode("02111")
        .birthDate(LocalDate.of(1990, 1, 1))
        .gender("남")
        .profileImg("https://example.com/profile.jpg")
        .build();

    testUser = userRepository.save(testUser);

    // JWT 토큰 생성
    accessToken = jwtUtil.generateAccessToken(testUser);
  }

  @Test
  @DisplayName("회원탈퇴 성공 - 정상적인 요청")
  void withdrawUserSuccess() throws Exception {
    // given
    WithdrawDto withdrawDto = WithdrawDto.builder()
        .password("password123!")
        .reason("서비스 불만족")
        .build();

    // when & then
    mockMvc.perform(delete("/api/v1/users/me")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(withdrawDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value("회원탈퇴가 완료되었습니다. 30일 후 개인정보가 완전히 삭제됩니다."));

    // 데이터베이스에서 사용자 상태 확인
    User withdrawnUser = userRepository.findById(testUser.getId()).orElseThrow();
    assertThat(withdrawnUser.getStatus()).isEqualTo(UserStatus.WITHDRAW);
    assertThat(withdrawnUser.getDeletedAt()).isNotNull();
    assertThat(withdrawnUser.getEmail()).startsWith("deleted_");
    assertThat(withdrawnUser.getEmail()).endsWith("@deleted.local");
  }

  @Test
  @DisplayName("회원탈퇴 실패 - 잘못된 비밀번호")
  void withdrawUserFailWrongPassword() throws Exception {
    // given
    WithdrawDto withdrawDto = WithdrawDto.builder()
        .password("wrongPassword")
        .reason("서비스 불만족")
        .build();

    // when & then
    mockMvc.perform(delete("/api/v1/users/me")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(withdrawDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("비밀번호가 올바르지 않습니다."));

    // 사용자 상태가 변경되지 않았는지 확인
    User user = userRepository.findById(testUser.getId()).orElseThrow();
    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(user.getDeletedAt()).isNull();
  }

  @Test
  @DisplayName("회원탈퇴 실패 - 인증 토큰 없음")
  void withdrawUserFailNoToken() throws Exception {
    // given
    WithdrawDto withdrawDto = WithdrawDto.builder()
        .password("password123!")
        .reason("서비스 불만족")
        .build();

    // when & then
    mockMvc.perform(delete("/api/v1/users/me")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(withdrawDto)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("회원탈퇴 실패 - 유효하지 않은 토큰")
  void withdrawUserFailInvalidToken() throws Exception {
    // given
    WithdrawDto withdrawDto = WithdrawDto.builder()
        .password("password123!")
        .reason("서비스 불만족")
        .build();

    // when & then
    mockMvc.perform(delete("/api/v1/users/me")
            .header("Authorization", "Bearer invalid.token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(withdrawDto)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("회원탈퇴 실패 - 소셜 로그인 사용자")
  void withdrawUserFailSocialUser() throws Exception {
    // given
    User socialUser = User.builder()
        .email("social@example.com")
        .password(passwordEncoder.encode("password123!"))
        .name("소셜사용자")
        .nickname("socialuser")
        .phoneNumber("01087654321")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.KAKAO)
        .build();

    socialUser = userRepository.save(socialUser);
    String socialAccessToken = jwtUtil.generateAccessToken(socialUser);

    WithdrawDto withdrawDto = WithdrawDto.builder()
        .password("password123!")
        .reason("서비스 불만족")
        .build();

    // when & then
    mockMvc.perform(delete("/api/v1/users/me")
            .header("Authorization", "Bearer " + socialAccessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(withdrawDto)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("소셜 로그인 사용자는 해당 기능을 이용할 수 없습니다."));
  }

  @Test
  @DisplayName("회원탈퇴 실패 - 이미 탈퇴된 사용자")
  void withdrawUserFailAlreadyWithdrawn() throws Exception {
    // given - 탈퇴 처리
    testUser.withdraw();
    userRepository.save(testUser);

    WithdrawDto withdrawDto = WithdrawDto.builder()
        .password("password123!")
        .reason("서비스 불만족")
        .build();

    // when & then
    mockMvc.perform(delete("/api/v1/users/me")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(withdrawDto)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("이용할수 없는 계정입니다."));
  }

  @Test
  @DisplayName("회원탈퇴 실패 - 유효성 검증 실패")
  void withdrawUserFailValidation() throws Exception {
    // given
    WithdrawDto invalidDto = WithdrawDto.builder()
        .password("") // 빈 비밀번호
        .reason("") // 빈 사유
        .build();

    // when & then
    mockMvc.perform(delete("/api/v1/users/me")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidDto)))
        .andExpect(status().isBadRequest());
  }
}
