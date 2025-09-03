package com.grow.notification_service.qna.application.service.impl;

import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.global.exception.QnaException;
import com.grow.notification_service.qna.application.dto.QnaThreadNode;
import com.grow.notification_service.qna.application.dto.QnaThreadResponse;
import com.grow.notification_service.qna.application.port.AuthorityCheckerPort;
import com.grow.notification_service.qna.application.service.QnaQueryService;
import com.grow.notification_service.qna.domain.model.QnaPost;
import com.grow.notification_service.qna.domain.model.enums.QnaType;
import com.grow.notification_service.qna.domain.repository.QnaPostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaQueryServiceImpl implements QnaQueryService {

	private final QnaPostRepository repository;
	private final AuthorityCheckerPort authorityCheckerPort; // 관리자 검증

	/** 관리자: 루트 QUESTION 기준 전체 스레드 조회 */
	@Override
	public QnaThreadResponse getThreadAsAdmin(Long rootQuestionId, Long viewerId) {
		assertAdmin(viewerId, "getThreadAsAdmin");
		return buildThreadChecked(rootQuestionId, /*ownerMustBe*/ null);
	}

	/** 개인: 본인 소유 루트 QUESTION의 전체 스레드 조회 */
	@Override
	public QnaThreadResponse getMyThread(Long rootQuestionId, Long viewerId) {
		if (viewerId == null) {
			log.warn("[Q&A][조회][스레드][거절] viewerId 누락");
			throw new QnaException(ErrorCode.QNA_FORBIDDEN);
		}
		return buildThreadChecked(rootQuestionId, viewerId);
	}

	/** ★ 관리자: 루트 질문 목록 */
	@Override
	public Page<QnaPost> getRootQuestionsAsAdmin(Pageable pageable, Long viewerId) {
		assertAdmin(viewerId, "getRootQuestionsAsAdmin");
		log.info("[Q&A][조회][루트목록-관리자][시도] viewerId={}, page={}, size={}",
			viewerId, pageable.getPageNumber(), pageable.getPageSize());

		Page<QnaPost> page = repository.findRootQuestions(pageable);

		log.info("[Q&A][조회][루트목록-관리자][성공] total={}", page.getTotalElements());
		return page;
	}

	/** ★ 개인: 내 루트 질문 목록 */
	@Override
	public Page<QnaPost> getMyRootQuestions(Pageable pageable, Long viewerId) {
		if (viewerId == null) {
			log.warn("[Q&A][조회][루트목록-개인][거절] viewerId 누락");
			throw new QnaException(ErrorCode.QNA_FORBIDDEN);
		}
		log.info("[Q&A][조회][루트목록-개인][시도] viewerId={}, page={}, size={}",
			viewerId, pageable.getPageNumber(), pageable.getPageSize());

		Page<QnaPost> page = repository.findMyRootQuestions(viewerId, pageable);

		log.info("[Q&A][조회][루트목록-개인][성공] viewerId={}, total={}", viewerId, page.getTotalElements());
		return page;
	}

	// 헬퍼 메서드
	/**
	 * 관리자 검증
	 * @param memberId 검증할 회원 ID
	 */
	private void assertAdmin(Long memberId, String actionTag) {
		boolean admin = authorityCheckerPort.isAdmin(memberId);
		log.debug("[Q&A][조회][검증][관리자] memberId={} isAdmin={} action={}", memberId, admin, actionTag);
		if (!admin) {
			log.warn("[Q&A][조회][거절][관리자필수] memberId={} action={}", memberId, actionTag);
			throw new QnaException(ErrorCode.QNA_FORBIDDEN);
		}
	}
	/**
	 * 스레드 조회 및 검증
	 * @param rootQuestionId 루트 질문 ID
	 * @param ownerMustBe 소유자 ID (null이면 관리자 조회로 간주)
	 * @return 스레드 응답
	 */
	private QnaThreadResponse buildThreadChecked(Long rootQuestionId, Long ownerMustBe) {
		log.info("[Q&A][조회][스레드][시도] rootQuestionId={}, ownerMustBe={}", rootQuestionId, ownerMustBe);

		// 1. CTE로 루트+후손 전체 로드
		List<QnaPost> flat = repository.findTreeFlat(rootQuestionId);
		if (flat.isEmpty()) {
			log.warn("[Q&A][조회][스레드][거절] rootQuestionId={} 미존재", rootQuestionId);
			throw new QnaException(ErrorCode.QNA_NOT_FOUND);
		}

		// 2. 루트 검증 (첫 원소가 루트, QUESTION)
		QnaPost root = flat.get(0);
		if (!root.getId().equals(rootQuestionId) || root.getType() != QnaType.QUESTION) {
			log.warn("[Q&A][조회][스레드][거절] root 검증 실패 id={}, type={}", root.getId(), root.getType());
			throw new QnaException(ErrorCode.INVALID_QNA_PARENT);
		}

		// 3. 개인 조회 시 소유자 검증
		if (ownerMustBe != null && !root.getMemberId().equals(ownerMustBe)) {
			log.warn("[Q&A][조회][스레드][거절] 소유자 불일치(owner={}, viewer={})", root.getMemberId(), ownerMustBe);
			throw new QnaException(ErrorCode.QNA_FORBIDDEN);
		}

		// 4. 링크 조립
		Map<Long, QnaThreadNode> nodeMap = new LinkedHashMap<Long, QnaThreadNode>(flat.size());
		for (QnaPost p : flat) {
			nodeMap.put(p.getId(), new QnaThreadNode(p));
		}

		QnaThreadNode rootNode = nodeMap.get(rootQuestionId);
		for (QnaPost p : flat) {
			if (p.getParentId() == null) continue;
			QnaThreadNode parent = nodeMap.get(p.getParentId());
			QnaThreadNode child = nodeMap.get(p.getId());
			if (parent != null) {
				parent.addChild(child);
			}
		}

		// 5. 재귀
		rootNode.sortRecursively();

		log.info("[Q&A][조회][스레드][성공] rootQuestionId={}, totalNodes={}", rootQuestionId, flat.size());
		return new QnaThreadResponse(rootNode);
	}
}