package com.grow.notification_service.quiz.application.mapping;

import com.grow.notification_service.global.exception.QuizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillTagToCategoryRegistryTest {

	private final SkillTagToCategoryRegistry registry = new SkillTagToCategoryRegistry();

	@Test
	@DisplayName("정상: 매핑된 SkillTag 코드는 올바른 categoryId 반환")
	void resolve_success() {
		Long javaCat = registry.resolveOrThrow("JAVA_PROGRAMMING");
		Long engCat = registry.resolveOrThrow("ENGLISH_CONVERSATION");
		Long dbCat = registry.resolveOrThrow("PYTHON_DATA_SCIENCE");

		assertEquals(1L, javaCat);
		assertEquals(2L, engCat);
		assertEquals(3L, dbCat);
	}

	@Test
	@DisplayName("예외: 존재하지 않는 SkillTag 코드는 QuizException 발생")
	void resolve_fail_invalidSkillTag() {
		assertThrows(QuizException.class,
			() -> registry.resolveOrThrow("UNKNOWN_TAG"));
	}
}