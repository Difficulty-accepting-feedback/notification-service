package com.grow.notification_service.qna.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.grow.notification_service.qna.domain.model.QnaPost;
import com.grow.notification_service.qna.domain.model.enums.QnaType;

public interface QnaPostRepository {
	Long save(QnaPost post);
	Optional<QnaPost> findById(Long id);
	Page<QnaPost> findQuestions(Pageable pageable);
	boolean existsById(Long id);
}