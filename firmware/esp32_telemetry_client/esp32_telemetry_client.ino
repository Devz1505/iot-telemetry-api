// ESP32 + DHT22 client for iot-telemetry-api
//
// Board:     any ESP32 dev board (Arduino-ESP32 core 3.x, Boards Manager: "esp32 by Espressif")
// Libraries: "DHT sensor library" by Adafruit (+ its dependency "Adafruit Unified Sensor")
// Wiring:    DHT22 VCC -> 3V3, GND -> GND, DATA -> GPIO4, 10k pull-up between DATA and 3V3
//
// On boot: joins Wi-Fi, starts NTP, registers itself (HTTP 409 = already registered, fine).
// Then every SAMPLE_INTERVAL_MS: reads the DHT22 and POSTs one batch with both readings.
//
// Setup: copy secrets.example.h to secrets.h and fill in your Wi-Fi and API address.
// secrets.h is git-ignored, so credentials never reach GitHub.

#include <WiFi.h>
#include <HTTPClient.h>
#include <DHT.h>
#include <time.h>

#if __has_include("secrets.h")
#include "secrets.h"   // WIFI_SSID, WIFI_PASSWORD, API_BASE
#else
#error "Copy secrets.example.h to secrets.h (same folder) and fill in your values"
#endif

// ---------- device settings ----------
const char* DEVICE_ID     = "esp32-lab-01";
const char* DEVICE_NAME   = "ESP32 DHT22 node";
const uint32_t SAMPLE_INTERVAL_MS = 15000;   // DHT22 needs >= 2 s between reads
// --------------------------------------

constexpr uint8_t DHT_PIN = 4;
DHT dht(DHT_PIN, DHT22);

void connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return;

  Serial.printf("Connecting to %s", WIFI_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  uint32_t start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < 15000) {
    delay(500);
    Serial.print('.');
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf(" connected, IP %s\n", WiFi.localIP().toString().c_str());
  } else {
    Serial.println(" failed, will retry next cycle");
  }
}

// Returns the HTTP status, or a negative HTTPClient error code if the request never completed.
int postJson(const char* path, const String& body) {
  HTTPClient http;
  http.begin(String(API_BASE) + path);
  http.addHeader("Content-Type", "application/json");
  int status = http.POST(body);
  if (status >= 400) {
    Serial.println(http.getString());   // problem+json "detail" says exactly what was wrong
  } else if (status < 0) {
    Serial.printf("HTTP error: %s\n", http.errorToString(status).c_str());
  }
  http.end();
  return status;
}

// NTP has set the clock once time() is past a date in the recent past (2023-11-14).
bool clockIsSynced() {
  return time(nullptr) > 1700000000;
}

// Returns ,"recordedAt":"2026-09-30T10:15:00Z" when the clock is synced, else "".
// Without it the server stamps the reading on arrival.
String recordedAtField() {
  if (!clockIsSynced()) return "";
  time_t now = time(nullptr);
  struct tm utc;
  gmtime_r(&now, &utc);
  char iso[25];
  strftime(iso, sizeof iso, "%Y-%m-%dT%H:%M:%SZ", &utc);
  return String(",\"recordedAt\":\"") + iso + "\"";
}

String readingJson(const char* sensorType, float value, const String& stamp) {
  char number[16];
  snprintf(number, sizeof number, "%.2f", value);
  return String("{\"sensorType\":\"") + sensorType + "\",\"value\":" + number + stamp + "}";
}

void registerDevice() {
  String body = String("{\"deviceId\":\"") + DEVICE_ID + "\",\"name\":\"" + DEVICE_NAME + "\"}";
  int status = postJson("/api/devices", body);
  Serial.printf("Register: HTTP %d%s\n", status, status == 409 ? " (already registered, fine)" : "");
}

void setup() {
  Serial.begin(115200);
  dht.begin();
  connectWiFi();
  configTime(0, 0, "pool.ntp.org", "time.google.com");   // UTC, no DST offset
  registerDevice();
}

void loop() {
  connectWiFi();
  if (WiFi.status() == WL_CONNECTED) {
    float temperature = dht.readTemperature();   // °C, NaN on a failed read
    float humidity = dht.readHumidity();          // %RH, NaN on a failed read

    // Leave failed (NaN) reads out instead of sending garbage.
    String stamp = recordedAtField();
    String readings;
    if (!isnan(temperature)) readings += readingJson("TEMPERATURE", temperature, stamp);
    if (!isnan(humidity)) {
      if (readings.length() > 0) readings += ",";
      readings += readingJson("HUMIDITY", humidity, stamp);
    }

    if (readings.length() == 0) {
      Serial.println("DHT22 read failed: check wiring and the pull-up resistor");
    } else {
      String path = String("/api/devices/") + DEVICE_ID + "/readings";
      int status = postJson(path.c_str(), String("{\"readings\":[") + readings + "]}");
      Serial.printf("T=%.2f C  RH=%.1f %%  -> HTTP %d\n", temperature, humidity, status);
    }
  }
  delay(SAMPLE_INTERVAL_MS);
}
