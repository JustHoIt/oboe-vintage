package com.oboe.backend.cart.repository;

import com.oboe.backend.cart.entity.CartItem;
import com.oboe.backend.product.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

  /**
   * 장바구니 ID로 장바구니 아이템 목록 조회
   */
  List<CartItem> findByCartIdOrderByCreatedAtAsc(Long cartId);

  /**
   * 장바구니와 상품으로 장바구니 아이템 조회
   */
  Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

  /**
   * 상품으로 장바구니 아이템 목록 조회 (해당 상품이 담긴 모든 장바구니)
   */
  List<CartItem> findByProduct(Product product);

  /**
   * 특정 상품의 장바구니 아이템 개수 조회
   */
  @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.product.id = :productId")
  Long countByProductId(@Param("productId") Long productId);

  /**
   * 사용자 ID로 장바구니 아이템 목록 조회
   */
  @Query("SELECT ci FROM CartItem ci " +
      "JOIN ci.cart c " +
      "WHERE c.user.id = :userId " +
      "ORDER BY ci.createdAt ASC")
  List<CartItem> findByUserId(@Param("userId") Long userId);

  /**
   * 장바구니 ID로 장바구니 아이템 개수 조회
   */
  Long countByCartId(Long cartId);

  /**
   * 특정 상품이 포함된 장바구니 아이템 삭제
   */
  void deleteByProductId(Long productId);

  /**
   * 장바구니의 모든 아이템 삭제
   */
  void deleteByCartId(Long cartId);

  /**
   * 특정 장바구니와 상품의 아이템 삭제
   */
  void deleteByCartIdAndProductId(Long cartId, Long productId);

  /**
   * 가격이 변경된 장바구니 아이템 목록 조회
   */
  @Query("SELECT ci FROM CartItem ci " +
      "WHERE ci.cart.id = :cartId " +
      "AND ci.unitPrice != ci.product.price")
  List<CartItem> findPriceChangedItemsByCartId(@Param("cartId") Long cartId);

  /**
   * 재고가 부족한 장바구니 아이템 목록 조회
   */
  @Query("SELECT ci FROM CartItem ci " +
      "WHERE ci.cart.id = :cartId " +
      "AND ci.quantity > ci.product.stockQuantity")
  List<CartItem> findStockShortageItemsByCartId(@Param("cartId") Long cartId);

  /**
   * 판매 중지된 상품의 장바구니 아이템 목록 조회
   */
  @Query("SELECT ci FROM CartItem ci " +
      "WHERE ci.cart.id = :cartId " +
      "AND ci.product.productStatus != 'ACTIVE'")
  List<CartItem> findUnavailableProductItemsByCartId(@Param("cartId") Long cartId);
}
