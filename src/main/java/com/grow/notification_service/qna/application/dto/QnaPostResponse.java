package com.grow.notification_service.qna.application.dto;

import java.time.LocalDateTime;

import com.grow.notification_service.qna.domain.model.QnaPost;
import com.grow.notification_service.qna.domain.model.enums.QnaStatus;
import com.grow.notification_service.qna.domain.model.enums.QnaType;

/**
 * QnA 게시글 응답
 * QnA 게시글의 세부 정보를 나타내는 DTO입니다.
 * @param id 게시글 ID
 * @param type 게시글 유형 (QUESTION 또는 ANSWER)
 * @param parentId 부모 게시글 ID (답변인 경우 질문의 ID, 질문인 경우 null)
 * @param memberId 작성자 회원 ID
 * @param content 게시글 내용
 * @param status 게시글 상태 (ACTIVE 또는 DELETED)
 * @param createdAt 생성 일시
 * @param updatedAt 수정 일시
 */
public record QnaPostResponse(
	Long id,
	QnaType type,
	Long parentId,
	Long memberId,
	String content,
	QnaStatus status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static QnaPostResponse from(QnaPost p) {
		return new QnaPostResponse(
			p.getId(),
			p.getType(),
			p.getParentId(),
			p.getMemberId(),
			p.getContent(),
			p.getStatus(),
			p.getCreatedAt(),
			p.getUpdatedAt()
		);
	}
}