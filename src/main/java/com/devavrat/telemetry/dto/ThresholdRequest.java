package com.devavrat.telemetry.dto;

/** Either bound may be null ("no limit on that side"), but not both. */
public record ThresholdRequest(Double min, Double max) {
}
