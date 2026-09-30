package com.devavrat.telemetry.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Devices send all sensors from one sampling cycle in a single request:
 * one HTTP round-trip instead of several saves radio time and battery.
 */
public record ReadingBatchRequest(
        @NotEmpty @Size(max = 100)
        List<@NotNull @Valid ReadingRequest> readings) {
}
