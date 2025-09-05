package com.oboe.backend.user.service;

import com.oboe.backend.common.component.RedisComponent;
import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.common.exception.CustomException;
import com.oboe.backend.common.exception.ErrorCode;
import com.oboe.backend.common.util.JwtUtil;
import com.oboe.backend.common.util.PhoneNumberUtil;
import com.oboe.backend.user.dto.LoginDto;
import com.oboe.backend.user.dto.LoginResponseDto;
import com.oboe.backend.user.dto.SignUpDto;
import com.oboe.backend.user.dto.TokenRefreshDto;
import com.oboe.backend.user.dto.TokenResponseDto;
import com.oboe.backend.user.dto.UserProfileDto;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final RedisComponent redisComponent;
  private final JwtUtil jwtUtil;

  // 회원가입 (SMS 인증 완료 후)
  public ResponseDto<User> signUp(SignUpDto dto) {
    // 1. 휴대폰번호 정규화
    String normalizedPhoneNumber = PhoneNumberUtil.normalizePhoneNumber(dto.getPhoneNumber());
    log.info("회원가입 휴대폰번호 정규화: '{}' -> '{}'", dto.getPhoneNumber(), normalizedPhoneNumber);
    
    // 2. SMS 인증 상태 확인 (정규화된 번호로 확인)
    validateSmsAuthentication(normalizedPhoneNumber);

    // 3. 중복 체크 (정규화된 번호로 확인)
    validateUniqueFields(dto, normalizedPhoneNumber);

    // 4. 사용자 생성 및 저장 (정규화된 번호로 저장)
    User user = User.builder()
        .email(dto.getEmail())
        .password(passwordEncoder.encode(dto.getPassword()))
        .name(dto.getName())
        .nickname(dto.getNickname())
        .phoneNumber(normalizedPhoneNumber)
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .roadAddress(dto.getRoadAddress())
        .detailAddress(dto.getDetailAddress())
        .zipCode(dto.getZipCode())
        .birthDate(dto.getBirthDate())
        .gender(dto.getGender())
        .isBanned(false)
        .socialProvider(SocialProvider.LOCAL)
        .profileImg(dto.getProfileImg())
        .build();

    userRepository.save(user);

    // 5. SMS 인증 상태 삭제 (일회성 사용) - 정규화된 번호로 삭제
    String authKey = "sms_verified:" + normalizedPhoneNumber;
    redisComponent.delete(authKey);

    log.info("회원가입 완료 - 이메일: {}, 닉네임: {}", dto.getEmail(), dto.getNickname());
    return ResponseDto.success(user);
  }

  // SMS 인증 상태 확인
  private void validateSmsAuthentication(String phoneNumber) {
    String authKey = "sms_verified:" + phoneNumber;
    if (!redisComponent.hasKey(authKey)) {
      throw new CustomException(ErrorCode.SMS_VERIFICATION_REQUIRED, "SMS 인증이 필요합니다.");
    }
  }

  // 중복 필드 검증 (정규화된 휴대폰번호로 중복 체크)
  private void validateUniqueFields(SignUpDto dto, String normalizedPhoneNumber) {
    if (userRepository.existsByEmail(dto.getEmail())) {
      throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS, "이미 가입된 이메일입니다.");
    }

    // 정규화된 휴대폰번호로 중복 체크
    if (userRepository.existsByPhoneNumber(normalizedPhoneNumber)) {
      log.warn("이미 가입된 휴대폰번호로 회원가입 시도 - 휴대폰번호: {}", normalizedPhoneNumber);
      throw new CustomException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS, "이미 가입된 휴대폰번호입니다.");
    }
  }

  // 사용자 조회 (이메일)
  public User findByEmail(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
  }

  // 사용자 조회 (ID)
  public User findById(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
  }

  // 로그인
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
      throw new CustomException(ErrorCode.FORBIDDEN, "비활성화된 계정입니다.");
    }

    // 4. 밴 상태 체크
    if (user.isBanned()) {
      log.warn("로그인 실패 - 밴된 사용자: {}", dto.getEmail());
      throw new CustomException(ErrorCode.FORBIDDEN, "정지된 계정입니다.");
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
    LoginResponseDto responseDto = LoginResponseDto.from(user, accessToken, refreshToken, expiresIn);

    log.info("로그인 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());
    return ResponseDto.success(responseDto);
  }

  // 토큰 갱신
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
      if (user.getStatus() != UserStatus.ACTIVE || user.isBanned()) {
        log.warn("토큰 갱신 실패 - 비활성 사용자: {}", email);
        throw new CustomException(ErrorCode.FORBIDDEN, "비활성화된 계정입니다.");
      }
      
      // 새 토큰 생성
      String newAccessToken = jwtUtil.generateAccessToken(user);
      String newRefreshToken = jwtUtil.generateRefreshToken(user);
      Long expiresIn = jwtUtil.getAccessTokenExpiration() / 1000;
      
      TokenResponseDto responseDto = TokenResponseDto.of(newAccessToken, newRefreshToken, expiresIn);
      
      log.info("토큰 갱신 성공 - 사용자: {}", email);
      return ResponseDto.success(responseDto);
      
    } catch (CustomException e) {
      throw e;
    } catch (Exception e) {
      log.error("토큰 갱신 중 오류 발생", e);
      throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "토큰 갱신 중 오류가 발생했습니다.");
    }
  }

  // 로그아웃 (토큰 무효화)
  public ResponseDto<String> logout(String accessToken) {
    log.info("로그아웃 요청");
    
    try {
      String token = jwtUtil.removeBearerPrefix(accessToken);
      String email = jwtUtil.getEmailFromToken(token);
      
      // Redis에 블랙리스트 토큰 저장 (선택사항)
      // 실제로는 토큰을 무효화하는 로직을 구현할 수 있습니다
      
      log.info("로그아웃 성공 - 사용자: {}", email);
      return ResponseDto.success("로그아웃되었습니다.");
      
    } catch (Exception e) {
      log.error("로그아웃 중 오류 발생", e);
      return ResponseDto.success("로그아웃되었습니다.");
    }
  }

  // 현재 사용자 정보 조회
  public ResponseDto<UserProfileDto> getCurrentUser(String accessToken) {
    log.info("현재 사용자 정보 조회 요청");
    
    try {
      String token = jwtUtil.removeBearerPrefix(accessToken);
      
      // 토큰 유효성 검증
      if (jwtUtil.isTokenExpired(token)) {
        log.warn("만료된 토큰으로 사용자 정보 조회 시도");
        throw new CustomException(ErrorCode.UNAUTHORIZED, "만료된 토큰입니다.");
      }
      
      // 토큰에서 이메일 추출
      String email = jwtUtil.getEmailFromToken(token);
      
      // 사용자 정보 조회
      User user = userRepository.findByEmail(email)
          .orElseThrow(() -> {
            log.warn("사용자 정보 조회 실패 - 존재하지 않는 이메일: {}", email);
            return new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
          });
      
      // 사용자 상태 확인
      if (user.getStatus() != UserStatus.ACTIVE) {
        log.warn("비활성 사용자 정보 조회 시도 - 이메일: {}, 상태: {}", email, user.getStatus());
        throw new CustomException(ErrorCode.FORBIDDEN, "비활성화된 계정입니다.");
      }
      
      if (user.isBanned()) {
        log.warn("차단된 사용자 정보 조회 시도 - 이메일: {}", email);
        throw new CustomException(ErrorCode.FORBIDDEN, "정지된 계정입니다.");
      }
      
      // DTO 변환
      UserProfileDto userProfile = UserProfileDto.from(user);
      
      log.info("현재 사용자 정보 조회 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), email);
      return ResponseDto.success(userProfile);
      
    } catch (CustomException e) {
      throw e;
    } catch (Exception e) {
      log.error("현재 사용자 정보 조회 중 오류 발생", e);
      throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "사용자 정보 조회 중 오류가 발생했습니다.");
    }
  }

}
