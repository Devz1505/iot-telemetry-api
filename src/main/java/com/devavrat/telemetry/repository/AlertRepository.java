package com.devavrat.telemetry.repository;

import com.devavrat.telemetry.domain.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    // @EntityGraph loads the device in the same query, avoiding one extra
    // SELECT per alert when we read alert.getDevice() (the "N+1" problem).
    @EntityGraph(attributePaths = "device")
    Page<Alert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "device")
    Page<Alert> findByAcknowledgedOrderByCreatedAtDesc(boolean acknowledged, Pageable pageable);
}
