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

import java.time.Instant;

/** Raised when a stored reading breaks its device's {@link ThresholdRule}. */
@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reading_id", nullable = false)
    private Reading reading;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false, length = 20)
    private SensorType sensorType;

    @Column(name = "measured_value", nullable = false)
    private double measuredValue;

    @Column(nullable = false, length = 255)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean acknowledged;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    protected Alert() {
        // required by JPA
    }

    public Alert(Reading reading, String message) {
        this.device = reading.getDevice();
        this.reading = reading;
        this.sensorType = reading.getSensorType();
        this.measuredValue = reading.getMeasuredValue();
        this.message = message;
        this.createdAt = Instant.now();
    }

    /** Idempotent: acknowledging twice keeps the first timestamp. */
    public void acknowledge(Instant when) {
        if (!acknowledged) {
            acknowledged = true;
            acknowledgedAt = when;
        }
    }

    public Long getId() {
        return id;
    }

    public Device getDevice() {
        return device;
    }

    public Reading getReading() {
        return reading;
    }

    public SensorType getSensorType() {
        return sensorType;
    }

    public double getMeasuredValue() {
        return measuredValue;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }
}
