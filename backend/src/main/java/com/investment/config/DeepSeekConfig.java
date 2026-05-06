package com.investment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekConfig {
    private String apiKey;
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-chat";
    private double temperature = 0.1;
    private int maxTokens = 4096;
    private int maxRetries = 3;
    private int timeoutMs = 90000;
    private int rateLimitDelayMs = 1200;
}
