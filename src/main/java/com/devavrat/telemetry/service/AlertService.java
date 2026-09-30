package com.devavrat.telemetry.service;

import com.devavrat.telemetry.domain.Alert;
import com.devavrat.telemetry.domain.Device;
import com.devavrat.telemetry.domain.Reading;
import com.devavrat.telemetry.domain.SensorType;
import com.devavrat.telemetry.domain.ThresholdRule;
import com.devavrat.telemetry.dto.AlertResponse;
import com.devavrat.telemetry.dto.PageResponse;
import com.devavrat.telemetry.dto.ThresholdRequest;
import com.devavrat.telemetry.dto.ThresholdResponse;
import com.devavrat.telemetry.exception.InvalidRequestException;
import com.devavrat.telemetry.exception.NotFoundException;
import com.devavrat.telemetry.repository.AlertRepository;
import com.devavrat.telemetry.repository.ThresholdRuleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Threshold rules per device, and the alerts raised when readings break them. */
@Service
public class AlertService {

    private final DeviceService deviceService;
    private final ThresholdRuleRepository rules;
    private final AlertRepository alerts;

    public AlertService(DeviceService deviceService, ThresholdRuleRepository rules, AlertRepository alerts) {
        this.deviceService = deviceService;
        this.rules = rules;
        this.alerts = alerts;
    }

    /** Creates or replaces ("upserts") the rule for one sensor type on one device. */
    @Transactional
    public ThresholdResponse setThreshold(String deviceId, SensorType sensorType, ThresholdRequest request) {
        if (request.min() == null && request.max() == null) {
            throw new InvalidRequestException("Give at least one of 'min' or 'max'");
        }
        if (request.min() != null && request.max() != null && request.min() >= request.max()) {
            throw new InvalidRequestException("'min' must be less than 'max'");
        }
        Device device = deviceService.require(deviceId);
        ThresholdRule rule = rules.findByDeviceAndSensorType(device, sensorType)
                .orElseGet(() -> new ThresholdRule(device, sensorType, null, null));
        rule.updateBounds(request.min(), request.max());
        return ThresholdResponse.from(rules.save(rule));
    }

    @Transactional(readOnly = true)
    public List<ThresholdResponse> thresholds(String deviceId) {
        Device device = deviceService.require(deviceId);
        return rules.findByDeviceOrderBySensorType(device).stream()
                .map(ThresholdResponse::from)
                .toList();
    }

    @Transactional
    public void deleteThreshold(String deviceId, SensorType sensorType) {
        Device device = deviceService.require(deviceId);
        ThresholdRule rule = rules.findByDeviceAndSensorType(device, sensorType)
                .orElseThrow(() -> new NotFoundException(
                        "No %s threshold set for device '%s'".formatted(sensorType, deviceId)));
        rules.delete(rule);
    }

    /**
     * Checks freshly stored readings (all from one device) against that
     * device's rules. Runs inside the ingest transaction, so readings and
     * their alerts are committed together or not at all.
     *
     * @return how many alerts were raised
     */
    @Transactional
    public int evaluate(List<Reading> newReadings) {
        if (newReadings.isEmpty()) {
            return 0;
        }
        Device device = newReadings.getFirst().getDevice();

        // One query for all of this device's rules, not one per reading.
        Map<SensorType, ThresholdRule> ruleByType = new EnumMap<>(SensorType.class);
        rules.findByDeviceOrderBySensorType(device)
                .forEach(rule -> ruleByType.put(rule.getSensorType(), rule));
        if (ruleByType.isEmpty()) {
            return 0;
        }

        List<Alert> raised = new ArrayList<>();
        for (Reading reading : newReadings) {
            ThresholdRule rule = ruleByType.get(reading.getSensorType());
            if (rule != null) {
                rule.violationFor(reading.getMeasuredValue())
                        .ifPresent(message -> raised.add(new Alert(reading, message)));
            }
        }
        alerts.saveAll(raised);
        return raised.size();
    }

    /** {@code acknowledged = null} returns all alerts. Newest first. */
    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> list(Boolean acknowledged, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Alert> result = acknowledged == null
                ? alerts.findAllByOrderByCreatedAtDesc(pageRequest)
                : alerts.findByAcknowledgedOrderByCreatedAtDesc(acknowledged, pageRequest);
        return PageResponse.from(result.map(AlertResponse::from));
    }

    @Transactional
    public AlertResponse acknowledge(long alertId) {
        Alert alert = alerts.findById(alertId)
                .orElseThrow(() -> new NotFoundException("Alert %d not found".formatted(alertId)));
        alert.acknowledge(Instant.now());
        return AlertResponse.from(alert);
    }
}
