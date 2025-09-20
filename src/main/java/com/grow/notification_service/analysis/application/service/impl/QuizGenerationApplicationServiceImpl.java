package com.grow.notification_service.analysis.application.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.grow.notification_service.analysis.application.port.LlmClientPort;
import com.grow.notification_service.analysis.application.prompt.QuizPrompt;
import com.grow.notification_service.analysis.application.service.QuizGenerationApplicationService;
import com.grow.notification_service.analysis.infra.llm.JsonSchemas;
import com.grow.notification_service.analysis.infra.llm.LlmJsonSanitizer;
import com.grow.notification_service.global.exception.AnalysisException;
import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.quiz.application.port.MemberQuizResultPort;
import com.grow.notification_service.quiz.application.mapping.SkillTagToCategoryRegistry;
import com.grow.notification_service.quiz.domain.model.Quiz;
import com.grow.notification_service.quiz.domain.repository.QuizRepository;
import com.grow.notification_service.quiz.infra.persistence.enums.QuizLevel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizGenerationApplicationServiceImpl implements QuizGenerationApplicationService {

	private final LlmClientPort llm;
	private final QuizRepository quizRepository;
	private final ObjectMapper mapper;
	private final SkillTagToCategoryRegistry skillTagToCategoryRegistry;
	private final MemberQuizResultPort memberQuizResultPort;

	/**
	 * LLM으로 5문제 생성 -> 파싱 -> 저장 후 반환
	 * @param memberId 호출자(로그 용도)
	 * @param categoryId 카테고리
	 * @param levelParam EASY|NORMAL|HARD|RANDOM (대소문자 무시)
	 * @param topic 주제(옵션)
	 */
	@Override
	@Transactional
	public List<Quiz> generateAndSave(Long memberId, Long categoryId, String levelParam, String topic) {
		boolean randomMode = isRandom(levelParam);
		QuizLevel forcedLevel = randomMode ? null : parseLevelOrNull(levelParam);

		String skillTagCode = skillTagToCategoryRegistry.toSkillTagOrThrow(categoryId);

		log.info("[QUIZ][GEN][START] memberId={}, categoryId={}, levelParam={}, topic={}",
			memberId, categoryId, levelParam, topic);

		try {
			// LLM 호출
			String system = QuizPrompt.GENERATE.getSystem();
			String user = buildUserPrompt(categoryId, levelParam, topic, skillTagCode);
			log.debug("[QUIZ][GEN][PROMPT] system={}, user={}", system, user);

			String raw  = llm.generateJson(system, user, JsonSchemas.quizArray());
			String json = LlmJsonSanitizer.sanitize(raw, true);
			log.info("[QUIZ][GEN][LLM] 호출 완료 - rawLen={}, sanitizedLen={}",
				raw == null ? 0 : raw.length(), json == null ? 0 : json.length());

			// 파싱
			List<Quiz> parsed = parseQuizArray(json, categoryId, forcedLevel, randomMode);
			log.info("[QUIZ][GEN][PARSE] parsedCount={}", parsed.size());

			// 저장
			List<Quiz> saved = new ArrayList<>(parsed.size());
			for (Quiz q : parsed) {
				saved.add(quizRepository.save(q));
			}
			log.info("[QUIZ][GEN][END] 저장 완료 - saved={}", saved.size());
			return saved;

		} catch (AnalysisException ae) {
			throw ae;
		} catch (Exception e) {
			log.error("[QUIZ][GEN] 처리 실패 - memberId={}, categoryId={}, err={}", memberId, categoryId, e.toString(), e);
			throw new AnalysisException(ErrorCode.ANALYSIS_UNEXPECTED_FAILED, e);
		}
	}

	/**
	 * 틀린 문제 기반 퀴즈 생성 + 저장
	 * @param memberId 멤버ID
	 * @param categoryId 카테고리ID
	 * @param levelParam EASY|NORMAL|HARD|RANDOM
	 * @param topic 주제 (사용자가 입력 가능, 빈 값 허용)
	 * @return 생성된 퀴즈 목록 (최대 5개)
	 */
	@Override
	@Transactional
	public List<Quiz> generateQuizzesFromWrong(Long memberId, Long categoryId, String levelParam, String topic) {
		log.info("[QUIZ][GEN][FROM_WRONG][START] memberId={}, categoryId={}, levelParam={}, topic={}",
			memberId, categoryId, levelParam, topic);

		// 1) 오답 수집
		List<Long> wrongIds = memberQuizResultPort.findAnsweredQuizIds(memberId, categoryId, Boolean.FALSE);
		if (wrongIds == null) wrongIds = List.of();
		List<Quiz> wrongQuizzes = wrongIds.isEmpty() ? List.of() : quizRepository.findByIds(wrongIds);
		Map<Long, Quiz> byId = wrongQuizzes.stream().collect(Collectors.toMap(Quiz::getQuizId, q -> q));

		// 카테고리 필터링
		if (categoryId != null) {
			wrongIds = wrongIds.stream()
				.filter(id -> byId.containsKey(id) && categoryId.equals(byId.get(id).getCategoryId()))
				.toList();
		}
		log.info("[QUIZ][GEN][FROM_WRONG] 컨텍스트 준비 - wrongIds={}, filteredSize={}", wrongIds.size(), wrongIds.size());

		// 2) 입력 payload 구성
		String itemsJson = buildItemsJson(byId, wrongIds);

		// 평균 난이도 계산
		QuizLevel avgLevel = averageLevel(wrongIds, byId); // 없으면 null 반환
		log.info("[QUIZ][GEN][FROM_WRONG] 평균 난이도 계산 - avgLevel={}", avgLevel);

		// levelParam이 RANDOM/빈값/AUTO면 평균 사용(있으면 강제, 없으면 RANDOM)
		boolean isRandomInput = isRandom(levelParam);
		boolean useAverage = isRandomInput || "AUTO".equalsIgnoreCase(levelParam);

		// 사용자가 명시했으면 우선, 아니면 평균 난이도로 강제 / 없으면 RANDOM
		QuizLevel forcedLevel;
		boolean randomMode;


		if (useAverage && avgLevel != null) {
			forcedLevel = avgLevel; // 평균으로 강제
			randomMode = false;
		} else if (isRandomInput) {
			forcedLevel = null; // 평균도 없고 입력은 RANDOM -> 진짜 랜덤
			randomMode = true;
		} else {
			forcedLevel = parseLevelOrNull(levelParam); // 사용자 명시 우선
			randomMode = false;
		}

		String levelForPrompt = randomMode ? "RANDOM" : forcedLevel.name();
		log.info("[QUIZ][GEN][FROM_WRONG] 평균 난이도={}, userParam={}, 최종레벨={}, randomMode={}",
			avgLevel, levelParam, levelForPrompt, randomMode);

		// 3) LLM 호출
		String system = QuizPrompt.GENERATE_FROM_WRONG.getSystem();
		String user = buildQuizFromWrongUserPrompt(itemsJson, categoryId, levelForPrompt, topic);
		log.debug("[QUIZ][GEN][FROM_WRONG][PROMPT] system={}, user={}", system, user);

		String raw  = llm.generateJson(system, user, JsonSchemas.quizArray());
		String json = LlmJsonSanitizer.sanitize(raw, true);
		log.info("[QUIZ][GEN][FROM_WRONG][LLM] 호출 완료 - rawLen={}, sanitizedLen={}",
			raw == null ? 0 : raw.length(), json == null ? 0 : json.length());

		// 4) 파싱/검증/중복제거
		List<Quiz> parsed = parseQuizArray(json, categoryId, forcedLevel, randomMode);
		log.info("[QUIZ][GEN][FROM_WRONG][PARSE] parsedCount={}", parsed.size());

		// 세션 내 중복 제거 + DB 중복 제거
		List<Quiz> filtered = new ArrayList<>(5);
		Set<String> seen = new HashSet<>();
		for (Quiz q : parsed) {
			String norm = q.getQuestion().trim().replaceAll("\\s+"," ").toLowerCase();
			if (!seen.add(norm)) {
				log.info("[QUIZ][GEN][FROM_WRONG] 세션 중복 스킵 - q='{}'", q.getQuestion());
				continue;
			}
			if (quizRepository.existsByCategoryIdAndQuestion(categoryId, q.getQuestion())) {
				log.info("[QUIZ][GEN][FROM_WRONG] DB 중복 스킵 - q='{}'", q.getQuestion());
				continue;
			}
			filtered.add(q);
		}

		// 5) 저장
		List<Quiz> saved = new ArrayList<>(filtered.size());
		for (Quiz q : filtered) {
			saved.add(quizRepository.save(q));
		}
		log.info("[QUIZ][GEN][FROM_WRONG][END] 저장 완료 - requested=5, saved={}", saved.size());
		return saved;
	}

	// 헬퍼 메서드

	/**
	 * LLM 프롬프트 생성
	 * @param categoryId 카테고리
	 * @param levelParam EASY|NORMAL|HARD|RANDOM
	 * @param topic 주제
	 * @param skillTagCode 스킬태그 코드
	 * @return 프롬프트 문자열
	 */
	private String buildUserPrompt(Long categoryId, String levelParam, String topic, String skillTagCode) {
		String lv = (levelParam == null || levelParam.isBlank()) ? "RANDOM" : levelParam.trim().toUpperCase();
		String tp = (topic == null || topic.isBlank()) ? "제한 없음" : topic.trim();
		return """
입력:
- categoryId: %d
- skillTagCode: %s
- 난이도: %s
- 문제 수: 5
- 주제: %s

요청:
- 위 입력을 반영해 스키마에 맞춘 '유효한 JSON 배열'만 반환하라.
""".formatted(categoryId, skillTagCode, lv, tp);
	}

	/**
	 * 틀린 문제 기반 퀴즈 생성용 유저 프롬프트 구성
	 * @param itemsJson LLM 입력 JSON
	 * @param categoryId 카테고리ID
	 * @param levelParam EASY|NORMAL|HARD|RANDOM|null|빈문자열
	 * @param topic 주제 (사용자가 입력 가능, 빈 값 허용)
	 * @return 유저 프롬프트
	 */
	private String buildQuizFromWrongUserPrompt(String itemsJson, Long categoryId, String levelParam, String topic) {
		String lv = (levelParam == null || levelParam.isBlank()) ? "RANDOM" : levelParam.trim().toUpperCase();
		String tp = (topic == null || topic.isBlank()) ? "제한 없음" : topic.trim();
		return """
입력:
- categoryId: %d
- level: %s
- count: 5
- topic: %s
- items(JSON): %s

요청:
- 위 입력을 반영해 [출력 스키마]에 정확히 맞는 '유효한 JSON 배열'만 반환하라.
""".formatted(categoryId, lv, tp, itemsJson);
	}

	/**
	 * LLM 입력용 JSON 구성
	 * @param byId 퀴즈 ID 맵
	 * @param wrongIds 오답 퀴즈 ID 목록
	 * @return 직렬화된 JSON 문자열
	 */
	private String buildItemsJson(Map<Long, Quiz> byId, List<Long> wrongIds) {
		try {
			List<Map<String, Object>> items = new ArrayList<>();
			for (Long qid : wrongIds) {
				Quiz q = byId.get(qid);
				if (q == null)
					continue;
				Map<String, Object> one = new LinkedHashMap<>();
				one.put("quizId", q.getQuizId());
				one.put("question", q.getQuestion());
				one.put("choices", q.getChoices());
				one.put("correctAnswer", q.getAnswer());
				one.put("explain", q.getExplain());
				items.add(one);
			}
			Map<String, Object> llmInput = new LinkedHashMap<>();
			llmInput.put("items", items);
			return mapper.writeValueAsString(llmInput);
		} catch (Exception ex) {
			log.error("[ANALYSIS][FOCUS] LLM 입력 직렬화 실패 - wrongIds={}, err={}", wrongIds, ex, ex);
			throw new AnalysisException(ErrorCode.ANALYSIS_INPUT_SERIALIZE_FAILED, ex);
		}
	}

	/**
	 * 단일 파서: 스키마가 형식을 보장하므로 단순 파싱(필요한 교차검증만 유지)
	 * @param json LLM 응답(JSON 배열)
	 * @param categoryId 요청 카테고리
	 * @param forcedLevel RANDOM이 아닐 때 강제 레벨
	 * @param randomMode RANDOM 모드 여부
	 */
	private List<Quiz> parseQuizArray(String json, Long categoryId, QuizLevel forcedLevel, boolean randomMode) {
		try {
			JsonNode root = mapper.readTree(json);
			if (!root.isArray()) {
				throw new AnalysisException(ErrorCode.ANALYSIS_QUIZ_GENERATION_PARSE_FAILED);
			}

			List<Quiz> out = new ArrayList<>();
			for (JsonNode n : root) {
				String question = n.path("question").asText("");
				ArrayNode ca = (ArrayNode) n.path("choices");
				String answer  = n.path("answer").asText("");
				String explain = n.path("explain").asText("");
				String levelStr = n.path("level").asText("");
				JsonNode catNode = n.path("categoryId");

				// categoryId 일치 확인
				long catFromLlm = catNode.isIntegralNumber()
					? catNode.asLong()
					: Long.parseLong(catNode.asText().trim());
				if (catFromLlm != categoryId) {
					throw new AnalysisException(ErrorCode.ANALYSIS_QUIZ_GENERATION_PARSE_FAILED);
				}

				// choices 파싱
				List<String> choices = new ArrayList<>(4);
				if (ca != null) {
					for (JsonNode c : ca) choices.add(c.asText(""));
				}
				// 교차검증: answer는 choices 중 하나
				if (!choices.contains(answer)) {
					throw new AnalysisException(ErrorCode.ANALYSIS_QUIZ_GENERATION_PARSE_FAILED);
				}

				// level 적용
				QuizLevel level = randomMode ? parseLevelOrNull(levelStr) : forcedLevel;
				if (level == null) {
					throw new AnalysisException(ErrorCode.ANALYSIS_QUIZ_GENERATION_PARSE_FAILED);
				}

				out.add(Quiz.create(question, choices, answer, explain, level, categoryId));
			}
			return out;

		} catch (AnalysisException ae) {
			throw ae;
		} catch (Exception e) {
			log.error("[QUIZ][GEN][PARSE] 실패 - err={}", e.toString(), e);
			throw new AnalysisException(ErrorCode.ANALYSIS_QUIZ_GENERATION_PARSE_FAILED, e);
		}
	}

	/**
	 * RANDOM 모드 여부 판단
	 * @param levelParam EASY|NORMAL|HARD|RANDOM|null|빈문자열
	 * @return RANDOM 모드 여부
	 */
	private boolean isRandom(String levelParam) {
		return levelParam == null || levelParam.isBlank() || "RANDOM".equalsIgnoreCase(levelParam.trim());
	}

	/**
	 * EASY|NORMAL|HARD 파싱
	 * @param v 파싱할 문자열
	 * @return 파싱된 QuizLevel, 알 수 없는 값이면 null 반환
	 */
	private QuizLevel parseLevelOrNull(String v) {
		if (v == null) return null;
		return switch (v.trim().toUpperCase()) {
			case "EASY" -> QuizLevel.EASY;
			case "NORMAL" -> QuizLevel.NORMAL;
			case "HARD" -> QuizLevel.HARD;
			default -> null;
		};
	}

	/** 오답들의 평균 난이도를 계산(EASY=1, NORMAL=2, HARD=3) → 최근접 버킷으로 환산 */
	private QuizLevel averageLevel(List<Long> wrongIds, Map<Long, Quiz> byId) {
		int sum = 0;
		int count = 0;

		for (Long id : wrongIds) {
			Quiz q = byId.get(id);
			if (q == null || q.getLevel() == null) continue;
			sum += levelToScore(q.getLevel());
			count++;
		}

		if (count == 0) return null; // 평균 불가 → RANDOM 사용

		double avg = (double) sum / (double) count;
		return scoreToNearestLevel(avg);
	}

	private int levelToScore(QuizLevel level) {
		switch (level) {
			case EASY: return 1;
			case NORMAL: return 2;
			case HARD: return 3;
			default: return 2;
		}
	}

	private QuizLevel scoreToNearestLevel(double avg) {
		if (avg < 1.5) return QuizLevel.EASY;
		if (avg < 2.5) return QuizLevel.NORMAL;
		return QuizLevel.HARD;
	}

}