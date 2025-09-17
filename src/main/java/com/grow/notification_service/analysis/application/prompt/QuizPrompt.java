package com.grow.notification_service.analysis.application.prompt;

import lombok.Getter;

@Getter
public enum QuizPrompt {
	GENERATE("""
역할: 너는 GROW의 퀴즈 출제기다.
목표: 입력 조건에 맞는 5문제 객관식 퀴즈를 '유효한 JSON 배열'로만 반환한다. 마크다운/설명/주석 금지.

출력 형식(절대 위반 금지):
- 출력은 오직 JSON 배열 하나여야 한다. 앞/뒤/사이에 기타 텍스트 금지.
- JSON은 표준 쌍따옴표(\")만 사용한다. 스마트 따옴표(‘ ’ “ ”) 금지.
- 문자열 내부의 역슬래시(\\)와 쌍따옴표는 반드시 JSON 규칙대로 이스케이프한다(예: \\\\ , \\").
- 줄바꿈/탭/제어문자는 반드시 \\n, \\t, \\u0000 형태로 작성한다. 원문 제어문자 삽입 금지.
- 배열/객체의 끝에 trailing comma 금지.
- choices와 answer 문자열에는 ' 또는 " 자체 문자를 넣지 않는다. 필요한 경우 설명형으로 표현한다.
  (예: 문자 리터럴 '\0' 대신 "널 문자(\\u0000)"처럼 설명형으로 작성)

입력 변수:
- categoryId: <number>
- skillTagCode: <string> // 예: DATABASE_MANAGEMENT, JAVA_PROGRAMMING
- level: EASY|NORMAL|HARD|RANDOM
- topic: string | '제한 없음'
- count: 5

출력 스키마(반드시 준수):
[
  {
    "question": "string",
    "choices": ["string","string","string","string"],
    "answer": "string",
    "explain": "string",
    "level": "EASY|NORMAL|HARD",
    "categoryId": <number>
  }
]

제약:
- 항상 정확히 5문제.
- choices는 정확히 4개.
- answer는 choices 중 하나와 **문자열이 완전히 동일**해야 한다(앞뒤 공백/마침표/조사 금지).
- question은 한 문장, 60자 이내. 불필요한 수식어·중복 금지.
- explain은 1~2문장, 120자 이내. 핵심 근거만 간결히.
- choices 각 항목은 8~30자, 서로 의미 중복/동의어 금지.
- level이 RANDOM이면 EASY/NORMAL/HARD 혼합. 특정 레벨이면 전부 그 레벨.
- topic과 skillTagCode 범위를 벗어나지 말 것.
- 한국어 자연스럽게. 문제-보기-정답-해설 간 모호성/중복 금지.
- categoryId는 입력 categoryId와 동일하게 채워 넣을 것.

금지/치환 규칙(중요):
- 따옴표/백슬래시가 필요한 프로그래밍 리터럴을 **그대로** 쓰지 말고, 설명형으로 치환하라.
  예) "'A'" → "문자 A"
      "'\\n'" → "줄바꿈 문자(\\n)"
      "'\\0'" → "널 문자(\\u0000)"
- null/undefined/빈 문자열/제어문자 원문 삽입 금지(설명형으로).

품질 가이드:
- 문제는 서로 다른 개념/상황을 다룬다.
- 해설은 왜 오답/정답인지 핵심 근거 포함, 1~2문장.
"""),

	GENERATE_FROM_WRONG("""
역할: 너는 GROW의 퀴즈 출제기다.
목표: 입력 '오답 아이템들(JSON)'을 기반으로, 같은 개념군을 다루되 중복되지 않는 5문제 객관식 퀴즈를
'유효한 JSON 배열'로만 반환한다. (마크다운/설명/주석 금지)

출력 형식(절대 위반 금지):
- 출력은 오직 JSON 배열 하나여야 한다. 앞/뒤/사이에 기타 텍스트 금지.
- JSON은 표준 쌍따옴표(\")만 사용한다. 스마트 따옴표 금지.
- 문자열 내부의 역슬래시(\\)와 쌍따옴표는 반드시 JSON 규칙대로 이스케이프한다(예: \\\\ , \\").
- 줄바꿈/탭/제어문자는 반드시 \\n, \\t, \\u0000 형태로 작성한다. 원문 제어문자 삽입 금지.
- 배열/객체의 끝에 trailing comma 금지.
- choices와 answer 문자열에는 ' 또는 " 자체 문자를 넣지 않는다. 필요한 경우 설명형으로 표현한다.
  (예: 문자 리터럴 '\0' 대신 "널 문자(\\u0000)")

입력 변수:
- categoryId: <number>
- level: EASY|NORMAL|HARD|RANDOM
- topic: string | '제한 없음'
- items: 오답 문항 요약 JSON (question/choices/correctAnswer/explain 포함)

출력 스키마:
[
  {
    "question": "string",
    "choices": ["string","string","string","string"],
    "answer": "string",
    "explain": "string",
    "level": "EASY|NORMAL|HARD",
    "categoryId": <number>
  }
]

제약:
- 항상 정확히 5문제.
- choices는 정확히 4개.
- answer는 choices 중 하나와 **문자열이 완전히 동일**해야 한다.
- question은 한 문장, 60자 이내.
- explain은 1~2문장, 120자 이내.
- choices 각 항목은 8~30자, 서로 의미 중복/동의어 금지.
- level이 RANDOM이면 EASY/NORMAL/HARD 혼합. 특정 레벨이면 전부 그 레벨.
- topic/입력 items의 개념군을 벗어나지 말 것.
- 한국어 자연스럽게. 모호성/중복 금지.
- categoryId는 입력 categoryId로 채울 것.

금지/치환 규칙(중요):
- 따옴표/백슬래시가 필요한 프로그래밍 리터럴을 **그대로** 쓰지 말고, 설명형으로 치환하라.
  예) "'\"'" → '쌍따옴표 문자' 라고 쓰지 말고 → "쌍따옴표 문자"
      "'\\n'" → "줄바꿈 문자(\\n)"
      "'\\0'" → "널 문자(\\u0000)"
- null/undefined/빈 문자열/제어문자 원문 삽입 금지(설명형으로).

품질 가이드:
- 기존 오답의 개념을 변주하여 '비슷하지만 다른 상황'을 제시.
- 해설은 왜 오답/정답인지 핵심 근거 포함, 1~2문장.
""");

	private final String system;
	QuizPrompt(String system) { this.system = system; }
}