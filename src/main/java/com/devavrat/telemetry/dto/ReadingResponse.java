package com.devavrat.telemetry.dto;

import com.devavrat.telemetry.domain.Reading;
import com.devavrat.telemetry.domain.SensorType;

import java.time.Instant;

public record ReadingResponse(
        Long id,
        SensorType sensorType,
        double value,
        String unit,
        Instant recordedAt,
        Instant receivedAt) {

    public static ReadingResponse from(Reading reading) {
        return new ReadingResponse(reading.getId(), reading.getSensorType(), reading.getMeasuredValue(),
                reading.getSensorType().unit(), reading.getRecordedAt(), reading.getReceivedAt());
    }
}
