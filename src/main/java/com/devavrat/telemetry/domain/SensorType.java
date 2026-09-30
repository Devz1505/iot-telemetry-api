package com.devavrat.telemetry.domain;

/**
 * The kinds of measurement a device can report.
 *
 * Each type carries its unit and the physically plausible range of the
 * common sensors used for it (e.g. DHT22 humidity is 0-100 %RH). A value
 * outside that range almost always means a wiring fault or a failed read,
 * so the API rejects it instead of storing garbage.
 */
public enum SensorType {

    TEMPERATURE("°C", -40, 125),     // DHT22 / DS18B20 range
    HUMIDITY("%RH", 0, 100),
    PRESSURE("hPa", 300, 1100),      // BMP280 range
    LIGHT("lx", 0, 200_000),
    VOLTAGE("V", 0, 36);             // battery / supply monitoring

    private final String unit;
    private final double minPlausible;
    private final double maxPlausible;

    SensorType(String unit, double minPlausible, double maxPlausible) {
        this.unit = unit;
        this.minPlausible = minPlausible;
        this.maxPlausible = maxPlausible;
    }

    public String unit() {
        return unit;
    }

    public double minPlausible() {
        return minPlausible;
    }

    public double maxPlausible() {
        return maxPlausible;
    }

    public boolean isPlausible(double value) {
        return Double.isFinite(value) && value >= minPlausible && value <= maxPlausible;
    }
}
