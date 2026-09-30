package com.devavrat.telemetry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeviceRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]{2,63}$",
                message = "must be 3-64 chars: lowercase letters, digits and '-' (e.g. esp32-lab-01)")
        String deviceId,

        @NotBlank @Size(max = 100)
        String name,

        @Size(max = 100)
        String location) {
}
