package com.grow.notification_service.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
			.allowedOrigins("http://localhost:3000") // Next.js
			.allowedMethods("GET", "POST", "DELETE", "OPTIONS")
			.allowedHeaders("X-Authorization-Id", "Content-Type", "Accept", "Origin")
			.allowCredentials(true)
			.maxAge(3600);
	}
}