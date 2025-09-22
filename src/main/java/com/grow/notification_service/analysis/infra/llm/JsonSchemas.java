package com.grow.notification_service.analysis.infra.llm;

import java.util.List;
import java.util.Map;

import com.google.genai.types.Schema;

/** Gemini responseSchema 재사용 유틸 (google-genai 1.16.0 호환) */
public final class JsonSchemas {
	private JsonSchemas() {}

	/** level: "EASY" | "NORMAL" | "HARD" (SDK enumValues 미사용: 정규식으로 제한) */
	private static Schema levelEnum() {
		return Schema.builder()
			.type("string")
			.pattern("^(EASY|NORMAL|HARD)$")
			.build();
	}

	/** choices: string[4] */
	private static Schema choicesArray() {
		return Schema.builder()
			.type("array")
			.minItems(4L)
			.maxItems(4L)
			.items(Schema.builder().type("string").build())
			.build();
	}

	/** 퀴즈 아이템 스키마 */
	private static Schema quizItem() {
		return Schema.builder()
			.type("object")
			.properties(Map.of(
				"question",   Schema.builder().type("string").build(),
				"choices",    choicesArray(),
				"answer",     Schema.builder().type("string").build(),
				"explain",    Schema.builder().type("string").build(),
				"level",      levelEnum(),
				"categoryId", Schema.builder().type("integer").build() // 필요 시 "number"
			))
			.required(List.of("question","choices","answer","explain","level","categoryId"))
			.build();
	}

	/** 퀴즈 배열(정확히 5문항) */
	public static Schema quizArray() {
		return Schema.builder()
			.type("array")
			.minItems(5L)
			.maxItems(5L)
			.items(quizItem())
			.build();
	}

	/** { "keywords": string[2..5] } */
	public static Schema focusKeywords() {
		return Schema.builder()
			.type("object")
			.properties(Map.of(
				"keywords", Schema.builder()
					.type("array")
					.minItems(2L)
					.maxItems(5L)
					.items(Schema.builder().type("string").build())
					.build()
			))
			.required(List.of("keywords"))
			.build();
	}

	/** { "focusConcepts":[{keyword,conceptSummary}], "futureConcepts": string[2..5] } */
	public static Schema focusSummary() {
		Schema focusConcept = Schema.builder()
			.type("object")
			.properties(Map.of(
				"keyword",        Schema.builder().type("string").build(),
				"conceptSummary", Schema.builder().type("string").build()
			))
			.required(List.of("keyword","conceptSummary"))
			.build();

		return Schema.builder()
			.type("object")
			.properties(Map.of(
				"focusConcepts", Schema.builder()
					.type("array")
					.minItems(1L)
					.items(focusConcept)
					.build(),
				"futureConcepts", Schema.builder()
					.type("array")
					.minItems(2L)
					.maxItems(5L)
					.items(Schema.builder().type("string").build())
					.build()
			))
			.required(List.of("focusConcepts","futureConcepts"))
			.build();
	}

	/** { "futureConcepts": string[2..5] } */
	public static Schema futureOnly() {
		return Schema.builder()
			.type("object")
			.properties(Map.of(
				"futureConcepts", Schema.builder()
					.type("array")
					.minItems(2L)
					.maxItems(5L)
					.items(Schema.builder().type("string").build())
					.build()
			))
			.required(List.of("futureConcepts"))
			.build();
	}

	/**
	 * 스케줄형 학습 로드맵(JSON) 스키마 생성
	 * - 구성 필드:
	 *   • 기간설정        : { 총주차(int), 주당시간(int) }                (필수)
	 *   • 핵심개념        : [{ name, why, priority(int), level }]        (최소 4개)
	 *   • 주차로드맵      : [{ week(int), theme, focus[], 학습활동[], 실습과제[], 평가[], 예상시간(int) }] (최소 4개)
	 *   • 마일스톤        : [{ week(int), goal, 검증방법[] }]            (최소 1개)
	 *   • 참고자료        : [{ type, title, url? }]                       (선택)
	 * - 제약: "기간설정", "핵심개념", "주차로드맵", "마일스톤" 필수
	 */
	public static Schema roadmap() {
		// 기간설정: 총주차, 주당시간
		Schema period = Schema.builder()
			.type("object")
			.properties(Map.of(
				"총주차",   Schema.builder().type("integer").build(),
				"주당시간", Schema.builder().type("integer").build()
			))
			.required(List.of("총주차","주당시간"))
			.build();

		// 핵심개념 항목
		Schema coreConcept = Schema.builder()
			.type("object")
			.properties(Map.of(
				"name",     Schema.builder().type("string").build(),
				"why",      Schema.builder().type("string").build(),
				"priority", Schema.builder().type("integer").build(),
				"level",    Schema.builder().type("string").build()
			))
			.required(List.of("name","why","priority","level"))
			.build();

		// 주차로드맵
		Schema weekPlan = Schema.builder()
			.type("object")
			.properties(Map.of(
				"week",     Schema.builder().type("integer").build(),
				"theme",    Schema.builder().type("string").build(),
				"focus",    Schema.builder()
					.type("array").minItems(1L)
					.items(Schema.builder().type("string").build())
					.build(),
				"학습활동", Schema.builder()
					.type("array").minItems(1L)
					.items(Schema.builder().type("string").build())
					.build(),
				"실습과제", Schema.builder()
					.type("array").minItems(1L)
					.items(Schema.builder().type("string").build())
					.build(),
				"평가",     Schema.builder()
					.type("array").minItems(1L)
					.items(Schema.builder().type("string").build())
					.build(),
				"예상시간", Schema.builder().type("integer").build()
			))
			.required(List.of("week","theme","focus","학습활동","실습과제","평가","예상시간"))
			.build();

		// 마일스톤
		Schema milestone = Schema.builder()
			.type("object")
			.properties(Map.of(
				"week",     Schema.builder().type("integer").build(),
				"goal",     Schema.builder().type("string").build(),
				"검증방법", Schema.builder()
					.type("array").minItems(1L)
					.items(Schema.builder().type("string").build())
					.build()
			))
			.required(List.of("week","goal","검증방법"))
			.build();

		// 참고자료
		Schema reference = Schema.builder()
			.type("object")
			.properties(Map.of(
				"type",  Schema.builder().type("string").build(),
				"title", Schema.builder().type("string").build(),
				"url",   Schema.builder().type("string").build()
			))
			.required(List.of("type","title"))
			.build();

		return Schema.builder()
			.type("object")
			.properties(Map.of(
				"기간설정",   period,
				"핵심개념",   Schema.builder().type("array").minItems(4L).items(coreConcept).build(),
				"주차로드맵", Schema.builder().type("array").minItems(4L).items(weekPlan).build(),
				"마일스톤",   Schema.builder().type("array").minItems(1L).items(milestone).build(),
				"참고자료",   Schema.builder().type("array").items(reference).build()
			))
			.required(List.of("기간설정","핵심개념","주차로드맵","마일스톤"))
			.build();
	}
}