package com.devavrat.telemetry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One measurement from one sensor on one device.
 *
 * The composite index matches the most common query: "readings of type X
 * for device Y in time window Z", so it stays fast as the table grows.
 */
@Entity
@Table(name = "readings", indexes = @Index(
        name = "idx_readings_device_type_time",
        columnList = "device_id, sensor_type, recorded_at"))
public class Reading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false, length = 20)
    private SensorType sensorType;

    // "value" is a reserved word in SQL/H2, hence the longer name.
    @Column(name = "measured_value", nullable = false)
    private double measuredValue;

    /** When the sensor took the sample (device clock, or server clock if the device has none). */
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    /** When the server stored it. The gap to recordedAt shows network/buffering delay. */
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected Reading() {
        // required by JPA
    }

    public Reading(Device device, SensorType sensorType, double measuredValue,
                   Instant recordedAt, Instant receivedAt) {
        this.device = device;
        this.sensorType = sensorType;
        this.measuredValue = measuredValue;
        this.recordedAt = recordedAt;
        this.receivedAt = receivedAt;
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

    public double getMeasuredValue() {
        return measuredValue;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
