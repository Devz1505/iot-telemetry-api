package com.devavrat.telemetry.repository;

import com.devavrat.telemetry.domain.Device;
import com.devavrat.telemetry.domain.SensorType;
import com.devavrat.telemetry.domain.ThresholdRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ThresholdRuleRepository extends JpaRepository<ThresholdRule, Long> {

    Optional<ThresholdRule> findByDeviceAndSensorType(Device device, SensorType sensorType);

    List<ThresholdRule> findByDeviceOrderBySensorType(Device device);
}
