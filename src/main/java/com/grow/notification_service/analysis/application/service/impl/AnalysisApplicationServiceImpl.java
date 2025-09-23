package com.grow.notification_service.analysis.application.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grow.notification_service.analysis.application.event.AnalysisNotificationProducer;
import com.grow.notification_service.analysis.application.port.GroupMembershipPort;
import com.grow.notification_service.analysis.application.port.LlmClientPort;
import com.grow.notification_service.analysis.application.prompt.AnalysisPrompt;
import com.grow.notification_service.analysis.application.service.AnalysisApplicationService;
import com.grow.notification_service.analysis.domain.model.Analysis;
import com.grow.notification_service.analysis.domain.repository.AiReviewSessionRepository;
import com.grow.notification_service.analysis.domain.repository.AnalysisRepository;
import com.grow.notification_service.analysis.infra.llm.JsonSchemas;
import com.grow.notification_service.analysis.infra.llm.LlmJsonSanitizer;
import com.grow.notification_service.global.exception.AnalysisException;
import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.quiz.application.mapping.SkillTagToCategoryRegistry;
import com.grow.notification_service.quiz.application.port.MemberQuizResultPort;
import com.grow.notification_service.quiz.domain.model.Quiz;
import com.grow.notification_service.quiz.domain.repository.QuizRepository;
import com.grow.notification_service.analysis.domain.model.KeywordConcept;
import com.grow.notification_service.analysis.domain.repository.KeywordConceptRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
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
	private final KeywordConceptRepository keywordConceptRepository;
	private final AiReviewSessionRepository sessionRepository;
	private final AnalysisNotificationProducer analysisNotificationProducer;
	private final GroupMembershipPort groupMembershipPort;
	private final SkillTagToCategoryRegistry skillTagToCategoryRegistry;

	/**
	 * 그룹 스킬태그 기반 학습 로드맵 생성
	 * - 1단계: study_service 재조회로 {category} 내 groupId 소속 검증 및 skillTag 확보
	 * - 2단계: skillTag → categoryId 매핑
	 * - 3단계: 세션 id("rdmp:skill:{skillTag}")로 카테고리+세션키 최신 1건 재사용
	 *         (있으면 payload만 복사해 요청자 기준으로 저장, LLM 생략)
	 * - 4단계: 재사용 실패 시 LLM 호출 -> 결과 JSON 저장
	 * - 5단계: 분석 완료 이벤트 발행
	 */
	@Override
	@Transactional
	public Analysis analyze(Long memberId, String category, Long groupId) {
		log.info("[ANALYSIS][ROADMAP][VERIFY] mid={}, category={}, gid={}", memberId, category, groupId);

		// 1) 서버 조회: category 내 groupId 소속 검증
		List<GroupMembershipPort.GroupSimpleResponse> groups =
			groupMembershipPort.getMyJoinedGroups(memberId, category);
		Optional<GroupMembershipPort.GroupSimpleResponse> found =
			groups.stream().filter(g -> Objects.equals(g.groupId(), groupId)).findFirst();
		if (found.isEmpty()) {
			log.warn("[ANALYSIS][ROADMAP][VERIFY][DENY] 그룹이 없거나 카테고리 불일치 - mid={}, gid={}, category={}",
				memberId, groupId, category);
			throw new AnalysisException(ErrorCode.MEMBER_RESULT_FETCH_FAILED);
		}

		// 2) 그룹명/스킬태그 확보
		GroupMembershipPort.GroupSimpleResponse auth = found.get();
		String groupName = auth.groupName();
		String skillTag  = auth.skillTag();

		// 3) 스킬태그 -> 내부 categoryId 매핑
		Long categoryId;
		try {
			categoryId = skillTagToCategoryRegistry.resolveOrThrow(skillTag);
		} catch (com.grow.notification_service.global.exception.QuizException e) {
			throw new AnalysisException(ErrorCode.CATEGORY_MISMATCH, e);
		}

		// 4) 세션id 생성
		String sessionId = "rdmp:skill:" + ((skillTag == null || skillTag.isBlank()) ? "NA" : skillTag);

		// 5) 카테고리+세션키 최신 1건 있으면 payload만 복사 저장
		Optional<Analysis> existing =
			analysisRepository.findTopByCategoryIdAndSessionIdOrderByAnalysisIdDesc(categoryId, sessionId);
		if (existing.isPresent()) {
			try {
				String existingJson = existing.get().getAnalysisResult();
				JsonNode existingRoot = objectMapper.readTree(existingJson);
				JsonNode payloadNode = existingRoot.has("payload") ? existingRoot.get("payload") : existingRoot;

				Map<String, Object> meta = new LinkedHashMap<>();
				meta.put("memberId", memberId);
				meta.put("groupId", groupId);
				meta.put("groupName", groupName);
				meta.put("category", category);
				meta.put("categoryId", categoryId);
				meta.put("skillTag", skillTag);
				meta.put("sessionId", sessionId);
				meta.put("sourceAnalysisId", existing.get().getAnalysisId());

				Map<String, Object> envelope = new LinkedHashMap<>();
				envelope.put("type", "ROADMAP");
				envelope.put("meta", meta);
				envelope.put("payload", payloadNode);

				String resultJson = objectMapper.writeValueAsString(envelope);

				Analysis saved = analysisRepository.save(new Analysis(memberId, categoryId, sessionId, resultJson));
				analysisNotificationProducer.analysisCompleted(memberId);
				log.info("[ANALYSIS][ROADMAP][REUSE] reused from analysisId={}, saved={}",
					existing.get().getAnalysisId(), saved.getAnalysisId());
				return saved;
			} catch (Exception e) {
				log.error("[ANALYSIS][ROADMAP][REUSE] wrap failed - err={}", e.toString(), e);
				throw new AnalysisException(ErrorCode.ANALYSIS_OUTPUT_SERIALIZE_FAILED, e);
			}
		}

		// 6) LLM 호출 (스킬태그/그룹명 기반)
		String systemPrompt = AnalysisPrompt.ROADMAP.getSystem();
		String userPrompt   = buildRoadmapUserPrompt(groupName, skillTag);
		String raw          = llmClient.generateJson(systemPrompt, userPrompt, JsonSchemas.roadmap());
		String safe         = LlmJsonSanitizer.sanitize(raw, true);
		log.info("[ANALYSIS][ROADMAP] LLM done - rawLen={}, safeLen={}",
			raw == null ? 0 : raw.length(), safe == null ? 0 : safe.length());

		// 7) 저장
		try {
			Map<String, Object> meta = new LinkedHashMap<>();
			meta.put("memberId", memberId);
			meta.put("groupId", groupId);
			meta.put("groupName", groupName);
			meta.put("category", category);
			meta.put("categoryId", categoryId);
			meta.put("skillTag", skillTag);
			meta.put("sessionId", sessionId);

			Map<String, Object> envelope = new LinkedHashMap<>();
			envelope.put("type", "ROADMAP");
			envelope.put("meta", meta);
			envelope.put("payload", objectMapper.readTree(safe));

			String resultJson = objectMapper.writeValueAsString(envelope);

			Analysis saved = analysisRepository.save(new Analysis(memberId, categoryId, sessionId, resultJson));
			analysisNotificationProducer.analysisCompleted(memberId);
			log.info("[ANALYSIS][ROADMAP][END] saved analysisId={}", saved.getAnalysisId());
			return saved;
		} catch (Exception e) {
			log.error("[ANALYSIS][ROADMAP] persist/serialize failed - err={}", e.toString(), e);
			throw new AnalysisException(ErrorCode.ANALYSIS_OUTPUT_SERIALIZE_FAILED, e);
		}
	}


	/**
	 * 틀린 문제 기반 학습 가이드 생성
	 * - 1단계: 키워드만 뽑기
	 * - 2단계: DB에서 없는 키워드만 개념 요약 생성(LLM)
	 * - 3단계: 기존+신규 합쳐서 focusConcepts 구성 / futureConcepts 생성
	 */
	@Override
	@Transactional
	public Analysis analyzeQuiz(Long memberId, Long categoryId) {
		log.info("[ANALYSIS][FOCUS][START] 종합 학습 가이드 시작 - memberId={}, categoryId={}", memberId, categoryId);

		// 오답 ID 조회
		List<Long> wrongIds = memberQuizResultPort.findAnsweredQuizIds(memberId, categoryId, Boolean.FALSE);
		if (wrongIds == null)
			wrongIds = List.of();
		log.debug("[ANALYSIS][FOCUS] 오답 ID 조회 완료 - wrongIds={}", wrongIds);

		// 동일 컨텍스트 내 퀴즈 메타 조회(필터 포함)
		List<Quiz> wrongQuizzes = wrongIds.isEmpty() ? List.of() : quizRepository.findByIds(wrongIds);
		Map<Long, Quiz> byId = wrongQuizzes.stream().collect(Collectors.toMap(Quiz::getQuizId, q -> q));
		log.info("[ANALYSIS][FOCUS] 퀴즈 메타 로드 - loaded={}", wrongQuizzes.size());

		if (categoryId != null) {
			wrongIds = wrongIds.stream()
				.filter(id -> byId.containsKey(id) && categoryId.equals(byId.get(id).getCategoryId()))
				.toList();
			log.debug("[ANALYSIS][FOCUS] 카테고리 필터링 완료 - filteredWrongIds={}", wrongIds);
		}

		// LLM 입력(JSON) 구성
		String itemsJson = buildItemsJson(byId, wrongIds);
		log.debug("[ANALYSIS][FOCUS] LLM 입력 직렬화 완료");

		// 1) 키워드 우선 추출 (Schema + Sanitizer)
		String kwUserPrompt = buildKeywordsUserPrompt(itemsJson);
		String kwSystem = AnalysisPrompt.FOCUS_KEYWORDS.getSystem();
		log.debug("[ANALYSIS][FOCUS] KEYWORDS 프롬프트 준비 완료");

		String kwRaw  = llmClient.generateJson(kwSystem, kwUserPrompt, JsonSchemas.focusKeywords());
		String kwJson = LlmJsonSanitizer.sanitize(kwRaw, false);
		log.info("[ANALYSIS][FOCUS] KEYWORDS LLM 호출 완료 - rawLen={}, sanitizedLen={}",
			kwRaw == null ? 0 : kwRaw.length(), kwJson == null ? 0 : kwJson.length());

		List<String> keywords = parseKeywords(kwJson);
		log.debug("[ANALYSIS][FOCUS] 키워드 파싱 완료 - keywords={}", keywords);

		// 정규화 + 중복 제거
		LinkedHashMap<String, String> normToOriginal = new LinkedHashMap<>();
		for (String k : keywords) {
			String norm = normalize(k);
			if (!norm.isBlank() && !normToOriginal.containsKey(norm)) {
				normToOriginal.put(norm, k);
			}
		}
		Set<String> normKeys = normToOriginal.keySet();
		log.info("[ANALYSIS][FOCUS] 정규화/중복 제거 - input={}, uniqueNorm={}", keywords.size(), normKeys.size());

		// 2) DB 조회, 존재하는 키워드 로드
		Map<String, KeywordConcept> existing = keywordConceptRepository.findByKeywordNormalizedIn(normKeys);
		int hit = existing.size();
		int miss = normKeys.size() - hit;
		log.info("[ANALYSIS][FOCUS] 키워드 캐시 조회 - hit={}, miss={}", hit, miss);

		// 3) 없는 키워드 목록
		List<String> missingNorm = normKeys.stream()
			.filter(n -> !existing.containsKey(n))
			.toList();
		log.debug("[ANALYSIS][FOCUS] 미보유 키워드 - {}", missingNorm);

		// 4) 없는 키워드에 대해서만 요약 생성
		List<Map<String, String>> newFocus = new ArrayList<>();
		List<String> futureConcepts; // 최종 반환용

		if (!missingNorm.isEmpty()) {
			List<String> targetKeywords = missingNorm.stream().map(normToOriginal::get).toList();
			log.debug("[ANALYSIS][FOCUS] SUMMARY 대상 키워드 - {}", targetKeywords);

			String sumUserPrompt = buildSummaryUserPrompt(itemsJson, targetKeywords);
			String sumSystem = AnalysisPrompt.FOCUS_SUMMARY.getSystem();
			log.debug("[ANALYSIS][FOCUS] SUMMARY 프롬프트 준비 완료");

			String sumRaw  = llmClient.generateJson(sumSystem, sumUserPrompt, JsonSchemas.focusSummary());
			String sumJson = LlmJsonSanitizer.sanitize(sumRaw, false);
			log.info("[ANALYSIS][FOCUS] SUMMARY LLM 호출 완료 - rawLen={}, sanitizedLen={}",
				sumRaw == null ? 0 : sumRaw.length(), sumJson == null ? 0 : sumJson.length());

			// 신규 focusConcepts 파싱
			List<Map<String, String>> focusConceptsNewOnly = parseFocusConcepts(sumJson);
			log.info("[ANALYSIS][FOCUS] 신규 focusConcepts 파싱 - count={}", focusConceptsNewOnly.size());

			// 저장
			int upserted = 0;
			for (Map<String, String> fc : focusConceptsNewOnly) {
				String original = fc.get("keyword");
				String summary = fc.get("conceptSummary");
				String norm = normalize(original);

				try {
					KeywordConcept saved = keywordConceptRepository.upsert(
						new KeywordConcept(norm, original, summary)
					);
					existing.put(norm, saved);
					newFocus.add(Map.of(
						"keyword", saved.getKeywordOriginal(),
						"conceptSummary", saved.getConceptSummary()
					));
					upserted++;
				} catch (DataIntegrityViolationException dup) {
					log.warn("[ANALYSIS][FOCUS] Upsert 충돌 - norm={}", norm);
					keywordConceptRepository.findByKeywordNormalized(norm)
						.ifPresent(found -> existing.put(norm, found));
				}
			}
			log.info("[ANALYSIS][FOCUS] Upsert 완료 - upserted={}", upserted);

			// 신규 + 기존 합쳐서 futureConcepts 파싱
			futureConcepts = parseFutureConcepts(sumJson);
			log.info("[ANALYSIS][FOCUS] futureConcepts 파싱 - count={}", futureConcepts.size());
		} else {
			// 신규 요약 생성 없이, 기존 키워드만으로 futureConcepts 별도 생성
			String futUserPrompt = buildFutureOnlyUserPrompt(itemsJson);
			String futSystem = AnalysisPrompt.FOCUS_FUTURE.getSystem();
			log.debug("[ANALYSIS][FOCUS] FUTURE 프롬프트 준비 완료");

			String futRaw  = llmClient.generateJson(futSystem, futUserPrompt, JsonSchemas.futureOnly());
			String futJson = LlmJsonSanitizer.sanitize(futRaw, false);
			log.info("[ANALYSIS][FOCUS] FUTURE LLM 호출 완료 - rawLen={}, sanitizedLen={}",
				futRaw == null ? 0 : futRaw.length(), futJson == null ? 0 : futJson.length());

			futureConcepts = parseFutureConcepts(futJson);
			log.info("[ANALYSIS][FOCUS] futureConcepts 파싱 - count={}", futureConcepts.size());
		}

		// 5) 기존 + 신규 합쳐서 최종 focusConcepts 구성
		List<Map<String, String>> mergedFocus = new ArrayList<>();
		for (String n : normKeys) {
			KeywordConcept c = existing.get(n);
			if (c != null) {
				mergedFocus.add(Map.of(
					"keyword", c.getKeywordOriginal(),
					"conceptSummary", c.getConceptSummary()
				));
			}
		}
		log.info("[ANALYSIS][FOCUS] 최종 focusConcepts 병합 - total={}, newAdded={}",
			mergedFocus.size(), newFocus.size());

		// 최종 JSON 직렬화
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("memberId", memberId);
		out.put("categoryId", categoryId);
		out.put("focusConcepts", mergedFocus);
		out.put("futureConcepts", futureConcepts);

		try {
			String finalJson = objectMapper.writeValueAsString(out);
			Analysis saved = analysisRepository.save(new Analysis(memberId, categoryId, null, finalJson));
			analysisNotificationProducer.analysisCompleted(memberId);
			return saved;
		} catch (Exception e) {
			log.error("[ANALYSIS][FOCUS] 분석 결과 직렬화 실패 - memberId={}, categoryId={}, err={}",
				memberId, categoryId, e, e);
			throw new AnalysisException(ErrorCode.ANALYSIS_OUTPUT_SERIALIZE_FAILED, e);
		}
	}

	/**
	 * 선택된 퀴즈 ID 기반 학습 가이드 생성
	 *
	 * @param memberId   멤버 ID
	 * @param categoryId 카테고리 ID
	 * @param sessionId  세션 ID (AI 생성 퀴즈 묶음)
	 * @param quizIds    선택된 퀴즈 ID 목록
	 */
	@Override
	@Transactional
	public void analyzeFromQuizIds(Long memberId, Long categoryId, String sessionId, List<Long> quizIds) {
		log.info("[ANALYSIS][FOCUS-SELECTED][START] memberId={}, categoryId={}, sessionId={}, ids={}",
			memberId, categoryId, sessionId, quizIds);
		// 입력 비었으면 아무 것도 하지 않고 종료
		if (quizIds == null || quizIds.isEmpty()) {
			log.debug("[ANALYSIS][FOCUS-SELECTED][SKIP] empty quizIds - memberId={}, categoryId={}",
				memberId, categoryId);
			return;
		}

		// 1) 퀴즈 조회(선택된 id만)
		List<Quiz> selected = quizRepository.findByIds(quizIds);
		Map<Long, Quiz> byId = selected.stream().collect(Collectors.toMap(Quiz::getQuizId, q -> q));

		// 카테고리 필터링
		List<Long> filtered = quizIds.stream()
			.filter(id -> byId.containsKey(id) && Objects.equals(byId.get(id).getCategoryId(), categoryId))
			.toList();

		// 필터 결과가 비면 저장 없이 종료
		if (filtered.isEmpty()) {
			log.debug("[ANALYSIS][FOCUS-SELECTED][SKIP] filtered empty - mid={}, cid={}", memberId, categoryId);
			return;
		}

		// 2) LLM 입력 JSON 구성
		String itemsJson = buildItemsJson(byId, filtered);

		// 3) 키워드 추출
		String kwUser = buildKeywordsUserPrompt(itemsJson);
		String kwSystem = AnalysisPrompt.FOCUS_KEYWORDS.getSystem();
		String kwRaw  = llmClient.generateJson(kwSystem, kwUser, JsonSchemas.focusKeywords());
		String kwJson = LlmJsonSanitizer.sanitize(kwRaw, false);
		List<String> keywords = parseKeywords(kwJson);

		// 4) 정규화 + 중복 제거
		LinkedHashMap<String, String> normToOriginal = new LinkedHashMap<>();
		for (String k : keywords) {
			String norm = normalize(k);
			if (!norm.isBlank() && !normToOriginal.containsKey(norm)) {
				normToOriginal.put(norm, k);
			}
		}
		Set<String> normKeys = normToOriginal.keySet();
		Map<String, KeywordConcept> existing = keywordConceptRepository.findByKeywordNormalizedIn(normKeys);

		// 5) 미스만 요약 생성
		List<String> missingNorm = normKeys.stream().filter(n -> !existing.containsKey(n)).toList();

		List<Map<String, String>> newFocus = new ArrayList<>();
		List<String> futureConcepts;

		if (!missingNorm.isEmpty()) {
			// 미보유 키워드에 대해서만 요약 생성
			List<String> targets = missingNorm.stream().map(normToOriginal::get).toList();
			String sumUser = buildSummaryUserPrompt(itemsJson, targets);
			String sumSystem = AnalysisPrompt.FOCUS_SUMMARY.getSystem();

			String sumRaw  = llmClient.generateJson(sumSystem, sumUser, JsonSchemas.focusSummary());
			String sumJson = LlmJsonSanitizer.sanitize(sumRaw, false);

			// 신규 focusConcepts 파싱 + 저장
			List<Map<String, String>> focusConceptsNewOnly = parseFocusConcepts(sumJson);

			int upserted = 0;
			for (Map<String, String> fc : focusConceptsNewOnly) {
				String original = fc.get("keyword");
				String summary  = fc.get("conceptSummary");
				String norm = normalize(original);
				try {
					KeywordConcept saved = keywordConceptRepository.upsert(
						new KeywordConcept(norm, original, summary)
					);
					existing.put(norm, saved);
					newFocus.add(Map.of(
						"keyword", saved.getKeywordOriginal(),
						"conceptSummary", saved.getConceptSummary()
					));
					upserted++;
				} catch (DataIntegrityViolationException dup) {
					keywordConceptRepository.findByKeywordNormalized(norm)
						.ifPresent(found -> existing.put(norm, found));
				}
			}
			futureConcepts = parseFutureConcepts(sumJson);
			log.info("[ANALYSIS][FOCUS-SELECTED] upserted={}, newFocus={}", upserted, newFocus.size());
		} else {
			String futUser = buildFutureOnlyUserPrompt(itemsJson);
			String futSystem = AnalysisPrompt.FOCUS_FUTURE.getSystem();
			String futRaw  = llmClient.generateJson(futSystem, futUser, JsonSchemas.futureOnly());
			String futJson = LlmJsonSanitizer.sanitize(futRaw, false);
			futureConcepts = parseFutureConcepts(futJson);
		}

		// 6) 최종 merge
		List<Map<String, String>> mergedFocus = new ArrayList<>();
		for (String n : normKeys) {
			KeywordConcept c = existing.get(n);
			if (c != null) {
				mergedFocus.add(Map.of("keyword", c.getKeywordOriginal(),
					"conceptSummary", c.getConceptSummary()));
			}
		}

		// 7) 메타 포함하여 JSON 직렬화 후 저장
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("memberId", memberId);
		out.put("categoryId", categoryId);
		out.put("source", "latest");
		out.put("sessionId", sessionId);
		out.put("usedQuizIds", filtered);
		out.put("focusConcepts", mergedFocus);
		out.put("futureConcepts", futureConcepts);

		try {
			String finalJson = objectMapper.writeValueAsString(out);

			// 세션 연계 저장
			Analysis saved = analysisRepository.save(new Analysis(memberId, categoryId, sessionId, finalJson));
			sessionRepository.linkAnalysis(sessionId, saved.getAnalysisId());

			log.info("[ANALYSIS][FOCUS-SELECTED][END] saved analysisId={}, focus={}, future={}",
				saved.getAnalysisId(), mergedFocus.size(), futureConcepts.size());
			analysisNotificationProducer.focusGuideReady(memberId);
		} catch (Exception e) {
			throw new AnalysisException(ErrorCode.ANALYSIS_OUTPUT_SERIALIZE_FAILED, e);
		}
	}

	// 헬퍼 메서드

	/**
	 * 키워드 정규화 (소문자, 공백제거)
	 * @param keyword 원본 키워드
	 * @return 정규화된 키워드
	 */
	private String normalize(String keyword) {
		if (keyword == null)
			return "";
		return keyword.trim().replaceAll("\\s+", " ").toLowerCase();
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
			return objectMapper.writeValueAsString(llmInput);
		} catch (Exception ex) {
			log.error("[ANALYSIS][FOCUS] LLM 입력 직렬화 실패 - wrongIds={}, err={}", wrongIds, ex, ex);
			throw new AnalysisException(ErrorCode.ANALYSIS_INPUT_SERIALIZE_FAILED, ex);
		}
	}

	/**
	 * 키워드 추출용 유저 프롬프트 구성
	 * @param itemsJson LLM 입력 JSON
	 * @return 유저 프롬프트
	 */
	private String buildKeywordsUserPrompt(String itemsJson) {
		return """
			입력 데이터(JSON):
			%s

			요청:
			- 오답들을 종합 분석하여 지금 학습해야 할 핵심 '키워드'만을 JSON으로 반환하라.
			- 반드시 스키마: { "keywords": ["string"] }
			- 한국어, 중복/유사표현 제거, 2~5개.
			""".formatted(itemsJson);
	}

	/**
	 * 요약 + 확장 키워드 생성용 유저 프롬프트 구성
	 * @param itemsJson LLM 입력 JSON
	 * @param targetKeywords 요약 대상 키워드 목록
	 * @return 유저 프롬프트
	 */
	private String buildSummaryUserPrompt(String itemsJson, List<String> targetKeywords) {
		try {
			Map<String, Object> input = new LinkedHashMap<>();
			input.put("itemsJson", objectMapper.readTree(itemsJson)); // 보기 좋게 중첩
			input.put("targetKeywords", targetKeywords);
			String payload = objectMapper.writeValueAsString(input);
			return """
				입력(JSON):
				%s

				요청:
				- targetKeywords에 해당하는 키워드에 대해서만 focusConcepts를 생성하고,
				  futureConcepts도 함께 제안하라.
				- 반드시 스키마:
				{
				  "focusConcepts": [{ "keyword": "string", "conceptSummary": "string" }],
				  "futureConcepts": ["string"]
				}
				- conceptSummary는 3~5문장, 최소 50자 이상, 한국어.
				""".formatted(payload);
		} catch (Exception e) {
			log.error("[ANALYSIS][FOCUS] 요약 요청 직렬화 실패 - targets={}, err={}", targetKeywords, e, e);
			throw new AnalysisException(ErrorCode.ANALYSIS_SUMMARY_PROMPT_BUILD_FAILED, e);
		}
	}

	/**
	 * 확장 키워드 전용 유저 프롬프트 구성
	 * @param itemsJson LLM 입력 JSON
	 * @return 유저 프롬프트
	 */
	private String buildFutureOnlyUserPrompt(String itemsJson) {
		return """
			입력 데이터(JSON):
			%s

			요청:
			- 추후 학습하면 좋은 확장 키워드만을 JSON으로 반환하라.
			- 반드시 스키마: { "futureConcepts": ["string"] }
			- 한국어, 2~5개.
			""".formatted(itemsJson);
	}

	/**
	 * 키워드 파싱
	 * @param json LLM 반환 JSON
	 * @return 키워드 목록
	 */
	private List<String> parseKeywords(String json) {
		try {
			JsonNode root = objectMapper.readTree(json);
			if (!root.has("keywords") || !root.get("keywords").isArray()) {
				throw new AnalysisException(ErrorCode.ANALYSIS_KEYWORDS_PARSE_FAILED);
			}
			List<String> keywords = new ArrayList<>();
			for (JsonNode n : root.get("keywords")) {
				String k = n.asText("");
				if (!k.isBlank())
					keywords.add(k);
			}
			if (keywords.isEmpty())
				throw new AnalysisException(ErrorCode.ANALYSIS_KEYWORDS_PARSE_FAILED);
			return keywords;
		} catch (Exception e) {
			log.warn("[ANALYSIS][FOCUS] keywords 파싱 실패 - err={}", e, e);
			throw new AnalysisException(ErrorCode.ANALYSIS_KEYWORDS_PARSE_FAILED, e);
		}
	}

	/**
	 * focusConcepts 파싱
	 * @param json LLM 반환 JSON
	 * @return 키워드+요약 목록
	 */
	private List<Map<String, String>> parseFocusConcepts(String json) {
		try {
			JsonNode root = objectMapper.readTree(json);
			if (!root.has("focusConcepts") || !root.get("focusConcepts").isArray()) {
				throw new AnalysisException(ErrorCode.ANALYSIS_FOCUS_PARSE_FAILED);
			}
			List<Map<String, String>> list = new ArrayList<>();
			for (JsonNode n : root.get("focusConcepts")) {
				if (!n.has("keyword") || !n.has("conceptSummary")) {
					throw new AnalysisException(ErrorCode.ANALYSIS_FOCUS_PARSE_FAILED);
				}
				String keyword = n.get("keyword").asText("");
				String summary = n.get("conceptSummary").asText("");
				if (keyword.isBlank() || summary.isBlank()) {
					throw new AnalysisException(ErrorCode.ANALYSIS_FOCUS_PARSE_FAILED);
				}
				Map<String, String> row = new LinkedHashMap<>();
				row.put("keyword", keyword);
				row.put("conceptSummary", summary);
				list.add(row);
			}
			if (list.isEmpty())
				throw new AnalysisException(ErrorCode.ANALYSIS_FOCUS_PARSE_FAILED);
			return list;
		} catch (Exception e) {
			log.warn("[ANALYSIS][FOCUS] focusConcepts 파싱 실패 - err={}", e, e);
			throw new AnalysisException(ErrorCode.ANALYSIS_FOCUS_PARSE_FAILED, e);
		}
	}

	/**
	 * futureConcepts 파싱
	 * @param json LLM 반환 JSON
	 * @return 키워드 목록
	 */
	private List<String> parseFutureConcepts(String json) {
		try {
			JsonNode root = objectMapper.readTree(json);
			if (!root.has("futureConcepts") || !root.get("futureConcepts").isArray()) {
				throw new AnalysisException(ErrorCode.ANALYSIS_FUTURE_PARSE_FAILED);
			}
			List<String> concepts = new ArrayList<>();
			for (JsonNode n : root.get("futureConcepts")) {
				String c = n.asText("");
				if (!c.isBlank())
					concepts.add(c);
			}
			if (concepts.isEmpty())
				throw new AnalysisException(ErrorCode.ANALYSIS_FUTURE_PARSE_FAILED);
			return concepts;
		} catch (Exception e) {
			log.warn("[ANALYSIS][FOCUS] futureConcepts 파싱 실패 - err={}", e, e);
			throw new AnalysisException(ErrorCode.ANALYSIS_FUTURE_PARSE_FAILED, e);
		}
	}

	/**
	 * 로드맵(주차형) 유저 프롬프트 생성
	 * - 1단계: 기본값 적용 (weeks=8, hoursPerWeek=5 등)
	 * - 2단계: 입력 컨텍스트 JSON 구성(skillTag, groupName, totalWeeks, hoursPerWeek)
	 * - 3단계: LLM 사용자 프롬프트 문자열로 반환
	 */
	private String buildRoadmapUserPrompt(String groupName, String skillTag) {
		String tag = (skillTag == null || skillTag.isBlank()) ? "포괄적인 공부법" : skillTag;
		String grp = (groupName == null || groupName.isBlank()) ? "미지정 그룹" : groupName;

		return """
    {
      "input": {
        "platform": "GROW",
        "mode": "GROUP_SKILLTAG_ROADMAP_SCHEDULE_AUTO",
        "skillTag": "%s",
        "groupName": "%s",
        "periodPolicy": {
          "infer": true,
          "guideline": {
            "minWeeks": 4,
            "maxWeeks": 12,
            "preferredHoursPerWeek": [3, 8],
            "notes": [
              "선행지식/학습 난이도에 따라 총주차와 주당시간을 합리적으로 산정",
              "주차별 예상시간은 기간설정의 주당시간을 과도하게 벗어나지 않게 배분"
            ]
          }
        }
      }
    }
    """.formatted(tag, grp);
	}
}