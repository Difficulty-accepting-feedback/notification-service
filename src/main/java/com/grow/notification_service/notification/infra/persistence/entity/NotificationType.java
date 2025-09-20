package com.grow.notification_service.notification.infra.persistence.entity;

import lombok.Getter;

@Getter
public enum NotificationType {
    COMMENT("[댓글]"),
    STUDY_NOTICE("[스터디 공지]"),
    LIKE("[👍]"),
    SERVICE_NOTICE("[GROW]"),
    ASSIGNMENT("[과제]"),
    POINT("[포인트]"),
    MATCHING_SUCCESS("[매칭 업데이트]"),
    MESSAGE("[쪽지]"),
    INQUIRY_ANSWER("[문의 답변]"),
    REVIEW("[리뷰]"),
    PAYMENT("[결제]"),
    QNA("[Q&A]"),
    NOTE("[쪽지]"),
    QUIZ("[퀴즈]"),
    ANALYSIS("[분석]"),

    GROUP_JOIN_REQUEST("[그룹 참여 요청]"),
    GROUP_JOIN_APPROVAL("[그룹 참여 승인]"),
    GROUP_JOIN_REJECTION("[그룹 참여 거절]"),;

    private final String title;

    NotificationType(String title) {
        this.title = title;
    }

}