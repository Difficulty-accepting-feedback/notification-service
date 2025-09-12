package com.grow.notification_service.quiz.infra.persistence.mapper;

import com.grow.notification_service.quiz.domain.model.Quiz;
import com.grow.notification_service.quiz.infra.persistence.entity.QuizJpaEntity;
import com.grow.notification_service.quiz.infra.persistence.enums.QuizLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuizMapperTest {

	private final QuizMapper mapper = new QuizMapper();

	private QuizJpaEntity buildEntity() {
		List<String> choices = new ArrayList<>();
		choices.add("A");
		choices.add("B");
		choices.add("C");

		return QuizJpaEntity.builder()
			.quizId(10L)
			.question("Q?")
			.answer("B")
			.explain("because")
			.level(QuizLevel.EASY)
			.categoryId(3L)
			.choices(choices)
			.build();
	}

	private Quiz buildDomain() {
		List<String> choices = List.of("X", "Y", "Z");
		return new Quiz(
			99L,
			"What is GROW?",
			choices,
			"Y",
			"GROW = msa+ddd+gateway on k8s",
			QuizLevel.NORMAL,
			7L
		);
	}

	@Test
	@DisplayName("toDomain: JPA 엔티티가 도메인으로 정확히 매핑된다")
	void toDomain_mapsAllFields() {
		QuizJpaEntity e = buildEntity();

		Quiz d = mapper.toDomain(e);

		assertAll(
			() -> assertEquals(e.getQuizId(), d.getQuizId()),
			() -> assertEquals(e.getQuestion(), d.getQuestion()),
			() -> assertEquals(e.getAnswer(), d.getAnswer()),
			() -> assertEquals(e.getExplain(), d.getExplain()),
			() -> assertEquals(e.getLevel(), d.getLevel()),
			() -> assertEquals(e.getCategoryId(), d.getCategoryId()),
			() -> assertEquals(e.getChoices(), d.getChoices(), "choices 내용이 동일해야 함")
		);
	}

	@Test
	@DisplayName("toDomain: choices는 방어적 복사가 적용되어 원본 변경의 영향을 받지 않는다")
	void toDomain_defensiveCopyOfChoices() {
		QuizJpaEntity e = buildEntity();
		Quiz d = mapper.toDomain(e);

		// 원본 엔티티 리스트를 변경
		e.getChoices().add("NEW");

		// 도메인 리스트는 변경되면 안 됨 (불변 사본이어야 함)
		assertEquals(3, d.getChoices().size(), "도메인 choices는 방어적 복사로 크기가 변하면 안 됨");
		assertFalse(d.getChoices().contains("NEW"));
		// 또한 도메인 리스트는 불변이어야 함
		assertThrows(UnsupportedOperationException.class, () -> d.getChoices().add("X"));
	}

	@Test
	@DisplayName("toEntity: 도메인이 JPA 엔티티로 정확히 매핑된다")
	void toEntity_mapsAllFields() {
		Quiz d = buildDomain();

		QuizJpaEntity e = mapper.toEntity(d);

		assertAll(
			() -> assertEquals(d.getQuizId(), e.getQuizId()),
			() -> assertEquals(d.getQuestion(), e.getQuestion()),
			() -> assertEquals(d.getAnswer(), e.getAnswer()),
			() -> assertEquals(d.getExplain(), e.getExplain()),
			() -> assertEquals(d.getLevel(), e.getLevel()),
			() -> assertEquals(d.getCategoryId(), e.getCategoryId()),
			() -> assertEquals(d.getChoices(), e.getChoices(), "choices 내용이 동일해야 함")
		);
	}

	@Test
	@DisplayName("왕복 변환(Entity→Domain→Entity) 시 값이 보존된다")
	void roundTrip_entity_to_domain_to_entity_preservesValues() {
		QuizJpaEntity original = buildEntity();

		Quiz domain = mapper.toDomain(original);
		QuizJpaEntity roundTripped = mapper.toEntity(domain);

		assertAll(
			() -> assertEquals(original.getQuizId(), roundTripped.getQuizId()),
			() -> assertEquals(original.getQuestion(), roundTripped.getQuestion()),
			() -> assertEquals(original.getAnswer(), roundTripped.getAnswer()),
			() -> assertEquals(original.getExplain(), roundTripped.getExplain()),
			() -> assertEquals(original.getLevel(), roundTripped.getLevel()),
			() -> assertEquals(original.getCategoryId(), roundTripped.getCategoryId()),
			() -> assertEquals(original.getChoices(), roundTripped.getChoices())
		);
	}
}