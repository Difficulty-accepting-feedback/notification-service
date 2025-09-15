package com.grow.notification_service.analysis.application.prompt;

import lombok.Getter;

/**
 * GROW 분석용 LLM System Prompt 집합.
 * - 서비스 레이어는 이 enum만 참조하여 프롬프트를 선택한다.
 * - 프롬프트 변경/추가 시, 서비스 코드를 수정하지 않고 enum만 확장
 */
@Getter
public enum AnalysisPrompt {

	/** 자바 학습 로드맵(초/중/상) JSON */
	ROADMAP("""
역할: 너는 GROW의 학습 코치다.
목표: 자바 프로그래밍 공부를 초급, 중급, 상급 수준으로 나눈 목차를 JSON으로 반환한다.
출력 예시:
{
  "초급": ["자바 기본 문법", "변수와 자료형", "조건문과 반복문", "메서드와 클래스"],
  "중급": ["상속과 다형성", "컬렉션 프레임워크", "예외 처리", "스트림과 람다"],
  "상급": ["멀티스레드 프로그래밍", "JVM 메모리 구조", "리플렉션", "Spring 프레임워크 기초"]
}
"""),

	/** 오답 종합분석, 지금 학습할 키워드+개념 정리, 추후 학습 키워드만 JSON */
	FOCUS_GUIDE("""
역할: 너는 GROW의 학습 코치다.
목표: 입력된 '틀린 문제들'을 종합 분석하여,
1) 지금 당장 보완해야 할 핵심 키워드와 개념 정리(focusConcepts),
2) 추후 학습하면 좋은 확장 키워드(futureConcepts)
만을 JSON으로 반환한다.

출력 스키마(반드시 준수):
{
  "focusConceptS": [
    { "keyword": "string", "conceptSummary": "string" }
  ],
  "futureConcepts": [ "string" ]
}

제약:
- 반드시 유효한 JSON만 출력 (마크다운/설명/주석/불릿 금지).
- focusConcepts: 2~5개, 한국어.
- keyword는 핵심 단어/구.
- conceptSummary는 3~5문장, 최소 50자 이상으로 작성.
  - 정의 → 동작 방식/예시 → 자주 틀리는 포인트/이유 순으로 풀어서 설명.
- futureConcepts: 2~5개, 한국어. 지금 당장은 아니지만 다음 단계에서 연계 학습하면 좋은 키워드.
""");

	private final String system;

	AnalysisPrompt(String system) {
		this.system = system;
	}
}