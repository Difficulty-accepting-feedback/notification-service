package com.grow.notification_service.qna.application.dto;

/**
 * QnA 스레드 응답
 * QnA 스레드의 루트 QUESTION 노드를 포함하는 DTO입니다.
 * @param root QnA 스레드의 루트 QUESTION 노드
 */
public record QnaThreadResponse(QnaThreadNode root) {}