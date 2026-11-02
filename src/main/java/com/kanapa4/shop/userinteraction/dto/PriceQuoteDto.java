package com.kanapa4.shop.userinteraction.dto;

import java.util.List;
import java.util.UUID;

public record PriceQuoteDto(
        UUID productId,
        String productName,
        PriceDto unitPrice,
        List<SelectedOptionDto> selectedOptions) {
}
