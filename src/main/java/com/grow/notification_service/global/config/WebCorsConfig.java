package com.grow.notification_service.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
			.allowedOriginPatterns("http://localhost:*")
			.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
			.allowedHeaders(
				"X-Authorization-Id",
				"Content-Type",
				"Accept",
				"Origin",
				"Last-Event-ID",
				"Cache-Control",
				"Pragma"
			)
			// 클라이언트에서 쿠키/세션 쓰면 필수
			.allowCredentials(true)
			// 노출 헤더(필수는 아님, 디버깅 편의용)
			.exposedHeaders(
				"Content-Type",
				"Cache-Control",
				"X-Accel-Buffering"
			)
			// 프리플라이트 캐시
			.maxAge(86400);
	}
}