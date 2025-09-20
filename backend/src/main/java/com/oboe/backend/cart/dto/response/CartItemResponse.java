package com.oboe.backend.cart.dto.response;

import com.oboe.backend.cart.dto.CartItemDto;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {

  private Long cartItemId;
  private Long productId;
  private String productName;
  private String productImage;
  private Integer quantity;
  private BigDecimal unitPrice;
  private BigDecimal totalPrice;
  private String brand;
  private String condition;
  private Boolean isStockAvailable;
  private Boolean isPriceChanged;
  private String warningMessage;

  public static CartItemResponse from(CartItemDto cartItemDto) {
    return CartItemResponse.builder()
        .cartItemId(cartItemDto.getCartItemId())
        .productId(cartItemDto.getProductId())
        .productName(cartItemDto.getProductName())
        .productImage(cartItemDto.getProductImage())
        .quantity(cartItemDto.getQuantity())
        .unitPrice(cartItemDto.getUnitPrice())
        .totalPrice(cartItemDto.getTotalPrice())
        .brand(cartItemDto.getBrand())
        .condition(cartItemDto.getCondition())
        .isStockAvailable(cartItemDto.getIsStockAvailable())
        .isPriceChanged(cartItemDto.getIsPriceChanged())
        .warningMessage(cartItemDto.getWarningMessage())
        .build();
  }
}

