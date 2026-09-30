package com.devavrat.telemetry.dto;

import com.devavrat.telemetry.domain.Device;

import java.time.Instant;

public record DeviceResponse(
        String deviceId,
        String name,
        String location,
        Instant createdAt,
        Instant lastSeenAt) {

    public static DeviceResponse from(Device device) {
        return new DeviceResponse(device.getDeviceId(), device.getName(), device.getLocation(),
                device.getCreatedAt(), device.getLastSeenAt());
    }
}
