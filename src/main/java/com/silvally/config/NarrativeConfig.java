package com.silvally.config;

import com.silvally.service.LlmNarrativeGenerator;
import com.silvally.service.NarrativeGenerator;
import com.silvally.service.TemplateNarrativeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(NarrativeLlmProperties.class)
public class NarrativeConfig {

    private static final Logger log = LoggerFactory.getLogger(NarrativeConfig.class);

    @Bean
    public NarrativeGenerator narrativeGenerator(NarrativeLlmProperties properties, ObjectMapper json) {
        if (!properties.enabled()) {
            log.info("Narrative generator: TEMPLATE (LLM API key not configured)");
            return new TemplateNarrativeGenerator();
        }
        log.info("Narrative generator: LLM model={} baseUrl={}", properties.model(), properties.baseUrl());
        return new LlmNarrativeGenerator(chatModel(properties), properties, json);
    }

    @Bean
    public TaskExecutor narrativeExecutor(NarrativeLlmProperties properties) {
        if (!properties.enabled()) {
            return new SyncTaskExecutor();
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("narrative-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    private static OpenAiChatModel chatModel(NarrativeLlmProperties properties) {
        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder()
                .apiKey(properties.apiKey())
                .model(properties.model())
                .timeout(properties.timeout())
                .maxRetries(0)
                .maxTokens(properties.maxTokens())
                .temperature(properties.temperature());
        if (properties.baseUrl() != null && !properties.baseUrl().isBlank()) {
            options.baseUrl(properties.baseUrl());
        }
        return OpenAiChatModel.builder().options(options.build()).build();
    }
}
