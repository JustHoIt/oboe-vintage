package com.oboe.backend.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.oboe.backend.common.exception.CustomException;
import com.oboe.backend.common.exception.ErrorCode;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import com.oboe.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomOAuth2UserService 테스트")
class CustomOAuth2UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private OAuth2UserRequest userRequest;

  @Mock
  private OAuth2User oAuth2User;

  private CustomOAuth2UserService customOAuth2UserService;

  private Map<String, Object> kakaoAttributes;
  private Map<String, Object> naverAttributes;

  @BeforeEach
  void setUp() {
    customOAuth2UserService = spy(new CustomOAuth2UserService(userRepository));

    // 카카오 OAuth2 attributes 설정
    kakaoAttributes = new HashMap<>();
    kakaoAttributes.put("id", "123456789");

    Map<String, Object> kakaoAccount = new HashMap<>();
    Map<String, Object> profile = new HashMap<>();
    profile.put("nickname", "kakaouser");
    profile.put("profile_image_url", "kakao-profile.jpg");
    kakaoAccount.put("profile", profile);
    kakaoAccount.put("email", "kakao@example.com");
    kakaoAccount.put("gender", "male");
    kakaoAccount.put("birthday", "01-01");
    kakaoAccount.put("birthyear", "1990");
    kakaoAccount.put("phone_number", "010-1234-5678");
    kakaoAttributes.put("kakao_account", kakaoAccount);

    // 네이버 OAuth2 attributes 설정
    naverAttributes = new HashMap<>();
    Map<String, Object> response = new HashMap<>();
    response.put("id", "naver123456");
    response.put("email", "naver@example.com");
    response.put("name", "네이버사용자");
    response.put("nickname", "naveruser");
    response.put("profile_image", "naver-profile.jpg");
    response.put("gender", "M");
    response.put("birthday", "01-01");
    response.put("birthyear", "1990");
    response.put("mobile", "010-9876-5432");
    naverAttributes.put("response", response);
  }

  @Test
  @DisplayName("카카오 OAuth2 사용자 정보 추출 테스트")
  void extractKakaoUserInfo() {
    // given
    CustomOAuth2UserService.KakaoOAuth2UserInfo kakaoUserInfo =
        new CustomOAuth2UserService.KakaoOAuth2UserInfo(kakaoAttributes);

    // when & then
    assertThat(kakaoUserInfo.getId()).isEqualTo("123456789");
    assertThat(kakaoUserInfo.getEmail()).isEqualTo("kakao@example.com");
    assertThat(kakaoUserInfo.getNickname()).isEqualTo("kakaouser");
    assertThat(kakaoUserInfo.getProfileImage()).isEqualTo("kakao-profile.jpg");
    assertThat(kakaoUserInfo.getGender()).isEqualTo("male");
    assertThat(kakaoUserInfo.getBirthday()).isEqualTo("01-01");
    assertThat(kakaoUserInfo.getBirthyear()).isEqualTo("1990");
    assertThat(kakaoUserInfo.getPhoneNumber()).isEqualTo("010-1234-5678");
  }

  @Test
  @DisplayName("네이버 OAuth2 사용자 정보 추출 테스트")
  void extractNaverUserInfo() {
    // given
    CustomOAuth2UserService.NaverOAuth2UserInfo naverUserInfo =
        new CustomOAuth2UserService.NaverOAuth2UserInfo(naverAttributes);

    // when & then
    assertThat(naverUserInfo.getId()).isEqualTo("naver123456");
    assertThat(naverUserInfo.getEmail()).isEqualTo("naver@example.com");
    assertThat(naverUserInfo.getName()).isEqualTo("네이버사용자");
    assertThat(naverUserInfo.getNickname()).isEqualTo("naveruser");
    assertThat(naverUserInfo.getProfileImage()).isEqualTo("naver-profile.jpg");
    assertThat(naverUserInfo.getGender()).isEqualTo("M");
    assertThat(naverUserInfo.getBirthday()).isEqualTo("01-01");
    assertThat(naverUserInfo.getBirthyear()).isEqualTo("1990");
    assertThat(naverUserInfo.getPhoneNumber()).isEqualTo("010-9876-5432");
  }


  @Test
  @DisplayName("성별 변환 테스트")
  void convertGender() {
    // given
    CustomOAuth2UserService service = new CustomOAuth2UserService(userRepository);

    // when & then - Reflection을 사용하여 private 메소드 테스트
    try {
      java.lang.reflect.Method convertGenderMethod = CustomOAuth2UserService.class.getDeclaredMethod(
          "convertGender", String.class);
      convertGenderMethod.setAccessible(true);

      assertThat((String) convertGenderMethod.invoke(service, "male")).isEqualTo("M");
      assertThat((String) convertGenderMethod.invoke(service, "female")).isEqualTo("F");
      assertThat((String) convertGenderMethod.invoke(service, "남성")).isEqualTo("M");
      assertThat((String) convertGenderMethod.invoke(service, "여성")).isEqualTo("F");
      assertThat((String) convertGenderMethod.invoke(service, "unknown")).isEqualTo("U");
      assertThat((String) convertGenderMethod.invoke(service, (String) null)).isEqualTo("U");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  @DisplayName("소셜 제공자 결정 테스트")
  void getSocialProvider() {
    // given
    CustomOAuth2UserService service = new CustomOAuth2UserService(userRepository);

    // when & then
    assertThat(service.getSocialProvider("kakao")).isEqualTo(SocialProvider.KAKAO);
    assertThat(service.getSocialProvider("naver")).isEqualTo(SocialProvider.NAVER);
    assertThat(service.getSocialProvider("unknown")).isEqualTo(SocialProvider.KAKAO); // 기본값
  }
}
