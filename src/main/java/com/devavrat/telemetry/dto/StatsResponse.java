package com.devavrat.telemetry.dto;

import com.devavrat.telemetry.domain.SensorType;

import java.time.Instant;

public record StatsResponse(
        String deviceId,
        SensorType sensorType,
        String unit,
        Instant from,
        Instant to,
        long count,
        Double min,
        Double max,
        Double avg) {
}
