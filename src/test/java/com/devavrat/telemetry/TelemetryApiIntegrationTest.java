package com.devavrat.telemetry;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end through the real HTTP layer, services and database (H2).
 * Every test registers its own device so tests never depend on each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TelemetryApiIntegrationTest {

    private static final AtomicInteger DEVICE_COUNTER = new AtomicInteger();

    @Autowired
    private MockMvc mvc;

    // --- devices -------------------------------------------------------------

    @Test
    void registeringADeviceReturns201AndItsLocation() throws Exception {
        mvc.perform(post("/api/devices").contentType(APPLICATION_JSON).content("""
                        {"deviceId": "esp32-new-01", "name": "Greenhouse node", "location": "Lab 2"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/devices/esp32-new-01")))
                .andExpect(jsonPath("$.deviceId").value("esp32-new-01"))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()));
    }

    @Test
    void registeringTheSameDeviceTwiceIs409() throws Exception {
        String deviceId = registerDevice();

        mvc.perform(post("/api/devices").contentType(APPLICATION_JSON).content("""
                        {"deviceId": "%s", "name": "Duplicate"}
                        """.formatted(deviceId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Device '%s' is already registered".formatted(deviceId)));
    }

    @Test
    void badDeviceIdIs400WithAFieldError() throws Exception {
        mvc.perform(post("/api/devices").contentType(APPLICATION_JSON).content("""
                        {"deviceId": "ESP 32!", "name": "Bad id"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.deviceId").exists());
    }

    @Test
    void unknownDeviceIs404() throws Exception {
        mvc.perform(get("/api/devices/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    // --- readings --------------------------------------------------------------

    @Test
    void ingestedReadingsCanBeQueriedAndSummarised() throws Exception {
        String deviceId = registerDevice();
        Instant now = Instant.now();

        mvc.perform(post("/api/devices/{id}/readings", deviceId).contentType(APPLICATION_JSON).content("""
                        {"readings": [
                          {"sensorType": "TEMPERATURE", "value": 20.0, "recordedAt": "%s"},
                          {"sensorType": "TEMPERATURE", "value": 30.0, "recordedAt": "%s"},
                          {"sensorType": "TEMPERATURE", "value": 25.0, "recordedAt": "%s"},
                          {"sensorType": "HUMIDITY",    "value": 50.0}
                        ]}
                        """.formatted(now.minusSeconds(180), now.minusSeconds(120), now.minusSeconds(60))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accepted").value(4))
                .andExpect(jsonPath("$.alertsRaised").value(0));

        mvc.perform(get("/api/devices/{id}/readings", deviceId).param("sensorType", "TEMPERATURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].value").value(25.0))
                .andExpect(jsonPath("$.content[0].unit").value("°C"));

        mvc.perform(get("/api/devices/{id}/stats", deviceId).param("sensorType", "TEMPERATURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.min").value(20.0))
                .andExpect(jsonPath("$.max").value(30.0))
                .andExpect(jsonPath("$.avg").value(25.0));

        mvc.perform(get("/api/devices/{id}", deviceId))
                .andExpect(jsonPath("$.lastSeenAt").value(notNullValue()));
    }

    @Test
    void readingWithoutAValueIs400() throws Exception {
        String deviceId = registerDevice();

        mvc.perform(post("/api/devices/{id}/readings", deviceId).contentType(APPLICATION_JSON).content("""
                        {"readings": [{"sensorType": "TEMPERATURE", "value": null}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['readings[0].value']").exists());
    }

    @Test
    void oneImplausibleReadingRejectsTheWholeBatch() throws Exception {
        String deviceId = registerDevice();

        mvc.perform(post("/api/devices/{id}/readings", deviceId).contentType(APPLICATION_JSON).content("""
                        {"readings": [
                          {"sensorType": "TEMPERATURE", "value": 24.0},
                          {"sensorType": "HUMIDITY",    "value": 140.0}
                        ]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(startsWith("readings[1]")));

        mvc.perform(get("/api/devices/{id}/readings", deviceId))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void unknownSensorTypeAndOversizedPagesAre400() throws Exception {
        String deviceId = registerDevice();

        mvc.perform(get("/api/devices/{id}/readings", deviceId).param("sensorType", "SMELL"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/devices/{id}/readings", deviceId).param("size", "10000"))
                .andExpect(status().isBadRequest());
    }

    // --- thresholds and alerts ---------------------------------------------------

    @Test
    void breakingAThresholdRaisesAnAlertThatCanBeAcknowledged() throws Exception {
        String deviceId = registerDevice();

        mvc.perform(put("/api/devices/{id}/thresholds/TEMPERATURE", deviceId).contentType(APPLICATION_JSON)
                        .content("""
                                {"max": 30}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.max").value(30.0));

        mvc.perform(post("/api/devices/{id}/readings", deviceId).contentType(APPLICATION_JSON).content("""
                        {"readings": [
                          {"sensorType": "TEMPERATURE", "value": 25.0},
                          {"sensorType": "TEMPERATURE", "value": 34.5}
                        ]}
                        """))
                .andExpect(jsonPath("$.alertsRaised").value(1));

        String body = mvc.perform(get("/api/alerts").param("acknowledged", "false"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(body, "$.content[?(@.deviceId == '%s')].id".formatted(deviceId));
        assertThat(ids).hasSize(1);

        mvc.perform(post("/api/alerts/{alertId}/acknowledge", ids.getFirst()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledged").value(true))
                .andExpect(jsonPath("$.value").value(34.5));

        mvc.perform(get("/api/alerts").param("acknowledged", "true"))
                .andExpect(jsonPath("$.content[*].id", hasItem(ids.getFirst())));
    }

    @Test
    void thresholdWithNoBoundsIs400() throws Exception {
        String deviceId = registerDevice();

        mvc.perform(put("/api/devices/{id}/thresholds/HUMIDITY", deviceId).contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- helpers -----------------------------------------------------------------

    private String registerDevice() throws Exception {
        String deviceId = "esp32-it-" + DEVICE_COUNTER.incrementAndGet();
        mvc.perform(post("/api/devices").contentType(APPLICATION_JSON).content("""
                        {"deviceId": "%s", "name": "Integration test board"}
                        """.formatted(deviceId)))
                .andExpect(status().isCreated());
        return deviceId;
    }
}
