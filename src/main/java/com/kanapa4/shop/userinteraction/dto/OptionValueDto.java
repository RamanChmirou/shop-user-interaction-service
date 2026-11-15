package com.kanapa4.shop.userinteraction.dto;

import java.math.BigDecimal;

public record OptionValueDto(
        String code,
        String label,
        BigDecimal priceDelta) {
}
