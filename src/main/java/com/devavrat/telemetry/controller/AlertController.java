package com.devavrat.telemetry.controller;

import com.devavrat.telemetry.domain.SensorType;
import com.devavrat.telemetry.dto.AlertResponse;
import com.devavrat.telemetry.dto.PageResponse;
import com.devavrat.telemetry.dto.ThresholdRequest;
import com.devavrat.telemetry.dto.ThresholdResponse;
import com.devavrat.telemetry.service.AlertService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Threshold rules (per device) and the alerts they raise. */
@RestController
@RequestMapping("/api")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    /** PUT, because setting the same threshold twice has the same effect as once (idempotent). */
    @PutMapping("/devices/{deviceId}/thresholds/{sensorType}")
    public ThresholdResponse setThreshold(@PathVariable String deviceId,
                                          @PathVariable SensorType sensorType,
                                          @RequestBody ThresholdRequest request) {
        return alertService.setThreshold(deviceId, sensorType, request);
    }

    @GetMapping("/devices/{deviceId}/thresholds")
    public List<ThresholdResponse> thresholds(@PathVariable String deviceId) {
        return alertService.thresholds(deviceId);
    }

    @DeleteMapping("/devices/{deviceId}/thresholds/{sensorType}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteThreshold(@PathVariable String deviceId, @PathVariable SensorType sensorType) {
        alertService.deleteThreshold(deviceId, sensorType);
    }

    @GetMapping("/alerts")
    public PageResponse<AlertResponse> alerts(
            @RequestParam(required = false) Boolean acknowledged,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) int size) {
        return alertService.list(acknowledged, page, size);
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    public AlertResponse acknowledge(@PathVariable long alertId) {
        return alertService.acknowledge(alertId);
    }
}
