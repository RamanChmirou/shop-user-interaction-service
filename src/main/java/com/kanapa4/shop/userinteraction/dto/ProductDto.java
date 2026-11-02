package com.kanapa4.shop.userinteraction.dto;

import java.util.Map;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        String description,
        ProductType type,
        PriceDto price,
        Map<String, String> attributes,
        boolean active) {
}
