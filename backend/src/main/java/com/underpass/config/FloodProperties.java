package com.underpass.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "flood")
public class FloodProperties {
    private int thresholdMm = 100;
    private int liftHeightCm = 15;
    private int confirmRounds = 3;
    private long cooldownSeconds = 60;

    private Forecast forecast = new Forecast();

    @Data
    public static class Forecast {
        private int preLiftHeightCm = 5;
        private double flowSurgeRatio = 3.0;
        private long flowWindowMinutes = 10;
        private long forecastCooldownMinutes = 30;
        private double minRainMmPerHour = 0.5;
    }
}
