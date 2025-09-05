package com.oboe.backend.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


@DisplayName("유저 엔티티 테스트")
class UserTest {

  private User user;

  @BeforeEach
  void setUp() {
    user = User.builder()
        .email("test@example.com")
        .password("password123")
        .name("홍길동")
        .nickname("hong123")
        .phoneNumber("010-1234-5678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .roadAddress("서울시 강남구")
        .detailAddress("1층")
        .zipCode("02111")
        .birthDate(LocalDate.of(1990, 1, 1))
        .gender("M")
        .socialProvider(SocialProvider.LOCAL)
        .lastLoginAt(LocalDateTime.now())
        .isBanned(false)
        .profileImg("profile.jpg")
        .build();
  }

  @Test
  @DisplayName("User 엔티티 생성 테스트")
  void createUser() {
    // given & when
    User newUser = User.builder()
        .email("new@example.com")
        .password("newpassword")
        .name("김철수")
        .nickname("kim123")
        .phoneNumber("010-9876-5432")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .lastLoginAt(LocalDateTime.now())
        .build();

    // then
    assertThat(newUser.getEmail()).isEqualTo("new@example.com");
    assertThat(newUser.getName()).isEqualTo("김철수");
    assertThat(newUser.getNickname()).isEqualTo("kim123");
    assertThat(newUser.getRole()).isEqualTo(UserRole.USER);
    assertThat(newUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(newUser.isBanned()).isFalse();
  }

  @Test
  @DisplayName("User.builder() 정적 팩토리 메서드 테스트")
  void createUserWithBuilderMethod() {
    // given
    LocalDateTime now = LocalDateTime.now();
    LocalDate birthDate = LocalDate.of(1995, 5, 15);

    // when
    User createdUser = User.builder()
        .email("factory@example.com")
        .password("factorypassword")
        .name("팩토리사용자")
        .nickname("factory123")
        .phoneNumber("010-1111-2222")
        .role(UserRole.ADMIN)
        .status(UserStatus.ACTIVE)
        .roadAddress("서울시 강남구")
        .detailAddress("1층")
        .zipCode("02111")
        .birthDate(birthDate)
        .gender("F")
        .socialProvider(SocialProvider.KAKAO)
        .lastLoginAt(now)
        .isBanned(false)
        .profileImg("factory_profile.jpg")
        .build();

    // then
    assertThat(createdUser.getEmail()).isEqualTo("factory@example.com");
    assertThat(createdUser.getName()).isEqualTo("팩토리사용자");
    assertThat(createdUser.getNickname()).isEqualTo("factory123");
    assertThat(createdUser.getRole()).isEqualTo(UserRole.ADMIN);
    assertThat(createdUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(createdUser.getRoadAddress()).isEqualTo("서울시 강남구");
    assertThat(createdUser.getDetailAddress()).isEqualTo("1층");
    assertThat(createdUser.getZipCode()).isEqualTo("02111");
    assertThat(createdUser.getBirthDate()).isEqualTo(birthDate);
    assertThat(createdUser.getGender()).isEqualTo("F");
    assertThat(createdUser.getSocialProvider()).isEqualTo(SocialProvider.KAKAO);
    assertThat(createdUser.getLastLoginAt()).isEqualTo(now);
    assertThat(createdUser.isBanned()).isFalse();
    assertThat(createdUser.getProfileImg()).isEqualTo("factory_profile.jpg");
  }

  @Test
  @DisplayName("User 필드 수정 테스트")
  void updateUserFields() {
    // given
    String newName = "수정된이름";
    String newNickname = "수정된닉네임";
    String newRoadAddress = "수정된주소";
    String newDetailAddress = "수정된주소";
    String newZipCode = "수정된 우편번호";
    UserStatus newStatus = UserStatus.SUSPENDED;

    // when
    user.setName(newName);
    user.setNickname(newNickname);
    user.setAddress(newRoadAddress, newDetailAddress, newZipCode);
    user.setStatus(newStatus);
    user.setBanned(true);

    // then
    assertThat(user.getName()).isEqualTo(newName);
    assertThat(user.getNickname()).isEqualTo(newNickname);
    assertThat(user.getDetailAddress()).isEqualTo(newRoadAddress);
    assertThat(user.getRoadAddress()).isEqualTo(newDetailAddress);
    assertThat(user.getZipCode()).isEqualTo(newZipCode);
    assertThat(user.getStatus()).isEqualTo(newStatus);
    assertThat(user.isBanned()).isTrue();
  }

  @Test
  @DisplayName("UserRole enum 테스트")
  void testUserRoleEnum() {
    // given & when & then
    assertThat(UserRole.USER).isNotNull();
    assertThat(UserRole.ADMIN).isNotNull();
    assertThat(UserRole.values()).hasSize(2);
  }

  @Test
  @DisplayName("UserStatus enum 테스트")
  void testUserStatusEnum() {
    // given & when & then
    assertThat(UserStatus.ACTIVE).isNotNull();
    assertThat(UserStatus.SUSPENDED).isNotNull();
    assertThat(UserStatus.DELETED).isNotNull();
    assertThat(UserStatus.values()).hasSize(3);
  }

  @Test
  @DisplayName("User 상태 변경 테스트")
  void testUserStatusChange() {
    // given
    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);

    // when - 정지 상태로 변경
    user.setStatus(UserStatus.SUSPENDED);

    // then
    assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);

    // when - 삭제 상태로 변경
    user.setStatus(UserStatus.DELETED);

    // then
    assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
  }

  @Test
  @DisplayName("User 역할 변경 테스트")
  void testUserRoleChange() {
    // given
    assertThat(user.getRole()).isEqualTo(UserRole.USER);

    // when
    user.setRole(UserRole.ADMIN);

    // then
    assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
  }

  @Test
  @DisplayName("User 로그인 시간 업데이트 테스트")
  void testLastLoginAtUpdate() {
    // given
    LocalDateTime originalLoginTime = user.getLastLoginAt();
    LocalDateTime newLoginTime = LocalDateTime.now().plusHours(1);

    // when
    user.setLastLoginAt(newLoginTime);

    // then
    assertThat(user.getLastLoginAt()).isEqualTo(newLoginTime);
    assertThat(user.getLastLoginAt()).isNotEqualTo(originalLoginTime);
  }

  @Test
  @DisplayName("User 밴 상태 테스트")
  void testUserBanStatus() {
    // given
    assertThat(user.isBanned()).isFalse();

    // when
    user.setBanned(true);

    // then
    assertThat(user.isBanned()).isTrue();

    // when - 밴 해제
    user.setBanned(false);

    // then
    assertThat(user.isBanned()).isFalse();
  }

  @Test
  @DisplayName("OAuth2 사용자 생성 테스트")
  void createOAuth2User() {
    // given & when
    User oauthUser = User.builder()
        .email("oauth@kakao.com")
        .name("OAuth사용자")
        .nickname("oauthuser")
        .socialProvider(SocialProvider.KAKAO)
        .socialId("123456789")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .profileImg("https://example.com/profile.jpg")
        .lastLoginAt(LocalDateTime.now())
        .isBanned(false)
        .build();

    // then
    assertThat(oauthUser.getEmail()).isEqualTo("oauth@kakao.com");
    assertThat(oauthUser.getName()).isEqualTo("OAuth사용자");
    assertThat(oauthUser.getNickname()).isEqualTo("oauthuser");
    assertThat(oauthUser.getSocialProvider()).isEqualTo(SocialProvider.KAKAO);
    assertThat(oauthUser.getSocialId()).isEqualTo("123456789");
    assertThat(oauthUser.getProfileImg()).isEqualTo("https://example.com/profile.jpg");
  }

  @Test
  @DisplayName("OAuth2 필드 수정 테스트")
  void updateOAuth2Fields() {
    // given
    String newSocialId = "987654321";
    String newProfileImg = "https://example.com/new-profile.jpg";
    SocialProvider newSocialProvider = SocialProvider.KAKAO;

    // when
    user.setSocialId(newSocialId);
    user.setProfileImg(newProfileImg);
    user.setSocialProvider(newSocialProvider);

    // then
    assertThat(user.getSocialId()).isEqualTo(newSocialId);
    assertThat(user.getProfileImg()).isEqualTo(newProfileImg);
    assertThat(user.getSocialProvider()).isEqualTo(newSocialProvider);
  }

  @Test
  @DisplayName("로컬 사용자를 OAuth2 사용자로 전환 테스트")
  void convertLocalUserToOAuth2() {
    // given
    assertThat(user.getSocialProvider()).isEqualTo(SocialProvider.LOCAL);
    assertThat(user.getSocialId()).isNull();

    // when
    user.setSocialProvider(SocialProvider.KAKAO);
    user.setSocialId("123456789");
    user.setProfileImg("https://example.com/kakao-profile.jpg");

    // then
    assertThat(user.getSocialProvider()).isEqualTo(SocialProvider.KAKAO);
    assertThat(user.getSocialId()).isEqualTo("123456789");
    assertThat(user.getProfileImg()).isEqualTo("https://example.com/kakao-profile.jpg");
  }

  @Test
  @DisplayName("네이버 OAuth2 사용자 생성 테스트")
  void createNaverOAuth2User() {
    // given & when
    User naverUser = User.builder()
        .email("naver@naver.com")
        .name("네이버사용자")
        .nickname("naveruser")
        .socialProvider(SocialProvider.NAVER)
        .socialId("naver123456")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .profileImg("https://example.com/naver-profile.jpg")
        .lastLoginAt(LocalDateTime.now())
        .isBanned(false)
        .build();

    // then
    assertThat(naverUser.getEmail()).isEqualTo("naver@naver.com");
    assertThat(naverUser.getName()).isEqualTo("네이버사용자");
    assertThat(naverUser.getNickname()).isEqualTo("naveruser");
    assertThat(naverUser.getSocialProvider()).isEqualTo(SocialProvider.NAVER);
    assertThat(naverUser.getSocialId()).isEqualTo("naver123456");
    assertThat(naverUser.getProfileImg()).isEqualTo("https://example.com/naver-profile.jpg");
  }
}
