package com.grow.notification_service.qna.application.service.impl;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.global.exception.QnaException;
import com.grow.notification_service.qna.application.port.AuthorityCheckerPort;
import com.grow.notification_service.qna.application.service.QnaCommandService;
import com.grow.notification_service.qna.domain.model.QnaPost;
import com.grow.notification_service.qna.domain.model.enums.QnaType;
import com.grow.notification_service.qna.domain.repository.QnaPostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QnaCommandServiceImpl implements QnaCommandService {

	private final QnaPostRepository repository;
	private final AuthorityCheckerPort authorityCheckerPort;
	private final Clock clock;

	/** 질문 작성: parentId=null -> 루트, parentId=answerId -> 추가 질문 */
	@Override
	@Transactional
	public Long createQuestion(Long memberId, String content, Long parentId) {
		final int contentLen = content == null ? 0 : content.length();
		log.info("[Q&A][질문][시도] memberId={}, parentId={}, contentLen={}", memberId, parentId, contentLen);

		if (parentId == null) {
			// 루트 질문
			log.debug("[Q&A][질문][루트] memberId={} 루트 질문 생성 진행", memberId);
			QnaPost root = QnaPost.newRootQuestion(memberId, content, clock);
			Long id = repository.save(root);
			log.info("[Q&A][질문][성공] memberId={}, postId={}, type={}, parentId=null", memberId, id, QnaType.QUESTION);
			return id;
		}

		// 추가 질문: parent는 반드시 ANSWER
		QnaPost parent = repository.findById(parentId)
			.orElseThrow(() -> {
				log.warn("[Q&A][질문][거절] parentId={} 를 찾을 수 없음 (추가 질문 불가)", parentId);
				return new QnaException(ErrorCode.QNA_NOT_FOUND);
			});

		if (parent.getType() != QnaType.ANSWER) {
			log.warn("[Q&A][질문][거절] parentId={}의 타입이 {} (ANSWER 하위에서만 추가 질문 허용)", parentId, parent.getType());
			throw new QnaException(ErrorCode.INVALID_QNA_PARENT); // ANSWER 밑에서만 질문 허용
		}

		// 추가 질문 작성
		log.debug("[Q&A][질문][추가] memberId={}, parent(answerId)={}, contentLen={}", memberId, parent.getId(), contentLen);
		QnaPost followUp = QnaPost.newFollowUpQuestion(memberId, parent.getId(), content, clock);
		Long id = repository.save(followUp);
		log.info("[Q&A][질문][성공] memberId={}, postId={}, type={}, parentId={}", memberId, id, QnaType.QUESTION, parent.getId());
		return id;
	}

	/** 답변 작성: parent(=questionId)는 반드시 QUESTION, 작성자는 관리자 */
	@Override
	@Transactional
	public Long createAnswer(Long memberId, Long questionId, String content) {
		final int contentLen = content == null ? 0 : content.length();
		log.info("[Q&A][답변][시도] memberId={}, questionId={}, contentLen={}", memberId, questionId, contentLen);

		// 작성자가 관리자인지 확인
		boolean admin = authorityCheckerPort.isAdmin(memberId);
		log.debug("[Q&A][답변][검증] memberId={} isAdmin={}", memberId, admin);
		if (!admin) {
			log.warn("[Q&A][답변][거절] memberId={} 관리자 권한 없음", memberId);
			throw new QnaException(ErrorCode.NO_PERMISSION_TO_WRITE_ANSWER);
		}

		// 부모 질문 조회
		QnaPost parent = repository.findById(questionId)
			.orElseThrow(() -> {
				log.warn("[Q&A][답변][거절] questionId={} 를 찾을 수 없음", questionId);
				return new QnaException(ErrorCode.QNA_NOT_FOUND);
			});

		// 부모가 QUESTION인지 확인
		if (parent.getType() != QnaType.QUESTION) {
			log.warn("[Q&A][답변][거절] parentId={}의 타입이 {} (QUESTION 하위에서만 답변 허용)", questionId, parent.getType());
			throw new QnaException(ErrorCode.INVALID_QNA_PARENT); // QUESTION 밑에서만 답변 허용
		}

		// 답변 작성
		log.debug("[Q&A][답변][작성] memberId={}, questionId={}, contentLen={}", memberId, questionId, contentLen);
		QnaPost answer = QnaPost.newAnswer(memberId, parent.getId(), content, clock);
		Long id = repository.save(answer);
		log.info("[Q&A][답변][성공] memberId={}, postId={}, type={}, parentId={}", memberId, id, QnaType.ANSWER, parent.getId());
		return id;
	}
}