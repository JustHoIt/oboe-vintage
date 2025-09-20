package com.oboe.backend.cart.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oboe.backend.cart.dto.request.CartItemRequest;
import com.oboe.backend.cart.dto.request.UpdateQuantityRequest;
import com.oboe.backend.cart.entity.Cart;
import com.oboe.backend.cart.entity.CartItem;
import com.oboe.backend.cart.repository.CartItemRepository;
import com.oboe.backend.cart.repository.CartRepository;
import com.oboe.backend.common.service.TokenProcessor;
import com.oboe.backend.product.entity.Condition;
import com.oboe.backend.product.entity.Product;
import com.oboe.backend.product.entity.ProductStatus;
import com.oboe.backend.product.repository.ProductRepository;
import com.oboe.backend.user.entity.SocialProvider;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.entity.UserRole;
import com.oboe.backend.user.entity.UserStatus;
import com.oboe.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@DisplayName("Cart Integration 테스트")
class CartIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private CartRepository cartRepository;

  @Autowired
  private CartItemRepository cartItemRepository;

  @MockBean
  private TokenProcessor tokenProcessor;

  private User testUser;
  private Product testProduct;
  private Product outOfStockProduct;
  private String bearerToken;

  @BeforeEach
  void setUp() {
    // 데이터 초기화
    cartItemRepository.deleteAll();
    cartRepository.deleteAll();
    productRepository.deleteAll();
    userRepository.deleteAll();

    // 테스트용 사용자 생성
    testUser = User.builder()
        .email("test@example.com")
        .password("password123")
        .name("홍길동")
        .nickname("hong123")
        .phoneNumber("010-1234-5678")
        .role(UserRole.USER)
        .status(UserStatus.ACTIVE)
        .socialProvider(SocialProvider.LOCAL)
        .build();
    testUser = userRepository.save(testUser);

    // 테스트용 상품 생성 (재고 충분)
    testProduct = Product.builder()
        .name("빈티지 데님 셔츠")
        .description("1980년대 빈티지 데님 셔츠")
        .price(new BigDecimal("150000"))
        .stockQuantity(10)
        .productStatus(ProductStatus.ACTIVE)
        .brand("리바이스")
        .condition(Condition.VERY_GOOD)
        .build();
    testProduct = productRepository.save(testProduct);

    // 재고 부족 상품
    outOfStockProduct = Product.builder()
        .name("품절 상품")
        .description("재고가 부족한 상품")
        .price(new BigDecimal("100000"))
        .stockQuantity(2)
        .productStatus(ProductStatus.ACTIVE)
        .brand("테스트")
        .condition(Condition.GOOD)
        .build();
    outOfStockProduct = productRepository.save(outOfStockProduct);

    bearerToken = "Bearer test-token";

    // TokenProcessor Mock 설정 - 더 구체적으로
    org.mockito.BDDMockito.given(tokenProcessor.extractEmailFromBearerToken(
            org.mockito.ArgumentMatchers.eq(bearerToken),
            org.mockito.ArgumentMatchers.anyString()))
        .willReturn(testUser.getEmail());

    // 모든 가능한 호출에 대해 Mock 설정
    org.mockito.BDDMockito.given(tokenProcessor.extractEmailFromBearerToken(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString()))
        .willReturn(testUser.getEmail());
  }

  @Nested
  @DisplayName("실제 HTTP API 통합 테스트")
  class HttpApiIntegrationTests {

    @Test
    @DisplayName("장바구니 전체 플로우: HTTP API → Service → Repository → Database")
    void fullCartWorkflowViaHttpApi() throws Exception {
      // 1. 빈 장바구니 조회 (GET /api/carts)
      mockMvc.perform(get("/api/v1/carts")
              .header("Authorization", bearerToken))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data.totalItems").value(0))
          .andExpect(jsonPath("$.data.items").isEmpty());

      // 2. 상품 추가 (POST /api/carts/items)
      CartItemRequest addRequest = CartItemRequest.builder()
          .productId(testProduct.getId())
          .quantity(3)
          .build();

      mockMvc.perform(post("/api/v1/carts/items")
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(addRequest)))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data.quantity").value(3))
          .andExpect(jsonPath("$.data.totalPrice").value(450000));

      // 3. 장바구니 조회 (상품 추가 후)
      mockMvc.perform(get("/api/v1/carts")
              .header("Authorization", bearerToken))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalItems").value(3))
          .andExpect(jsonPath("$.data.items").isArray())
          .andExpect(jsonPath("$.data.items[0].quantity").value(3));

      // 4. 데이터베이스에서 직접 확인
      Cart savedCart = cartRepository.findByUserId(testUser.getId()).orElseThrow();
      assertThat(savedCart.getCartItems()).hasSize(1);
      assertThat(savedCart.getTotalItems()).isEqualTo(3);

      // 5. 수량 변경 (PUT /api/carts/items/{id})
      CartItem cartItem = savedCart.getCartItems().get(0);
      UpdateQuantityRequest updateRequest = new UpdateQuantityRequest(7);

      mockMvc.perform(put("/api/v1/carts/items/" + cartItem.getId())
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateRequest)))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.quantity").value(7))
          .andExpect(jsonPath("$.data.totalPrice").value(1050000)); // 150000 * 7

      // 6. 아이템 제거 (DELETE /api/carts/items/{id})
      mockMvc.perform(delete("/api/v1/carts/items/" + cartItem.getId())
              .header("Authorization", bearerToken))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200));

      // 7. 장바구니 조회 (아이템 제거 후)
      mockMvc.perform(get("/api/v1/carts")
              .header("Authorization", bearerToken))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalItems").value(0))
          .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    @DisplayName("재고 검증 API 통합 테스트")
    void stockValidationApiIntegration() throws Exception {
      // 1. 정상 상품 추가 (재고 10개 중 5개)
      CartItemRequest normalRequest = CartItemRequest.builder()
          .productId(testProduct.getId())
          .quantity(5)
          .build();

      mockMvc.perform(post("/api/v1/carts/items")
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(normalRequest)))
          .andExpect(status().isOk());

      // 2. 같은 상품 추가로 재고 초과 시도 (409 Conflict 응답)
      CartItemRequest overStockRequest = CartItemRequest.builder()
          .productId(testProduct.getId())
          .quantity(6) // 기존 5개 + 새로 6개 = 11개, 재고는 10개
          .build();

      mockMvc.perform(post("/api/v1/carts/items")
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(overStockRequest)))
          .andDo(print())
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value(409));

      // 3. 재고 부족 상품 추가 시도 (409 Conflict 응답)
      CartItemRequest insufficientRequest = CartItemRequest.builder()
          .productId(outOfStockProduct.getId())
          .quantity(5) // 재고 2개인데 5개 요청
          .build();

      mockMvc.perform(post("/api/v1/carts/items")
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(insufficientRequest)))
          .andDo(print())
          .andExpect(status().isConflict());

      // 4. 장바구니 상태 확인 (정상 상품만 들어있어야 함)
      mockMvc.perform(get("/api/v1/carts")
              .header("Authorization", bearerToken))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalItems").value(5))
          .andExpect(jsonPath("$.data.items").isArray())
          .andExpect(jsonPath("$.data.items[0].productId").value(testProduct.getId()));
    }

    @Test
    @DisplayName("장바구니 유효성 검증 API 통합 테스트")
    void cartValidationApiIntegration() throws Exception {
      // 1. 상품을 장바구니에 추가
      CartItemRequest addRequest = CartItemRequest.builder()
          .productId(testProduct.getId())
          .quantity(5)
          .build();

      mockMvc.perform(post("/api/v1/carts/items")
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(addRequest)))
          .andExpect(status().isOk());

      // 2. 상품 재고를 인위적으로 줄임 (10개 → 3개)
      setStockQuantityForTest(testProduct, 3);
      testProduct = productRepository.save(testProduct);
      productRepository.flush(); // 강제로 DB에 반영

      // 3. 장바구니 유효성 검증 API 호출 (POST /api/carts/validate)
      mockMvc.perform(post("/api/v1/carts/validate")
              .header("Authorization", bearerToken))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data.items[0].isStockAvailable").value(false))
          .andExpect(jsonPath("$.data.items[0].warningMessage").exists());

      // 4. 데이터베이스에서 직접 확인
      Cart cart = cartRepository.findByUserId(testUser.getId()).orElseThrow();
      CartItem cartItem = cart.getCartItems().get(0);
      assertThat(cartItem.getQuantity()).isEqualTo(5);
      assertThat(cartItem.getProduct().getStockQuantity()).isEqualTo(3);
      assertThat(cartItem.isStockAvailable()).isFalse();
    }
  }

  @Nested
  @DisplayName("API 에러 응답 통합 테스트")
  class ApiErrorIntegrationTests {

    @Test
    @DisplayName("존재하지 않는 상품 추가 시 404 응답")
    void addNonExistentProduct_Returns404() throws Exception {
      CartItemRequest request = CartItemRequest.builder()
          .productId(999L)
          .quantity(1)
          .build();

      mockMvc.perform(post("/api/v1/carts/items")
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("재고 부족 상품 추가 시 409 응답")
    void addInsufficientStockProduct_Returns409() throws Exception {
      CartItemRequest request = CartItemRequest.builder()
          .productId(outOfStockProduct.getId())
          .quantity(10) // 재고 2개인데 10개 요청
          .build();

      mockMvc.perform(post("/api/v1/carts/items")
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    @DisplayName("잘못된 수량으로 상품 추가 시 400 응답")
    void addInvalidQuantityProduct_Returns400() throws Exception {
      CartItemRequest request = CartItemRequest.builder()
          .productId(testProduct.getId())
          .quantity(-1) // 음수 수량
          .build();

      mockMvc.perform(post("/api/v1/carts/items")
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("부족한 테스트 케이스 추가")
  class MissingTestCases {

    @Test
    @DisplayName("장바구니 요약 API 테스트")
    void getCartSummary_IntegrationTest() throws Exception {
      // 1. 여러 상품을 장바구니에 추가
      CartItemRequest request1 = CartItemRequest.builder()
          .productId(testProduct.getId())
          .quantity(3)
          .build();

      mockMvc.perform(post("/api/v1/carts/items")
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request1)))
          .andExpect(status().isOk());

      CartItemRequest request2 = CartItemRequest.builder()
          .productId(outOfStockProduct.getId())
          .quantity(1)
          .build();

      mockMvc.perform(post("/api/v1/carts/items")
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request2)))
          .andExpect(status().isOk());

      // 2. 장바구니 요약 조회 (GET /api/v1/carts/summary)
      mockMvc.perform(get("/api/v1/carts/summary")
              .header("Authorization", bearerToken))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data.totalItems").value(4)) // 3 + 1
          .andExpect(jsonPath("$.data.totalPrice").value(550000)) // (150000*3) + (100000*1)
          .andExpect(jsonPath("$.data.itemCount").value(2));
    }

    @Test
    @DisplayName("대량 수량 변경 테스트 - 경계값")
    void updateQuantity_LargeAmount() throws Exception {
      // 1. 상품 추가
      CartItemRequest addRequest = CartItemRequest.builder()
          .productId(testProduct.getId())
          .quantity(1)
          .build();

      mockMvc.perform(post("/api/v1/carts/items")
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(addRequest)))
          .andExpect(status().isOk());

      // 2. CartItem ID 조회
      Cart cart = cartRepository.findByUserId(testUser.getId()).orElseThrow();
      CartItem cartItem = cart.getCartItems().get(0);

      // 3. 재고 한계까지 수량 변경 (재고 10개)
      UpdateQuantityRequest updateRequest = new UpdateQuantityRequest(10);

      mockMvc.perform(put("/api/v1/carts/items/" + cartItem.getId())
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateRequest)))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.quantity").value(10))
          .andExpect(jsonPath("$.data.totalPrice").value(1500000)); // 150000 * 10
    }

    @Test
    @DisplayName("연속된 상품 추가 및 제거 테스트")
    void continuousAddAndRemove() throws Exception {
      // 1. 첫 번째 상품 추가
      CartItemRequest request1 = CartItemRequest.builder()
          .productId(testProduct.getId())
          .quantity(2)
          .build();

      mockMvc.perform(post("/api/v1/carts/items")
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request1)))
          .andExpect(status().isOk());

      // 2. 두 번째 상품 추가
      CartItemRequest request2 = CartItemRequest.builder()
          .productId(outOfStockProduct.getId())
          .quantity(1)
          .build();

      mockMvc.perform(post("/api/v1/carts/items")
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request2)))
          .andExpect(status().isOk());

      // 3. 장바구니 확인 (2개 아이템)
      mockMvc.perform(get("/api/v1/carts")
              .header("Authorization", bearerToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.itemCount").value(2));

      // 4. 첫 번째 아이템 제거
      Cart cart = cartRepository.findByUserId(testUser.getId()).orElseThrow();
      CartItem firstItem = cart.getCartItems().stream()
          .filter(item -> item.getProduct().getId().equals(testProduct.getId()))
          .findFirst().orElseThrow();

      mockMvc.perform(delete("/api/v1/carts/items/" + firstItem.getId())
              .header("Authorization", bearerToken))
          .andExpect(status().isOk());

      // 5. 장바구니 확인 (1개 아이템 남음)
      mockMvc.perform(get("/api/v1/carts")
              .header("Authorization", bearerToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.itemCount").value(1))
          .andExpect(jsonPath("$.data.totalItems").value(1));
    }

    @Test
    @DisplayName("빈 장바구니 요약 조회 테스트")
    void getCartSummary_EmptyCart() throws Exception {
      // 빈 장바구니 요약 조회
      mockMvc.perform(get("/api/v1/carts/summary")
              .header("Authorization", bearerToken))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data.totalItems").value(0))
          .andExpect(jsonPath("$.data.totalPrice").value(0))
          .andExpect(jsonPath("$.data.itemCount").value(0));
    }
  }

  @Nested
  @DisplayName("데이터 일관성 통합 테스트")
  class DataConsistencyIntegrationTests {

    @Test
    @DisplayName("HTTP API를 통한 장바구니 총액 계산 일관성")
    void cartTotalPriceConsistencyViaApi() throws Exception {
      // 1. 첫 번째 상품 추가
      CartItemRequest request1 = CartItemRequest.builder()
          .productId(testProduct.getId())
          .quantity(3)
          .build();

      mockMvc.perform(post("/api/v1/carts/items")
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request1)))
          .andExpect(status().isOk());

      // 2. 두 번째 상품 추가
      CartItemRequest request2 = CartItemRequest.builder()
          .productId(outOfStockProduct.getId())
          .quantity(2)
          .build();

      mockMvc.perform(post("/api/v1/carts/items")
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request2)))
          .andExpect(status().isOk());

      // 3. 장바구니 조회로 총액 확인
      mockMvc.perform(get("/api/v1/carts")
              .header("Authorization", bearerToken))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalItems").value(5))
          .andExpect(jsonPath("$.data.totalPrice").value(650000)); // (150000*3) + (100000*2)

      // 4. 데이터베이스에서 직접 확인
      Cart savedCart = cartRepository.findByUserId(testUser.getId()).orElseThrow();
      assertThat(savedCart.getTotalItems()).isEqualTo(5);
      assertThat(savedCart.getTotalPrice()).isEqualByComparingTo(new BigDecimal("650000"));
      assertThat(savedCart.getCartItems()).hasSize(2);
    }

    @Test
    @DisplayName("수량 변경 후 총액 재계산 HTTP API 통합")
    void quantityUpdateRecalculationViaApi() throws Exception {
      // 1. 상품 추가
      CartItemRequest addRequest = CartItemRequest.builder()
          .productId(testProduct.getId())
          .quantity(2)
          .build();

      mockMvc.perform(post("/api/v1/carts/items")
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(addRequest)))
          .andExpect(status().isOk());

      // 2. 데이터베이스에서 CartItem ID 조회
      cartRepository.flush();
      Cart cart = cartRepository.findByUserId(testUser.getId()).orElseThrow();
      CartItem cartItem = cart.getCartItems().get(0);

      // 3. 수량 변경 API 호출
      UpdateQuantityRequest updateRequest = new UpdateQuantityRequest(8);

      mockMvc.perform(put("/api/v1/carts/items/" + cartItem.getId())
              .header("Authorization", bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateRequest)))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.quantity").value(8))
          .andExpect(jsonPath("$.data.totalPrice").value(1200000)); // 150000 * 8

      // 4. 장바구니 전체 조회로 총액 확인
      mockMvc.perform(get("/api/v1/carts")
              .header("Authorization", bearerToken))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalItems").value(8))
          .andExpect(jsonPath("$.data.totalPrice").value(1200000));

      // 5. 데이터베이스 일관성 확인
      Cart updatedCart = cartRepository.findByUserId(testUser.getId()).orElseThrow();
      assertThat(updatedCart.getTotalItems()).isEqualTo(8);
      assertThat(updatedCart.getTotalPrice()).isEqualByComparingTo(new BigDecimal("1200000"));
    }
  }

  private void setStockQuantityForTest(Product product, Integer stockQuantity) {
    try {
      java.lang.reflect.Field stockQuantityField = Product.class.getDeclaredField("stockQuantity");
      stockQuantityField.setAccessible(true);
      stockQuantityField.set(product, stockQuantity);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set stock quantity for test", e);
    }
  }
}
