package com.grow.ai_coaching_service.question.domain.repository;

import com.grow.ai_coaching_service.question.domain.model.Question;

public interface QuestionRepository {
    Question save(Question question);
}
