package com.devavrat.telemetry.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit test: no Spring, no database, runs in milliseconds. */
class ThresholdRuleTest {

    private final Device device = new Device("esp32-test", "Test board", null);

    @Test
    void valueInsideBandIsNotAViolation() {
        ThresholdRule rule = new ThresholdRule(device, SensorType.TEMPERATURE, 18.0, 27.0);

        assertThat(rule.violationFor(22.5)).isEmpty();
    }

    @Test
    void boundsThemselvesAreAllowed() {
        ThresholdRule rule = new ThresholdRule(device, SensorType.TEMPERATURE, 18.0, 27.0);

        assertThat(rule.violationFor(18.0)).isEmpty();
        assertThat(rule.violationFor(27.0)).isEmpty();
    }

    @Test
    void valueAboveMaxIsReportedWithUnits() {
        ThresholdRule rule = new ThresholdRule(device, SensorType.TEMPERATURE, 18.0, 27.0);

        assertThat(rule.violationFor(31.25))
                .hasValue("TEMPERATURE 31.25 °C is above maximum 27.00 °C");
    }

    @Test
    void valueBelowMinIsReported() {
        ThresholdRule rule = new ThresholdRule(device, SensorType.TEMPERATURE, 18.0, 27.0);

        assertThat(rule.violationFor(12.0)).get().asString().contains("below minimum");
    }

    @Test
    void nullBoundMeansNoLimitOnThatSide() {
        ThresholdRule maxOnly = new ThresholdRule(device, SensorType.HUMIDITY, null, 60.0);

        assertThat(maxOnly.violationFor(0.0)).isEmpty();
        assertThat(maxOnly.violationFor(75.0)).isPresent();
    }
}
