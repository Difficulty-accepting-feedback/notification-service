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
	Page<QnaPost> findByType(QnaType type, Pageable pageable);
	List<QnaPost> findChildren(Long parentId);
	Page<QnaPost> findMyQuestions(Long memberId, Pageable pageable);
	Page<QnaPost> findRootQuestions(Pageable pageable);
	Page<QnaPost> findMyRootQuestions(Long memberId, Pageable pageable);
	boolean existsById(Long id);
}