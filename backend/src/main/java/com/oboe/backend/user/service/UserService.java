package com.oboe.backend.user.service;

import com.oboe.backend.common.component.RedisComponent;
import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.common.exception.CustomException;
import com.oboe.backend.common.exception.ErrorCode;
import com.oboe.backend.common.service.FileUploadService;
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
import com.oboe.backend.user.dto.WithdrawDto;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import com.oboe.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final RedisComponent redisComponent;
  private final JwtUtil jwtUtil;
  private final FileUploadService fileUploadService;
  private final com.oboe.backend.message.service.MessageService messageService;

  // 회원가입 (SMS 인증 완료 후)
  @Transactional
  public ResponseDto<User> signUp(SignUpDto dto) {
    // 1. 검증
    validateSmsAuthentication(dto.getPhoneNumber());
    validateUniqueFields(dto);

    // 2. 사용자 생성 및 저장
    User user = createUser(dto);
    User savedUser = userRepository.save(user);

    // 3. 후처리
    cleanupSmsAuthentication(dto.getPhoneNumber());
    
    log.info("회원가입 성공 - 사용자 ID: {}, 이메일: {}, 닉네임: {}", 
        savedUser.getId(), savedUser.getEmail(), savedUser.getNickname());
    
    return ResponseDto.success(savedUser);
  }

  // 회원가입 관련 메서드
  private User createUser(SignUpDto dto) {
    return User.builder()
        .email(dto.getEmail())
        .password(passwordEncoder.encode(dto.getPassword()))
        .name(dto.getName())
        .nickname(dto.getNickname())
        .phoneNumber(dto.getPhoneNumber())
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .roadAddress(dto.getRoadAddress())
        .detailAddress(dto.getDetailAddress())
        .zipCode(dto.getZipCode())
        .birthDate(dto.getBirthDate())
        .gender(dto.getGender())
        .socialProvider(SocialProvider.LOCAL)
        .profileImg(dto.getProfileImg())
        .build();
  }

  // SMS 인증 상태 확인
  private void validateSmsAuthentication(String phoneNumber) {
    String authKey = "sms_verified:" + phoneNumber;
    if (!redisComponent.hasKey(authKey)) {
      throw new CustomException(ErrorCode.SMS_VERIFICATION_REQUIRED, "SMS 인증이 필요합니다.");
    }
  }

  // 중복 필드 검증
  private void validateUniqueFields(SignUpDto dto) {
    if (userRepository.existsByEmail(dto.getEmail())) {
      throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS, "이미 가입된 이메일입니다.");
    }

    if (userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
      log.warn("이미 가입된 휴대폰번호로 회원가입 시도 - 휴대폰번호: {}", dto.getPhoneNumber());
      throw new CustomException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS, "이미 가입된 휴대폰번호입니다.");
    }
  }

  private void cleanupSmsAuthentication(String phoneNumber) {
    String authKey = "sms_verified:" + phoneNumber;
    redisComponent.delete(authKey);
  }


  // ✅사용자 조회 (이메일)
  @Transactional(readOnly = true)
  public User findByEmail(String email) {
    return findActiveUserByEmail(email);
  }

  // ✅사용자 조회 (ID)
  @Transactional(readOnly = true)
  public User findById(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
  }

  // ✅일반 로그인
  @Transactional
  public ResponseDto<LoginResponseDto> login(LoginDto dto) {
    log.info("로그인 시도 - 이메일: {}", dto.getEmail());

    // 1. 사용자 조회 및 기본 검증
    User user = findActiveUserByEmail(dto.getEmail());

    // 2. 로그인 검증
    validateLocalUser(user, "로그인");
    validatePassword(dto.getPassword(), user.getPassword(), "로그인");

    // 3. 로그인 처리
    user.updateLastLoginAt();
    userRepository.save(user);

    // 4. 토큰 생성 및 응답
    LoginResponseDto responseDto = generateLoginResponse(user);
    
    log.info("로그인 성공 - 사용자 ID: {}, 이메일: {}, 닉네임: {}", 
        user.getId(), user.getEmail(), user.getNickname());
    
    return ResponseDto.success(responseDto);
  }

  private LoginResponseDto generateLoginResponse(User user) {
    String accessToken = jwtUtil.generateAccessToken(user);
    String refreshToken = jwtUtil.generateRefreshToken(user);
    Long expiresIn = jwtUtil.getAccessTokenExpiration() / 1000;

    return LoginResponseDto.from(user, accessToken, refreshToken, expiresIn);
  }

  // ✅토큰 갱신
  @Transactional
  public ResponseDto<TokenResponseDto> refreshToken(TokenRefreshDto dto) {
    log.info("토큰 갱신 요청");

    try {
      String refreshToken = jwtUtil.removeBearerPrefix(dto.getRefreshToken());

      // Refresh Token 유효성 검증
      if (!jwtUtil.isRefreshToken(refreshToken)) {
        log.warn("토큰 갱신 실패 - 유효하지 않은 Refresh Token");
        throw new CustomException(ErrorCode.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다.");
      }

      if (jwtUtil.isTokenExpired(refreshToken)) {
        log.warn("토큰 갱신 실패 - 만료된 Refresh Token");
        throw new CustomException(ErrorCode.UNAUTHORIZED, "만료된 Refresh Token입니다.");
      }

      // 사용자 정보 조회 및 검증
      String email = jwtUtil.getEmailFromToken(refreshToken);
      User user = findActiveUserByEmail(email);

      // 새 토큰 생성
      TokenResponseDto responseDto = generateTokenResponse(user);

      log.info("토큰 갱신 성공 - 사용자: {}", email);
      return ResponseDto.success(responseDto);

    } catch (CustomException e) {
      throw e;
    } catch (Exception e) {
      log.error("토큰 갱신 중 오류 발생", e);
      throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "토큰 갱신 중 오류가 발생했습니다.");
    }
  }

  // 토큰 응답 생성 (공통 메서드)
  private TokenResponseDto generateTokenResponse(User user) {
    String newAccessToken = jwtUtil.generateAccessToken(user);
    String newRefreshToken = jwtUtil.generateRefreshToken(user);
    Long expiresIn = jwtUtil.getAccessTokenExpiration() / 1000;

    return TokenResponseDto.of(newAccessToken, newRefreshToken, expiresIn);
  }

  // ✅로그아웃 (토큰 무효화)
  @Transactional
  public ResponseDto<String> logout(String accessToken) {
    log.info("로그아웃 요청");

    try {
      String token = jwtUtil.removeBearerPrefix(accessToken);
      String email = jwtUtil.getEmailFromToken(token);

      log.info("로그아웃 성공 - 사용자: {}", email);
      return ResponseDto.success("로그아웃되었습니다.");
    } catch (Exception e) {
      log.error("로그아웃 중 오류 발생", e);
      throw new CustomException(ErrorCode.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
    }
  }

  // ✅현재 사용자 정보 조회
  @Transactional(readOnly = true)
  public ResponseDto<UserProfileDto> getCurrentUser(String accessToken) {
    log.info("현재 사용자 정보 조회 요청");

    User user = validateTokenAndGetUser(accessToken);
    UserProfileDto userProfile = UserProfileDto.from(user);

    log.info("현재 사용자 정보 조회 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());
    return ResponseDto.success(userProfile);
  }

  // ✅사용자 정보 수정
  @Transactional
  public ResponseDto<UserProfileDto> updateUser(String accessToken, UserUpdateDto dto) {
    log.info("사용자 정보 수정 요청");

    User user = validateTokenAndGetUser(accessToken);
    updateUserFields(user, dto);
    User savedUser = userRepository.save(user);
    UserProfileDto userProfile = UserProfileDto.from(savedUser);

    log.info("사용자 정보 수정 성공 - 사용자 ID: {}, 이메일: {}, 수정된 필드: {}", 
        savedUser.getId(), savedUser.getEmail(), dto.toString());
    return ResponseDto.success(userProfile);
  }

  // 사용자 필드 업데이트
  private void updateUserFields(User user, UserUpdateDto dto) {
    updateNicknameIfChanged(user, dto.getNickname());
    updateAddressIfProvided(user, dto);
    updateProfileImageIfProvided(user, dto.getProfileImg());
  }

  // 닉네임 변경 처리
  private void updateNicknameIfChanged(User user, String newNickname) {
    if (newNickname != null && !newNickname.equals(user.getNickname())) {
      if (userRepository.existsByNickname(newNickname)) {
        log.warn("사용자 정보 수정 실패 - 이미 사용 중인 닉네임: {}", newNickname);
        throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS, "이미 사용 중인 닉네임입니다.");
      }
      user.setNickname(newNickname);
    }
  }

  // 주소 정보 업데이트
  private void updateAddressIfProvided(User user, UserUpdateDto dto) {
    if (dto.getRoadAddress() != null || dto.getDetailAddress() != null || dto.getZipCode() != null) {
      user.setAddress(
          dto.getRoadAddress() != null ? dto.getRoadAddress() : user.getRoadAddress(),
          dto.getDetailAddress() != null ? dto.getDetailAddress() : user.getDetailAddress(),
          dto.getZipCode() != null ? dto.getZipCode() : user.getZipCode()
      );
    }
  }

  // 프로필 이미지 업데이트
  private void updateProfileImageIfProvided(User user, String profileImg) {
    if (profileImg != null) {
      user.setProfileImg(profileImg);
    }
  }

  // ✅프로필 이미지 업로드
  @Transactional
  public ResponseDto<String> uploadProfileImage(String accessToken, String filePath) {
    log.info("프로필 이미지 업로드 요청");

    User user = validateTokenAndGetUser(accessToken);
    replaceProfileImage(user, filePath);

    log.info("프로필 이미지 업로드 성공 - 사용자 ID: {}, 이메일: {}, 파일 경로: {}",
        user.getId(), user.getEmail(), filePath);
    return ResponseDto.success(filePath);
  }

  // 프로필 이미지 관련 메서드
  private void replaceProfileImage(User user, String newFilePath) {
    deleteExistingProfileImage(user);
    user.setProfileImg(newFilePath);
    userRepository.save(user);
  }

  private void deleteExistingProfileImage(User user) {
    if (user.getProfileImg() != null && !user.getProfileImg().isEmpty()) {
      fileUploadService.deleteFile(user.getProfileImg());
    }
  }

  // ✅비밀번호 변경
  @Transactional
  public ResponseDto<String> changePassword(String accessToken, PasswordChangeDto dto) {
    log.info("비밀번호 변경 요청");

    User user = validateTokenAndGetUser(accessToken);
    validateLocalUser(user, "비밀번호 변경");
    validatePassword(dto.getCurrentPassword(), user.getPassword(), "비밀번호 변경");
    updatePassword(user, dto.getNewPassword());

    log.info("비밀번호 변경 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());
    return ResponseDto.success("비밀번호가 성공적으로 변경되었습니다.");
  }

  // 비밀번호 변경 관련 메서드
  private void updatePassword(User user, String newPassword) {
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
  }

  // ✅아이디 찾기 - 로그인 전 (이름, 휴대폰번호로 검색)
  @Transactional(readOnly = true)
  public ResponseDto<FindIdResponseDto> findId(FindIdDto dto) {
    log.info("아이디 찾기 요청 - 이름: {}, 휴대폰번호: {}", dto.getName(), dto.getPhoneNumber());

    // 1. 이름과 휴대폰번호로 사용자 검색
    User user = userRepository.findByNameAndPhoneNumberAndSocialProvider(dto.getName(),
            dto.getPhoneNumber())
        .orElseThrow(() -> {
          log.warn("아이디 찾기 실패 - 일치하는 사용자 없음: 이름={}, 휴대폰번호={}", dto.getName(),
              dto.getPhoneNumber());
          return new CustomException(ErrorCode.USER_NOT_FOUND, "입력하신 정보와 일치하는 계정을 찾을 수 없습니다.");
        });

    // 2. 사용자 검증
    validateLocalUser(user, "아이디 찾기");
    validateUserStatus(user);


    // 4. 응답 DTO 생성
    FindIdResponseDto responseDto = FindIdResponseDto.from(user.getEmail());

    log.info("아이디 찾기 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());
    return ResponseDto.success(responseDto);
  }


  // ✅비밀번호 재설정 (SMS 인증 완료 후)
  @Transactional
  public ResponseDto<String> resetPassword(String email, String newPassword) {
    log.info("비밀번호 재설정 요청 - 이메일: {}", email);

    // 1. 사용자 조회 및 검증
    User user = findActiveUserByEmail(email);
    validateLocalUser(user, "비밀번호 재설정");

    // 4. 새 비밀번호로 업데이트
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    log.info("비밀번호 재설정 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), email);
    return ResponseDto.success("비밀번호가 성공적으로 재설정되었습니다.");
  }

  // ✅회원탈퇴 (1단계: 탈퇴 처리 - 최소 개인정보만 유지)
  @Transactional
  public ResponseDto<String> withdrawUser(String accessToken, WithdrawDto dto) {
    log.info("회원탈퇴 요청");

    User user = validateWithdrawRequest(accessToken, dto);
    String originalEmail = user.getEmail();
    processWithdrawal(user, originalEmail);
    
    log.info("회원탈퇴 성공 - 사용자 ID: {}, 원본 이메일: {}, 익명화된 이메일: {}, 탈퇴사유: {}",
        user.getId(), originalEmail, user.getEmail(), dto.getReason());

    return ResponseDto.success("회원탈퇴가 완료되었습니다. 30일 후 개인정보가 완전히 삭제됩니다.");
  }

  // ✅완전 삭제 (30일 후 개인정보 완전 삭제)
  @Transactional
  public void permanentlyDeleteUsers() {
    log.info("완전 삭제 작업 시작");

    LocalDateTime now = LocalDateTime.now();

    // 정리 예정일이 현재 시간보다 이전인 사용자들 조회 (30일이 지난 사용자들)
    var usersToDelete = userRepository.findByStatusAndPiiClearedAtBeforeAndPiiClearedAtIsNotNull(
        UserStatus.WITHDRAW, now);

    int deletedCount = 0;
    for (User user : usersToDelete) {
      try {
        // 실제 DB에서 완전히 삭제
        userRepository.delete(user);
        deletedCount++;

        log.info("완전 삭제 완료 - 사용자 ID: {}", user.getId());
      } catch (Exception e) {
        log.error("완전 삭제 중 오류 발생 - 사용자 ID: {}", user.getId(), e);
      }
    }

    log.info("완전 삭제 작업 완료 - 처리된 사용자 수: {}", deletedCount);
  }

  // 회원탈퇴 관련 메서드

  private User validateWithdrawRequest(String accessToken, WithdrawDto dto) {
    User user = validateTokenAndGetUser(accessToken);
    validateLocalUser(user, "회원탈퇴");
    validatePassword(dto.getPassword(), user.getPassword(), "회원탈퇴");
    return user;
  }

  private void processWithdrawal(User user, String originalEmail) {
    LocalDateTime now = LocalDateTime.now();
    
    user.setStatus(UserStatus.WITHDRAW);
    user.setDeletedAt(now);
    user.setPiiClearedAt(now.plusDays(30)); // 30일 후 완전 삭제
    
    String anonymizedEmail = anonymizeEmail(originalEmail, now);
    user.setEmail(anonymizedEmail);
    
    userRepository.save(user);
  }

  // 이메일 익명화
  private String anonymizeEmail(String originalEmail, LocalDateTime deletedAt) {
    String timestamp = deletedAt.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String hash = DigestUtils.md5DigestAsHex(originalEmail.getBytes()).substring(0, 8);
    return "deleted_" + timestamp + "_" + hash + "@deleted.local";
  }


  // ==============================
  // 공통 검증 메서드들 (일관성 있는 처리)
  // ==============================

  // 토큰 검증 및 사용자 조회
  private User validateTokenAndGetUser(String accessToken) {
    String token = jwtUtil.removeBearerPrefix(accessToken);
    
    if (jwtUtil.isTokenExpired(token)) {
      log.warn("만료된 토큰 사용 시도");
      throw new CustomException(ErrorCode.UNAUTHORIZED, "만료된 토큰입니다.");
    }

    String email;
    try {
      email = jwtUtil.getEmailFromToken(token);
    } catch (Exception e) {
      log.error("토큰에서 이메일 추출 실패", e);
      throw new CustomException(ErrorCode.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
    }

    User user = findActiveUserByEmail(email);
    return user;
  }

  // 활성 사용자 조회 (공통 메서드)
  private User findActiveUserByEmail(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("사용자 조회 실패 - 존재하지 않는 이메일: {}", email);
          return new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
        });

    validateUserStatus(user);
    return user;
  }

  // 사용자 상태 검증 (공통 메서드)
  private void validateUserStatus(User user) {
    if (user.getStatus() != UserStatus.ACTIVE) {
      log.warn("사용자 상태 검증 실패 - 비활성 사용자: {}, 상태: {}", user.getEmail(), user.getStatus());
      throw new CustomException(ErrorCode.FORBIDDEN, "이용할수 없는 계정입니다.");
    }
  }

  // 로컬 사용자 검증 (공통 메서드)  
  private void validateLocalUser(User user, String context) {
    if (user.getSocialProvider() != SocialProvider.LOCAL) {
      log.warn("{} 실패 - 소셜 로그인 사용자: {}, 제공자: {}", context, user.getEmail(), user.getSocialProvider());
      throw new CustomException(ErrorCode.FORBIDDEN, "소셜 로그인 사용자는 해당 기능을 사용할 수 없습니다.");
    }
  }

  // 비밀번호 검증 (공통 메서드)
  private void validatePassword(String inputPassword, String userPassword, String context) {
    if (!passwordEncoder.matches(inputPassword, userPassword)) {
      log.warn("{} 실패 - 비밀번호 불일치", context);
      throw new CustomException(ErrorCode.INVALID_PASSWORD, "비밀번호가 올바르지 않습니다.");
    }
  }

}
