package com.grow.ai_coaching_service.question.domain.model;

import lombok.Getter;

import java.time.Clock;
import java.time.LocalDateTime;

@Getter
public class Question {

    private Long questionId;
    private String content; // 문의 내용
    private LocalDateTime createdAt; // 문의 시각

    // 최초 저장 시에 사용
    public Question(String content,
                    Clock createAt) {
        this.questionId = null;
        this.content = content;

        if (createAt != null) {
            createdAt = LocalDateTime.now(createAt);
        } else {
            createdAt = LocalDateTime.now();
        }
    }

    // 이후 값 조회 시에 사용 (전체를 다 조회할 수 있어야 하나...?)
    public Question(Long questionId,
                    String content,
                    LocalDateTime createdAt
    ) {
        this.questionId = questionId;
        this.content = content;
        this.createdAt = createdAt;
    }
}
