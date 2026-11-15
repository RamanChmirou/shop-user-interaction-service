package com.kanapa4.shop.userinteraction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record ProductRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 5000) String description,
        @NotNull ProductType type,
        @NotNull @Valid PriceRequest price,
        Map<String, String> attributes,
        @Valid List<OptionGroupRequest> optionGroups) {

    public ProductRequest {
        if (attributes == null) {
            attributes = Map.of();
        }
        if (optionGroups == null) {
            optionGroups = List.of();
        }
    }
}
