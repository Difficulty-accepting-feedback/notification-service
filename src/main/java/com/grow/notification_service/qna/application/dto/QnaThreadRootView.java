package com.grow.notification_service.qna.application.dto;

import java.time.LocalDateTime;

import com.grow.notification_service.qna.domain.model.QnaPost;

/**
 * QnA 스레드 루트 뷰
 * QnA 스레드의 루트 QUESTION 노드의 요약 정보를 나타내는 DTO입니다.
 * @param id QUESTION ID
 * @param memberId 작성자 회원 ID
 * @param content QUESTION 내용
 * @param createdAt 생성 일시
 * @param updatedAt 수정 일시
 * @param totalAnswers 루트 QUESTION의 1차 ANSWER 총 개수
 */
public record QnaThreadRootView(
	Long id,
	Long memberId,
	String content,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	long totalAnswers // 루트의 1차 ANSWER 총 개수
) {
	public static QnaThreadRootView of(QnaPost root, long totalAnswers) {
		return new QnaThreadRootView(
			root.getId(),
			root.getMemberId(),
			root.getContent(),
			root.getCreatedAt(),
			root.getUpdatedAt(),
			totalAnswers
		);
	}
}