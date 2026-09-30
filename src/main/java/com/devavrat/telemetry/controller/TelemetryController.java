package com.devavrat.telemetry.controller;

import com.devavrat.telemetry.domain.SensorType;
import com.devavrat.telemetry.dto.IngestResponse;
import com.devavrat.telemetry.dto.PageResponse;
import com.devavrat.telemetry.dto.ReadingBatchRequest;
import com.devavrat.telemetry.dto.ReadingResponse;
import com.devavrat.telemetry.dto.StatsResponse;
import com.devavrat.telemetry.service.TelemetryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Ingest and query sensor readings for one device. Times are ISO-8601 UTC, e.g. 2026-09-30T10:15:00Z. */
@RestController
@RequestMapping("/api/devices/{deviceId}")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping("/readings")
    @ResponseStatus(HttpStatus.CREATED)
    public IngestResponse ingest(@PathVariable String deviceId,
                                 @Valid @RequestBody ReadingBatchRequest batch) {
        return telemetryService.ingest(deviceId, batch);
    }

    @GetMapping("/readings")
    public PageResponse<ReadingResponse> readings(
            @PathVariable String deviceId,
            @RequestParam(required = false) SensorType sensorType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) int size) {
        return telemetryService.readings(deviceId, sensorType, from, to, page, size);
    }

    @GetMapping("/stats")
    public StatsResponse stats(@PathVariable String deviceId,
                               @RequestParam SensorType sensorType,
                               @RequestParam(required = false) Instant from,
                               @RequestParam(required = false) Instant to) {
        return telemetryService.stats(deviceId, sensorType, from, to);
    }
}
