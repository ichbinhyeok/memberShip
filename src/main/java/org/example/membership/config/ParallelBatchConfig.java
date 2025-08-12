package org.example.membership.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// ParallelBatchConfig.java
@Configuration
public class ParallelBatchConfig {
    @Bean("batchExecutor")
    public ThreadPoolTaskExecutor batchExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        int cores = Runtime.getRuntime().availableProcessors();
        ex.setCorePoolSize(Math.min(8, Math.max(2, cores))); // 2~8 사이
        ex.setMaxPoolSize(ex.getCorePoolSize());
        ex.setQueueCapacity(0); // 큐 적체 대신 바로 거절 → 호출부에서 back-pressure
        ex.setThreadNamePrefix("batch-chunk-");
        ex.initialize();
        return ex;
    }
}
