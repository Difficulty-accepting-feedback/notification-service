package com.grow.ai_coaching_service.answer.domain.model;

import java.time.Clock;
import java.time.LocalDateTime;

public class Answer {

    private Long answerId;
    private Long questionId; // 문의 ID
    private String content; // 문의에 대한 답변 내용
    private LocalDateTime createdAt; // 문의 답변 시각

    // 첫 생성 용도
    public Answer(Long questionId,
                  String content,
                  Clock createdAt
    ) {
        this.answerId = null;
        this.questionId = questionId;
        this.content = content;

        if (createdAt != null) {
            this.createdAt = LocalDateTime.now(createdAt);
        } else  {
            this.createdAt = LocalDateTime.now();
        }
    }

    // 이후 조회 용도
    public Answer(Long answerId,
                  Long questionId,
                  String content,
                  LocalDateTime createdAt
    ) {
        this.answerId = answerId;
        this.questionId = questionId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getAnswerId() {
        return answerId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
