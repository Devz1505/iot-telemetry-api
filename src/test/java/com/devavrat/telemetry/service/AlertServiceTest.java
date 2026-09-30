package com.devavrat.telemetry.service;

import com.devavrat.telemetry.domain.Alert;
import com.devavrat.telemetry.domain.Device;
import com.devavrat.telemetry.domain.Reading;
import com.devavrat.telemetry.domain.SensorType;
import com.devavrat.telemetry.domain.ThresholdRule;
import com.devavrat.telemetry.dto.ThresholdRequest;
import com.devavrat.telemetry.exception.InvalidRequestException;
import com.devavrat.telemetry.repository.AlertRepository;
import com.devavrat.telemetry.repository.ThresholdRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static com.devavrat.telemetry.domain.SensorType.HUMIDITY;
import static com.devavrat.telemetry.domain.SensorType.TEMPERATURE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test of the alerting logic with the repositories replaced by
 * Mockito mocks, so no database is involved.
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private DeviceService deviceService;

    @Mock
    private ThresholdRuleRepository rules;

    @Mock
    private AlertRepository alerts;

    @InjectMocks
    private AlertService alertService;

    @Captor
    private ArgumentCaptor<List<Alert>> savedAlerts;

    private final Device device = new Device("esp32-test", "Test board", null);

    @Test
    void raisesAnAlertOnlyForReadingsThatBreakARule() {
        when(rules.findByDeviceOrderBySensorType(device))
                .thenReturn(List.of(new ThresholdRule(device, TEMPERATURE, null, 30.0)));

        int raised = alertService.evaluate(List.of(
                reading(TEMPERATURE, 25.0),   // fine
                reading(TEMPERATURE, 35.0),   // too hot
                reading(HUMIDITY, 95.0)));    // no humidity rule, so ignored

        assertThat(raised).isEqualTo(1);
        verify(alerts).saveAll(savedAlerts.capture());
        assertThat(savedAlerts.getValue()).singleElement().satisfies(alert -> {
            assertThat(alert.getMeasuredValue()).isEqualTo(35.0);
            assertThat(alert.getMessage()).contains("above maximum");
        });
    }

    @Test
    void deviceWithoutRulesRaisesNothingAndWritesNothing() {
        when(rules.findByDeviceOrderBySensorType(device)).thenReturn(List.of());

        assertThat(alertService.evaluate(List.of(reading(TEMPERATURE, 99.0)))).isZero();
        verifyNoInteractions(alerts);
    }

    @Test
    void thresholdNeedsAtLeastOneBound() {
        assertThatThrownBy(() -> alertService.setThreshold("esp32-test", TEMPERATURE, new ThresholdRequest(null, null)))
                .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(deviceService, rules);
    }

    @Test
    void thresholdMinMustBeBelowMax() {
        assertThatThrownBy(() -> alertService.setThreshold("esp32-test", TEMPERATURE, new ThresholdRequest(30.0, 20.0)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("less than");
    }

    private Reading reading(SensorType type, double value) {
        Instant now = Instant.now();
        return new Reading(device, type, value, now, now);
    }
}
