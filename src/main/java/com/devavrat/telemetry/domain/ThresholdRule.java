package com.devavrat.telemetry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Locale;
import java.util.Optional;

/**
 * An allowed band for one sensor type on one device, e.g. "server-room
 * temperature must stay between 18 and 27 °C". Either bound may be null,
 * meaning "no limit on that side". At most one rule per (device, type).
 */
@Entity
@Table(name = "threshold_rules",
        uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "sensor_type"}))
public class ThresholdRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false, length = 20)
    private SensorType sensorType;

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    protected ThresholdRule() {
        // required by JPA
    }

    public ThresholdRule(Device device, SensorType sensorType, Double minValue, Double maxValue) {
        this.device = device;
        this.sensorType = sensorType;
        updateBounds(minValue, maxValue);
    }

    public void updateBounds(Double minValue, Double maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * Pure business rule, no database or Spring involved, which makes it
     * trivial to unit-test.
     *
     * @return a human-readable reason if {@code value} breaks this rule
     */
    public Optional<String> violationFor(double value) {
        String unit = sensorType.unit();
        // Locale.ROOT: always "27.50", never "27,50" on a machine with a European locale.
        if (minValue != null && value < minValue) {
            return Optional.of(String.format(Locale.ROOT, "%s %.2f %s is below minimum %.2f %s",
                    sensorType, value, unit, minValue, unit));
        }
        if (maxValue != null && value > maxValue) {
            return Optional.of(String.format(Locale.ROOT, "%s %.2f %s is above maximum %.2f %s",
                    sensorType, value, unit, maxValue, unit));
        }
        return Optional.empty();
    }

    public Long getId() {
        return id;
    }

    public Device getDevice() {
        return device;
    }

    public SensorType getSensorType() {
        return sensorType;
    }

    public Double getMinValue() {
        return minValue;
    }

    public Double getMaxValue() {
        return maxValue;
    }
}
