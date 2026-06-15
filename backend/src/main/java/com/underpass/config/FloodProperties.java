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
}
