package com.example.blog.modules.article.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 文章向量化相关配置：
 * EmbeddingModel Bean 由 langchain4j-open-ai-spring-boot-starter 根据
 * application.yml 中的 langchain4j.open-ai.embedding-model.* 自动装配，
 * 这里只负责异步执行器与开启 @Async 支持。
 */
@Configuration
@EnableAsync
public class EmbeddingConfig {

    /**
     * 向量化任务专用线程池，避免阻塞 Web 请求
     */
    @Bean(name = "vectorTaskExecutor")
    public Executor vectorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("vector-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
