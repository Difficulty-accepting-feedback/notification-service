package com.grow.notification_service.analysis.infra.llm;

import java.util.List;
import java.util.Map;

import com.google.genai.types.Schema;

/** Gemini responseSchema 재사용 유틸 (google-genai 1.16.0 호환) */
public final class JsonSchemas {
	private JsonSchemas() {
	}

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
				"question", Schema.builder().type("string").build(),
				"choices", choicesArray(),
				"answer", Schema.builder().type("string").build(),
				"explain", Schema.builder().type("string").build(),
				"level", levelEnum(),
				"categoryId", Schema.builder().type("integer").build() // 필요 시 "number"
			))
			.required(List.of("question", "choices", "answer", "explain", "level", "categoryId"))
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
				"keyword", Schema.builder().type("string").build(),
				"conceptSummary", Schema.builder().type("string").build()
			))
			.required(List.of("keyword", "conceptSummary"))
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
			.required(List.of("focusConcepts", "futureConcepts"))
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
	 * 로드맵
	 * - Structure:
	 *   • period        : { totalWeeks(int), hoursPerWeek(int) }
	 *   • coreConcepts  : [{
	 *        name, why(minLen 120), priority(int), level,
	 *        benefits[string>=2], realWorldExample(string), commonMistakes[string>=1],
	 *        learningOutcomes[string>=1], prerequisites[string*]
	 *     }] (minItems=4; 권장 6~12)
	 *   • weeklyPlan    : [{ week(int), theme, focus[], activities[], assignments[], evaluation[], expectedHours(int) }] (minItems=4)
	 *   • milestones    : [{ week(int), goal, verification[] }]                                (minItems=1)
	 *   • references    : [{ type, title, url? }]                                              (optional)
	 */
	public static Schema roadmap() {
		Schema period = Schema.builder()
			.type("object")
			.properties(Map.of(
				"totalWeeks", Schema.builder().type("integer").build(),
				"hoursPerWeek", Schema.builder().type("integer").build()
			))
			.required(List.of("totalWeeks", "hoursPerWeek"))
			.build();

		// array utils
		Schema stringArrayAtLeast1 = Schema.builder()
			.type("array").minItems(1L)
			.items(Schema.builder().type("string").build())
			.build();

		Schema stringArrayAtLeast2 = Schema.builder()
			.type("array").minItems(2L)
			.items(Schema.builder().type("string").build())
			.build();

		// coreConcept item
		Schema coreConcept = Schema.builder()
			.type("object")
			.properties(Map.of(
				"name", Schema.builder().type("string").build(),
				"why", Schema.builder().type("string").minLength(120L).build(),
				"priority", Schema.builder().type("integer").build(),
				"level", Schema.builder().type("string").build(),
				"benefits", stringArrayAtLeast2, // 도움이되는점
				"realWorldExample", Schema.builder().type("string").build(),  // 현업예시
				"commonMistakes", stringArrayAtLeast1, // 자주틀리는포인트
				"learningOutcomes", stringArrayAtLeast1, // 학습성과
				"prerequisites", Schema.builder() // 선수지식 (optional)
					.type("array")
					.items(Schema.builder().type("string").build())
					.build()
			))
			.required(List.of("name", "why", "priority", "level", "benefits", "realWorldExample", "commonMistakes",
				"learningOutcomes"))
			.build();

		// weekly plan (english keys)
		Schema weekPlan = Schema.builder()
			.type("object")
			.properties(Map.of(
				"week", Schema.builder().type("integer").build(),
				"theme", Schema.builder().type("string").build(),
				"focus", stringArrayAtLeast1,
				"activities", stringArrayAtLeast1, // 학습활동
				"assignments", stringArrayAtLeast1, // 실습과제
				"evaluation", stringArrayAtLeast1, // 평가
				"expectedHours", Schema.builder().type("integer").build() // 예상시간
			))
			.required(List.of("week", "theme", "focus", "activities", "assignments", "evaluation", "expectedHours"))
			.build();

		// milestones (english keys)
		Schema milestone = Schema.builder()
			.type("object")
			.properties(Map.of(
				"week", Schema.builder().type("integer").build(),
				"goal", Schema.builder().type("string").build(),
				"verification", stringArrayAtLeast1 // 검증방법
			))
			.required(List.of("week", "goal", "verification"))
			.build();

		// references
		Schema reference = Schema.builder()
			.type("object")
			.properties(Map.of(
				"type", Schema.builder().type("string").build(),
				"title", Schema.builder().type("string").build(),
				"url", Schema.builder().type("string").build()
			))
			.required(List.of("type", "title"))
			.build();

		return Schema.builder()
			.type("object")
			.properties(Map.of(
				"period", period, // 기간설정
				"coreConcepts", Schema.builder().type("array").minItems(4L).items(coreConcept).build(), // 핵심개념
				"weeklyPlan", Schema.builder().type("array").minItems(4L).items(weekPlan).build(), // 주차로드맵
				"milestones", Schema.builder().type("array").minItems(1L).items(milestone).build(), // 마일스톤
				"references", Schema.builder().type("array").items(reference).build() // 참고자료
			))
			.required(List.of("period", "coreConcepts", "weeklyPlan", "milestones"))
			.build();
	}

}