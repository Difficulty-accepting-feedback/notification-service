package com.grow.notification_service.analysis.application.port;

import com.google.genai.types.Schema;

public interface LlmClientPort {
	String generateJson(String systemPrompt, String userPrompt, Schema responseSchema);
}