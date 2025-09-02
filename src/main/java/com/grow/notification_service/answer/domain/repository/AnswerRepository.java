package com.grow.ai_coaching_service.answer.domain.repository;

import com.grow.ai_coaching_service.answer.domain.model.Answer;

public interface AnswerRepository {
    Answer save(Answer answer);
}
