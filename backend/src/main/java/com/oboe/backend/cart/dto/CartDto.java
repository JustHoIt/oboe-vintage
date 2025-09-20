package com.oboe.backend.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDto {

  private Long cartId;
  private Integer totalItems;
  private BigDecimal totalPrice;
  private Integer itemCount;
  private List<CartItemDto> items;
}

