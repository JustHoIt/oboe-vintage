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
  @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
  Optional<Cart> findByUserId(@Param("userId") Long userId);

  /**
   * 사용자로 장바구니 조회
   */
  Optional<Cart> findByUser(User user);

  /**
   * 사용자의 장바구니 존재 여부 확인
   */
  boolean existsByUser(User user);

  /**
   * 사용자 ID로 장바구니 존재 여부 확인
   */
  @Query("SELECT COUNT(c) > 0 FROM Cart c WHERE c.user.id = :userId")
  boolean existsByUserId(@Param("userId") Long userId);

  /**
   * 사용자의 모든 장바구니 삭제
   */
  void deleteByUser(User user);
}
