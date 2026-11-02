package com.kanapa4.shop.userinteraction.dto;

import java.util.List;

public record RepriceResponse(
        CartResponse cart,
        List<PriceChangeDto> priceChanges) {

    public RepriceResponse {
        if (priceChanges == null) {
            priceChanges = List.of();
        }
    }
}
