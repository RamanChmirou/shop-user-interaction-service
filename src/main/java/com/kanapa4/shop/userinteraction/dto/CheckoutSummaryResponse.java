package com.kanapa4.shop.userinteraction.dto;

import com.kanapa4.shop.userinteraction.model.CheckoutData;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutSummaryResponse(
        CartResponse cart,
        CheckoutData checkoutData,
        BigDecimal deliveryCost,
        BigDecimal grandTotal,
        String currency,
        boolean readyToSubmit,
        List<String> missingFields) {
}