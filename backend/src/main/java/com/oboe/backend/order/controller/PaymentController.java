package com.oboe.backend.order.controller;

import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.order.dto.request.SaveAmountRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "결제", description = "결제 관련 API")
public class PaymentController {

  /**
   * 결제 금액을 세션에 임시 저장
   */
  @PostMapping("/save-amount")
  @Operation(summary = "결제 금액 세션 저장", description = "결제 과정에서 악의적인 금액 변경을 방지하기 위해 금액을 세션에 임시 저장합니다.")
  @ApiResponse(responseCode = "200", description = "결제 금액 저장 성공")
  @ApiResponse(responseCode = "400", description = "잘못된 요청")
  public ResponseEntity<ResponseDto<String>> savePaymentAmount(
      HttpSession session,
      @Valid @RequestBody SaveAmountRequest request) {
    
    log.info("결제 금액 세션 저장 요청 - OrderId: {}, Amount: {}", request.getOrderId(), request.getAmount());
    
    // 세션에 orderId와 amount 저장
    session.setAttribute(request.getOrderId(), request.getAmount());
    
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(ResponseDto.success("결제 금액이 세션에 저장되었습니다"));
  }

  /**
   * 결제 금액 검증
   */
  @PostMapping("/verify-amount")
  @Operation(summary = "결제 금액 검증", description = "세션에 저장된 금액과 현재 요청된 금액이 일치하는지 검증합니다.")
  @ApiResponse(responseCode = "200", description = "결제 금액 검증 성공")
  @ApiResponse(responseCode = "400", description = "결제 금액 불일치")
  public ResponseEntity<ResponseDto<String>> verifyPaymentAmount(
      HttpSession session,
      @Valid @RequestBody SaveAmountRequest request) {
    
    log.info("결제 금액 검증 요청 - OrderId: {}, Amount: {}", request.getOrderId(), request.getAmount());
    
    // 세션에서 저장된 금액 조회
    Long savedAmount = (Long) session.getAttribute(request.getOrderId());
    
    // 세션이 없거나 금액이 일치하지 않는 경우
    if (savedAmount == null || !savedAmount.equals(request.getAmount())) {
      return ResponseEntity.badRequest()
          .contentType(MediaType.APPLICATION_JSON)
          .body(ResponseDto.error(400, "결제 금액이 일치하지 않습니다"));
    }
    
    // 검증 성공 - 세션에서 제거
    session.removeAttribute(request.getOrderId());
    
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(ResponseDto.success("결제 금액 검증이 완료되었습니다"));
  }
}
