package com.grow.notification_service.analysis.application.port;

public interface LlmClientPort {
	String generateJson(String systemPrompt, String userPrompt);
}