package com.oboe.backend.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.oboe.backend.common.component.RedisComponent;
import com.oboe.backend.common.exception.CustomException;
import com.oboe.backend.common.exception.ErrorCode;
import com.oboe.backend.common.dto.ResponseDto;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("유저 서비스 테스트")
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private RedisComponent redisComponent;

  @InjectMocks
  private UserService userService;

  private SignUpDto validSignUpDto;
  private User savedUser;

  @BeforeEach
  void setUp() {
    validSignUpDto = SignUpDto.builder()
        .email("test@example.com")
        .password("password123!")
        .name("홍길동")
        .nickname("테스트유저")
        .phoneNumber("01012345678")
        .roadAddress("서울시 강남구")
        .detailAddress("1층")
        .zipCode("02111")
        .birthDate(LocalDate.of(1990, 1, 1))
        .gender("남")
        .profileImg("https://example.com/profile.jpg")
        .build();

    savedUser = User.builder()
        .id(1L)
        .email("test@example.com")
        .password("encoded_password")
        .name("홍길동")
        .nickname("테스트유저")
        .phoneNumber("01012345678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .roadAddress("서울시 강남구")
        .detailAddress("1층")
        .zipCode("02111")
        .birthDate(LocalDate.of(1990, 1, 1))
        .gender("남")
        .socialProvider(SocialProvider.LOCAL)
        .isBanned(false)
        .profileImg("https://example.com/profile.jpg")
        .build();
  }

  @Test
  @DisplayName("회원가입 성공 - 모든 조건 만족")
  void signUp_Success() {
    // given
    when(redisComponent.hasKey("sms_verified:01012345678")).thenReturn(true);
    when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123!")).thenReturn("encoded_password");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // when
    ResponseDto<User> result = userService.signUp(validSignUpDto);

    // then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getCode()).isEqualTo(200);
    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getEmail()).isEqualTo("test@example.com");
    assertThat(result.getData().getNickname()).isEqualTo("테스트유저");
    assertThat(result.getData().getRole()).isEqualTo(UserRole.USER);
    assertThat(result.getData().getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(result.getData().getSocialProvider()).isEqualTo(SocialProvider.LOCAL);

    // SMS 인증 상태 삭제 확인
    verify(redisComponent).delete("sms_verified:01012345678");
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("회원가입 실패 - SMS 인증 미완료")
  void signUp_Fail_SmsNotVerified() {
    // given
    when(redisComponent.hasKey("sms_verified:01012345678")).thenReturn(false);

    // when & then
    assertThatThrownBy(() -> userService.signUp(validSignUpDto))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SMS_VERIFICATION_REQUIRED)
        .hasMessage("SMS 인증이 필요합니다.");

    // 사용자 저장되지 않았는지 확인
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("회원가입 실패 - 이메일 중복")
  void signUp_Fail_EmailAlreadyExists() {
    // given
    when(redisComponent.hasKey("sms_verified:01012345678")).thenReturn(true);
    when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

    // when & then
    assertThatThrownBy(() -> userService.signUp(validSignUpDto))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);

    // 사용자 저장되지 않았는지 확인
    verify(userRepository, never()).save(any(User.class));
  }


  @Test
  @DisplayName("이메일로 사용자 조회 성공")
  void findByEmail_Success() {
    // given
    when(userRepository.findByEmail("test@example.com")).thenReturn(
        java.util.Optional.of(savedUser));

    // when
    User result = userService.findByEmail("test@example.com");

    // then
    assertThat(result).isNotNull();
    assertThat(result.getEmail()).isEqualTo("test@example.com");
    assertThat(result.getNickname()).isEqualTo("테스트유저");
  }

  @Test
  @DisplayName("이메일로 사용자 조회 실패 - 사용자 없음")
  void findByEmail_Fail_UserNotFound() {
    // given
    when(userRepository.findByEmail("notfound@example.com")).thenReturn(java.util.Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.findByEmail("notfound@example.com"))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("ID로 사용자 조회 성공")
  void findById_Success() {
    // given
    when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(savedUser));

    // when
    User result = userService.findById(1L);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getEmail()).isEqualTo("test@example.com");
  }

  @Test
  @DisplayName("ID로 사용자 조회 실패 - 사용자 없음")
  void findById_Fail_UserNotFound() {
    // given
    when(userRepository.findById(999L)).thenReturn(java.util.Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.findById(999L))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("회원가입 - 선택적 필드가 null인 경우")
  void signUp_Success_WithNullOptionalFields() {
    // given
    SignUpDto dtoWithNulls = SignUpDto.builder()
        .email("test@example.com")
        .password("password123!")
        .name("홍길동")
        .nickname("테스트유저")
        .phoneNumber("01012345678")
        .roadAddress(null)
        .detailAddress(null)
        .zipCode(null)
        .birthDate(null)
        .gender(null)
        .profileImg(null)
        .build();

    when(redisComponent.hasKey("sms_verified:01012345678")).thenReturn(true);
    when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123!")).thenReturn("encoded_password");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // when
    ResponseDto<User> result = userService.signUp(dtoWithNulls);

    // then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getCode()).isEqualTo(200);
    assertThat(result.getData()).isNotNull();
    verify(userRepository).save(any(User.class));
  }
}
