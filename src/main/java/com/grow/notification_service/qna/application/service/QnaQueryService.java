package com.grow.notification_service.qna.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.grow.notification_service.qna.application.dto.QnaThreadResponse;
import com.grow.notification_service.qna.domain.model.QnaPost;

public interface QnaQueryService {
	QnaThreadResponse getThreadAsAdmin(Long rootQuestionId, Long viewerId);
	QnaThreadResponse getMyThread(Long rootQuestionId, Long viewerId);
	Page<QnaPost> getRootQuestionsAsAdmin(Pageable pageable, Long viewerId);
	Page<QnaPost> getMyRootQuestions(Pageable pageable, Long viewerId);
}