package com.oboe.backend.cart.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.oboe.backend.cart.entity.Cart;
import com.oboe.backend.cart.repository.CartRepository;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import com.oboe.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Cart Repository 테스트")
class CartRepositoryTest {

  @Autowired
  private CartRepository cartRepository;

  @Autowired
  private UserRepository userRepository;

  private User user;
  private Cart cart;

  @BeforeEach
  void setUp() {
    // 테스트 데이터 초기화
    cartRepository.deleteAll();
    userRepository.deleteAll();

    // 테스트용 사용자 생성 및 저장
    user = User.builder()
        .email("test@example.com")
        .password("password123")
        .name("홍길동")
        .nickname("hong123")
        .phoneNumber("010-1234-5678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .build();
    user = userRepository.save(user);

    // 테스트용 장바구니 생성 및 저장
    cart = Cart.builder()
        .user(user)
        .totalItems(0)
        .totalPrice(BigDecimal.ZERO)
        .build();
    cart = cartRepository.save(cart);
  }

  @Test
  @DisplayName("사용자 ID로 장바구니 조회 테스트")
  void findByUserId() {
    // when
    Optional<Cart> foundCart = cartRepository.findByUserId(user.getId());

    // then
    assertThat(foundCart).isPresent();
    assertThat(foundCart.get().getUser()).isEqualTo(user);
  }

  @Test
  @DisplayName("사용자로 장바구니 조회 테스트")
  void findByUser() {
    // when
    Optional<Cart> foundCart = cartRepository.findByUser(user);

    // then
    assertThat(foundCart).isPresent();
    assertThat(foundCart.get()).isEqualTo(cart);
  }

  @Test
  @DisplayName("사용자의 장바구니 존재 여부 확인 테스트")
  void existsByUser() {
    // when
    boolean exists = cartRepository.existsByUser(user);

    // then
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("장바구니 저장 및 조회 테스트")
  void saveAndFindCart() {
    // given
    User newUser = User.builder()
        .email("new@example.com")
        .password("password123")
        .name("김철수")
        .nickname("kim123")
        .phoneNumber("010-9876-5432")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .build();

    Cart newCart = Cart.builder()
        .user(newUser)
        .totalItems(2)
        .totalPrice(new BigDecimal("300000"))
        .build();

    newUser = userRepository.save(newUser);

    // when
    Cart savedCart = cartRepository.save(newCart);

    Cart foundCart = cartRepository.findById(savedCart.getId()).orElse(null);

    // then
    assertThat(foundCart).isNotNull();
    assertThat(foundCart.getUser()).isEqualTo(newUser);
    assertThat(foundCart.getTotalItems()).isEqualTo(2);
    assertThat(foundCart.getTotalPrice()).isEqualByComparingTo(new BigDecimal("300000"));
  }

  @Test
  @DisplayName("모든 장바구니 조회 테스트")
  void findAllCarts() {
    // when
    var allCarts = cartRepository.findAll();

    // then
    assertThat(allCarts).hasSize(1);
    assertThat(allCarts).contains(cart);
  }

  @Test
  @DisplayName("장바구니 ID로 조회 테스트")
  void findById() {
    // when
    Optional<Cart> foundCart = cartRepository.findById(cart.getId());

    // then
    assertThat(foundCart).isPresent();
    assertThat(foundCart.get()).isEqualTo(cart);
  }

  @Test
  @DisplayName("존재하지 않는 ID로 조회 테스트")
  void findById_NonExistent() {
    // when
    Optional<Cart> foundCart = cartRepository.findById(999L);

    // then
    assertThat(foundCart).isEmpty();
  }

  @Test
  @DisplayName("장바구니 삭제 테스트")
  void deleteCart() {
    // given
    Long cartId = cart.getId();

    // when
    cartRepository.deleteById(cartId);

    // then
    assertThat(cartRepository.findById(cartId)).isEmpty();
    assertThat(cartRepository.findAll()).isEmpty();
  }

  @Test
  @DisplayName("장바구니 조회 테스트")
  void findByUserId_Success() {
    // when - 사용자 장바구니 조회
    Optional<Cart> foundCart = cartRepository.findByUserId(user.getId());

    // then - 장바구니가 조회됨
    assertThat(foundCart).isPresent();
    assertThat(foundCart.get().getUser()).isEqualTo(user);
    assertThat(foundCart.get().getTotalItems()).isEqualTo(0);
    assertThat(foundCart.get().getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("존재하지 않는 사용자 ID로 장바구니 조회")
  void findByUserId_NonExistentUser() {
    // when
    Optional<Cart> foundCart = cartRepository.findByUserId(999L);

    // then
    assertThat(foundCart).isEmpty();
  }

  @Test
  @DisplayName("장바구니 업데이트 테스트")
  void updateCart() {
    // given - 기존 장바구니 확인
    assertThat(cart.getTotalItems()).isEqualTo(0);
    assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    
    // when - 장바구니 비우기 (clear 메서드 테스트)
    cart.clear();
    Cart updatedCart = cartRepository.save(cart);

    // then - 장바구니가 비워졌는지 확인
    assertThat(updatedCart.isEmpty()).isTrue();
    assertThat(updatedCart.getTotalItems()).isEqualTo(0);
    assertThat(updatedCart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(updatedCart.getUser()).isEqualTo(user);
    assertThat(updatedCart.getId()).isEqualTo(cart.getId());
  }

}