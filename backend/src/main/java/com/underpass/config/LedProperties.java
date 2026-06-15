package com.underpass.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "led")
public class LedProperties {
    private String apiUrl;
    private String apiKey;
    private String dangerText;
}
