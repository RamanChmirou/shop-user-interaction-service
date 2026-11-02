package com.kanapa4.shop.userinteraction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String orderNumber,
        OrderStatus status,
        BigDecimal grandTotal,
        String currency,
        Instant createdAt) {
}
