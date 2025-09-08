package com.oboe.backend.user.repository;

import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자 찾기
    Optional<User> findByEmail(String email);

    // 닉네임으로 사용자 찾기
    Optional<User> findByNickname(String nickname);

    // 전화번호로 사용자 찾기
    Optional<User> findByPhoneNumber(String phoneNumber);

    // 이메일 존재 여부 확인
    boolean existsByEmail(String email);

    // 닉네임 존재 여부 확인
    boolean existsByNickname(String nickname);

    // 전화번호 존재 여부 확인
    boolean existsByPhoneNumber(String phoneNumber);

    // 역할별 사용자 목록 조회
    List<User> findByRole(UserRole role);

    // 상태별 사용자 목록 조회
    List<User> findByStatus(UserStatus status);

    // 밴된 사용자 목록 조회
    List<User> findByIsBannedTrue();

    // 활성 사용자 목록 조회
    @Query("SELECT u FROM User u WHERE u.status = :status")
    List<User> findActiveUsers(@Param("status") UserStatus status);

    // 이름으로 사용자 검색 (부분 일치)
    List<User> findByNameContaining(String name);

    // 이메일로 사용자 검색 (부분 일치)
    List<User> findByEmailContaining(String email);

    // 소셜 프로바이더별 사용자 조회
    List<User> findBySocialProvider(SocialProvider socialProvider);

    // 소셜 ID로 사용자 찾기
    Optional<User> findBySocialId(String socialId);

    // 이메일과 소셜 프로바이더로 사용자 찾기
    Optional<User> findByEmailAndSocialProvider(String email, SocialProvider socialProvider);

    // 소셜 ID 존재 여부 확인
    boolean existsBySocialId(String socialId);

    // 특정 기간 이후 로그인한 사용자 조회
    @Query("SELECT u FROM User u WHERE u.lastLoginAt >= :since")
    List<User> findUsersLoggedInSince(@Param("since") java.time.LocalDateTime since);

    // 이름과 휴대폰번호로 사용자 찾기 (일반 계정만)
    @Query("SELECT u FROM User u WHERE u.name = :name AND u.phoneNumber = :phoneNumber AND u.socialProvider = 'LOCAL'")
    Optional<User> findByNameAndPhoneNumberAndSocialProvider(@Param("name") String name, 
                                                           @Param("phoneNumber") String phoneNumber);

    // 이메일과 휴대폰번호로 사용자 찾기 (일반 계정만)
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.phoneNumber = :phoneNumber AND u.socialProvider = 'LOCAL'")
    Optional<User> findByEmailAndPhoneNumberAndSocialProvider(@Param("email") String email, 
                                                            @Param("phoneNumber") String phoneNumber);
}
