package com.kanapa4.shop.userinteraction.dto;

import jakarta.validation.Valid;

import java.util.List;

public record PriceQuoteRequest(
        @Valid List<SelectedOptionRequest> selectedOptions) {

    public PriceQuoteRequest {
        if (selectedOptions == null) {
            selectedOptions = List.of();
        }
    }
}
