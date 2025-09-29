package com.oboe.backend.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 금액을 세션에 임시 저장하기 위한 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "결제 금액 세션 저장 요청 DTO")
public class SaveAmountRequest {

    @NotBlank(message = "주문 ID는 필수입니다")
    @Schema(description = "주문 ID", example = "ORDER-123", required = true)
    private String orderId;
    
    @NotNull(message = "결제 금액은 필수입니다")
    @Positive(message = "결제 금액은 양수여야 합니다")
    @Schema(description = "결제 금액", example = "50000", required = true)
    private Long amount;
}