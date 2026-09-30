package com.devavrat.telemetry.repository;

import com.devavrat.telemetry.domain.Device;
import com.devavrat.telemetry.domain.Reading;
import com.devavrat.telemetry.domain.SensorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;

import static com.devavrat.telemetry.domain.SensorType.HUMIDITY;
import static com.devavrat.telemetry.domain.SensorType.TEMPERATURE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real JPQL against an in-memory database. Only the JPA layer is
 * started, and each test is rolled back afterwards.
 */
@DataJpaTest
class ReadingRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-09-30T10:00:00Z");

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ReadingRepository readings;

    private Device device;

    @BeforeEach
    void setUp() {
        device = em.persist(new Device("esp32-repo", "Repository test board", null));
        save(TEMPERATURE, 20.0, T0);
        save(TEMPERATURE, 30.0, T0.plusSeconds(60));
        save(TEMPERATURE, 25.0, T0.plusSeconds(120));
        save(HUMIDITY, 55.0, T0.plusSeconds(60));
        save(TEMPERATURE, 99.0, T0.plusSeconds(3600));   // outside the 10-minute window below
        em.flush();
    }

    @Test
    void statsCoverOnlyTheRequestedTypeInsideTheWindow() {
        ReadingStats stats = readings.statsFor(device, TEMPERATURE, T0, T0.plusSeconds(600));

        assertThat(stats.count()).isEqualTo(3);
        assertThat(stats.min()).isEqualTo(20.0);
        assertThat(stats.max()).isEqualTo(30.0);
        assertThat(stats.avg()).isEqualTo(25.0);
    }

    @Test
    void statsOfAnEmptyWindowAreZeroAndNull() {
        ReadingStats stats = readings.statsFor(device, TEMPERATURE, T0.minusSeconds(600), T0);

        assertThat(stats.count()).isZero();
        assertThat(stats.min()).isNull();
        assertThat(stats.avg()).isNull();
    }

    @Test
    void windowIncludesFromButExcludesTo() {
        Page<Reading> page = readings.findInWindow(device, TEMPERATURE, T0, T0.plusSeconds(120), PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(Reading::getMeasuredValue)
                .containsExactly(30.0, 20.0);   // newest first; the reading at exactly T0+120s is excluded
    }

    @Test
    void allTypesArePagedNewestFirst() {
        Page<Reading> firstPage = readings.findInWindow(device, T0, T0.plusSeconds(600), PageRequest.of(0, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(4);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent().getFirst().getMeasuredValue()).isEqualTo(25.0);
    }

    private void save(SensorType type, double value, Instant recordedAt) {
        em.persist(new Reading(device, type, value, recordedAt, recordedAt));
    }
}
