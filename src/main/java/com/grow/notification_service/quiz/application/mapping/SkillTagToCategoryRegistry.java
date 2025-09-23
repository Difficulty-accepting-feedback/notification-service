package com.grow.notification_service.quiz.application.mapping;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.global.exception.QuizException;

/**
 * 외부(Study.Group.SkillTag 코드) <-> 내부(quiz.categoryId) 매핑.
 */
@Component
public class SkillTagToCategoryRegistry {

	// 매핑: SkillTag.name() -> quiz.categoryId
	private static final Map<String, Long> MAP = Map.ofEntries(
		// STUDY
		Map.entry("JAVA_PROGRAMMING", 1L),
		Map.entry("PYTHON_DATA_SCIENCE", 2L),
		Map.entry("ENGLISH_CONVERSATION", 3L),
		Map.entry("MATH_PROBLEM_SOLVING", 4L),
		Map.entry("HISTORY_EXPLORATION", 5L),
		Map.entry("FRENCH_LANGUAGE", 6L),
		Map.entry("ECONOMICS", 7L),
		Map.entry("AI_MACHINE_LEARNING", 8L),
		Map.entry("WEB_DEVELOPMENT", 9L),
		Map.entry("DATABASE_MANAGEMENT", 10L),
		Map.entry("TOEIC", 11L),

		// HOBBY
		Map.entry("HIKING", 12L),
		Map.entry("COOKING", 13L),
		Map.entry("GUITAR_PLAYING", 14L),
		Map.entry("PHOTOGRAPHY", 15L),
		Map.entry("BOOK_READING", 16L),
		Map.entry("GARDENING", 17L),
		Map.entry("BOARD_GAMES", 18L),
		Map.entry("PAINTING", 19L),
		Map.entry("YOGA", 20L),
		Map.entry("DANCING", 21L),
		Map.entry("FISHING", 22L),
		Map.entry("CYCLING", 23L),
		Map.entry("KNITTING", 24L),
		Map.entry("TRAVEL_PLANNING", 25L),
		Map.entry("MOVIE_APPRECIATION", 26L),

		// MENTORING
		Map.entry("CAREER_COACHING", 27L),
		Map.entry("STARTUP_GUIDANCE", 28L),
		Map.entry("LEADERSHIP_COACHING", 29L),
		Map.entry("IT_JOB_PREPARATION", 30L),
		Map.entry("ART_MENTORING", 31L),
		Map.entry("FINANCIAL_INVESTMENT", 32L),
		Map.entry("HEALTH_COACHING", 33L),
		Map.entry("PUBLIC_SPEAKING", 34L),
		Map.entry("PROJECT_MANAGEMENT", 35L),
		Map.entry("MARKETING_STRATEGY", 36L),
		Map.entry("NEGOTIATION_SKILLS", 37L),
		Map.entry("TIME_MANAGEMENT", 38L),
		Map.entry("CREATIVE_WRITING", 39L),
		Map.entry("NETWORKING", 40L),
		Map.entry("EMOTIONAL_INTELLIGENCE", 41L),
		Map.entry("BACKEND_DEVELOPMENT", 42L),
		Map.entry("FRONTEND_DEVELOPMENT", 43L),
		Map.entry("MOBILE_APP_DEVELOPMENT", 44L),
		Map.entry("GRAPHIC_DESIGN", 45L),
		Map.entry("CONTENT_CREATION", 46L)
	);

	// 매핑: quiz.categoryId -> SkillTag.name()
	private static final Map<Long, String> REVERSE =
		MAP.entrySet().stream()
			.collect(Collectors.toUnmodifiableMap(
				Map.Entry::getValue, // categoryId
				Map.Entry::getKey    // skillTagCode
			));

	/** 외부 SkillTag -> 내부 categoryId */
	public Long resolveOrThrow(String skillTagCode) {
		Long id = MAP.get(skillTagCode);
		if (id == null) throw new QuizException(ErrorCode.INVALID_SKILL_TAG);
		return id;
	}

	/** 내부 categoryId -> 외부 SkillTag */
	public String toSkillTagOrThrow(Long categoryId) {
		String code = REVERSE.get(categoryId);
		if (code == null) throw new QuizException(ErrorCode.CATEGORY_MISMATCH);
		return code;
	}
}