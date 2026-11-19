package com.silvally.config;

import com.silvally.service.InterviewQuestionGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import java.util.concurrent.*;

@Configuration
public class InterviewConfig {
    @Bean @ConditionalOnMissingBean(InterviewQuestionGenerator.class)
    public InterviewQuestionGenerator mockInterviewQuestionGenerator() {
        return context -> new InterviewQuestionGenerator.Generated(context.baseQuestion()
                + (context.recentActivities().isEmpty() ? "" : " 최근 활동: " + String.join(" / ", context.recentActivities())), "MOCK");
    }
    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService interviewQuestionExecutor() { return Executors.newVirtualThreadPerTaskExecutor(); }
}
