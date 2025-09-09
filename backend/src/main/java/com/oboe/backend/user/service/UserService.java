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

    // 2. SMS 인증 상태 확인
    validateSmsAuthentication(dto.getPhoneNumber());

    // 3. 중복 체크
    validateUniqueFields(dto);

    // 4. 사용자 생성 및 저장
    User user = User.builder()
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
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .profileImg(dto.getProfileImg())
        .build();

    userRepository.save(user);

    // 5. SMS 인증 상태 삭제 (일회성 사용) - 정규화된 번호로 삭제
    String authKey = "sms_verified:" + dto.getPhoneNumber();
    redisComponent.delete(authKey);

    log.info("회원가입 성공 - 사용자 ID: {}, 이메일: {}, 닉네임: {}, 가입일: {}", 
        user.getId(), user.getEmail(), user.getNickname(), user.getCreatedAt());
    return ResponseDto.success(user);
  }

  // ✅사용자 조회 (이메일)
  @Transactional(readOnly = true)
  public User findByEmail(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
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

    // 1. 사용자 조회
    User user = userRepository.findByEmail(dto.getEmail())
        .orElseThrow(() -> {
          log.warn("로그인 실패 - 존재하지 않는 이메일: {}", dto.getEmail());
          return new CustomException(ErrorCode.USER_NOT_FOUND, "이메일 또는 비밀번호가 올바르지 않습니다.");
        });

    // 2. 소셜 로그인 사용자 체크
    if (user.getSocialProvider() != SocialProvider.LOCAL) {
      log.warn("로그인 실패 - 소셜 로그인 사용자: {}, 제공자: {}", dto.getEmail(), user.getSocialProvider());
      throw new CustomException(ErrorCode.INVALID_PASSWORD, "소셜 로그인 사용자는 일반 로그인을 사용할 수 없습니다.");
    }

    // 3. 사용자 상태 체크
    if (user.getStatus() != UserStatus.ACTIVE) {
      log.warn("로그인 실패 - 비활성 사용자: {}, 상태: {}", dto.getEmail(), user.getStatus());
      throw new CustomException(ErrorCode.FORBIDDEN, "이용할수 없는 계정입니다.");
    }

    // 5. 비밀번호 검증
    if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
      log.warn("로그인 실패 - 비밀번호 불일치: {}", dto.getEmail());
      throw new CustomException(ErrorCode.INVALID_PASSWORD, "이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    // 6. 마지막 로그인 시간 업데이트
    user.updateLastLoginAt();
    userRepository.save(user);

    // 7. JWT 토큰 생성
    String accessToken = jwtUtil.generateAccessToken(user);
    String refreshToken = jwtUtil.generateRefreshToken(user);
    Long expiresIn = jwtUtil.getAccessTokenExpiration() / 1000; // 초 단위로 변환

    // 8. 응답 DTO 생성
    LoginResponseDto responseDto = LoginResponseDto.from(user, accessToken, refreshToken,
        expiresIn);

    log.info("로그인 성공 - 사용자 ID: {}, 이메일: {}, 닉네임: {}, 마지막 로그인: {}", 
        user.getId(), user.getEmail(), user.getNickname(), user.getLastLoginAt());
    return ResponseDto.success(responseDto);
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

      // 사용자 정보 조회
      String email = jwtUtil.getEmailFromToken(refreshToken);
      User user = userRepository.findByEmail(email)
          .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

      // 사용자 상태 확인
      if (user.getStatus() != UserStatus.ACTIVE) {
        log.warn("토큰 갱신 실패 - 비활성 사용자: {}, 상태: {}", user.getEmail(), user.getStatus());
        throw new CustomException(ErrorCode.FORBIDDEN, "이용할수 없는 계정입니다.");
      }

      // 새 토큰 생성
      String newAccessToken = jwtUtil.generateAccessToken(user);
      String newRefreshToken = jwtUtil.generateRefreshToken(user);
      Long expiresIn = jwtUtil.getAccessTokenExpiration() / 1000;

      TokenResponseDto responseDto = TokenResponseDto.of(newAccessToken, newRefreshToken,
          expiresIn);

      log.info("토큰 갱신 성공 - 사용자: {}", email);
      return ResponseDto.success(responseDto);

    } catch (CustomException e) {
      throw e;
    } catch (Exception e) {
      log.error("토큰 갱신 중 오류 발생", e);
      throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "토큰 갱신 중 오류가 발생했습니다.");
    }
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

    String token = jwtUtil.removeBearerPrefix(accessToken);

    // 토큰 유효성 검증
    if (jwtUtil.isTokenExpired(token)) {
      log.warn("만료된 토큰으로 사용자 정보 조회 시도");
      throw new CustomException(ErrorCode.UNAUTHORIZED, "만료된 토큰입니다.");
    }

    // 토큰에서 이메일 추출 (예외 처리 추가)
    String email;
    try {
      email = jwtUtil.getEmailFromToken(token);
    } catch (Exception e) {
      log.error("토큰에서 이메일 추출 실패", e);
      throw new CustomException(ErrorCode.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
    }

    // 사용자 정보 조회
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("사용자 정보 조회 실패 - 존재하지 않는 이메일: {}", email);
          return new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
        });

    // 사용자 상태 확인
    if (user.getStatus() != UserStatus.ACTIVE) {
      log.warn("현재 사용자 정보 조회 실패 - 비활성 사용자: {}, 상태: {}", user.getEmail(), user.getStatus());
      throw new CustomException(ErrorCode.FORBIDDEN, "이용할수 없는 계정입니다.");
    }

    // DTO 변환
    UserProfileDto userProfile = UserProfileDto.from(user);

    log.info("현재 사용자 정보 조회 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), email);
    return ResponseDto.success(userProfile);

  }

  // ✅사용자 정보 수정
  @Transactional
  public ResponseDto<UserProfileDto> updateUser(String accessToken, UserUpdateDto dto) {
    log.info("사용자 정보 수정 요청");

    String token = jwtUtil.removeBearerPrefix(accessToken);

    // 토큰 유효성 검증
    if (jwtUtil.isTokenExpired(token)) {
      log.warn("만료된 토큰으로 사용자 정보 수정 시도");
      throw new CustomException(ErrorCode.UNAUTHORIZED, "만료된 토큰입니다.");
    }

    // 토큰에서 이메일 추출
    String email = jwtUtil.getEmailFromToken(token);

    // 사용자 정보 조회
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("사용자 정보 수정 실패 - 존재하지 않는 이메일: {}", email);
          return new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
        });

    // 사용자 상태 확인
    if (user.getStatus() != UserStatus.ACTIVE) {
      log.warn("사용자 정보 수정 실패 - 비활성 사용자: {}, 상태: {}", user.getEmail(), user.getStatus());
      throw new CustomException(ErrorCode.FORBIDDEN, "이용할수 없는 계정입니다.");
    }

    // 닉네임 중복 체크 (변경 시)
    if (dto.getNickname() != null && !dto.getNickname().equals(user.getNickname())) {
      if (userRepository.existsByNickname(dto.getNickname())) {
        log.warn("사용자 정보 수정 실패 - 이미 사용 중인 닉네임: {}", dto.getNickname());
        throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS, "이미 사용 중인 닉네임입니다.");
      }
      user.setNickname(dto.getNickname());
    }

    // 주소 정보 업데이트
    if (dto.getRoadAddress() != null || dto.getDetailAddress() != null
        || dto.getZipCode() != null) {
      user.setAddress(
          dto.getRoadAddress() != null ? dto.getRoadAddress() : user.getRoadAddress(),
          dto.getDetailAddress() != null ? dto.getDetailAddress() : user.getDetailAddress(),
          dto.getZipCode() != null ? dto.getZipCode() : user.getZipCode()
      );
    }

    // 프로필 이미지 업데이트
    if (dto.getProfileImg() != null) {
      user.setProfileImg(dto.getProfileImg());
    }

    // 사용자 정보 저장
    userRepository.save(user);

    // DTO 변환
    UserProfileDto userProfile = UserProfileDto.from(user);

    log.info("사용자 정보 수정 성공 - 사용자 ID: {}, 이메일: {}, 수정된 필드: {}", 
        user.getId(), email, dto.toString());
    return ResponseDto.success(userProfile);
  }

  // ✅비밀번호 변경
  @Transactional
  public ResponseDto<String> changePassword(String accessToken, PasswordChangeDto dto) {
    log.info("비밀번호 변경 요청");

    String token = jwtUtil.removeBearerPrefix(accessToken);

    // 토큰 유효성 검증
    if (jwtUtil.isTokenExpired(token)) {
      log.warn("만료된 토큰으로 비밀번호 변경 시도");
      throw new CustomException(ErrorCode.UNAUTHORIZED, "만료된 토큰입니다.");
    }

    // 토큰에서 이메일 추출
    String email = jwtUtil.getEmailFromToken(token);

    // 사용자 정보 조회
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("비밀번호 변경 실패 - 존재하지 않는 이메일: {}", email);
          return new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
        });

    // 사용자 상태 확인
    if (user.getStatus() != UserStatus.ACTIVE) {
      log.warn("비밀번호 변경 실패 - 비활성 사용자: {}, 상태: {}", user.getEmail(), user.getStatus());
      throw new CustomException(ErrorCode.FORBIDDEN, "이용할수 없는 계정입니다.");
    }

    // 소셜 로그인 사용자 체크
    if (user.getSocialProvider() != SocialProvider.LOCAL) {
      log.warn("소셜 로그인 사용자 비밀번호 변경 시도 - 이메일: {}, 제공자: {}", email, user.getSocialProvider());
      throw new CustomException(ErrorCode.FORBIDDEN, "소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
    }

    // 현재 비밀번호 검증
    if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
      log.warn("비밀번호 변경 실패 - 현재 비밀번호 불일치: {}", email);
      throw new CustomException(ErrorCode.INVALID_PASSWORD, "현재 비밀번호가 올바르지 않습니다.");
    }

    // 새 비밀번호로 업데이트
    user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
    userRepository.save(user);

    log.info("비밀번호 변경 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), email);
    return ResponseDto.success("비밀번호가 성공적으로 변경되었습니다.");
  }

  // 프로필 이미지 업로드
  @Transactional
  public ResponseDto<String> uploadProfileImage(String accessToken, String filePath) {
    log.info("프로필 이미지 업로드 요청");

    String token = jwtUtil.removeBearerPrefix(accessToken);

    // 토큰 유효성 검증
    if (jwtUtil.isTokenExpired(token)) {
      log.warn("만료된 토큰으로 프로필 이미지 업로드 시도");
      throw new CustomException(ErrorCode.UNAUTHORIZED, "만료된 토큰입니다.");
    }

    // 토큰에서 이메일 추출
    String email = jwtUtil.getEmailFromToken(token);

    // 사용자 정보 조회
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("프로필 이미지 업로드 실패 - 존재하지 않는 이메일: {}", email);
          return new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
        });

    // 사용자 상태 확인
    if (user.getStatus() != UserStatus.ACTIVE) {
      log.warn("프로필 업로드 실패 - 비활성 사용자: {}, 상태: {}", user.getEmail(), user.getStatus());
      throw new CustomException(ErrorCode.FORBIDDEN, "이용할수 없는 계정입니다.");
    }

    // 기존 프로필 이미지 삭제
    if (user.getProfileImg() != null && !user.getProfileImg().isEmpty()) {
      fileUploadService.deleteFile(user.getProfileImg());
    }

    // 새 프로필 이미지 설정
    user.setProfileImg(filePath);
    userRepository.save(user);

    log.info("프로필 이미지 업로드 성공 - 사용자 ID: {}, 이메일: {}, 파일 경로: {}", user.getId(), email, filePath);
    return ResponseDto.success(filePath);
  }

  // ✅아이디 찾기 - 로그인 전 (이름, 휴대폰번호로 검색)
  @Transactional(readOnly = true)
  public ResponseDto<FindIdResponseDto> findId(FindIdDto dto) {
    log.info("아이디 찾기 요청 - 이름: {}, 휴대폰번호: {}", dto.getName(), dto.getPhoneNumber());

    // 1. 이름과 휴대폰번호로 사용자 검색 (일반 계정만)
    User user = userRepository.findByNameAndPhoneNumberAndSocialProvider(dto.getName(),
            dto.getPhoneNumber())
        .orElseThrow(() -> {
          log.warn("아이디 찾기 실패 - 일치하는 사용자 없음: 이름={}, 휴대폰번호={}", dto.getName(),
              dto.getPhoneNumber());
          return new CustomException(ErrorCode.USER_NOT_FOUND, "입력하신 정보와 일치하는 계정을 찾을 수 없습니다.");
        });

    // 2. 일반 계정인지 확인
    if (user.getSocialProvider() != SocialProvider.LOCAL) {
      log.warn("아이디 찾기 실패 - 소셜 로그인 사용자: {}, 제공자: {}", user.getEmail(), user.getSocialProvider());
      throw new CustomException(ErrorCode.FORBIDDEN, "소셜 로그인 사용자는 비밀번호를 재설정할 수 없습니다.");
    }

    // 3. 사용자 상태 확인
    if (user.getStatus() != UserStatus.ACTIVE) {
      log.warn("아이디찾기 실패 - 비활성 사용자: {}, 상태: {}", user.getEmail(), user.getStatus());
      throw new CustomException(ErrorCode.FORBIDDEN, "이용할수 없는 계정입니다.");
    }


    // 4. 응답 DTO 생성
    FindIdResponseDto responseDto = FindIdResponseDto.from(user.getEmail());

    log.info("아이디 찾기 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());
    return ResponseDto.success(responseDto);
  }


  // ✅비밀번호 재설정 (SMS 인증 완료 후)
  @Transactional
  public ResponseDto<String> resetPassword(String email, String newPassword) {
    log.info("비밀번호 재설정 요청 - 이메일: {}", email);

    // 1. 사용자 조회
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("비밀번호 재설정 실패 - 존재하지 않는 이메일: {}", email);
          return new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
        });

    // 2. 일반 계정인지 확인
    if (user.getSocialProvider() != SocialProvider.LOCAL) {
      log.warn("비밀번호 재설정 실패 - 소셜 로그인 사용자: {}, 제공자: {}", email, user.getSocialProvider());
      throw new CustomException(ErrorCode.FORBIDDEN, "소셜 로그인 사용자는 비밀번호를 재설정할 수 없습니다.");
    }

    // 3. 사용자 상태 확인
    if (user.getStatus() != UserStatus.ACTIVE) {
      log.warn("비밀번호 재설정 실패 - 비활성 사용자: {}, 상태: {}", email, user.getStatus());
      throw new CustomException(ErrorCode.FORBIDDEN, "이용할수 없는 계정입니다.");
    }

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

    String token = jwtUtil.removeBearerPrefix(accessToken);

    // 토큰 유효성 검증
    if (jwtUtil.isTokenExpired(token)) {
      log.warn("만료된 토큰으로 회원탈퇴 시도");
      throw new CustomException(ErrorCode.UNAUTHORIZED, "만료된 토큰입니다.");
    }

    // 토큰에서 이메일 추출 (예외 처리 추가)
    String email;
    try {
      email = jwtUtil.getEmailFromToken(token);
    } catch (Exception e) {
      log.error("토큰에서 이메일 추출 실패", e);
      throw new CustomException(ErrorCode.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
    }

    // 사용자 정보 조회
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("회원탈퇴 실패 - 존재하지 않는 이메일: {}", email);
          return new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
        });

    // 사용자 상태 확인
    if (user.getStatus() != UserStatus.ACTIVE) {
      log.warn("회원탈퇴 실패 - 비활성 사용자: {}, 상태: {}", user.getEmail(), user.getStatus());
      throw new CustomException(ErrorCode.FORBIDDEN, "이용할수 없는 계정입니다.");
    }

    // 소셜 로그인 사용자 체크
    if (user.getSocialProvider() != SocialProvider.LOCAL) {
      log.warn("소셜 로그인 사용자 회원탈퇴 시도 - 이메일: {}, 제공자: {}", email, user.getSocialProvider());
      throw new CustomException(ErrorCode.FORBIDDEN, "소셜 로그인 사용자는 회원탈퇴를 할 수 없습니다.");
    }

    // 비밀번호 검증
    if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
      log.warn("회원탈퇴 실패 - 비밀번호 불일치: {}", email);
      throw new CustomException(ErrorCode.INVALID_PASSWORD, "비밀번호가 올바르지 않습니다.");
    }

    // 1단계: 탈퇴시 유저 상태 변경 처리
    LocalDateTime now = LocalDateTime.now();
    user.setStatus(UserStatus.WITHDRAW);
    user.setDeletedAt(now);

    // 회원탈퇴 후 저장 기한 설정 (탈퇴 시점으로부터 30일 후)
    user.setPiiClearedAt(now.plusDays(30));

    // 이메일 익명화
    String anonymizedEmail = anonymizeEmail(email, now);
    user.setEmail(anonymizedEmail);

    userRepository.save(user);

    log.info("회원탈퇴 성공 - 사용자 ID: {}, 원본 이메일: {}, 익명화된 이메일: {}, 탈퇴사유: {}",
        user.getId(), email, anonymizedEmail, dto.getReason());

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

  // ✅SMS 인증 상태 확인
  private void validateSmsAuthentication(String phoneNumber) {
    String authKey = "sms_verified:" + phoneNumber;
    if (!redisComponent.hasKey(authKey)) {
      throw new CustomException(ErrorCode.SMS_VERIFICATION_REQUIRED, "SMS 인증이 필요합니다.");
    }
  }

  // ✅중복 필드 검증 (정규화된 휴대폰번호로 중복 체크)
  private void validateUniqueFields(SignUpDto dto) {
    if (userRepository.existsByEmail(dto.getEmail())) {
      throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS, "이미 가입된 이메일입니다.");
    }

    // 휴대폰번호로 중복 체크
    if (userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
      log.warn("이미 가입된 휴대폰번호로 회원가입 시도 - 휴대폰번호: {}", dto.getPhoneNumber());
      throw new CustomException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS, "이미 가입된 휴대폰번호입니다.");
    }
  }

  // ✅이메일 익명화 메서드
  private String anonymizeEmail(String originalEmail, LocalDateTime deletedAt) {
    String timestamp = deletedAt.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String hash = DigestUtils.md5DigestAsHex(originalEmail.getBytes()).substring(0, 8);
    return "deleted_" + timestamp + "_" + hash + "@deleted.local";
  }

}
