package com.grow.notification_service.analysis.application.service.impl;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grow.notification_service.analysis.application.port.LlmClientPort;
import com.grow.notification_service.analysis.application.service.AnalysisApplicationService;
import com.grow.notification_service.analysis.domain.model.Analysis;
import com.grow.notification_service.analysis.domain.repository.AnalysisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisApplicationServiceImpl implements AnalysisApplicationService {

	private final AnalysisRepository analysisRepository;
	private final LlmClientPort llmClient;

	@Override
	@Transactional
	public Analysis analyze(Long memberId, Long categoryId) {
		log.info("[ANALYSIS][START] 분석 요청 시작 - memberId={}, categoryId={}", memberId, categoryId);

		// 1) 시스템 프롬프트
		String systemPrompt = """
            역할: 너는 GROW의 학습 코치다.
            목표: 자바 프로그래밍 공부를 초급, 중급, 상급 수준으로 나눈 목차를 JSON으로 반환한다.
            출력 예시:
            {
              "초급": ["자바 기본 문법", "변수와 자료형", "조건문과 반복문", "메서드와 클래스"],
              "중급": ["상속과 다형성", "컬렉션 프레임워크", "예외 처리", "스트림과 람다"],
              "상급": ["멀티스레드 프로그래밍", "JVM 메모리 구조", "리플렉션", "Spring 프레임워크 기초"]
            }
            """;

		// 2) 사용자 프롬프트
		String userPrompt = "자바 프로그래밍 학습 로드맵을 수준별 목차로 정리해줘.";
		log.debug("[ANALYSIS][PROMPT] systemPrompt={}, userPrompt={}", systemPrompt, userPrompt);

		// 3) Gemini 호출
		String resultJson = llmClient.generateJson(systemPrompt, userPrompt);
		log.info("[ANALYSIS][GEMINI] Gemini 호출 완료 - resultJson={}", resultJson);

		// 4) 도메인 모델 생성
		Analysis analysis = new Analysis(memberId, categoryId, resultJson);
		log.debug("[ANALYSIS][ENTITY] Analysis 객체 생성 - {}", analysis);

		// 5) DB 저장
		Analysis saved = analysisRepository.save(analysis);
		log.info("[ANALYSIS][END] 분석 결과 저장 완료 - analysisId={}, memberId={}, categoryId={}",
			saved.getAnalysisId(), saved.getMemberId(), saved.getCategoryId());

		return saved;
	}
}