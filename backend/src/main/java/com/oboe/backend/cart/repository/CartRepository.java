package com.oboe.backend.cart.repository;

import com.oboe.backend.cart.entity.Cart;
import com.oboe.backend.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

  /**
   * 사용자 ID로 장바구니 조회
   */
  @Query("SELECT c FROM Cart c WHERE c.user.id = :userId AND c.isActive = true")
  Optional<Cart> findByUserIdAndActive(@Param("userId") Long userId);

  /**
   * 사용자로 장바구니 조회
   */
  Optional<Cart> findByUserAndIsActiveTrue(User user);

  /**
   * 사용자의 활성 장바구니 존재 여부 확인
   */
  boolean existsByUserAndIsActiveTrue(User user);

  /**
   * 사용자 ID로 활성 장바구니 존재 여부 확인
   */
  @Query("SELECT COUNT(c) > 0 FROM Cart c WHERE c.user.id = :userId AND c.isActive = true")
  boolean existsByUserIdAndActive(@Param("userId") Long userId);

  /**
   * 비활성 장바구니들 삭제 (사용자 탈퇴 시 등)
   */
  void deleteByUserAndIsActiveFalse(User user);

  /**
   * 사용자의 모든 장바구니 삭제
   */
  void deleteByUser(User user);
}
