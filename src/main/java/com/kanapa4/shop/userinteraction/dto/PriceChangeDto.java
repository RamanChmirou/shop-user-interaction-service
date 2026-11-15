package com.kanapa4.shop.userinteraction.dto;

import java.util.UUID;

public record PriceChangeDto(
        UUID itemId,
        String productName,
        PriceDto oldPrice,
        PriceDto newPrice) {
}
