package com.kanapa4.shop.userinteraction.dto;

import java.util.List;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        String productName,
        PriceDto unitPrice,
        int quantity,
        PriceDto lineTotal,
        List<SelectedOptionDto> selectedOptions) {
}
