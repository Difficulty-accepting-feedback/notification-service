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

	/** { "초급": string[], "중급": string[], "상급": string[] } */
	public static Schema roadmap() {
		Schema stringArray = Schema.builder()
			.type("array")
			.minItems(3L) // 필요 시 조정
			.items(Schema.builder().type("string").build())
			.build();

		return Schema.builder()
			.type("object")
			.properties(Map.of(
				"초급", stringArray,
				"중급", stringArray,
				"상급", stringArray
			))
			.required(List.of("초급","중급","상급"))
			.build();
	}
}