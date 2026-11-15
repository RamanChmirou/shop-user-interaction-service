package com.kanapa4.shop.userinteraction.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID userId,
        String status,
        List<CartItemResponse> items,
        int itemCount,
        BigDecimal totalAmount,
        String currency) {

    public CartResponse {
        if (items == null) {
            items = List.of();
        }
    }
}
