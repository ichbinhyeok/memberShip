package org.example.membership.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {
    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(10);
    }

    @Bean("batchExecutorService")
    public ExecutorService batchExecutorService() {
        return Executors.newFixedThreadPool(6, r -> {
            Thread t = new Thread(r, "batch-chunk-");
            t.setDaemon(true);
            return t;
        });
    }
}