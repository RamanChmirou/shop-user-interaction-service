package com.kanapa4.shop.userinteraction.dto;

import java.util.UUID;

public record ProductSummaryDto(
        UUID id,
        String name,
        ProductType type,
        PriceDto price) {
}
