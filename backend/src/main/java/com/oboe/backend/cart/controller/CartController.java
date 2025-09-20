package com.oboe.backend.cart.controller;

import com.oboe.backend.cart.dto.request.CartItemRequest;
import com.oboe.backend.cart.dto.request.UpdateQuantityRequest;
import com.oboe.backend.cart.dto.response.CartItemResponse;
import com.oboe.backend.cart.dto.response.CartResponse;
import com.oboe.backend.cart.dto.response.CartSummaryResponse;
import com.oboe.backend.cart.service.CartService;
import com.oboe.backend.common.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "장바구니", description = "장바구니 관리 API")
public class CartController {

  private final CartService cartService;

  /**
   * 사용자의 장바구니 조회
   */
  @Operation(summary = "장바구니 조회", description = "사용자의 장바구니 정보를 조회합니다")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증 필요"),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @GetMapping
  public ResponseEntity<ResponseDto<CartResponse>> getCart(
      @Parameter(description = "JWT 토큰", required = true)
      @RequestHeader("Authorization") String authorization) {
    CartResponse cartResponse = CartResponse.from(cartService.getCartByUser(authorization));

    return ResponseEntity.ok(ResponseDto.success("장바구니 조회 성공", cartResponse));
  }

  /**
   * 장바구니에 상품 추가
   */
  @Operation(summary = "상품 추가", description = "장바구니에 상품을 추가합니다")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "추가 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (수량이 0 이하 등)"),
      @ApiResponse(responseCode = "401", description = "인증 필요"),
      @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
      @ApiResponse(responseCode = "409", description = "재고 부족, 판매 중지된 상품, 또는 재고 정보 없음"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @PostMapping("/items")
  public ResponseEntity<ResponseDto<CartItemResponse>> addCartItem(
      @Parameter(description = "JWT 토큰", required = true)
      @RequestHeader("Authorization") String authorization,
      @Valid @RequestBody CartItemRequest request) {
    CartItemResponse cartItemResponse = CartItemResponse.from(
        cartService.addCartItem(authorization, request));

    return ResponseEntity.ok(ResponseDto.success("상품이 장바구니에 추가되었습니다", cartItemResponse));
  }

  /**
   * 장바구니 아이템 수량 변경
   */
  @Operation(summary = "수량 변경", description = "장바구니 아이템의 수량을 변경합니다")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "변경 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (수량이 0 이하 등)"),
      @ApiResponse(responseCode = "401", description = "인증 필요"),
      @ApiResponse(responseCode = "403", description = "장바구니 접근 권한 없음"),
      @ApiResponse(responseCode = "404", description = "장바구니 아이템 또는 상품을 찾을 수 없음"),
      @ApiResponse(responseCode = "409", description = "재고 부족, 판매 중지된 상품, 또는 재고 정보 없음"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @PutMapping("/items/{itemId}")
  public ResponseEntity<ResponseDto<CartItemResponse>> updateCartItemQuantity(
      @Parameter(description = "JWT 토큰", required = true)
      @RequestHeader("Authorization") String authorization,
      @Parameter(description = "장바구니 아이템 ID", required = true)
      @PathVariable Long itemId,
      @RequestBody UpdateQuantityRequest request) {

    log.info("장바구니 아이템 수량 변경 요청 - 아이템 ID: {}, 새 수량: {}",
        itemId, request.getQuantity());

    CartItemResponse cartItemResponse = CartItemResponse.from(
        cartService.updateCartItemQuantity(authorization, itemId, request.getQuantity()));

    return ResponseEntity.ok(ResponseDto.success("수량이 변경되었습니다", cartItemResponse));
  }

  /**
   * 장바구니에서 아이템 제거
   */
  @Operation(summary = "아이템 제거", description = "장바구니에서 특정 아이템을 제거합니다")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "제거 성공"),
      @ApiResponse(responseCode = "401", description = "인증 필요"),
      @ApiResponse(responseCode = "403", description = "장바구니 접근 권한 없음"),
      @ApiResponse(responseCode = "404", description = "장바구니 아이템을 찾을 수 없음"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @DeleteMapping("/items/{itemId}")
  public ResponseEntity<ResponseDto<Void>> removeCartItem(
      @Parameter(description = "JWT 토큰", required = true)
      @RequestHeader("Authorization") String authorization,
      @Parameter(description = "장바구니 아이템 ID", required = true)
      @PathVariable Long itemId) {

    log.info("장바구니 아이템 제거 요청 - 아이템 ID: {}", itemId);

    cartService.removeCartItem(authorization, itemId);

    return ResponseEntity.ok(ResponseDto.success("상품이 장바구니에서 제거되었습니다", null));
  }

  /**
   * 장바구니 전체 비우기
   */
  @Operation(summary = "장바구니 비우기", description = "장바구니의 모든 아이템을 제거합니다")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "비우기 성공"),
      @ApiResponse(responseCode = "401", description = "인증 필요"),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @DeleteMapping("/items")
  public ResponseEntity<ResponseDto<Void>> clearCart(
      @Parameter(description = "JWT 토큰", required = true)
      @RequestHeader("Authorization") String authorization) {
    cartService.clearCart(authorization);

    return ResponseEntity.ok(ResponseDto.success("장바구니가 비워졌습니다", null));
  }

  /**
   * 장바구니 요약 정보 조회
   */
  @Operation(summary = "장바구니 요약", description = "장바구니의 요약 정보를 조회합니다")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증 필요"),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @GetMapping("/summary")
  public ResponseEntity<ResponseDto<CartSummaryResponse>> getCartSummary(
      @Parameter(description = "JWT 토큰", required = true)
      @RequestHeader("Authorization") String authorization) {
    CartSummaryResponse summaryResponse = cartService.getCartSummary(authorization);

    return ResponseEntity.ok(ResponseDto.success("장바구니 요약 조회 성공", summaryResponse));
  }

  /**
   * 장바구니 유효성 검증
   */
  @Operation(summary = "유효성 검증", description = "장바구니의 상품 재고 및 가격을 검증합니다 (재고/가격 변경 경고 포함)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "검증 완료 (재고 부족이나 가격 변경이 있어도 경고만 표시)"),
      @ApiResponse(responseCode = "401", description = "인증 필요"),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @PostMapping("/validate")
  public ResponseEntity<ResponseDto<CartResponse>> validateCart(
      @Parameter(description = "JWT 토큰", required = true)
      @RequestHeader("Authorization") String authorization) {
    CartResponse cartResponse = CartResponse.from(cartService.validateCart(authorization));

    return ResponseEntity.ok(ResponseDto.success("장바구니 유효성 검증 완료", cartResponse));
  }
}
