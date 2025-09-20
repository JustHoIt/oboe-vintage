package com.oboe.backend.cart.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oboe.backend.cart.dto.CartDto;
import com.oboe.backend.cart.dto.CartItemDto;
import com.oboe.backend.cart.dto.request.CartItemRequest;
import com.oboe.backend.cart.dto.request.UpdateQuantityRequest;
import com.oboe.backend.cart.dto.response.CartSummaryResponse;
import com.oboe.backend.cart.service.CartService;
import com.oboe.backend.common.exception.CustomException;
import com.oboe.backend.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@DisplayName("CartController 테스트")
class CartControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private CartService cartService;

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_TOKEN = "Bearer valid-token";

  @Test
  @DisplayName("장바구니 조회 성공")
  void getCart_Success() throws Exception {
    // given
    List<CartItemDto> items = new ArrayList<>();
    items.add(CartItemDto.builder()
        .cartItemId(1L)
        .productId(1L)
        .productName("빈티지 데님 셔츠")
        .quantity(2)
        .unitPrice(new BigDecimal("150000"))
        .totalPrice(new BigDecimal("300000"))
        .brand("리바이스")
        .condition("VERY_GOOD")
        .isStockAvailable(true)
        .isPriceChanged(false)
        .build());

    CartDto cartDto = CartDto.builder()
        .cartId(1L)
        .totalItems(2)
        .totalPrice(new BigDecimal("300000"))
        .itemCount(1)
        .items(items)
        .build();

    given(cartService.getCartByUser(BEARER_TOKEN)).willReturn(cartDto);

    // when & then
    mockMvc.perform(get("/api/v1/carts")
            .header(AUTHORIZATION_HEADER, BEARER_TOKEN))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.message").value("장바구니 조회 성공"))
        .andExpect(jsonPath("$.data.cartId").value(1L))
        .andExpect(jsonPath("$.data.totalItems").value(2))
        .andExpect(jsonPath("$.data.totalPrice").value(300000))
        .andExpect(jsonPath("$.data.itemCount").value(1))
        .andExpect(jsonPath("$.data.items").isArray())
        .andExpect(jsonPath("$.data.items[0].cartItemId").value(1L))
        .andExpect(jsonPath("$.data.items[0].productName").value("빈티지 데님 셔츠"));
  }

  @Test
  @DisplayName("유효하지 않은 사용자로 장바구니 조회 실패")
  void getCart_InvalidUser_Failure() throws Exception {
    // given
    given(cartService.getCartByUser(BEARER_TOKEN))
        .willThrow(new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다"));

    // when & then
    mockMvc.perform(get("/api/v1/carts")
            .header(AUTHORIZATION_HEADER, BEARER_TOKEN))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("장바구니에 상품 추가 성공")
  void addCartItem_Success() throws Exception {
    // given
    CartItemRequest request = CartItemRequest.builder()
        .productId(1L)
        .quantity(3)
        .build();

    CartItemDto cartItemDto = CartItemDto.builder()
        .cartItemId(1L)
        .productId(1L)
        .productName("빈티지 데님 셔츠")
        .quantity(3)
        .unitPrice(new BigDecimal("150000"))
        .totalPrice(new BigDecimal("450000"))
        .brand("리바이스")
        .condition("VERY_GOOD")
        .isStockAvailable(true)
        .isPriceChanged(false)
        .build();

    given(cartService.addCartItem(anyString(), any(CartItemRequest.class)))
        .willReturn(cartItemDto);

    // when & then
    mockMvc.perform(post("/api/v1/carts/items")
            .header(AUTHORIZATION_HEADER, BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.message").value("상품이 장바구니에 추가되었습니다"))
        .andExpect(jsonPath("$.data.cartItemId").value(1L))
        .andExpect(jsonPath("$.data.productId").value(1L))
        .andExpect(jsonPath("$.data.quantity").value(3))
        .andExpect(jsonPath("$.data.unitPrice").value(150000))
        .andExpect(jsonPath("$.data.totalPrice").value(450000));
  }

  @Test
  @DisplayName("재고 부족으로 상품 추가 실패")
  void addCartItem_InsufficientStock_Failure() throws Exception {
    // given
    CartItemRequest request = CartItemRequest.builder()
        .productId(1L)
        .quantity(10)
        .build();

    given(cartService.addCartItem(anyString(), any(CartItemRequest.class)))
        .willThrow(new CustomException(ErrorCode.CART_INSUFFICIENT_STOCK, "재고가 부족합니다"));

    // when & then
    mockMvc.perform(post("/api/v1/carts/items")
            .header(AUTHORIZATION_HEADER, BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("판매 중지된 상품 추가 실패")
  void addCartItem_ProductUnavailable_Failure() throws Exception {
    // given
    CartItemRequest request = CartItemRequest.builder()
        .productId(1L)
        .quantity(1)
        .build();

    given(cartService.addCartItem(anyString(), any(CartItemRequest.class)))
        .willThrow(new CustomException(ErrorCode.CART_PRODUCT_UNAVAILABLE, "판매 중지된 상품입니다"));

    // when & then
    mockMvc.perform(post("/api/v1/carts/items")
            .header(AUTHORIZATION_HEADER, BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("존재하지 않는 상품 추가 실패")
  void addCartItem_ProductNotFound_Failure() throws Exception {
    // given
    CartItemRequest request = CartItemRequest.builder()
        .productId(999L)
        .quantity(1)
        .build();

    given(cartService.addCartItem(anyString(), any(CartItemRequest.class)))
        .willThrow(new CustomException(ErrorCode.PRODUCT_NOT_FOUND, "상품을 찾을 수 없습니다"));

    // when & then
    mockMvc.perform(post("/api/v1/carts/items")
            .header(AUTHORIZATION_HEADER, BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("장바구니 아이템 수량 변경 성공")
  void updateCartItemQuantity_Success() throws Exception {
    // given
    UpdateQuantityRequest request = new UpdateQuantityRequest(5);

    CartItemDto cartItemDto = CartItemDto.builder()
        .cartItemId(1L)
        .productId(1L)
        .productName("빈티지 데님 셔츠")
        .quantity(5)
        .unitPrice(new BigDecimal("150000"))
        .totalPrice(new BigDecimal("750000"))
        .brand("리바이스")
        .condition("VERY_GOOD")
        .isStockAvailable(true)
        .isPriceChanged(false)
        .build();

    given(cartService.updateCartItemQuantity(anyString(), anyLong(), any(Integer.class)))
        .willReturn(cartItemDto);

    // when & then
    mockMvc.perform(put("/api/v1/carts/items/1")
            .header(AUTHORIZATION_HEADER, BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.message").value("수량이 변경되었습니다"))
        .andExpect(jsonPath("$.data.quantity").value(5))
        .andExpect(jsonPath("$.data.totalPrice").value(750000));
  }

  @Test
  @DisplayName("재고 부족으로 수량 변경 실패")
  void updateCartItemQuantity_InsufficientStock_Failure() throws Exception {
    // given
    UpdateQuantityRequest request = new UpdateQuantityRequest(15);

    given(cartService.updateCartItemQuantity(anyString(), anyLong(), any(Integer.class)))
        .willThrow(new CustomException(ErrorCode.CART_INSUFFICIENT_STOCK, "재고가 부족합니다"));

    // when & then
    mockMvc.perform(put("/api/v1/carts/items/1")
            .header(AUTHORIZATION_HEADER, BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("장바구니 아이템 제거 성공")
  void removeCartItem_Success() throws Exception {
    // when & then
    mockMvc.perform(delete("/api/v1/carts/items/1")
            .header(AUTHORIZATION_HEADER, BEARER_TOKEN))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.message").value("상품이 장바구니에서 제거되었습니다"));
  }

  @Test
  @DisplayName("존재하지 않는 장바구니 아이템 제거 실패")
  void removeCartItem_ItemNotFound_Failure() throws Exception {
    // given
    willThrow(new CustomException(ErrorCode.CART_ITEM_NOT_FOUND, "장바구니 아이템을 찾을 수 없습니다"))
        .given(cartService).removeCartItem(anyString(), anyLong());

    // when & then
    mockMvc.perform(delete("/api/v1/carts/items/999")
            .header(AUTHORIZATION_HEADER, BEARER_TOKEN))
        .andDo(print())
        .andExpect(status().isNotFound());
  }


  @Test
  @DisplayName("장바구니 비우기 성공")
  void clearCart_Success() throws Exception {
    // when & then
    mockMvc.perform(delete("/api/v1/carts/items")
            .header(AUTHORIZATION_HEADER, BEARER_TOKEN))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.message").value("장바구니가 비워졌습니다"));
  }


  @Test
  @DisplayName("장바구니 요약 조회 성공")
  void getCartSummary_Success() throws Exception {
    // given
    CartSummaryResponse summaryResponse = CartSummaryResponse.builder()
        .totalItems(5)
        .totalPrice(new BigDecimal("500000"))
        .itemCount(3)
        .build();

    given(cartService.getCartSummary(BEARER_TOKEN)).willReturn(summaryResponse);

    // when & then
    mockMvc.perform(get("/api/v1/carts/summary")
            .header(AUTHORIZATION_HEADER, BEARER_TOKEN))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.message").value("장바구니 요약 조회 성공"))
        .andExpect(jsonPath("$.data.totalItems").value(5))
        .andExpect(jsonPath("$.data.totalPrice").value(500000))
        .andExpect(jsonPath("$.data.itemCount").value(3));
  }

  @Test
  @DisplayName("장바구니 유효성 검증 성공 - 재고 부족 경고 포함")
  void validateCart_WithStockWarning_Success() throws Exception {
    // given
    List<CartItemDto> items = new ArrayList<>();
    items.add(CartItemDto.builder()
        .cartItemId(1L)
        .productId(1L)
        .productName("빈티지 데님 셔츠")
        .quantity(2)
        .unitPrice(new BigDecimal("150000"))
        .totalPrice(new BigDecimal("300000"))
        .brand("리바이스")
        .condition("VERY_GOOD")
        .isStockAvailable(true)
        .isPriceChanged(false)
        .build());

    items.add(CartItemDto.builder()
        .cartItemId(2L)
        .productId(2L)
        .productName("재고 부족 상품")
        .quantity(5)
        .unitPrice(new BigDecimal("100000"))
        .totalPrice(new BigDecimal("500000"))
        .brand("테스트")
        .condition("GOOD")
        .isStockAvailable(false)
        .isPriceChanged(false)
        .warningMessage("재고가 부족합니다. 현재 재고: 2개, 요청 수량: 5개")
        .build());

    CartDto cartDto = CartDto.builder()
        .cartId(1L)
        .totalItems(7)
        .totalPrice(new BigDecimal("800000"))
        .itemCount(2)
        .items(items)
        .build();

    given(cartService.validateCart(BEARER_TOKEN)).willReturn(cartDto);

    // when & then
    mockMvc.perform(post("/api/v1/carts/validate")
            .header(AUTHORIZATION_HEADER, BEARER_TOKEN))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.message").value("장바구니 유효성 검증 완료"))
        .andExpect(jsonPath("$.data.items").isArray())
        .andExpect(jsonPath("$.data.items[0].isStockAvailable").value(true))
        .andExpect(jsonPath("$.data.items[1].isStockAvailable").value(false))
        .andExpect(
            jsonPath("$.data.items[1].warningMessage").value("재고가 부족합니다. 현재 재고: 2개, 요청 수량: 5개"));
  }

  @Test
  @DisplayName("다른 사용자의 장바구니 아이템 수량 변경 시도 - 403 Forbidden")
  void updateCartItemQuantity_AccessDenied() throws Exception {
    // given
    UpdateQuantityRequest request = new UpdateQuantityRequest(3);

    given(cartService.updateCartItemQuantity(anyString(), anyLong(), any(Integer.class)))
        .willThrow(new CustomException(ErrorCode.CART_ACCESS_DENIED, "장바구니 접근 권한이 없습니다"));

    // when & then
    mockMvc.perform(put("/api/v1/carts/items/1")
            .header(AUTHORIZATION_HEADER, BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(403));
  }

  @Test
  @DisplayName("재고 정보가 없는 상품 추가 시도 - 400 Bad Request")
  void addCartItem_InvalidStock() throws Exception {
    // given
    CartItemRequest request = CartItemRequest.builder()
        .productId(1L)
        .quantity(1)
        .build();

    given(cartService.addCartItem(anyString(), any(CartItemRequest.class)))
        .willThrow(new CustomException(ErrorCode.PRODUCT_INVALID_STOCK, "상품 재고 정보가 없습니다"));

    // when & then
    mockMvc.perform(post("/api/v1/carts/items")
            .header(AUTHORIZATION_HEADER, BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
  }
}
