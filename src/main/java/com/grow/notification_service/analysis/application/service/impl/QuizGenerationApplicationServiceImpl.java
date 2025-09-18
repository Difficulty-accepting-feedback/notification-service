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
import com.grow.notification_service.global.exception.AnalysisException;
import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.quiz.application.MemberQuizResultPort;
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

			String json = llm.generateJson(system, user);
			log.info("[QUIZ][GEN][LLM] 호출 완료");

			// 파싱
			List<Quiz> parsed = parseQuizzes(json, categoryId, forcedLevel, randomMode);
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
		boolean randomMode = isRandom(levelParam);
		QuizLevel forcedLevel = randomMode ? null : parseLevelOrNull(levelParam);

		// 3) LLM 호출
		String system = QuizPrompt.GENERATE_FROM_WRONG.getSystem();
		String user = buildQuizFromWrongUserPrompt(itemsJson, categoryId, levelParam, topic);
		log.debug("[QUIZ][GEN][FROM_WRONG][PROMPT] system={}, user={}", system, user);

		String json = llm.generateJson(system, user);
		log.info("[QUIZ][GEN][FROM_WRONG][LLM] 호출 완료");

		// 4) 파싱/검증/중복제거
		List<Quiz> parsed = parseGeneratedQuizzes(json, categoryId, forcedLevel, randomMode);
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
	 * LLM 응답 파싱
	 * @param json LLM 응답 JSON 문자열
	 * @param categoryId 카테고리
	 * @param forcedLevel EASY|NORMAL|HARD (RANDOM 모드가 아닐 때만 유효)
	 * @param randomMode RANDOM 모드 여부
	 * @return 파싱된 Quiz 리스트(항상 5개)
	 */
	private List<Quiz> parseQuizzes(String json, Long categoryId, QuizLevel forcedLevel, boolean randomMode) {
		try {
			JsonNode root = mapper.readTree(json);
			// 배열/크기 검사
			if (!root.isArray()) {
				throw new AnalysisException(ErrorCode.ANALYSIS_QUIZ_GENERATION_PARSE_FAILED);
			}
			ArrayNode arr = (ArrayNode) root;
			// 5문제 검사
			if (arr.size() != 5) {
				throw new AnalysisException(ErrorCode.ANALYSIS_QUIZ_GENERATION_PARSE_FAILED);
			}

			// 각 항목 검사 및 파싱
			List<Quiz> out = new ArrayList<>(5);
			for (JsonNode n : arr) {
				String question = textReq(n, "question");
				List<String> choices = readChoices(n);
				String answer = textReq(n, "answer");
				String explain = textReq(n, "explain");

				// 제약 조건 검사
				if (!choices.contains(answer)) {
					throw new AnalysisException(ErrorCode.ANALYSIS_QUIZ_GENERATION_PARSE_FAILED);
				}

				QuizLevel level;
				if (randomMode) {
					level = parseLevelOrNull(n.has("level") ? n.get("level").asText("") : null);
					if (level == null) {
						throw new AnalysisException(ErrorCode.ANALYSIS_QUIZ_GENERATION_PARSE_FAILED);
					}
				} else {
					level = forcedLevel; // EASY/NORMAL/HARD로 강제
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
	 * 생성된 퀴즈 파싱
	 * @param json LLM 응답 JSON 문자열
	 * @param categoryId 카테고리
	 * @param forcedLevel EASY|NORMAL|HARD (RANDOM 모드가 아닐 때만 유효)
	 * @param randomMode RANDOM 모드 여부
	 * @return 파싱된 Quiz 리스트(항상 5개)
	 */
	private List<Quiz> parseGeneratedQuizzes(String json, Long categoryId, QuizLevel forcedLevel, boolean randomMode) {
		try {
			JsonNode root = mapper.readTree(json); // objectMapper → mapper로 통일
			// 배열/크기 검사
			if (!root.isArray()) {
				throw new AnalysisException(ErrorCode.ANALYSIS_QUIZ_GENERATION_PARSE_FAILED);
			}
			ArrayNode arr = (ArrayNode) root;
			// 5문제 검사
			if (arr.size() != 5) {
				throw new AnalysisException(ErrorCode.ANALYSIS_QUIZ_GENERATION_PARSE_FAILED);
			}
			List<Quiz> out = new ArrayList<>(5);
			// 각 항목 검사 및 파싱
			for (JsonNode n : arr) {
				long catFromLlm = longReq(n, "categoryId");
				if (catFromLlm != categoryId) {
					throw new AnalysisException(ErrorCode.ANALYSIS_QUIZ_GENERATION_PARSE_FAILED);
				}
				String question = textReq(n, "question");
				List<String> choices = readChoices(n);
				String answer = textReq(n, "answer");
				String explain = textReq(n, "explain");

				if (!choices.contains(answer))
					// 정답이 선택지에 포함되어야 함
					throw new AnalysisException(ErrorCode.ANALYSIS_QUIZ_GENERATION_PARSE_FAILED);

				QuizLevel level;
				if (randomMode) {
					level = parseLevelOrNull(n.has("level") ? n.get("level").asText("") : null);
					if (level == null) {
						throw new AnalysisException(ErrorCode.ANALYSIS_QUIZ_GENERATION_PARSE_FAILED);
					}
				} else {
					level = forcedLevel; // EASY/NORMAL/HARD로 강제
				}

				out.add(Quiz.create(question, choices, answer, explain, level, categoryId));
			}
			return out;

		} catch (AnalysisException ae) {
			throw ae;
		} catch (Exception e) {
			log.error("[QUIZ][GEN][FROM_WRONG][PARSE] 실패 - err={}", e.toString(), e);
			throw new AnalysisException(ErrorCode.ANALYSIS_QUIZ_GENERATION_PARSE_FAILED, e);
		}
	}

	/**
	 * 필수 텍스트 필드 읽기
	 * @param n JSON 노드
	 * @param field 필드명
	 * @return 읽은 값
	 */
	private String textReq(JsonNode n, String field) {
		// 필수/빈값 검사
		if (!n.hasNonNull(field)) {
			throw new AnalysisException(ErrorCode.ANALYSIS_REQUIRED_FIELD_MISSING);
		}
		String v = n.get(field).asText("");
		// 빈값 검사
		if (v.isBlank()) {
			throw new AnalysisException(ErrorCode.ANALYSIS_INVALID_FIELD_VALUE);
		}
		return v;
	}

	/**
	 * choices 필드 읽기
	 * @param n JSON 노드
	 * @return 읽은 값 리스트
	 */
	private List<String> readChoices(JsonNode n) {
		// 필수/형식/크기 검사
		if (!n.has("choices") || !n.get("choices").isArray()) {
			throw new AnalysisException(ErrorCode.ANALYSIS_INVALID_FIELD_VALUE);
		}
		// 4개 검사
		ArrayNode ca = (ArrayNode) n.get("choices");
		if (ca.size() != 4) {
			throw new AnalysisException(ErrorCode.ANALYSIS_INVALID_FIELD_VALUE);
		}
		// 각 항목 빈값 검사
		List<String> list = new ArrayList<>(4);
		for (JsonNode c : ca) {
			String s = c.asText("");
			if (s.isBlank()) throw new AnalysisException(ErrorCode.ANALYSIS_INVALID_FIELD_VALUE);
			list.add(s);
		}
		return list;
	}

	/**
	 * 필수 long 필드 읽기
	 * @param n JSON 노드
	 * @param field 필드명
	 * @return 읽은 값
	 */
	private long longReq(JsonNode n, String field) {
		if (!n.hasNonNull(field)) {
			throw new AnalysisException(ErrorCode.ANALYSIS_REQUIRED_FIELD_MISSING);
		}
		JsonNode v = n.get(field);
		if (v.isIntegralNumber()) return v.asLong();
		if (v.isTextual()) {
			try {
				return Long.parseLong(v.asText().trim());
			} catch (NumberFormatException ex) {
				throw new AnalysisException(ErrorCode.ANALYSIS_INVALID_FIELD_VALUE, ex);
			}
		}
		throw new AnalysisException(ErrorCode.ANALYSIS_INVALID_FIELD_VALUE);
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
}