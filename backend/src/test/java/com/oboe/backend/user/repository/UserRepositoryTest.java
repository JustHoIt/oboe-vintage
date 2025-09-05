package com.oboe.backend.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("유저 레포지토리 테스트")
class UserRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private UserRepository userRepository;

  private User testUser1;
  private User testUser2;
  private User testUser3;

  @BeforeEach
  void setUp() {
    // 테스트 데이터 생성
    testUser1 = User.builder()
        .email("user1@example.com")
        .password("password1")
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
        .lastLoginAt(LocalDateTime.now().minusDays(1))
        .isBanned(false)
        .profileImg("profile1.jpg")
        .build();

    testUser2 = User.builder()
        .email("user2@example.com")
        .password("password2")
        .name("김철수")
        .nickname("kim456")
        .phoneNumber("010-9876-5432")
        .role(UserRole.ADMIN)
        .status(UserStatus.ACTIVE)
        .roadAddress("서울시 강남구")
        .detailAddress("1층")
        .zipCode("02111")
        .birthDate(LocalDate.of(1985, 5, 15))
        .gender("F")
        .socialProvider(SocialProvider.LOCAL)
        .lastLoginAt(LocalDateTime.now().minusHours(2))
        .isBanned(false)
        .profileImg("profile2.jpg")
        .build();

    testUser3 = User.builder()
        .email("user3@example.com")
        .password("password3")
        .name("이영희")
        .nickname("lee789")
        .phoneNumber("010-5555-7777")
        .role(UserRole.USER)
        .status(UserStatus.SUSPENDED)
        .roadAddress("대전시 수성구")
        .detailAddress("301호")
        .zipCode("02468")
        .birthDate(LocalDate.of(1992, 8, 20))
        .gender("F")
        .socialProvider(SocialProvider.KAKAO)
        .lastLoginAt(LocalDateTime.now().minusDays(3))
        .isBanned(true)
        .profileImg("profile3.jpg")
        .build();

    // 데이터베이스에 저장
    entityManager.persistAndFlush(testUser1);
    entityManager.persistAndFlush(testUser2);
    entityManager.persistAndFlush(testUser3);
  }

  @Test
  @DisplayName("이메일로 사용자 찾기 테스트")
  void findByEmail() {
    // when
    Optional<User> foundUser = userRepository.findByEmail("user1@example.com");

    // then
    assertThat(foundUser).isPresent();
    assertThat(foundUser.get().getName()).isEqualTo("홍길동");
    assertThat(foundUser.get().getNickname()).isEqualTo("hong123");
  }

  @Test
  @DisplayName("존재하지 않는 이메일로 사용자 찾기 테스트")
  void findByEmailNotFound() {
    // when
    Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

    // then
    assertThat(foundUser).isEmpty();
  }

  @Test
  @DisplayName("닉네임으로 사용자 찾기 테스트")
  void findByNickname() {
    // when
    Optional<User> foundUser = userRepository.findByNickname("kim456");

    // then
    assertThat(foundUser).isPresent();
    assertThat(foundUser.get().getName()).isEqualTo("김철수");
    assertThat(foundUser.get().getRole()).isEqualTo(UserRole.ADMIN);
  }

  @Test
  @DisplayName("전화번호로 사용자 찾기 테스트")
  void findByPhoneNumber() {
    // when
    Optional<User> foundUser = userRepository.findByPhoneNumber("010-5555-7777");

    // then
    assertThat(foundUser).isPresent();
    assertThat(foundUser.get().getName()).isEqualTo("이영희");
    assertThat(foundUser.get().getStatus()).isEqualTo(UserStatus.SUSPENDED);
  }

  @Test
  @DisplayName("이메일 존재 여부 확인 테스트")
  void existsByEmail() {
    // when & then
    assertThat(userRepository.existsByEmail("user1@example.com")).isTrue();
    assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
  }


  @Test
  @DisplayName("전화번호 존재 여부 확인 테스트")
  void existsByPhoneNumber() {
    // when & then
    assertThat(userRepository.existsByPhoneNumber("010-1234-5678")).isTrue();
    assertThat(userRepository.existsByPhoneNumber("010-0000-0000")).isFalse();
  }

  @Test
  @DisplayName("역할별 사용자 목록 조회 테스트")
  void findByRole() {
    // when
    List<User> users = userRepository.findByRole(UserRole.USER);
    List<User> admins = userRepository.findByRole(UserRole.ADMIN);

    // then
    assertThat(users).hasSize(2);
    assertThat(users).extracting(User::getName).containsExactlyInAnyOrder("홍길동", "이영희");

    assertThat(admins).hasSize(1);
    assertThat(admins.get(0).getName()).isEqualTo("김철수");
  }

  @Test
  @DisplayName("상태별 사용자 목록 조회 테스트")
  void findByStatus() {
    // when
    List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);
    List<User> suspendedUsers = userRepository.findByStatus(UserStatus.SUSPENDED);

    // then
    assertThat(activeUsers).hasSize(2);
    assertThat(activeUsers).extracting(User::getName).containsExactlyInAnyOrder("홍길동", "김철수");

    assertThat(suspendedUsers).hasSize(1);
    assertThat(suspendedUsers.get(0).getName()).isEqualTo("이영희");
  }

  @Test
  @DisplayName("밴된 사용자 목록 조회 테스트")
  void findByIsBannedTrue() {
    // when
    List<User> bannedUsers = userRepository.findByIsBannedTrue();

    // then
    assertThat(bannedUsers).hasSize(1);
    assertThat(bannedUsers.get(0).getName()).isEqualTo("이영희");
  }

  @Test
  @DisplayName("활성 사용자 목록 조회 테스트 (JPQL)")
  void findActiveUsers() {
    // when
    List<User> activeUsers = userRepository.findActiveUsers(UserStatus.ACTIVE);

    // then
    assertThat(activeUsers).hasSize(2);
    assertThat(activeUsers).extracting(User::getStatus).containsOnly(UserStatus.ACTIVE);
    assertThat(activeUsers).extracting(User::getName).containsExactlyInAnyOrder("홍길동", "김철수");
  }

  @Test
  @DisplayName("이름으로 사용자 검색 테스트 (부분 일치)")
  void findByNameContaining() {
    // when
    List<User> users = userRepository.findByNameContaining("길");

    // then
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getName()).isEqualTo("홍길동");
  }

  @Test
  @DisplayName("이메일로 사용자 검색 테스트 (부분 일치)")
  void findByEmailContaining() {
    // when
    List<User> users = userRepository.findByEmailContaining("user1");

    // then
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getEmail()).isEqualTo("user1@example.com");
  }

  @Test
  @DisplayName("소셜 프로바이더별 사용자 조회 테스트")
  void findBySocialProvider() {
    // when
    List<User> localUsers = userRepository.findBySocialProvider(SocialProvider.LOCAL);
    List<User> kakaoUsers = userRepository.findBySocialProvider(SocialProvider.KAKAO);

    // then
    assertThat(localUsers).hasSize(2);
    assertThat(localUsers).extracting(User::getName).containsExactlyInAnyOrder("홍길동", "김철수");

    assertThat(kakaoUsers).hasSize(1);
    assertThat(kakaoUsers.get(0).getName()).isEqualTo("이영희");
  }

  @Test
  @DisplayName("특정 기간 이후 로그인한 사용자 조회 테스트")
  void findUsersLoggedInSince() {
    // given
    LocalDateTime since = LocalDateTime.now().minusDays(3);

    // when
    List<User> recentUsers = userRepository.findUsersLoggedInSince(since);

    // then
    assertThat(recentUsers).hasSize(2);
    assertThat(recentUsers).extracting(User::getName).containsExactlyInAnyOrder("홍길동", "김철수");
  }

  @Test
  @DisplayName("사용자 저장 및 조회 테스트")
  void saveAndFindUser() {
    // given
    User newUser = User.builder()
        .email("newuser@example.com")
        .password("newpassword")
        .name("새사용자")
        .nickname("newuser123")
        .phoneNumber("010-9999-8888")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .lastLoginAt(LocalDateTime.now())
        .build();

    // when
    User savedUser = userRepository.save(newUser);
    Optional<User> foundUser = userRepository.findById(savedUser.getId());

    // then
    assertThat(foundUser).isPresent();
    assertThat(foundUser.get().getEmail()).isEqualTo("newuser@example.com");
    assertThat(foundUser.get().getName()).isEqualTo("새사용자");
  }

  @Test
  @DisplayName("사용자 삭제 테스트")
  void deleteUser() {
    // given
    Long userId = testUser1.getId();

    // when
    userRepository.deleteById(userId);
    Optional<User> deletedUser = userRepository.findById(userId);

    // then
    assertThat(deletedUser).isEmpty();
  }

  @Test
  @DisplayName("전체 사용자 수 조회 테스트")
  void countUsers() {
    // when
    long userCount = userRepository.count();

    // then
    assertThat(userCount).isEqualTo(3);
  }
}
