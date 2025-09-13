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
    assertThat(createdUser.getProfileImg()).isEqualTo("factory_profile.jpg");
  }

  @Test
  @DisplayName("User 프로필 업데이트 테스트")
  void updateUserProfile() {
    // given
    String newNickname = "수정된닉네임";
    String newRoadAddress = "수정된도로주소";
    String newDetailAddress = "수정된상세주소";
    String newZipCode = "12345";

    // when
    user.updateProfile(newNickname, newRoadAddress, newDetailAddress, newZipCode);

    // then
    assertThat(user.getNickname()).isEqualTo(newNickname);
    assertThat(user.getRoadAddress()).isEqualTo(newRoadAddress);
    assertThat(user.getDetailAddress()).isEqualTo(newDetailAddress);
    assertThat(user.getZipCode()).isEqualTo(newZipCode);
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
    assertThat(UserStatus.WITHDRAW).isNotNull();
    assertThat(UserStatus.values()).hasSize(3);
  }

  @Test
  @DisplayName("User 상태 변경 테스트")
  void testUserStatusChange() {
    // given
    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);

    // when - 정지 상태로 변경
    user.changeStatus(UserStatus.SUSPENDED);

    // then
    assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);

    // when - 탈퇴 상태로 변경
    user.changeStatus(UserStatus.WITHDRAW);

    // then
    assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAW);
  }

  @Test
  @DisplayName("User 역할 변경 테스트")
  void testUserRoleChange() {
    // given
    assertThat(user.getRole()).isEqualTo(UserRole.USER);

    // when
    user.changeRole(UserRole.ADMIN);

    // then
    assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
  }

  @Test
  @DisplayName("User 로그인 시간 업데이트 테스트")
  void testLastLoginAtUpdate() {
    // given
    LocalDateTime originalLoginTime = user.getLastLoginAt();
    
    // 시간 차이를 만들기 위해 잠시 대기
    try {
      Thread.sleep(1);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    
    // when
    user.updateLastLoginAt();

    // then
    assertThat(user.getLastLoginAt()).isNotNull();
    assertThat(user.getLastLoginAt()).isAfterOrEqualTo(originalLoginTime);
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
        .build();

    // then
    assertThat(naverUser.getEmail()).isEqualTo("naver@naver.com");
    assertThat(naverUser.getName()).isEqualTo("네이버사용자");
    assertThat(naverUser.getNickname()).isEqualTo("naveruser");
    assertThat(naverUser.getSocialProvider()).isEqualTo(SocialProvider.NAVER);
    assertThat(naverUser.getSocialId()).isEqualTo("naver123456");
    assertThat(naverUser.getProfileImg()).isEqualTo("https://example.com/naver-profile.jpg");
  }

  @Test
  @DisplayName("주소 업데이트 테스트")
  void updateAddress() {
    // given
    String newRoadAddress = "부산광역시 해운대구";
    String newDetailAddress = "우동 123번지";
    String newZipCode = "48000";

    // when
    user.updateAddress(newRoadAddress, newDetailAddress, newZipCode);

    // then
    assertThat(user.getRoadAddress()).isEqualTo(newRoadAddress);
    assertThat(user.getDetailAddress()).isEqualTo(newDetailAddress);
    assertThat(user.getZipCode()).isEqualTo(newZipCode);
  }

  @Test
  @DisplayName("프로필 이미지 업데이트 테스트")
  void updateProfileImage() {
    // given
    String newProfileImg = "https://example.com/new-profile.jpg";

    // when
    user.updateProfileImage(newProfileImg);

    // then
    assertThat(user.getProfileImg()).isEqualTo(newProfileImg);
  }

  @Test
  @DisplayName("비밀번호 변경 테스트")
  void changePassword() {
    // given
    String newPassword = "newSecurePassword123!";

    // when
    user.changePassword(newPassword);

    // then
    assertThat(user.getPassword()).isEqualTo(newPassword);
  }

  @Test
  @DisplayName("비밀번호 변경 시 null 또는 빈 문자열 예외 테스트")
  void changePasswordWithInvalidInput() {
    // when & then
    assertThatThrownBy(() -> user.changePassword(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("비밀번호는 필수입니다.");

    assertThatThrownBy(() -> user.changePassword(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("비밀번호는 필수입니다.");

    assertThatThrownBy(() -> user.changePassword("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("비밀번호는 필수입니다.");
  }

  @Test
  @DisplayName("회원 탈퇴 처리 테스트")
  void withdraw() {
    // given
    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(user.getDeletedAt()).isNull();
    assertThat(user.getPiiClearedAt()).isNull();

    // when
    user.withdraw();

    // then
    assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAW);
    assertThat(user.getDeletedAt()).isNotNull();
    assertThat(user.getPiiClearedAt()).isNotNull();
    assertThat(user.getPiiClearedAt()).isAfter(user.getDeletedAt());
  }

  @Test
  @DisplayName("이메일 익명화 테스트")
  void anonymizeEmail() {
    // given
    String originalEmail = user.getEmail();
    String anonymizedEmail = "anonymous_123@deleted.com";

    // when
    user.anonymizeEmail(anonymizedEmail);

    // then
    assertThat(user.getEmail()).isEqualTo(anonymizedEmail);
    assertThat(user.getEmail()).isNotEqualTo(originalEmail);
  }

  @Test
  @DisplayName("이메일 익명화 시 null 또는 빈 문자열 예외 테스트")
  void anonymizeEmailWithInvalidInput() {
    // when & then
    assertThatThrownBy(() -> user.anonymizeEmail(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("익명화된 이메일은 필수입니다.");

    assertThatThrownBy(() -> user.anonymizeEmail(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("익명화된 이메일은 필수입니다.");

    assertThatThrownBy(() -> user.anonymizeEmail("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("익명화된 이메일은 필수입니다.");
  }

  @Test
  @DisplayName("개인정보 정리 처리 테스트")
  void clearPersonalInfo() {
    // given
    String originalEmail = user.getEmail();
    String originalName = user.getName();
    String originalNickname = user.getNickname();
    String originalPhoneNumber = user.getPhoneNumber();

    // when
    user.clearPersonalInfo();

    // then
    assertThat(user.getEmail()).isEqualTo("deleted_user_" + user.getId() + "@deleted.com");
    assertThat(user.getName()).isEqualTo("탈퇴한사용자");
    assertThat(user.getNickname()).isEqualTo("탈퇴한사용자");
    assertThat(user.getPhoneNumber()).isEqualTo("000-0000-0000");
    assertThat(user.getRoadAddress()).isNull();
    assertThat(user.getDetailAddress()).isNull();
    assertThat(user.getZipCode()).isNull();
    assertThat(user.getBirthDate()).isNull();
    assertThat(user.getGender()).isNull();
    assertThat(user.getProfileImg()).isNull();
    assertThat(user.getPiiClearedAt()).isNotNull();

    // 원래 정보와 다른지 확인
    assertThat(user.getEmail()).isNotEqualTo(originalEmail);
    assertThat(user.getName()).isNotEqualTo(originalName);
    assertThat(user.getNickname()).isNotEqualTo(originalNickname);
    assertThat(user.getPhoneNumber()).isNotEqualTo(originalPhoneNumber);
  }

  @Test
  @DisplayName("소셜 로그인 정보 설정 테스트")
  void setSocialInfo() {
    // given
    SocialProvider newProvider = SocialProvider.KAKAO;
    String newSocialId = "kakao987654321";

    // when
    user.setSocialInfo(newProvider, newSocialId);

    // then
    assertThat(user.getSocialProvider()).isEqualTo(newProvider);
    assertThat(user.getSocialId()).isEqualTo(newSocialId);
  }

  @Test
  @DisplayName("소셜 로그인 정보 설정 시 null 값 예외 테스트")
  void setSocialInfoWithInvalidInput() {
    // when & then
    assertThatThrownBy(() -> user.setSocialInfo(null, "socialId"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("소셜 로그인 정보는 필수입니다.");

    assertThatThrownBy(() -> user.setSocialInfo(SocialProvider.KAKAO, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("소셜 로그인 정보는 필수입니다.");
  }

  @Test
  @DisplayName("활성 사용자 확인 테스트")
  void isActive() {
    // given - setUp에서 ACTIVE 상태로 설정됨
    assertThat(user.isActive()).isTrue();

    // when - 상태를 SUSPENDED로 변경
    user.changeStatus(UserStatus.SUSPENDED);

    // then
    assertThat(user.isActive()).isFalse();

    // when - 상태를 WITHDRAW로 변경
    user.changeStatus(UserStatus.WITHDRAW);

    // then
    assertThat(user.isActive()).isFalse();
  }

  @Test
  @DisplayName("소셜 사용자 확인 테스트")
  void isSocialUser() {
    // given - setUp에서 SocialProvider.LOCAL로 설정됨
    assertThat(user.isSocialUser()).isFalse();

    // when - 카카오 소셜 정보 설정
    user.setSocialInfo(SocialProvider.KAKAO, "kakao123");

    // then
    assertThat(user.isSocialUser()).isTrue();

    // when - 네이버 소셜 정보 설정
    user.setSocialInfo(SocialProvider.NAVER, "naver123");

    // then
    assertThat(user.isSocialUser()).isTrue();
  }

  @Test
  @DisplayName("탈퇴한 사용자 확인 테스트")
  void isWithdrawn() {
    // given - setUp에서 ACTIVE 상태로 설정됨
    assertThat(user.isWithdrawn()).isFalse();

    // when - 탈퇴 처리
    user.withdraw();

    // then
    assertThat(user.isWithdrawn()).isTrue();
  }

  @Test
  @DisplayName("PII 정리된 사용자 확인 테스트")
  void isPiiCleared() {
    // given - 초기 상태에서는 PII가 정리되지 않음
    assertThat(user.isPiiCleared()).isFalse();

    // when - 개인정보 정리 처리
    user.clearPersonalInfo();

    // then
    assertThat(user.isPiiCleared()).isTrue();
  }

  @Test
  @DisplayName("관리자 확인 테스트")
  void isAdmin() {
    // given - setUp에서 UserRole.USER로 설정됨
    assertThat(user.isAdmin()).isFalse();

    // when - 관리자 역할로 변경
    user.changeRole(UserRole.ADMIN);

    // then
    assertThat(user.isAdmin()).isTrue();

    // when - 다시 일반 사용자로 변경
    user.changeRole(UserRole.USER);

    // then
    assertThat(user.isAdmin()).isFalse();
  }

  @Test
  @DisplayName("상태 변경 시 null 값 예외 테스트")
  void changeStatusWithNull() {
    // when & then
    assertThatThrownBy(() -> user.changeStatus(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("사용자 상태는 필수입니다.");
  }

  @Test
  @DisplayName("역할 변경 시 null 값 예외 테스트")
  void changeRoleWithNull() {
    // when & then
    assertThatThrownBy(() -> user.changeRole(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("사용자 역할은 필수입니다.");
  }

  @Test
  @DisplayName("빌더 패턴으로 최소 필드만 설정한 사용자 생성 테스트")
  void createUserWithMinimalFields() {
    // given & when
    User minimalUser = User.builder()
        .email("minimal@example.com")
        .password("password123")
        .name("최소사용자")
        .nickname("minimal")
        .phoneNumber("010-0000-0000")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .build();

    // then
    assertThat(minimalUser.getEmail()).isEqualTo("minimal@example.com");
    assertThat(minimalUser.getName()).isEqualTo("최소사용자");
    assertThat(minimalUser.getNickname()).isEqualTo("minimal");
    assertThat(minimalUser.getRole()).isEqualTo(UserRole.USER);
    assertThat(minimalUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(minimalUser.getSocialProvider()).isEqualTo(SocialProvider.LOCAL);
    assertThat(minimalUser.isActive()).isTrue();
    assertThat(minimalUser.isSocialUser()).isFalse();
    assertThat(minimalUser.isAdmin()).isFalse();
  }
}
