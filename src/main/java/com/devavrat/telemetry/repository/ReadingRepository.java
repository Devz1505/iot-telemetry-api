package com.devavrat.telemetry.repository;

import com.devavrat.telemetry.domain.Device;
import com.devavrat.telemetry.domain.Reading;
import com.devavrat.telemetry.domain.SensorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ReadingRepository extends JpaRepository<Reading, Long> {

    // Time windows are half-open [from, to) so back-to-back windows never
    // count the same reading twice.

    @Query("""
            select r from Reading r
            where r.device = :device
              and r.recordedAt >= :from and r.recordedAt < :to
            order by r.recordedAt desc
            """)
    Page<Reading> findInWindow(@Param("device") Device device,
                               @Param("from") Instant from,
                               @Param("to") Instant to,
                               Pageable pageable);

    @Query("""
            select r from Reading r
            where r.device = :device
              and r.sensorType = :sensorType
              and r.recordedAt >= :from and r.recordedAt < :to
            order by r.recordedAt desc
            """)
    Page<Reading> findInWindow(@Param("device") Device device,
                               @Param("sensorType") SensorType sensorType,
                               @Param("from") Instant from,
                               @Param("to") Instant to,
                               Pageable pageable);

    /** The database does the maths; we never pull every row into Java. */
    @Query("""
            select new com.devavrat.telemetry.repository.ReadingStats(
                count(r), min(r.measuredValue), max(r.measuredValue), avg(r.measuredValue))
            from Reading r
            where r.device = :device
              and r.sensorType = :sensorType
              and r.recordedAt >= :from and r.recordedAt < :to
            """)
    ReadingStats statsFor(@Param("device") Device device,
                          @Param("sensorType") SensorType sensorType,
                          @Param("from") Instant from,
                          @Param("to") Instant to);
}
