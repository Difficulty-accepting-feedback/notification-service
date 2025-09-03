package com.grow.notification_service.qna.application.service;

public interface QnaCommandService {
	Long createQuestion(Long memberId, String content, Long parentId);
	Long createAnswer(Long memberId, Long questionId, String content);
}