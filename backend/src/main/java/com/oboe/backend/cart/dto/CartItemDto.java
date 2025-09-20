package com.oboe.backend.cart.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDto {

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
}

