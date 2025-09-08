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
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Builder를 위해 private 생성자로 제한
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

  @Column(nullable = false)
  private boolean isBanned;

  private String profileImg;

  // 테스트를 위한 setter 메서드들 (password 제외)
  public void setName(String name) {
    this.name = name;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public void setAddress(String roadAddress, String detailAddress, String zipCode) {
    this.roadAddress = roadAddress;
    this.detailAddress = detailAddress;
    this.zipCode = zipCode;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }

  public void setLastLoginAt(LocalDateTime lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }

  public void setBanned(boolean isBanned) {
    this.isBanned = isBanned;
  }

  public void setSocialProvider(SocialProvider socialProvider) {
    this.socialProvider = socialProvider;
  }

  public void setSocialId(String socialId) {
    this.socialId = socialId;
  }

  public void setProfileImg(String profileImg) {
    this.profileImg = profileImg;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  // 비즈니스 로직을 위한 메서드들
  public void updateLastLoginAt() {
    this.lastLoginAt = LocalDateTime.now();
  }
}