package com.grow.notification_service.qna.application.dto;

import java.util.List;

/**
 * QnA 스레드 페이지 응답
 * QnA 스레드의 페이징된 응답을 나타내는 DTO입니다.
 * @param page 현재 페이지 번호 (0부터 시작)
 * @param size 페이지 크기 (한 페이지당 상위 ANSWER 서브트리 수)
 * @param totalElements 전체 상위 ANSWER 서브트리 수
 * @param totalPages 전체 페이지 수
 * @param root QnA 스레드의 루트 QUESTION 노드
 * @param items 현재 페이지의 상위 ANSWER 서브트리 목록
 */
public record QnaThreadPageResponse(
	int page,
	int size,
	long totalElements,
	int totalPages,
	QnaThreadRootView root,
	List<QnaThreadNode> items // 이 페이지에 포함된 상위 ANSWER 서브트리들
) {
	public static QnaThreadPageResponse of(
		int page, int size, long totalElements, int totalPages,
		QnaThreadRootView root, List<QnaThreadNode> items
	) {
		return new QnaThreadPageResponse(page, size, totalElements, totalPages, root, items);
	}
}