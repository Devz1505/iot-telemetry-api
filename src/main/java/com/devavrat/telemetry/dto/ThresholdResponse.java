package com.devavrat.telemetry.dto;

import com.devavrat.telemetry.domain.SensorType;
import com.devavrat.telemetry.domain.ThresholdRule;

public record ThresholdResponse(SensorType sensorType, String unit, Double min, Double max) {

    public static ThresholdResponse from(ThresholdRule rule) {
        return new ThresholdResponse(rule.getSensorType(), rule.getSensorType().unit(),
                rule.getMinValue(), rule.getMaxValue());
    }
}
