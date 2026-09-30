package com.devavrat.telemetry.dto;

import com.devavrat.telemetry.domain.Alert;
import com.devavrat.telemetry.domain.SensorType;

import java.time.Instant;

public record AlertResponse(
        Long id,
        String deviceId,
        SensorType sensorType,
        double value,
        String message,
        Instant createdAt,
        boolean acknowledged,
        Instant acknowledgedAt) {

    public static AlertResponse from(Alert alert) {
        return new AlertResponse(alert.getId(), alert.getDevice().getDeviceId(), alert.getSensorType(),
                alert.getMeasuredValue(), alert.getMessage(), alert.getCreatedAt(),
                alert.isAcknowledged(), alert.getAcknowledgedAt());
    }
}
