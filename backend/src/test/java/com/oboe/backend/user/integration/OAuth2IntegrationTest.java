package com.oboe.backend.user.integration;

import static org.assertj.core.api.Assertions.*;

import com.oboe.backend.config.JpaConfig;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import com.oboe.backend.user.repository.UserRepository;
import com.oboe.backend.user.service.CustomOAuth2UserService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@Import({JpaConfig.class, CustomOAuth2UserService.class})
@DisplayName("OAuth2 통합 테스트")
class OAuth2IntegrationTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CustomOAuth2UserService customOAuth2UserService;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("카카오 OAuth2 사용자 생성 및 조회 통합 테스트")
  void createAndRetrieveKakaoUser() {
    // given
    User kakaoUser = User.builder()
        .email("kakao@example.com")
        .name("카카오사용자")
        .nickname("kakaouser")
        .password("SOCIAL_LOGIN_123456789_1234567890")
        .phoneNumber("010-1234-5678")
        .socialProvider(SocialProvider.KAKAO)
        .socialId("123456789")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .gender("M")
        .profileImg("kakao-profile.jpg")
        .lastLoginAt(LocalDateTime.now())
        .build();
    
    // when
    User savedUser = userRepository.save(kakaoUser);
    entityManager.flush();
    entityManager.clear();

    // then
    assertThat(savedUser).isNotNull();
    assertThat(savedUser.getEmail()).isEqualTo("kakao@example.com");
    assertThat(savedUser.getName()).isEqualTo("카카오사용자");
    assertThat(savedUser.getNickname()).isEqualTo("kakaouser");
    assertThat(savedUser.getSocialId()).isEqualTo("123456789");
    assertThat(savedUser.getSocialProvider()).isEqualTo(SocialProvider.KAKAO);
    assertThat(savedUser.getProfileImg()).isEqualTo("kakao-profile.jpg");
    assertThat(savedUser.getGender()).isEqualTo("M");
    assertThat(savedUser.getPhoneNumber()).isEqualTo("010-1234-5678");
    assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
    assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(savedUser.getCreatedAt()).isNotNull();
    assertThat(savedUser.getUpdatedAt()).isNotNull();

    // 소셜 ID로 조회 테스트
    Optional<User> foundUser = userRepository.findBySocialId("123456789");
    assertThat(foundUser).isPresent();
    assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
  }

  @Test
  @DisplayName("네이버 OAuth2 사용자 생성 및 조회 통합 테스트")
  void createAndRetrieveNaverUser() {
    // given
    User naverUser = User.builder()
        .email("naver@example.com")
        .name("네이버사용자")
        .nickname("naveruser")
        .password("SOCIAL_LOGIN_naver123456_1234567890")
        .phoneNumber("010-1234-5678")
        .socialProvider(SocialProvider.NAVER)
        .socialId("naver123456")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .gender("M")
        .profileImg("naver-profile.jpg")
        .lastLoginAt(LocalDateTime.now())
        .build();
    
    // when
    User savedUser = userRepository.save(naverUser);
    entityManager.flush();
    entityManager.clear();

    // then
    assertThat(savedUser).isNotNull();
    assertThat(savedUser.getEmail()).isEqualTo("naver@example.com");
    assertThat(savedUser.getName()).isEqualTo("네이버사용자");
    assertThat(savedUser.getNickname()).isEqualTo("naveruser");
    assertThat(savedUser.getSocialId()).isEqualTo("naver123456");
    assertThat(savedUser.getSocialProvider()).isEqualTo(SocialProvider.NAVER);
    assertThat(savedUser.getProfileImg()).isEqualTo("naver-profile.jpg");
    assertThat(savedUser.getGender()).isEqualTo("M");
    assertThat(savedUser.getPhoneNumber()).isEqualTo("010-1234-5678");
    assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
    assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);

    // 소셜 ID로 조회 테스트
    Optional<User> foundUser = userRepository.findBySocialId("naver123456");
    assertThat(foundUser).isPresent();
    assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
  }

  @Test
  @DisplayName("닉네임 중복 허용 통합 테스트")
  void allowDuplicateNickname() {
    // given
    User user1 = User.builder()
        .email("kakao@example.com")
        .name("카카오사용자")
        .nickname("duplicate123")
        .password("SOCIAL_LOGIN_123456789_1234567890")
        .phoneNumber("010-1234-5678")
        .socialProvider(SocialProvider.KAKAO)
        .socialId("123456789")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .build();

    User user2 = User.builder()
        .email("naver@example.com")
        .name("네이버사용자")
        .nickname("duplicate123") // 같은 닉네임 사용
        .password("SOCIAL_LOGIN_naver123456_1234567890")
        .phoneNumber("010-9876-5432")
        .socialProvider(SocialProvider.NAVER)
        .socialId("naver123456")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .build();

    // when
    User savedUser1 = userRepository.save(user1);
    User savedUser2 = userRepository.save(user2);
    entityManager.flush();
    entityManager.clear();

    // then - 닉네임 중복이 허용되어 정상 저장됨
    assertThat(savedUser1).isNotNull();
    assertThat(savedUser2).isNotNull();
    assertThat(savedUser1.getNickname()).isEqualTo("duplicate123");
    assertThat(savedUser2.getNickname()).isEqualTo("duplicate123");
    assertThat(savedUser1.getEmail()).isEqualTo("kakao@example.com");
    assertThat(savedUser2.getEmail()).isEqualTo("naver@example.com");
  }
}
