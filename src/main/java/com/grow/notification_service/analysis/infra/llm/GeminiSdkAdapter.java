package com.grow.notification_service.analysis.infra.llm;

import org.springframework.stereotype.Component;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.grow.notification_service.analysis.application.port.LlmClientPort;
import com.grow.notification_service.analysis.infra.config.GeminiProperties;

import lombok.RequiredArgsConstructor;

/**
 * Gemini LLM SDK 어댑터
 * LlmClientPort 인터페이스를 구현하여 Gemini SDK를 통해 텍스트 생성을 수행합니다.
 * systemInstruction과 userPrompt를 받아 Gemini API를 호출하고, JSON 형식의 응답을 반환합니다.
 * GeminiProperties를 통해 모델 설정과 최대 토큰 수를 주입받습니다.
 */
@Component
@RequiredArgsConstructor
public class GeminiSdkAdapter implements LlmClientPort {

	private final Client client;
	private final GeminiProperties properties;

	/**
	 * Gemini SDK를 사용하여 JSON 형식의 텍스트 생성
	 * @param systemPrompt 시스템 프롬프트
	 * @param userPrompt 사용자 프롬프트
	 * @return 생성된 JSON 텍스트
	 */
	@Override
	public String generateJson(String systemPrompt, String userPrompt) {
		Content system = Content.fromParts(Part.fromText(systemPrompt));

		GenerateContentConfig config = GenerateContentConfig.builder()
			.responseMimeType("application/json")
			.systemInstruction(system)
			.maxOutputTokens(properties.maxOutputTokens())
			.build();

		GenerateContentResponse resp =
			client.models.generateContent(properties.model(), userPrompt, config);

		return resp.text();
	}
}