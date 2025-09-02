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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

  @Column(nullable = false, unique = true)
  private String nickname;

  @Column(nullable = false, unique = true)
  private String phoneNumber;

  @Column(nullable = false)
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserStatus status;

  private String address;

  private LocalDate birthDate;

  private String gender;

  private String socialProvider;

  @Column(nullable = false)
  private LocalDateTime lastLoginAt;

  private boolean isBanned;

  //TODO: 수정 필요
  private String profileImg;

  public static User of(String email, String password, String name, String nickname, String phoneNumber,UserRole role, UserStatus status, String address, LocalDate birthDate, String gender, String socialProvider, LocalDateTime lastLoginAt, boolean isBanned, String profileImg) {
    User user = new User();
    user.setEmail(email);
    user.setPassword(password);
    user.setName(name);
    user.setNickname(nickname);
    user.setPhoneNumber(phoneNumber);
    user.setRole(role);
    user.setStatus(status);
    user.setAddress(address);
    user.setBirthDate(birthDate);
    user.setGender(gender);
    user.setSocialProvider(socialProvider);
    user.setLastLoginAt(lastLoginAt);
    user.setBanned(false);
    user.setProfileImg(profileImg);

    return user;
  }
}