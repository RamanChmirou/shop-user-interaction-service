package com.kanapa4.shop.userinteraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

import static java.util.Objects.requireNonNullElse;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public OrderPageResponse {
        content = requireNonNullElse(content, List.of());
    }
}

