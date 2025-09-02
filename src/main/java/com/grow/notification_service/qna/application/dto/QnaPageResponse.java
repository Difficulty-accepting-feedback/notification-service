package com.grow.notification_service.qna.application.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.grow.notification_service.qna.domain.model.QnaPost;

/**
 * QnA 게시글 페이지 응답
 * QnA 게시글의 페이징된 응답을 나타내는 DTO입니다.
 * @param page 현재 페이지 번호 (0부터 시작)
 * @param size 페이지 크기 (한 페이지당 게시글 수)
 * @param totalElements 전체 게시글 수
 * @param totalPages 전체 페이지 수
 * @param content 현재 페이지의 QnA 게시글 목록
 */
public record QnaPageResponse(
	int page,
	int size,
	long totalElements,
	int totalPages,
	List<QnaPostResponse> content
) {
	public static QnaPageResponse from(Page<QnaPost> p) {
		List<QnaPostResponse> mapped = p.map(QnaPostResponse::from).getContent();
		return new QnaPageResponse(
			p.getNumber(),
			p.getSize(),
			p.getTotalElements(),
			p.getTotalPages(),
			mapped
		);
	}
}