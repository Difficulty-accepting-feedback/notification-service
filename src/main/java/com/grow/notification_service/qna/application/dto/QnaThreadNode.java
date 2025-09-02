package com.grow.notification_service.qna.application.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.grow.notification_service.qna.domain.model.QnaPost;
import com.grow.notification_service.qna.domain.model.enums.QnaStatus;
import com.grow.notification_service.qna.domain.model.enums.QnaType;

import lombok.Getter;

/**
 * QnA 스레드 노드
 * QnA 게시글과 그 자식 게시글들을 트리 구조로 표현하는 DTO입니다.
 * 각 노드는 게시글의 세부 정보와 자식 노드 목록을 포함합니다.
 */
@Getter
public class QnaThreadNode {
	private final Long id;
	private final QnaType type;
	private final Long parentId;
	private final Long memberId;
	private final String content;
	private final QnaStatus status;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;
	private final List<QnaThreadNode> children = new ArrayList<>();

	public QnaThreadNode(QnaPost p) {
		this.id = p.getId();
		this.type = p.getType();
		this.parentId = p.getParentId();
		this.memberId = p.getMemberId();
		this.content = p.getContent();
		this.status = p.getStatus();
		this.createdAt = p.getCreatedAt();
		this.updatedAt = p.getUpdatedAt();
	}

	public void addChild(QnaThreadNode child) { this.children.add(child); }

	public void sortRecursively() {
		this.children.sort(Comparator.comparing(QnaThreadNode::getCreatedAt));
		this.children.forEach(QnaThreadNode::sortRecursively);
	}
}