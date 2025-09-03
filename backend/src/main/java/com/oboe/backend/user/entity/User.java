package com.oboe.backend.user.entity;

import com.oboe.backend.common.domain.BaseTimeEntity;
import com.oboe.backend.user.dto.SignUpDto;
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

  @Column(nullable = false, unique = true)
  private String nickname;

  @Column(nullable = false, unique = true)
  private String phoneNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserStatus status;

  private String address;

  private LocalDate birthDate;

  private String gender;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SocialProvider socialProvider;

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
  
  public void setAddress(String address) {
    this.address = address;
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
  
  // 비즈니스 로직을 위한 메서드들
  public void updateLastLoginAt() {
    this.lastLoginAt = LocalDateTime.now();
  }
}