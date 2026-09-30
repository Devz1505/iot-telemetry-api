package com.devavrat.telemetry.service;

import com.devavrat.telemetry.domain.Device;
import com.devavrat.telemetry.domain.Reading;
import com.devavrat.telemetry.domain.SensorType;
import com.devavrat.telemetry.dto.IngestResponse;
import com.devavrat.telemetry.dto.PageResponse;
import com.devavrat.telemetry.dto.ReadingBatchRequest;
import com.devavrat.telemetry.dto.ReadingRequest;
import com.devavrat.telemetry.dto.ReadingResponse;
import com.devavrat.telemetry.dto.StatsResponse;
import com.devavrat.telemetry.exception.InvalidRequestException;
import com.devavrat.telemetry.repository.ReadingRepository;
import com.devavrat.telemetry.repository.ReadingStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TelemetryService {

    /** Used when the client gives no "from". */
    static final Duration DEFAULT_WINDOW = Duration.ofHours(24);

    /** How far ahead of the server clock a device timestamp may be before we call it wrong. */
    static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);

    private final DeviceService deviceService;
    private final ReadingRepository readings;
    private final AlertService alertService;

    public TelemetryService(DeviceService deviceService, ReadingRepository readings, AlertService alertService) {
        this.deviceService = deviceService;
        this.readings = readings;
        this.alertService = alertService;
    }

    /**
     * Stores a batch atomically: if any reading is invalid, none are saved,
     * so a device can simply fix the problem and resend the whole batch.
     */
    @Transactional
    public IngestResponse ingest(String deviceId, ReadingBatchRequest batch) {
        Device device = deviceService.require(deviceId);
        Instant now = Instant.now();

        List<Reading> toSave = new ArrayList<>();
        for (int i = 0; i < batch.readings().size(); i++) {
            ReadingRequest r = batch.readings().get(i);
            SensorType type = r.sensorType();

            if (!type.isPlausible(r.value())) {
                throw new InvalidRequestException(
                        "readings[%d]: %s value %s is outside the plausible range %s to %s %s (sensor fault?)"
                                .formatted(i, type, r.value(), type.minPlausible(), type.maxPlausible(), type.unit()));
            }
            Instant recordedAt = r.recordedAt() != null ? r.recordedAt() : now;
            if (recordedAt.isAfter(now.plus(MAX_CLOCK_SKEW))) {
                throw new InvalidRequestException(
                        "readings[%d]: recordedAt %s is in the future (check the device clock / NTP sync)"
                                .formatted(i, recordedAt));
            }
            toSave.add(new Reading(device, type, r.value(), recordedAt, now));
        }

        List<Reading> saved = readings.saveAll(toSave);
        // No save() needed here: JPA tracks this managed entity and writes
        // the change when the transaction commits ("dirty checking").
        device.markSeen(now);
        int alertsRaised = alertService.evaluate(saved);
        return new IngestResponse(saved.size(), alertsRaised);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReadingResponse> readings(String deviceId, SensorType sensorType,
                                                  Instant from, Instant to, int page, int size) {
        Device device = deviceService.require(deviceId);
        TimeWindow window = TimeWindow.of(from, to);
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<Reading> result = sensorType == null
                ? readings.findInWindow(device, window.from(), window.to(), pageRequest)
                : readings.findInWindow(device, sensorType, window.from(), window.to(), pageRequest);
        return PageResponse.from(result.map(ReadingResponse::from));
    }

    @Transactional(readOnly = true)
    public StatsResponse stats(String deviceId, SensorType sensorType, Instant from, Instant to) {
        Device device = deviceService.require(deviceId);
        TimeWindow window = TimeWindow.of(from, to);
        ReadingStats stats = readings.statsFor(device, sensorType, window.from(), window.to());
        return new StatsResponse(deviceId, sensorType, sensorType.unit(), window.from(), window.to(),
                stats.count(), stats.min(), stats.max(), roundTo2(stats.avg()));
    }

    private static Double roundTo2(Double value) {
        return value == null ? null : Math.round(value * 100.0) / 100.0;
    }

    /** Half-open [from, to). Defaults to the last 24 hours. */
    private record TimeWindow(Instant from, Instant to) {

        static TimeWindow of(Instant from, Instant to) {
            Instant end = to != null ? to : Instant.now();
            Instant start = from != null ? from : end.minus(DEFAULT_WINDOW);
            if (!start.isBefore(end)) {
                throw new InvalidRequestException("'from' must be before 'to'");
            }
            return new TimeWindow(start, end);
        }
    }
}
