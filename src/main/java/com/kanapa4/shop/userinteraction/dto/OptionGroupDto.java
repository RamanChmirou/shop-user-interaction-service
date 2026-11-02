package com.kanapa4.shop.userinteraction.dto;

import java.util.List;

public record OptionGroupDto(
        String code,
        String name,
        SelectionType selectionType,
        boolean required,
        List<OptionValueDto> values) {
}
