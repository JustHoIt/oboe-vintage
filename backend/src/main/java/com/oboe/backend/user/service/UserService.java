package com.oboe.backend.user.service;

import com.oboe.backend.common.component.RedisComponent;
import com.oboe.backend.common.exception.CustomException;
import com.oboe.backend.common.exception.ErrorCode;
import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.user.dto.SignUpDto;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import com.oboe.backend.user.entity.SocialProvider;
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

  // 회원가입 (SMS 인증 완료 후)
  public ResponseDto<User> signUp(SignUpDto dto) {
    // 1. SMS 인증 상태 확인
    validateSmsAuthentication(dto.getPhoneNumber());
    
    // 2. 중복 체크
    validateUniqueFields(dto);
    
    // 3. 사용자 생성 및 저장
    User user = User.builder()
        .email(dto.getEmail())
        .password(passwordEncoder.encode(dto.getPassword()))
        .name(dto.getName())
        .nickname(dto.getNickname())
        .phoneNumber(dto.getPhoneNumber())
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .address(dto.getAddress())
        .birthDate(dto.getBirthDate())
        .gender(dto.getGender())
        .isBanned(false)
        .socialProvider(SocialProvider.LOCAL)
        .profileImg(dto.getProfileImg())
        .build();
    
    userRepository.save(user);
    
    // 4. SMS 인증 상태 삭제 (일회성 사용)
    String authKey = "sms_verified:" + dto.getPhoneNumber();
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

  // 중복 필드 검증 (휴대폰 번호는 MessageService에서 이미 검증됨)
  private void validateUniqueFields(SignUpDto dto) {
    if (userRepository.existsByEmail(dto.getEmail())) {
      throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
    }
    
    if (userRepository.existsByNickname(dto.getNickname())) {
      throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
    }
    
    // 휴대폰 번호 중복 체크는 MessageService에서 SMS 발송 전에 이미 처리됨
    // SMS 인증이 완료된 상태이므로 중복 체크 불필요
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


}
