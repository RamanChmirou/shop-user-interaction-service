package com.kanapa4.shop.userinteraction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OptionValueRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 255) String label,
        @DecimalMin("0.00") BigDecimal priceDelta) {
}
