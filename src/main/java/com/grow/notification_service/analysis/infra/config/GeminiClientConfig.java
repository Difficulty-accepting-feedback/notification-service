package com.grow.notification_service.analysis.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.genai.Client;

import lombok.RequiredArgsConstructor;

/**
 * Gemini LLM 클라이언트 설정
 * GeminiProperties를 통해 API 키와 모델 정보를 주입받아 초기화됩니다.
 */
@Configuration
@RequiredArgsConstructor
public class GeminiClientConfig {

	private final GeminiProperties properties;

	@Bean
	public Client geminiClient() {
		return Client.builder()
			.apiKey(properties.apiKey())
			.build();
	}
}