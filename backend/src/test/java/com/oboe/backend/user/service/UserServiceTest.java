package com.oboe.backend.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.oboe.backend.common.component.RedisComponent;
import com.oboe.backend.common.exception.CustomException;
import com.oboe.backend.common.exception.ErrorCode;
import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.common.util.JwtUtil;
import com.oboe.backend.user.dto.FindIdDto;
import com.oboe.backend.user.dto.FindIdResponseDto;
import com.oboe.backend.user.dto.LoginDto;
import com.oboe.backend.user.dto.LoginResponseDto;
import com.oboe.backend.user.dto.PasswordChangeDto;
import com.oboe.backend.user.dto.SignUpDto;
import com.oboe.backend.user.dto.TokenRefreshDto;
import com.oboe.backend.user.dto.TokenResponseDto;
import com.oboe.backend.user.dto.UserProfileDto;
import com.oboe.backend.user.dto.UserUpdateDto;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import com.oboe.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
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

  @Mock
  private JwtUtil jwtUtil;

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

    verify(userRepository, never()).save(any(User.class));
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

    verify(userRepository, never()).save(any(User.class));
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
  @DisplayName("아이디 찾기 성공")
  void findId_Success() {
    // given
    FindIdDto dto = FindIdDto.builder()
        .name("홍길동")
        .phoneNumber("01012345678")
        .build();

    when(userRepository.findByNameAndPhoneNumberAndSocialProvider("홍길동", "01012345678"))
        .thenReturn(Optional.of(savedUser));

    // when
    ResponseDto<FindIdResponseDto> result = userService.findId(dto);

    // then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getData().getEmail()).isEqualTo("test@example.com");

    verify(userRepository, never()).save(any(User.class));
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

  // ========== 로그인 테스트 ==========

  @Test
  @DisplayName("정상 로그인 성공")
  void loginSuccess() {
    // given
    User loginUser = User.builder()
        .id(1L)
        .email("test@example.com")
        .password("encodedPassword")
        .name("테스트사용자")
        .nickname("testuser")
        .phoneNumber("01012345678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .isBanned(false)
        .lastLoginAt(LocalDateTime.now().minusDays(1))
        .build();

    LoginDto loginDto = LoginDto.builder()
        .email("test@example.com")
        .password("password123!")
        .build();

    String accessToken = "access.token.here";
    String refreshToken = "refresh.token.here";

    when(userRepository.findByEmail("test@example.com"))
        .thenReturn(Optional.of(loginUser));
    when(passwordEncoder.matches("password123!", "encodedPassword"))
        .thenReturn(true);
    when(jwtUtil.generateAccessToken(loginUser))
        .thenReturn(accessToken);
    when(jwtUtil.generateRefreshToken(loginUser))
        .thenReturn(refreshToken);
    when(jwtUtil.getAccessTokenExpiration())
        .thenReturn(86400000L); // 24시간 (밀리초)

    // when
    ResponseDto<LoginResponseDto> result = userService.login(loginDto);

    // then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getEmail()).isEqualTo("test@example.com");
    assertThat(result.getData().getName()).isEqualTo("테스트사용자");
    assertThat(result.getData().getNickname()).isEqualTo("testuser");
    assertThat(result.getData().getRole()).isEqualTo(UserRole.USER);
    assertThat(result.getData().getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(result.getData().getAccessToken()).isEqualTo(accessToken);
    assertThat(result.getData().getRefreshToken()).isEqualTo(refreshToken);

    verify(userRepository).save(any(User.class));
    verify(jwtUtil).generateAccessToken(loginUser);
    verify(jwtUtil).generateRefreshToken(loginUser);
  }

  @Test
  @DisplayName("존재하지 않는 이메일로 로그인 실패")
  void loginFailWithNonExistentEmail() {
    // given
    LoginDto loginDto = LoginDto.builder()
        .email("notfound@example.com")
        .password("password123!")
        .build();

    when(userRepository.findByEmail("notfound@example.com"))
        .thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.login(loginDto))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("잘못된 비밀번호로 로그인 실패")
  void loginFailWithWrongPassword() {
    // given
    User loginUser = User.builder()
        .id(1L)
        .email("test@example.com")
        .password("encodedPassword")
        .name("테스트사용자")
        .nickname("testuser")
        .phoneNumber("01012345678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .isBanned(false)
        .build();

    LoginDto wrongPasswordDto = LoginDto.builder()
        .email("test@example.com")
        .password("wrongPassword")
        .build();

    when(userRepository.findByEmail("test@example.com"))
        .thenReturn(Optional.of(loginUser));
    when(passwordEncoder.matches("wrongPassword", "encodedPassword"))
        .thenReturn(false);

    // when & then
    assertThatThrownBy(() -> userService.login(wrongPasswordDto))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
  }

  @Test
  @DisplayName("소셜 로그인 사용자의 일반 로그인 시도 실패")
  void loginFailWithSocialUser() {
    // given
    User socialUser = User.builder()
        .id(2L)
        .email("social@example.com")
        .password("encodedPassword")
        .name("소셜사용자")
        .nickname("socialuser")
        .phoneNumber("01087654321")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.KAKAO)
        .isBanned(false)
        .build();

    LoginDto socialLoginDto = LoginDto.builder()
        .email("social@example.com")
        .password("password123!")
        .build();

    when(userRepository.findByEmail("social@example.com"))
        .thenReturn(Optional.of(socialUser));

    // when & then
    assertThatThrownBy(() -> userService.login(socialLoginDto))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
  }

  @Test
  @DisplayName("비활성화된 사용자 로그인 실패")
  void loginFailWithInactiveUser() {
    // given
    User inactiveUser = User.builder()
        .id(3L)
        .email("inactive@example.com")
        .password("encodedPassword")
        .name("비활성사용자")
        .nickname("inactiveuser")
        .phoneNumber("01011111111")
        .role(UserRole.USER)
        .status(UserStatus.SUSPENDED)
        .socialProvider(SocialProvider.LOCAL)
        .isBanned(false)
        .build();

    LoginDto inactiveLoginDto = LoginDto.builder()
        .email("inactive@example.com")
        .password("password123!")
        .build();

    when(userRepository.findByEmail("inactive@example.com"))
        .thenReturn(Optional.of(inactiveUser));

    // when & then
    assertThatThrownBy(() -> userService.login(inactiveLoginDto))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("차단된 사용자 로그인 실패")
  void loginFailWithBannedUser() {
    // given
    User bannedUser = User.builder()
        .id(4L)
        .email("banned@example.com")
        .password("encodedPassword")
        .name("차단사용자")
        .nickname("banneduser")
        .phoneNumber("01022222222")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .isBanned(true)
        .build();

    LoginDto bannedLoginDto = LoginDto.builder()
        .email("banned@example.com")
        .password("password123!")
        .build();

    when(userRepository.findByEmail("banned@example.com"))
        .thenReturn(Optional.of(bannedUser));

    // when & then
    assertThatThrownBy(() -> userService.login(bannedLoginDto))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("로그인 시 마지막 로그인 시간 업데이트")
  void updateLastLoginAtOnLogin() {
    // given
    User loginUser = User.builder()
        .id(1L)
        .email("test@example.com")
        .password("encodedPassword")
        .name("테스트사용자")
        .nickname("testuser")
        .phoneNumber("01012345678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .isBanned(false)
        .lastLoginAt(LocalDateTime.now().minusDays(1))
        .build();

    LoginDto loginDto = LoginDto.builder()
        .email("test@example.com")
        .password("password123!")
        .build();

    String accessToken = "access.token.here";
    String refreshToken = "refresh.token.here";
    LocalDateTime beforeLogin = loginUser.getLastLoginAt();
    
    when(userRepository.findByEmail("test@example.com"))
        .thenReturn(Optional.of(loginUser));
    when(passwordEncoder.matches("password123!", "encodedPassword"))
        .thenReturn(true);
    when(jwtUtil.generateAccessToken(loginUser))
        .thenReturn(accessToken);
    when(jwtUtil.generateRefreshToken(loginUser))
        .thenReturn(refreshToken);
    when(jwtUtil.getAccessTokenExpiration())
        .thenReturn(86400000L);

    // when
    userService.login(loginDto);

    // then
    verify(userRepository).save(argThat(user -> 
        user.getLastLoginAt().isAfter(beforeLogin)
    ));
    verify(jwtUtil).generateAccessToken(loginUser);
    verify(jwtUtil).generateRefreshToken(loginUser);
  }

  // ========== JWT 토큰 갱신 테스트 ==========

  @Test
  @DisplayName("토큰 갱신 성공")
  void refreshTokenSuccess() {
    // given
    User user = User.builder()
        .id(1L)
        .email("test@example.com")
        .name("테스트사용자")
        .nickname("testuser")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .isBanned(false)
        .build();

    String oldRefreshToken = "Bearer old.refresh.token";
    String newAccessToken = "new.access.token";
    String newRefreshToken = "new.refresh.token";

    TokenRefreshDto refreshDto = TokenRefreshDto.builder()
        .refreshToken(oldRefreshToken)
        .build();

    when(jwtUtil.removeBearerPrefix(oldRefreshToken)).thenReturn("old.refresh.token");
    when(jwtUtil.isRefreshToken("old.refresh.token")).thenReturn(true);
    when(jwtUtil.isTokenExpired("old.refresh.token")).thenReturn(false);
    when(jwtUtil.getEmailFromToken("old.refresh.token")).thenReturn("test@example.com");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(jwtUtil.generateAccessToken(user)).thenReturn(newAccessToken);
    when(jwtUtil.generateRefreshToken(user)).thenReturn(newRefreshToken);
    when(jwtUtil.getAccessTokenExpiration()).thenReturn(86400000L);

    // when
    ResponseDto<TokenResponseDto> result = userService.refreshToken(refreshDto);

    // then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getAccessToken()).isEqualTo(newAccessToken);
    assertThat(result.getData().getRefreshToken()).isEqualTo(newRefreshToken);
    assertThat(result.getData().getTokenType()).isEqualTo("Bearer");
    assertThat(result.getData().getExpiresIn()).isEqualTo(86400L);

    verify(jwtUtil).generateAccessToken(user);
    verify(jwtUtil).generateRefreshToken(user);
  }

  @Test
  @DisplayName("토큰 갱신 실패 - 유효하지 않은 토큰")
  void refreshTokenFailInvalidToken() {
    // given
    String invalidToken = "Bearer invalid.token";
    TokenRefreshDto refreshDto = TokenRefreshDto.builder()
        .refreshToken(invalidToken)
        .build();

    when(jwtUtil.removeBearerPrefix(invalidToken)).thenReturn("invalid.token");
    when(jwtUtil.isRefreshToken("invalid.token")).thenReturn(false);

    // when & then
    assertThatThrownBy(() -> userService.refreshToken(refreshDto))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
  }

  @Test
  @DisplayName("토큰 갱신 실패 - 사용자 없음")
  void refreshTokenFailUserNotFound() {
    // given
    String validToken = "Bearer valid.token";
    TokenRefreshDto refreshDto = TokenRefreshDto.builder()
        .refreshToken(validToken)
        .build();

    when(jwtUtil.removeBearerPrefix(validToken)).thenReturn("valid.token");
    when(jwtUtil.isRefreshToken("valid.token")).thenReturn(true);
    when(jwtUtil.isTokenExpired("valid.token")).thenReturn(false);
    when(jwtUtil.getEmailFromToken("valid.token")).thenReturn("notfound@example.com");
    when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.refreshToken(refreshDto))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
  }

  // ========== 현재 사용자 정보 조회 테스트 ==========

  @Test
  @DisplayName("현재 사용자 정보 조회 성공")
  void getCurrentUserSuccess() {
    // given
    User user = User.builder()
        .id(1L)
        .email("test@example.com")
        .name("테스트사용자")
        .nickname("testuser")
        .phoneNumber("01012345678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .isBanned(false)
        .roadAddress("서울시 강남구")
        .detailAddress("1층")
        .zipCode("02111")
        .birthDate(LocalDate.of(1990, 1, 1))
        .gender("M")
        .profileImg("https://example.com/profile.jpg")
        .lastLoginAt(LocalDateTime.now().minusDays(1))
        .build();

    String accessToken = "Bearer valid.access.token";

    when(jwtUtil.removeBearerPrefix(accessToken)).thenReturn("valid.access.token");
    when(jwtUtil.isTokenExpired("valid.access.token")).thenReturn(false);
    when(jwtUtil.getEmailFromToken("valid.access.token")).thenReturn("test@example.com");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

    // when
    ResponseDto<UserProfileDto> result = userService.getCurrentUser(accessToken);

    // then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getData()).isNotNull();
    
    UserProfileDto userProfile = result.getData();
    assertThat(userProfile.getId()).isEqualTo(1L);
    assertThat(userProfile.getEmail()).isEqualTo("test@example.com");
    assertThat(userProfile.getName()).isEqualTo("테스트사용자");
    assertThat(userProfile.getNickname()).isEqualTo("testuser");
    assertThat(userProfile.getRole()).isEqualTo(UserRole.USER);
    assertThat(userProfile.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(userProfile.getSocialProvider()).isEqualTo(SocialProvider.LOCAL);

    verify(jwtUtil).removeBearerPrefix(accessToken);
    verify(jwtUtil).isTokenExpired("valid.access.token");
    verify(jwtUtil).getEmailFromToken("valid.access.token");
    verify(userRepository).findByEmail("test@example.com");
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("현재 사용자 정보 조회 실패 - 만료된 토큰")
  void getCurrentUserFailExpiredToken() {
    // given
    String expiredToken = "Bearer expired.token";

    when(jwtUtil.removeBearerPrefix(expiredToken)).thenReturn("expired.token");
    when(jwtUtil.isTokenExpired("expired.token")).thenReturn(true);

    // when & then
    assertThatThrownBy(() -> userService.getCurrentUser(expiredToken))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
  }

  @Test
  @DisplayName("현재 사용자 정보 조회 실패 - 사용자 없음")
  void getCurrentUserFailUserNotFound() {
    // given
    String validToken = "Bearer valid.token";

    when(jwtUtil.removeBearerPrefix(validToken)).thenReturn("valid.token");
    when(jwtUtil.isTokenExpired("valid.token")).thenReturn(false);
    when(jwtUtil.getEmailFromToken("valid.token")).thenReturn("notfound@example.com");
    when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.getCurrentUser(validToken))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("현재 사용자 정보 조회 실패 - 비활성 사용자")
  void getCurrentUserFailInactiveUser() {
    // given
    User inactiveUser = User.builder()
        .id(2L)
        .email("inactive@example.com")
        .name("비활성사용자")
        .nickname("inactiveuser")
        .role(UserRole.USER)
        .status(UserStatus.SUSPENDED)
        .socialProvider(SocialProvider.LOCAL)
        .isBanned(false)
        .build();

    String validToken = "Bearer valid.token";

    when(jwtUtil.removeBearerPrefix(validToken)).thenReturn("valid.token");
    when(jwtUtil.isTokenExpired("valid.token")).thenReturn(false);
    when(jwtUtil.getEmailFromToken("valid.token")).thenReturn("inactive@example.com");
    when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(inactiveUser));

    // when & then
    assertThatThrownBy(() -> userService.getCurrentUser(validToken))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("현재 사용자 정보 조회 실패 - 차단된 사용자")
  void getCurrentUserFailBannedUser() {
    // given
    User bannedUser = User.builder()
        .id(3L)
        .email("banned@example.com")
        .name("차단사용자")
        .nickname("banneduser")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .isBanned(true)
        .build();

    String validToken = "Bearer valid.token";

    when(jwtUtil.removeBearerPrefix(validToken)).thenReturn("valid.token");
    when(jwtUtil.isTokenExpired("valid.token")).thenReturn(false);
    when(jwtUtil.getEmailFromToken("valid.token")).thenReturn("banned@example.com");
    when(userRepository.findByEmail("banned@example.com")).thenReturn(Optional.of(bannedUser));

    // when & then
    assertThatThrownBy(() -> userService.getCurrentUser(validToken))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
  }
}
