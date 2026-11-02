package com.kanapa4.shop.userinteraction.dto;

import java.math.BigDecimal;

public record PriceDto(
        BigDecimal amount,
        String currency,
        VatRate vatRate) {
}
