package com.oboe.backend.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oboe.backend.config.JpaConfig;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import com.oboe.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
@Import(JpaConfig.class)
@DisplayName("User Repository 통합 테스트")
class UserIntegrationTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    // 테스트 데이터 초기화
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("사용자 생성 및 조회 통합 테스트")
  void createAndRetrieveUser() {
    // given
    User user = User.builder()
        .email("integration@example.com")
        .password("password123")
        .name("통합테스트사용자")
        .nickname("integration123")
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

    // when
    User savedUser = userRepository.save(user);
    entityManager.flush();
    entityManager.clear();

    User foundUser = userRepository.findById(savedUser.getId()).orElse(null);

    // then
    assertThat(foundUser).isNotNull();
    assertThat(foundUser.getEmail()).isEqualTo("integration@example.com");
    assertThat(foundUser.getName()).isEqualTo("통합테스트사용자");
    assertThat(foundUser.getNickname()).isEqualTo("integration123");
    assertThat(foundUser.getRole()).isEqualTo(UserRole.USER);
    assertThat(foundUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(foundUser.getCreatedAt()).isNotNull();
    assertThat(foundUser.getUpdatedAt()).isNotNull();
  }

  @Test
  @DisplayName("사용자 정보 수정 통합 테스트")
  void updateUser() {
    // given
    User user = createTestUser();
    User savedUser = userRepository.save(user);
    entityManager.flush();

    // when
    savedUser.setName("수정된이름");
    savedUser.setNickname("수정된닉네임");
    savedUser.setAddress("수정된주소", "수정된상세주소", "01111");
    savedUser.setStatus(UserStatus.SUSPENDED);

    User updatedUser = userRepository.save(savedUser);
    entityManager.flush();
    entityManager.clear();

    User foundUser = userRepository.findById(updatedUser.getId()).orElse(null);

    // then
    assertThat(foundUser).isNotNull();
    assertThat(foundUser.getName()).isEqualTo("수정된이름");
    assertThat(foundUser.getNickname()).isEqualTo("수정된닉네임");
    assertThat(foundUser.getRoadAddress()).isEqualTo("수정된주소");
    assertThat(foundUser.getDetailAddress()).isEqualTo("수정된상세주소");
    assertThat(foundUser.getZipCode()).isEqualTo("01111");
    assertThat(foundUser.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    assertThat(foundUser.getUpdatedAt()).isAfter(foundUser.getCreatedAt());
  }

  @Test
  @DisplayName("사용자 삭제 통합 테스트")
  void deleteUser() {
    // given
    User user = createTestUser();
    User savedUser = userRepository.save(user);
    Long userId = savedUser.getId();
    entityManager.flush();

    // when
    userRepository.deleteById(userId);
    entityManager.flush();
    entityManager.clear();

    // then
    assertThat(userRepository.findById(userId)).isEmpty();
    assertThat(userRepository.count()).isEqualTo(0);
  }

  @Test
  @DisplayName("중복 이메일 검증 통합 테스트")
  void duplicateEmailValidation() {
    // given
    User user1 = createTestUser();
    user1.setEmail("duplicate@example.com");
    userRepository.save(user1);
    entityManager.flush();

    User user2 = createTestUser();
    user2.setEmail("duplicate@example.com");
    user2.setNickname("different123");
    user2.setPhoneNumber("010-9999-8888");

    // when & then
    assertThatThrownBy(() -> {
      userRepository.save(user2);
      entityManager.flush();
    }).isInstanceOf(Exception.class);
  }

  @Test
  @DisplayName("닉네임 중복 허용 통합 테스트")
  void allowDuplicateNickname() {
    // given
    User user1 = createTestUser();
    user1.setNickname("duplicate123");
    userRepository.save(user1);
    entityManager.flush();

    User user2 = createTestUser();
    user2.setEmail("different@example.com");
    user2.setNickname("duplicate123"); // 같은 닉네임 사용
    user2.setPhoneNumber("010-9999-8888");

    // when
    User savedUser2 = userRepository.save(user2);
    entityManager.flush();

    // then - 닉네임 중복이 허용되어 정상 저장됨
    assertThat(savedUser2).isNotNull();
    assertThat(savedUser2.getNickname()).isEqualTo("duplicate123");
    assertThat(savedUser2.getEmail()).isEqualTo("different@example.com");
  }

  @Test
  @DisplayName("중복 전화번호 검증 통합 테스트")
  void duplicatePhoneNumberValidation() {
    // given
    User user1 = createTestUser();
    user1.setPhoneNumber("010-1111-2222");
    userRepository.save(user1);
    entityManager.flush();

    User user2 = createTestUser();
    user2.setEmail("different@example.com");
    user2.setNickname("different123");
    user2.setPhoneNumber("010-1111-2222");

    // when & then
    assertThatThrownBy(() -> {
      userRepository.save(user2);
      entityManager.flush();
    }).isInstanceOf(Exception.class);
  }

  @Test
  @DisplayName("사용자 상태별 조회 통합 테스트")
  void findUsersByStatus() {
    // given
    User activeUser1 = createTestUser();
    activeUser1.setEmail("active1@example.com");
    activeUser1.setNickname("active1");
    activeUser1.setPhoneNumber("010-1111-1111");
    activeUser1.setStatus(UserStatus.ACTIVE);

    User activeUser2 = createTestUser();
    activeUser2.setEmail("active2@example.com");
    activeUser2.setNickname("active2");
    activeUser2.setPhoneNumber("010-2222-2222");
    activeUser2.setStatus(UserStatus.ACTIVE);

    User suspendedUser = createTestUser();
    suspendedUser.setEmail("suspended@example.com");
    suspendedUser.setNickname("suspended");
    suspendedUser.setPhoneNumber("010-3333-3333");
    suspendedUser.setStatus(UserStatus.SUSPENDED);

    userRepository.saveAll(List.of(activeUser1, activeUser2, suspendedUser));
    entityManager.flush();

    // when
    List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);
    List<User> suspendedUsers = userRepository.findByStatus(UserStatus.SUSPENDED);

    // then
    assertThat(activeUsers).hasSize(2);
    assertThat(activeUsers).extracting(User::getStatus).containsOnly(UserStatus.ACTIVE);

    assertThat(suspendedUsers).hasSize(1);
    assertThat(suspendedUsers.get(0).getStatus()).isEqualTo(UserStatus.SUSPENDED);
  }

  @Test
  @DisplayName("사용자 역할별 조회 통합 테스트")
  void findUsersByRole() {
    // given
    User user1 = createTestUser();
    user1.setEmail("user1@example.com");
    user1.setNickname("user1");
    user1.setPhoneNumber("010-1111-1111");
    user1.setRole(UserRole.USER);

    User user2 = createTestUser();
    user2.setEmail("user2@example.com");
    user2.setNickname("user2");
    user2.setPhoneNumber("010-2222-2222");
    user2.setRole(UserRole.USER);

    User admin = createTestUser();
    admin.setEmail("admin@example.com");
    admin.setNickname("admin");
    admin.setPhoneNumber("010-3333-3333");
    admin.setRole(UserRole.ADMIN);

    userRepository.saveAll(List.of(user1, user2, admin));
    entityManager.flush();

    // when
    List<User> users = userRepository.findByRole(UserRole.USER);
    List<User> admins = userRepository.findByRole(UserRole.ADMIN);

    // then
    assertThat(users).hasSize(2);
    assertThat(users).extracting(User::getRole).containsOnly(UserRole.USER);

    assertThat(admins).hasSize(1);
    assertThat(admins.get(0).getRole()).isEqualTo(UserRole.ADMIN);
  }

  @Test
  @DisplayName("사용자 검색 통합 테스트")
  void searchUsers() {
    // given
    User user1 = createTestUser();
    user1.setEmail("hong@example.com");
    user1.setNickname("hong123");
    user1.setPhoneNumber("010-1111-1111");
    user1.setName("홍길동");

    User user2 = createTestUser();
    user2.setEmail("kim@example.com");
    user2.setNickname("kim456");
    user2.setPhoneNumber("010-2222-2222");
    user2.setName("김철수");

    userRepository.saveAll(List.of(user1, user2));
    entityManager.flush();

    // when
    List<User> usersByName = userRepository.findByNameContaining("길");
    List<User> usersByEmail = userRepository.findByEmailContaining("hong");

    // then
    assertThat(usersByName).hasSize(1);
    assertThat(usersByName.get(0).getName()).isEqualTo("홍길동");

    assertThat(usersByEmail).hasSize(1);
    assertThat(usersByEmail.get(0).getEmail()).isEqualTo("hong@example.com");
  }

  @Test
  @DisplayName("사용자 존재 여부 확인 통합 테스트")
  void checkUserExistence() {
    // given
    User user = createTestUser();
    userRepository.save(user);
    entityManager.flush();

    // when & then
    assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
    assertThat(userRepository.existsByPhoneNumber("010-1234-5678")).isTrue();

    assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    assertThat(userRepository.existsByPhoneNumber("010-0000-0000")).isFalse();
  }

  private User createTestUser() {
    return User.builder()
        .email("test@example.com")
        .password("password123")
        .name("테스트사용자")
        .nickname("test123")
        .phoneNumber("010-1234-5678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .roadAddress("테스트주소")
        .detailAddress("테스트 상세주소")
        .zipCode("00000")
        .birthDate(LocalDate.of(1990, 1, 1))
        .gender("M")
        .socialProvider(SocialProvider.LOCAL)
        .lastLoginAt(LocalDateTime.now())
        .isBanned(false)
        .profileImg("test.jpg")
        .build();
  }
}
