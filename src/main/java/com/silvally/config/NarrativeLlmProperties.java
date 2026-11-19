package com.silvally.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "silvally.narrative.llm")
public record NarrativeLlmProperties(
        String apiKey,
        String baseUrl,
        @DefaultValue("k-exaone_v2.5") String model,
        @DefaultValue("20s") Duration timeout,
        @DefaultValue("700") int maxTokens,
        @DefaultValue("0.8") double temperature
) {
    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String toString() {
        return "NarrativeLlmProperties[apiKey=" + (enabled() ? "****" : "") + ", baseUrl=" + baseUrl
                + ", model=" + model + ", timeout=" + timeout + "]";
    }
}
