package com.devavrat.telemetry.controller;

import com.devavrat.telemetry.dto.DeviceRequest;
import com.devavrat.telemetry.dto.DeviceResponse;
import com.devavrat.telemetry.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controllers stay thin: translate HTTP to a service call and back.
 * All rules live in the service layer.
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /** 201 Created + a Location header pointing at the new device. */
    @PostMapping
    public ResponseEntity<DeviceResponse> register(@Valid @RequestBody DeviceRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        DeviceResponse created = deviceService.register(request);
        URI location = uriBuilder.path("/api/devices/{deviceId}")
                .buildAndExpand(created.deviceId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<DeviceResponse> list() {
        return deviceService.list();
    }

    @GetMapping("/{deviceId}")
    public DeviceResponse get(@PathVariable String deviceId) {
        return deviceService.get(deviceId);
    }
}
