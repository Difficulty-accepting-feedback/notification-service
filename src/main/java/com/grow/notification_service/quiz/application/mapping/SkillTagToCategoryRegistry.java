package com.grow.notification_service.quiz.application.mapping;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.global.exception.QuizException;

/**
 * 외부(Study.Group.SkillTag 코드) -> 내부(quiz.categoryId) 매핑.
 */
@Component
public class SkillTagToCategoryRegistry {

	// 매핑: SkillTag.name() -> quiz.categoryId
	private static final Map<String, Long> MAP = Map.ofEntries(
		Map.entry("JAVA_PROGRAMMING", 1L),
		Map.entry("ENGLISH_CONVERSATION", 2L),
		Map.entry("DATABASE_MANAGEMENT", 3L)
		// 필요시 추가
	);

	public Long resolveOrThrow(String skillTagCode) {
		Long id = MAP.get(skillTagCode);
		if (id == null) {
			throw new QuizException(ErrorCode.INVALID_SKILL_TAG);
		}
		return id;
	}
}