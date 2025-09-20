package com.oboe.backend.cart.dto.response;

import com.oboe.backend.cart.dto.CartDto;
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
public class CartResponse {

  private Long cartId;
  private Integer totalItems;
  private BigDecimal totalPrice;
  private Integer itemCount;
  private List<CartItemResponse> items;

  public static CartResponse from(CartDto cartDto) {
    List<CartItemResponse> itemResponses = cartDto.getItems().stream()
        .map(CartItemResponse::from)
        .toList();

    return CartResponse.builder()
        .cartId(cartDto.getCartId())
        .totalItems(cartDto.getTotalItems())
        .totalPrice(cartDto.getTotalPrice())
        .itemCount(cartDto.getItemCount())
        .items(itemResponses)
        .build();
  }
}

