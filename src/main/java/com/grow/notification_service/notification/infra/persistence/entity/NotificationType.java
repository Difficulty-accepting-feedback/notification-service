package com.grow.notification_service.notification.infra.persistence.entity;

public enum NotificationType {
    COMMENT,            // 댓글
    STUDY_NOTICE,       // 스터디 공지
    LIKE,               // 좋아요
    SERVICE_NOTICE,     // 서비스 공지
    ASSIGNMENT,         // 과제
    POINT,              // 포인트
    MATCHING_SUCCESS,   // 매칭 성공
    MESSAGE,            // 쪽지
    INQUIRY_ANSWER,     // 문의 답변
    REVIEW              // 리뷰
}