package com.devavrat.telemetry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A physical board (ESP32, Raspberry Pi, ...) that sends readings.
 *
 * {@code id} is the database's surrogate key; {@code deviceId} is the
 * human-chosen identifier flashed into the firmware (e.g. "esp32-lab-01").
 * The API only ever exposes {@code deviceId}.
 */
@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, unique = true, length = 64)
    private String deviceId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String location;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    protected Device() {
        // required by JPA
    }

    public Device(String deviceId, String name, String location) {
        this.deviceId = deviceId;
        this.name = name;
        this.location = location;
        this.createdAt = Instant.now();
    }

    public void markSeen(Instant when) {
        this.lastSeenAt = when;
    }

    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }
}
