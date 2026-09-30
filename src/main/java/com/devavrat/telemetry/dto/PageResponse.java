package com.devavrat.telemetry.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * A small, stable JSON shape for paged results. Serialising Spring's own
 * Page type directly would leak internal fields that can change between
 * Spring versions.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
