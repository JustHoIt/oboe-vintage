package com.oboe.backend.user.service;

import com.oboe.backend.common.exception.CustomException;
import com.oboe.backend.common.exception.ErrorCode;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import com.oboe.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final UserRepository userRepository;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    try {
      log.info("OAuth2 로그인 시작 - 제공자: {}", userRequest.getClientRegistration().getRegistrationId());
      
      OAuth2User oAuth2User = super.loadUser(userRequest);
      log.info("OAuth2 사용자 정보 로드 성공");

      String registrationId = userRequest.getClientRegistration().getRegistrationId();
      log.info("OAuth2 제공자: {}", registrationId);
      
      OAuth2UserInfo userInfo = getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
      log.info("OAuth2UserInfo 생성 완료: {}", userInfo != null ? "성공" : "실패");

      if (userInfo == null) {
        log.error("지원하지 않는 OAuth2 제공자: {}", registrationId);
        throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 제공자입니다: " + registrationId);
      }

      log.info("사용자 처리 시작 - 이메일: {}", userInfo.getEmail());
      User user = processOAuth2User(userInfo, registrationId);
      log.info("사용자 처리 완료 - 사용자 ID: {}", user.getId());

      return new CustomOAuth2User(user, oAuth2User.getAttributes());
    } catch (Exception e) {
      log.error("OAuth2 로그인 실패", e);
      throw new OAuth2AuthenticationException("OAuth2 로그인 중 오류가 발생했습니다: " + e.getMessage());
    }
  }

  private OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
    if ("kakao".equals(registrationId)) {
      return new KakaoOAuth2UserInfo(attributes);
    } else if ("naver".equals(registrationId)) {
      return new NaverOAuth2UserInfo(attributes);
    }
    return null;
  }

  private User processOAuth2User(OAuth2UserInfo userInfo, String registrationId) {
    Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());

    if (existingUser.isPresent()) {
      User user = existingUser.get();
      // 기존 사용자가 있는 경우
      if (user.getSocialProvider() == SocialProvider.LOCAL) {
        // 로컬 사용자인 경우 에러 발생
        log.warn("OAuth2 로그인 시도 - 이미 가입된 로컬 이메일: {}", userInfo.getEmail());
        throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
      } else {
        // 이미 소셜 사용자인 경우 기존 사용자 반환
        log.info("기존 소셜 사용자 로그인 - 사용자 ID: {}, 소셜 제공자: {}",
            user.getId(), user.getSocialProvider());
        return user;
      }
    } else {
      // 새 사용자 생성
      return createNewOAuth2User(userInfo, registrationId);
    }
  }


  private User createNewOAuth2User(OAuth2UserInfo userInfo, String registrationId) {
    log.info("새 OAuth2 사용자 생성 시작 - 이메일: {}, 제공자: {}", userInfo.getEmail(), registrationId);
    
    // 생년월일 처리 (birthday + birthyear)
    LocalDate birthDate = null;
    if (userInfo.getBirthday() != null && userInfo.getBirthyear() != null) {
      try {
        String[] dateParts = userInfo.getBirthday().split("-");
        if (dateParts.length == 2) {
          int month = Integer.parseInt(dateParts[0]);
          int day = Integer.parseInt(dateParts[1]);
          int year = Integer.parseInt(userInfo.getBirthyear());
          birthDate = LocalDate.of(year, month, day);
          log.debug("생년월일 파싱 성공: {}", birthDate);
        }
      } catch (Exception e) {
        log.warn("생년월일 파싱 실패: birthday={}, birthyear={}",
            userInfo.getBirthday(), userInfo.getBirthyear(), e);
      }
    }

    // 소셜 로그인용 기본 비밀번호 생성 (실제 로그인에는 사용되지 않음)
    String socialPassword = "SOCIAL_LOGIN_" + userInfo.getId() + "_" + System.currentTimeMillis();

    // OAuth2 제공자에 따른 소셜 제공자 결정
    SocialProvider socialProvider = getSocialProvider(registrationId);

    log.info("사용자 빌더 시작 - 이메일: {}, 이름: {}, 닉네임: {}", 
        userInfo.getEmail(), userInfo.getName(), userInfo.getNickname());
    
    // OAuth2 사용자 닉네임 처리 (닉네임이 없으면 이름 사용)
    String nickname = userInfo.getNickname();
    if (nickname == null || nickname.trim().isEmpty()) {
      nickname = userInfo.getName();
    }
    
    // OAuth2 사용자 전화번호 처리 (전화번호가 없으면 기본값 사용)
    String phoneNumber = userInfo.getPhoneNumber();
    if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
      phoneNumber = "SOCIAL_USER_" + userInfo.getId();
    }

    User user = User.builder()
        .email(userInfo.getEmail())
        .name(userInfo.getName())
        .nickname(nickname)
        .password(socialPassword) // 소셜 로그인용 기본 비밀번호 설정
        .phoneNumber(phoneNumber)
        .socialProvider(socialProvider) // 동적으로 결정된 소셜 제공자
        .socialId(userInfo.getId())
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .birthDate(birthDate)
        .gender(convertGender(userInfo.getGender()))
        .profileImg(userInfo.getProfileImage())
        .lastLoginAt(LocalDateTime.now())
        .isBanned(false)
        .build();

    log.info("사용자 빌더 완료 - 사용자 정보: {}", user);
    
    try {
      User savedUser = userRepository.save(user);
      log.info("사용자 저장 성공 - 사용자 ID: {}", savedUser.getId());
      return savedUser;
    } catch (Exception e) {
      log.error("사용자 저장 실패", e);
      throw e;
    }
  }

  public String convertGender(String genderString) {
    if (genderString == null) {
      return "U";
    }

    // OAuth2 제공자에서 받은 gender 값을 M/F/U로 변환
    switch (genderString.toLowerCase()) {
      case "male":
      case "m":
      case "남성":
        return "M";
      case "female":
      case "f":
      case "여성":
        return "F";
      default:
        return "U";
    }
  }

  public SocialProvider getSocialProvider(String registrationId) {
    switch (registrationId.toLowerCase()) {
      case "kakao":
        return SocialProvider.KAKAO;
      case "naver":
        return SocialProvider.NAVER;
      default:
        log.warn("알 수 없는 OAuth2 제공자: {}, 기본값 KAKAO 사용", registrationId);
        return SocialProvider.KAKAO;
    }
  }

  // OAuth2UserInfo 인터페이스
  public interface OAuth2UserInfo {

    String getId();

    String getEmail();

    String getName();

    String getNickname();

    String getProfileImage();

    String getGender();

    String getBirthday();

    String getBirthyear();

    String getPhoneNumber();
  }

  // Kakao OAuth2UserInfo 구현
  public static class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
      this.attributes = attributes;
      log.debug("카카오 OAuth2 attributes: {}", attributes);
    }

    @Override
    public String getId() {
      String id = String.valueOf(attributes.get("id"));
      log.debug("카카오 ID 추출: {}", id);
      return id;
    }

    @Override
    public String getEmail() {
      Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
      if (kakaoAccount != null) {
        String email = (String) kakaoAccount.get("email");
        log.debug("카카오 이메일 추출: {}", email);
        return email;
      }
      log.warn("카카오 계정 정보가 null입니다");
      return null;
    }

    @Override
    public String getName() {
      Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
      if (kakaoAccount != null) {
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        if (profile != null) {
          return (String) profile.get("nickname");
        }
      }
      return null;
    }

    @Override
    public String getNickname() {
      Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
      if (kakaoAccount != null) {
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        if (profile != null) {
          return (String) profile.get("nickname");
        }
      }
      return null;
    }

    @Override
    public String getProfileImage() {
      Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
      if (kakaoAccount != null) {
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        if (profile != null) {
          return (String) profile.get("profile_image_url");
        }
      }
      return null;
    }

    @Override
    public String getGender() {
      Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
      if (kakaoAccount != null) {
        return (String) kakaoAccount.get("gender");
      }
      return null;
    }

    @Override
    public String getBirthday() {
      Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
      if (kakaoAccount != null) {
        return (String) kakaoAccount.get("birthday");
      }
      return null;
    }

    @Override
    public String getBirthyear() {
      Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
      if (kakaoAccount != null) {
        return (String) kakaoAccount.get("birthyear");
      }
      return null;
    }

    @Override
    public String getPhoneNumber() {
      Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
      if (kakaoAccount != null) {
        return (String) kakaoAccount.get("phone_number");
      }
      return null;
    }

  }

  // Naver OAuth2UserInfo 구현
  public static class NaverOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
      this.attributes = attributes;
    }

    @Override
    public String getId() {
      Map<String, Object> response = (Map<String, Object>) attributes.get("response");
      if (response != null) {
        return (String) response.get("id");
      }
      return null;
    }

    @Override
    public String getEmail() {
      Map<String, Object> response = (Map<String, Object>) attributes.get("response");
      if (response != null) {
        return (String) response.get("email");
      }
      return null;
    }

    @Override
    public String getName() {
      Map<String, Object> response = (Map<String, Object>) attributes.get("response");
      if (response != null) {
        return (String) response.get("name");
      }
      return null;
    }

    @Override
    public String getNickname() {
      Map<String, Object> response = (Map<String, Object>) attributes.get("response");
      if (response != null) {
        return (String) response.get("nickname");
      }
      return null;
    }

    @Override
    public String getProfileImage() {
      Map<String, Object> response = (Map<String, Object>) attributes.get("response");
      if (response != null) {
        return (String) response.get("profile_image");
      }
      return null;
    }

    @Override
    public String getGender() {
      Map<String, Object> response = (Map<String, Object>) attributes.get("response");
      if (response != null) {
        return (String) response.get("gender");
      }
      return null;
    }

    @Override
    public String getBirthday() {
      Map<String, Object> response = (Map<String, Object>) attributes.get("response");
      if (response != null) {
        return (String) response.get("birthday");
      }
      return null;
    }

    @Override
    public String getBirthyear() {
      Map<String, Object> response = (Map<String, Object>) attributes.get("response");
      if (response != null) {
        return (String) response.get("birthyear");
      }
      return null;
    }

    @Override
    public String getPhoneNumber() {
      Map<String, Object> response = (Map<String, Object>) attributes.get("response");
      if (response != null) {
        return (String) response.get("mobile");
      }
      return null;
    }
  }

  // CustomOAuth2User 클래스
  public static class CustomOAuth2User implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(User user, Map<String, Object> attributes) {
      this.user = user;
      this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
      return attributes;
    }

    @Override
    public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
      return java.util.Collections.singletonList(() -> "ROLE_" + user.getRole().name());
    }

    @Override
    public String getName() {
      return user.getEmail();
    }

    public User getUser() {
      return user;
    }
  }
}
