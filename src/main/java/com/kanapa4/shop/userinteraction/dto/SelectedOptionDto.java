package com.kanapa4.shop.userinteraction.dto;

import java.math.BigDecimal;

public record SelectedOptionDto(
        String groupCode,
        String valueCode,
        String valueLabel,
        BigDecimal priceDelta) {
}
