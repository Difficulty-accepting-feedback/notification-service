package com.grow.notification_service.analysis.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gemini LLM 설정 프로퍼티
 * @param apiKey Gemini API 키
 * @param model 사용할 모델 이름
 * @param maxOutputTokens 최대 출력 토큰 수
 */
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
	String apiKey,
	String model,
	Integer maxOutputTokens
) {}