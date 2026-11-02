package com.kanapa4.shop.userinteraction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OptionGroupRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 255) String name,
        @NotNull SelectionType selectionType,
        boolean required,
        @NotEmpty @Valid List<OptionValueRequest> values) {
}
