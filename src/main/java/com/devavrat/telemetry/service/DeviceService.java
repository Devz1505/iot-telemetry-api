package com.devavrat.telemetry.service;

import com.devavrat.telemetry.domain.Device;
import com.devavrat.telemetry.dto.DeviceRequest;
import com.devavrat.telemetry.dto.DeviceResponse;
import com.devavrat.telemetry.exception.ConflictException;
import com.devavrat.telemetry.exception.NotFoundException;
import com.devavrat.telemetry.repository.DeviceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository devices;

    // Constructor injection: dependencies are explicit, final, and easy to
    // replace with mocks in unit tests.
    public DeviceService(DeviceRepository devices) {
        this.devices = devices;
    }

    @Transactional
    public DeviceResponse register(DeviceRequest request) {
        if (devices.existsByDeviceId(request.deviceId())) {
            throw new ConflictException("Device '%s' is already registered".formatted(request.deviceId()));
        }
        Device device = new Device(request.deviceId(), request.name().trim(), request.location());
        return DeviceResponse.from(devices.save(device));
    }

    public List<DeviceResponse> list() {
        return devices.findAll(Sort.by("deviceId")).stream()
                .map(DeviceResponse::from)
                .toList();
    }

    public DeviceResponse get(String deviceId) {
        return DeviceResponse.from(require(deviceId));
    }

    /** Shared lookup for the other services: the device, or a 404. */
    public Device require(String deviceId) {
        return devices.findByDeviceId(deviceId)
                .orElseThrow(() -> new NotFoundException("Device '%s' not found".formatted(deviceId)));
    }
}
