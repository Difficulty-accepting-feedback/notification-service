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
    REVIEW("[리뷰]");

    private final String title;

    NotificationType(String title) {
        this.title = title;
    }

}