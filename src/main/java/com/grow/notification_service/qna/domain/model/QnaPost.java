package com.grow.notification_service.qna.domain.model;

import lombok.Getter;

import java.time.Clock;
import java.time.LocalDateTime;

import com.grow.notification_service.qna.domain.exception.QnaDomainException;
import com.grow.notification_service.qna.domain.model.enums.QnaStatus;
import com.grow.notification_service.qna.domain.model.enums.QnaType;

@Getter
public class QnaPost {

	private final Long id; // 질문/답변 ID
	private final QnaType type; // 질문 or 답변
	private final Long parentId; // 부모 질문 ID (답변인 경우에만 필요)
	private final Long memberId; // 작성자 ID
	private final String content; // 내용
	private final QnaStatus status; // 상태 (활성/삭제)
	private final LocalDateTime createdAt; // 생성 시간
	private final LocalDateTime updatedAt; // 수정 시간

	public QnaPost(Long id, QnaType type, Long parentId, Long memberId,
		String content, QnaStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
		if (type == QnaType.ANSWER && parentId == null) {
			throw QnaDomainException.AnswerParentIdRequired();
		}
		if (content == null || content.isBlank()) {
			throw QnaDomainException.ContentCannotBeBlank();
		}
		this.id = id;
		this.type = type;
		this.parentId = parentId;
		this.memberId = memberId;
		this.content = content;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	/**
	 * 질문 생성
	 * @param authorId 작성자 ID
	 * @param content 내용
	 * @param clock 시간
	 * @return 생성된 질문
	 */
	public static QnaPost newQuestion(Long authorId, String content, Clock clock) {
		return new QnaPost(null, QnaType.QUESTION, null, authorId, content,
			QnaStatus.ACTIVE, LocalDateTime.now(clock), null);
	}

	/**
	 * 답변 생성
	 * @param authorId 작성자 ID
	 * @param questionId 질문 ID
	 * @param content 내용
	 * @param clock 시간
	 * @return 생성된 답변
	 */
	public static QnaPost newAnswer(Long authorId, Long questionId, String content, Clock clock) {
		return new QnaPost(null, QnaType.ANSWER, questionId, authorId, content,
			QnaStatus.ACTIVE, LocalDateTime.now(clock), null);
	}

	/**
	 * 삭제 처리
	 * @return 삭제된 QnaPost
	 */
	public QnaPost delete() {
		return new QnaPost(id, type, parentId, memberId, content,
			QnaStatus.DELETED, createdAt, updatedAt);
	}
}