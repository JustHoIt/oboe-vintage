package com.oboe.backend.cart.dto.response;

import com.oboe.backend.cart.dto.CartDto;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartSummaryResponse {

  private Integer totalItems;
  private BigDecimal totalPrice;
  private Integer itemCount;
  private BigDecimal estimatedDeliveryFee;
  private Boolean hasIssues;
  private Integer issueCount;

  public static CartSummaryResponse from(CartDto cartDto) {
    // 배송비는 5만원 이상 무료, 미만은 3000원
    BigDecimal deliveryFee = cartDto.getTotalPrice().compareTo(new BigDecimal("50000")) >= 0 
        ? BigDecimal.ZERO 
        : new BigDecimal("3000");

    // 이슈 개수 계산 (재고 부족, 가격 변경 등)
    int issueCount = (int) cartDto.getItems().stream()
        .filter(item -> !item.getIsStockAvailable() || item.getIsPriceChanged())
        .count();

    return CartSummaryResponse.builder()
        .totalItems(cartDto.getTotalItems())
        .totalPrice(cartDto.getTotalPrice())
        .itemCount(cartDto.getItemCount())
        .estimatedDeliveryFee(deliveryFee)
        .hasIssues(issueCount > 0)
        .issueCount(issueCount)
        .build();
  }
}

