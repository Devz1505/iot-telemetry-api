package com.devavrat.telemetry.repository;

import com.devavrat.telemetry.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Spring Data generates the SQL for these from the method names.
public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceId(String deviceId);

    boolean existsByDeviceId(String deviceId);
}
