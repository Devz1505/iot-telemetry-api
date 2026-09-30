package com.devavrat.telemetry.dto;

import com.devavrat.telemetry.domain.SensorType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * One sample from the device.
 *
 * {@code value} is a boxed Double so a missing value becomes a clean 400
 * instead of silently turning into 0.0. (A DHT22 read failure gives NaN,
 * which JSON encoders such as ArduinoJson write as null.)
 *
 * {@code recordedAt} is optional: a board without an RTC or NTP sync can
 * leave it out and the server stamps the reading on arrival.
 */
public record ReadingRequest(
        @NotNull SensorType sensorType,
        @NotNull Double value,
        Instant recordedAt) {
}
