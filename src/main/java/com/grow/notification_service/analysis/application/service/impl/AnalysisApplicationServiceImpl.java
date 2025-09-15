package com.grow.notification_service.analysis.application.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grow.notification_service.analysis.application.port.LlmClientPort;
import com.grow.notification_service.analysis.application.prompt.AnalysisPrompt;
import com.grow.notification_service.analysis.application.service.AnalysisApplicationService;
import com.grow.notification_service.analysis.domain.model.Analysis;
import com.grow.notification_service.analysis.domain.repository.AnalysisRepository;
import com.grow.notification_service.quiz.application.MemberQuizResultPort;
import com.grow.notification_service.quiz.domain.model.Quiz;
import com.grow.notification_service.quiz.domain.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisApplicationServiceImpl implements AnalysisApplicationService {

	private final AnalysisRepository analysisRepository;
	private final LlmClientPort llmClient;
	private final QuizRepository quizRepository;
	private final MemberQuizResultPort memberQuizResultPort;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public Analysis analyze(Long memberId, Long categoryId) {
		log.info("[ANALYSIS][START] 분석 요청 시작 - memberId={}, categoryId={}", memberId, categoryId);

		// 1) 시스템 프롬프트
		String systemPrompt = AnalysisPrompt.ROADMAP.getSystem();

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

	/**
	 * 틀린 문제 기반 학습 가이드 생성
	 * @param memberId 멤버 ID
	 * @param categoryId 카테고리 ID (null이면 전체)
	 * @return 저장된 Analysis 엔티티
	 */
	@Override
	@Transactional
	public Analysis analyzeQuiz(Long memberId, Long categoryId) {
		log.info("[ANALYSIS][FOCUS][START] 종합 학습 가이드 시작 - memberId={}, categoryId={}", memberId, categoryId);

		// 1) 멤버 서비스에서 오답 ID 조회
		List<Long> wrongIds = memberQuizResultPort.findAnsweredQuizIds(memberId, categoryId, Boolean.FALSE);
		if (wrongIds == null) wrongIds = List.of();
		log.debug("[ANALYSIS][FOCUS] 오답 ID 조회 완료 - wrongIds={}", wrongIds);

		// 2) 동일 컨텍스트 내 퀴즈 메타 조회
		List<Quiz> wrongQuizzes = wrongIds.isEmpty() ? List.of() : quizRepository.findByIds(wrongIds);
		Map<Long, Quiz> byId = wrongQuizzes.stream()
			.collect(Collectors.toMap(Quiz::getQuizId, q -> q));
		log.debug("[ANALYSIS][FOCUS] 오답 퀴즈 메타 로드 완료 - size={}", wrongQuizzes.size());

		if (categoryId != null) {
			wrongIds = wrongIds.stream()
				.filter(id -> byId.containsKey(id) && categoryId.equals(byId.get(id).getCategoryId()))
				.toList();
		}
		log.debug("[ANALYSIS][FOCUS] 카테고리 필터링 완료 - filteredWrongIds={}", wrongIds);

		// 3) LLM 입력 JSON 구성
		String userPrompt = buildItemsUserPrompt(byId, wrongIds);
		String systemPrompt = AnalysisPrompt.FOCUS_GUIDE.getSystem();
		log.debug("[ANALYSIS][FOCUS][PROMPT] systemPrompt={}, userPrompt={}", systemPrompt, userPrompt);

		// 4) LLM 호출
		String resultJson = llmClient.generateJson(systemPrompt, userPrompt);
		log.info("[ANALYSIS][FOCUS][LLM] Gemini 호출 완료 - resultJson={}", resultJson);

		// 5) 결과 파싱/검증
		List<Map<String, String>> focusConcepts = parseFocusConcepts(resultJson);
		List<String> futureConcepts = parseFutureConcepts(resultJson);
		log.debug("[ANALYSIS][FOCUS][VALIDATE] 검증 완료 - focusConcepts.size={}, futureConcepts.size={}",
			focusConcepts.size(), futureConcepts.size());

		// 6) 최종 결과 JSON 구성
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("memberId", memberId);
		out.put("categoryId", categoryId);
		out.put("focusConcepts", focusConcepts);
		out.put("futureConcepts", futureConcepts);

		String finalJson;
		try {
			finalJson = objectMapper.writeValueAsString(out);
		} catch (Exception e) {
			throw new RuntimeException("분석 결과 직렬화 실패", e);
		}
		log.debug("[ANALYSIS][FOCUS][FINAL] 최종 결과 JSON 직렬화 완료");

		Analysis saved = analysisRepository.save(new Analysis(memberId, categoryId, finalJson));
		log.info("[ANALYSIS][FOCUS][END] 종합 학습 가이드 저장 완료 - analysisId={}, memberId={}, categoryId={}",
			saved.getAnalysisId(), saved.getMemberId(), saved.getCategoryId());

		return saved;
	}

	/** 오답 문항 최소 컨텍스트를 LLM user prompt(JSON String 포함)로 구성 */
	private String buildItemsUserPrompt(Map<Long, Quiz> byId, List<Long> wrongIds) {
		try {
			List<Map<String, Object>> items = new ArrayList<>();
			for (Long qid : wrongIds) {
				Quiz q = byId.get(qid);
				if (q == null) continue;
				Map<String, Object> one = new LinkedHashMap<>();
				one.put("quizId", q.getQuizId());     // 저장 안 함, 판단용
				one.put("question", q.getQuestion());
				one.put("choices", q.getChoices());
				one.put("correctAnswer", q.getAnswer());
				one.put("explain", q.getExplain());
				items.add(one);
			}
			Map<String, Object> llmInput = new LinkedHashMap<>();
			llmInput.put("items", items);
			String itemsJson = objectMapper.writeValueAsString(llmInput);

			return """
입력 데이터(JSON):
%s

요청:
- 오답들을 종합 분석하여 지금 학습해야 할 핵심 키워드와 개념 정리(focusConcepts),
  그리고 추후 학습하면 좋은 키워드(futureConcepts)만을 JSON으로 반환하라.
- conceptSummary는 3~5문장, 최소 50자 이상으로 서술하라.
- 반드시 지정된 스키마만 출력하라.
""".formatted(itemsJson);
		} catch (Exception ex) {
			throw new RuntimeException("LLM 입력 직렬화 실패", ex);
		}
	}

	/** resultJson에서 focusConcepts 배열 파싱/검증 */
	private List<Map<String, String>> parseFocusConcepts(String json) {
		try {
			JsonNode root = objectMapper.readTree(json);
			if (!root.has("focusConcepts") || !root.get("focusConcepts").isArray()) {
				throw new IllegalStateException("LLM 결과 형식 오류: focusConcepts 배열 누락");
			}
			List<Map<String, String>> list = new ArrayList<>();
			for (JsonNode n : root.get("focusConcepts")) {
				if (!n.has("keyword") || !n.has("conceptSummary")) {
					throw new IllegalStateException("LLM 결과 형식 오류: focusConcepts의 필수 필드 누락");
				}
				String keyword = n.get("keyword").asText("");
				String summary = n.get("conceptSummary").asText("");
				if (keyword.isBlank() || summary.isBlank()) {
					throw new IllegalStateException("LLM 결과 형식 오류: keyword/conceptSummary 비어있음");
				}
				Map<String, String> row = new LinkedHashMap<>();
				row.put("keyword", keyword);
				row.put("conceptSummary", summary);
				list.add(row);
			}
			if (list.isEmpty()) {
				throw new IllegalStateException("LLM 결과 형식 오류: focusConcepts가 비어있음");
			}
			return list;
		} catch (Exception e) {
			throw new RuntimeException("LLM 결과 검증 실패 (focusConcepts)", e);
		}
	}

	/** resultJson에서 futureConcepts 배열 파싱/검증 */
	private List<String> parseFutureConcepts(String json) {
		try {
			JsonNode root = objectMapper.readTree(json);
			if (!root.has("futureConcepts") || !root.get("futureConcepts").isArray()) {
				throw new IllegalStateException("LLM 결과 형식 오류: futureConcepts 배열 누락");
			}
			List<String> concepts = new ArrayList<>();
			for (JsonNode n : root.get("futureConcepts")) {
				String c = n.asText("");
				if (!c.isBlank()) concepts.add(c);
			}
			if (concepts.isEmpty()) {
				throw new IllegalStateException("LLM 결과 형식 오류: futureConcepts가 비어있음");
			}
			return concepts;
		} catch (Exception e) {
			throw new RuntimeException("LLM 결과 검증 실패 (futureConcepts)", e);
		}
	}
}