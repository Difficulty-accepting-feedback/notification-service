package com.grow.notification_service.analysis.presentation.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.grow.notification_service.quiz.application.dto.QuizItem;

/**
 * 퀴즈 아이템과 세션 정보를 함께 담는 DTO
 * @param sessionId 세션 ID
 * @param items 퀴즈 아이템 목록
 * @param createdAt 생성일
 */
public record QuizItemWithSession(
	String sessionId,
	List<QuizItem> items,
	LocalDateTime createdAt
) {}