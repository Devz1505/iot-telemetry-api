package com.devavrat.telemetry.repository;

/**
 * Result of the aggregate query in {@link ReadingRepository#statsFor}.
 * min/max/avg are null when the window contains no readings.
 */
public record ReadingStats(Long count, Double min, Double max, Double avg) {
}
