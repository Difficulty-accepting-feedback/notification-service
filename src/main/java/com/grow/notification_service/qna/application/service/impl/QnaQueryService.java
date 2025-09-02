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
import com.grow.notification_service.qna.domain.model.QnaPost;
import com.grow.notification_service.qna.domain.model.enums.QnaType;
import com.grow.notification_service.qna.domain.repository.QnaPostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaQueryService {

	private final QnaPostRepository repository;
	private final AuthorityCheckerPort authorityCheckerPort; // 관리자 검증

	private void assertAdmin(Long memberId, String actionTag) {
		boolean admin = authorityCheckerPort.isAdmin(memberId);
		log.debug("[Q&A][조회][검증][관리자] memberId={} isAdmin={} action={}", memberId, admin, actionTag);
		if (!admin) {
			log.warn("[Q&A][조회][거절][관리자필수] memberId={} action={}", memberId, actionTag);
			throw new QnaException(ErrorCode.QNA_FORBIDDEN);
		}
	}

	/** 관리자: 루트 QUESTION 기준 전체 스레드 조회 */
	public QnaThreadResponse getThreadAsAdmin(Long rootQuestionId, Long viewerId) {
		assertAdmin(viewerId, "getThreadAsAdmin");
		return buildThreadChecked(rootQuestionId, /*ownerMustBe*/ null);
	}

	/** 개인: 본인 소유 루트 QUESTION의 전체 스레드 조회 */
	public QnaThreadResponse getMyThread(Long rootQuestionId, Long viewerId) {
		if (viewerId == null) {
			log.warn("[Q&A][조회][스레드][거절] viewerId 누락");
			throw new QnaException(ErrorCode.QNA_FORBIDDEN);
		}
		return buildThreadChecked(rootQuestionId, viewerId);
	}

	/** ★ 관리자: 루트 질문 목록 */
	public Page<QnaPost> getRootQuestionsAsAdmin(Pageable pageable, Long viewerId) {
		assertAdmin(viewerId, "getRootQuestionsAsAdmin");
		log.info("[Q&A][조회][루트목록-관리자][시도] viewerId={}, page={}, size={}",
			viewerId, pageable.getPageNumber(), pageable.getPageSize());

		Page<QnaPost> page = repository.findRootQuestions(pageable);

		log.info("[Q&A][조회][루트목록-관리자][성공] total={}", page.getTotalElements());
		return page;
	}

	/** ★ 개인: 내 루트 질문 목록 */
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

	/**
	 * 루트 질문을 기준으로 BFS 탐색하여 전체 트리를 구성
	 *
	 * @param rootQuestionId 루트 질문 ID
	 * @param ownerMustBe    소유자 검증 ID (관리자 호출 시 null)
	 * @return QnaThreadResponse (트리 구조로 변환된 응답)
	 */
	private QnaThreadResponse buildThreadChecked(Long rootQuestionId, Long ownerMustBe) {
		log.info("[Q&A][조회][스레드][시도] rootQuestionId={}, ownerMustBe={}", rootQuestionId, ownerMustBe);

		// 1. 루트 질문 조회
		QnaPost root = repository.findById(rootQuestionId)
			.orElseThrow(() -> {
				log.warn("[Q&A][조회][스레드][거절] rootQuestionId={} 미존재", rootQuestionId);
				return new QnaException(ErrorCode.QNA_NOT_FOUND);
			});

		// 2. 루트는 QUESTION 타입만 가능
		if (root.getType() != QnaType.QUESTION) {
			log.warn("[Q&A][조회][스레드][거절] rootQuestionId={} 타입={} (QUESTION만 루트 허용)", rootQuestionId, root.getType());
			throw new QnaException(ErrorCode.INVALID_QNA_PARENT);
		}

		// 3. 개인 조회 시 소유자 검증
		if (ownerMustBe != null && !root.getMemberId().equals(ownerMustBe)) {
			log.warn("[Q&A][조회][스레드][거절] 소유자 불일치(owner={}, viewer={})", root.getMemberId(), ownerMustBe);
			throw new QnaException(ErrorCode.QNA_FORBIDDEN);
		}

		// 4. BFS 탐색으로 모든 자식 수집
		Map<Long, QnaPost> collected = new LinkedHashMap<>();
		Deque<QnaPost> q = new ArrayDeque<>();
		q.add(root);
		collected.put(root.getId(), root);

		while (!q.isEmpty()) {
			QnaPost cur = q.poll();
			List<QnaPost> children = repository.findChildren(cur.getId());
			children.sort(Comparator.comparing(QnaPost::getCreatedAt)); // 시간순 정렬
			for (QnaPost ch : children) {
				if (!collected.containsKey(ch.getId())) {
					collected.put(ch.getId(), ch);
					q.add(ch);
				}
			}
		}

		// 5. 수집된 QnaPost를 QnaThreadNode로 변환
		Map<Long, QnaThreadNode> nodeMap = new HashMap<>();
		for (QnaPost p : collected.values()) {
			nodeMap.put(p.getId(), new QnaThreadNode(p));
		}

		// 6. 부모-자식 관계 연결
		QnaThreadNode rootNode = nodeMap.get(root.getId());
		for (QnaPost p : collected.values()) {
			if (p.getParentId() == null) continue;
			QnaThreadNode parent = nodeMap.get(p.getParentId());
			QnaThreadNode child = nodeMap.get(p.getId());
			if (parent != null) parent.addChild(child);
		}

		// 7. 재귀
		rootNode.sortRecursively();

		log.info("[Q&A][조회][스레드][성공] rootQuestionId={}, totalNodes={}", rootQuestionId, collected.size());
		return new QnaThreadResponse(rootNode);
	}
}