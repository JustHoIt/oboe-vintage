package com.oboe.backend.user.entity;

import com.oboe.backend.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"socialProvider", "socialId"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User extends BaseTimeEntity {

  //고유 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String nickname;

  @Column(nullable = false, unique = true)
  private String phoneNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserStatus status;

  private String roadAddress;

  private String detailAddress;

  private String zipCode;

  private LocalDate birthDate;

  private String gender;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SocialProvider socialProvider;

  @Column(unique = true)
  private String socialId;

  private LocalDateTime lastLoginAt;

  private String profileImg;

  private LocalDateTime deletedAt;

  private LocalDateTime piiClearedAt;

  // ==============================
  // 도메인 비즈니스 메서드들
  // ==============================

  /**
   * 사용자 프로필 정보 업데이트
   */
  public void updateProfile(String nickname, String roadAddress, String detailAddress, String zipCode) {
    if (nickname != null && !nickname.trim().isEmpty()) {
      this.nickname = nickname;
    }
    updateAddress(roadAddress, detailAddress, zipCode);
  }

  /**
   * 주소 정보 업데이트
   */
  public void updateAddress(String roadAddress, String detailAddress, String zipCode) {
    this.roadAddress = roadAddress;
    this.detailAddress = detailAddress;
    this.zipCode = zipCode;
  }

  /**
   * 프로필 이미지 업데이트
   */
  public void updateProfileImage(String profileImg) {
    this.profileImg = profileImg;
  }

  /**
   * 비밀번호 변경
   */
  public void changePassword(String newPassword) {
    if (newPassword == null || newPassword.trim().isEmpty()) {
      throw new IllegalArgumentException("비밀번호는 필수입니다.");
    }
    this.password = newPassword;
  }

  /**
   * 마지막 로그인 시간 업데이트
   */
  public void updateLastLoginAt() {

    this.lastLoginAt = LocalDateTime.now();
  }

  /**
   * 사용자 상태 변경 (관리자용)
   */
  public void changeStatus(UserStatus newStatus) {
    if (newStatus == null) {
      throw new IllegalArgumentException("사용자 상태는 필수입니다.");
    }
    this.status = newStatus;
  }

  /**
   * 사용자 역할 변경 (관리자용)
   */
  public void changeRole(UserRole newRole) {
    if (newRole == null) {
      throw new IllegalArgumentException("사용자 역할은 필수입니다.");
    }
    this.role = newRole;
  }

  /**
   * 회원 탈퇴 처리
   */
  public void withdraw() {
    this.status = UserStatus.WITHDRAW;
    this.deletedAt = LocalDateTime.now();
    this.piiClearedAt = LocalDateTime.now().plusDays(30);
  }

  /**
   * 즉시 이메일 익명화 (탈퇴 시)
   */
  public void anonymizeEmail(String anonymizedEmail) {
    if (anonymizedEmail == null || anonymizedEmail.trim().isEmpty()) {
      throw new IllegalArgumentException("익명화된 이메일은 필수입니다.");
    }
    this.email = anonymizedEmail;
  }

  /**
   * PII(개인식별정보) 정리 처리 (배치 작업용)
   */
  public void clearPersonalInfo() {
    this.email = "deleted_user_" + this.id + "@deleted.com";
    this.name = "탈퇴한사용자";
    this.nickname = "탈퇴한사용자";
    this.phoneNumber = "000-0000-0000";
    this.roadAddress = null;
    this.detailAddress = null;
    this.zipCode = null;
    this.birthDate = null;
    this.gender = null;
    this.profileImg = null;
    this.piiClearedAt = LocalDateTime.now();
  }

  /**
   * 소셜 로그인 정보 설정 (OAuth2용)
   */
  public void setSocialInfo(SocialProvider provider, String socialId) {
    if (provider == null || socialId == null) {
      throw new IllegalArgumentException("소셜 로그인 정보는 필수입니다.");
    }
    this.socialProvider = provider;
    this.socialId = socialId;
  }

  // ==============================
  // 도메인 검증 메서드들
  // ==============================

  /**
   * 활성 사용자인지 확인
   */
  public boolean isActive() {
    return this.status == UserStatus.ACTIVE;
  }

  /**
   * 소셜 로그인 사용자인지 확인
   */
  public boolean isSocialUser() {
    return this.socialProvider != SocialProvider.LOCAL;
  }

  /**
   * 탈퇴한 사용자인지 확인
   */
  public boolean isWithdrawn() {
    return this.status == UserStatus.WITHDRAW;
  }

  /**
   * PII가 정리된 사용자인지 확인
   */
  public boolean isPiiCleared() {
    return this.piiClearedAt != null;
  }

  /**
   * 관리자인지 확인
   */
  public boolean isAdmin() {
    return this.role == UserRole.ADMIN;
  }

  // ==============================
  // 테스트 전용 메서드들 (패키지 접근 제한)
  // ==============================

  // 테스트에서만 사용하는 메서드들을 패키지 접근으로 제한
  void setNameForTest(String name) {
    this.name = name;
  }

  void setEmailForTest(String email) {
    this.email = email;
  }

  void setPhoneNumberForTest(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  void setStatusForTest(UserStatus status) {
    this.status = status;
  }

  void setDeletedAtForTest(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }
}