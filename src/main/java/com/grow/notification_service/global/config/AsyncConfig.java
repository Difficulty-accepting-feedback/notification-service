package com.grow.notification_service.global.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "aiReviewExecutor")
	public Executor aiReviewExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(100);
		ex.setThreadNamePrefix("ai-review-");
		ex.setWaitForTasksToCompleteOnShutdown(false);
		ex.initialize();
		return ex;
	}
}