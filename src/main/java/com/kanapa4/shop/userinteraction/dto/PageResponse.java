package com.kanapa4.shop.userinteraction.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static java.util.Objects.requireNonNullElse;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PageResponse<T>(
        List<T> content,
        int number,
        int size,
        long totalElements,
        int totalPages) {

    public PageResponse {
        content = requireNonNullElse(content, List.of());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PageMetadata(int number, int size, long totalElements, int totalPages) {}

    @JsonCreator
    public static <T> PageResponse<T> of(
            @JsonProperty("content") List<T> content,
            @JsonProperty("number") Integer number,
            @JsonProperty("size") Integer size,
            @JsonProperty("totalElements") Long totalElements,
            @JsonProperty("totalPages") Integer totalPages,
            @JsonProperty("page") PageMetadata page) {
        if (page != null) {
            return new PageResponse<>(content, page.number(), page.size(), page.totalElements(), page.totalPages());
        }
        return new PageResponse<>(
                content,
                requireNonNullElse(number, 0),
                requireNonNullElse(size, 0),
                requireNonNullElse(totalElements, 0L),
                requireNonNullElse(totalPages, 0)
        );
    }
}
