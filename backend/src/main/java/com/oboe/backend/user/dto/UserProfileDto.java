package com.oboe.backend.user.dto;

import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 프로필 정보 DTO")
public class UserProfileDto {

    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "닉네임", example = "hong123")
    private String nickname;

    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phoneNumber;

    @Schema(description = "사용자 역할", example = "USER")
    private UserRole role;

    @Schema(description = "사용자 상태", example = "ACTIVE")
    private UserStatus status;

    @Schema(description = "소셜 제공자", example = "LOCAL")
    private SocialProvider socialProvider;

    @Schema(description = "소셜 ID", example = "123456789")
    private String socialId;

    @Schema(description = "도로명 주소", example = "서울시 강남구 테헤란로 123")
    private String roadAddress;

    @Schema(description = "상세 주소", example = "1층")
    private String detailAddress;

    @Schema(description = "우편번호", example = "02111")
    private String zipCode;

    @Schema(description = "생년월일", example = "1990-01-01")
    private LocalDate birthDate;

    @Schema(description = "성별", example = "M")
    private String gender;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImg;

    @Schema(description = "마지막 로그인 시간", example = "2024-01-01T12:00:00")
    private LocalDateTime lastLoginAt;

    @Schema(description = "계정 생성 시간", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "계정 수정 시간", example = "2024-01-01T11:00:00")
    private LocalDateTime updatedAt;

    /**
     * User 엔티티에서 UserProfileDto로 변환
     */
    public static UserProfileDto from(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .nickname(user.getNickname())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .socialProvider(user.getSocialProvider())
                .socialId(user.getSocialId())
                .roadAddress(user.getRoadAddress())
                .detailAddress(user.getDetailAddress())
                .zipCode(user.getZipCode())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .profileImg(user.getProfileImg())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
