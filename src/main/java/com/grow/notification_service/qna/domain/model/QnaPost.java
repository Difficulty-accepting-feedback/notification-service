package com.grow.notification_service.qna.domain.model;

import lombok.Getter;

import java.time.Clock;
import java.time.LocalDateTime;

import com.grow.notification_service.qna.domain.model.enums.QnaStatus;
import com.grow.notification_service.qna.domain.model.enums.QnaType;

@Getter
public class QnaPost {

	private final Long id;
	private final QnaType type;
	private final Long parentId;
	private final Long authorId;
	private final String content;
	private final QnaStatus status;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;

	public QnaPost(Long id, QnaType type, Long parentId, Long authorId,
		String content, QnaStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
		if (type == QnaType.ANSWER && parentId == null) {
			throw new IllegalArgumentException("ANSWER는 parentId가 필요합니다.");
		}
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("내용은 비어 있을 수 없습니다.");
		}
		this.id = id;
		this.type = type;
		this.parentId = parentId;
		this.authorId = authorId;
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
		return new QnaPost(id, type, parentId, authorId, content,
			QnaStatus.DELETED, createdAt, updatedAt);
	}
}