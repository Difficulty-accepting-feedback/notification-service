package com.grow.notification_service.qna.infra.persistence.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.grow.notification_service.qna.domain.model.enums.QnaType;
import com.grow.notification_service.qna.infra.persistence.entity.QnaPostJpaEntity;

public interface QnaPostJpaRepository extends JpaRepository<QnaPostJpaEntity, Long> {

	Page<QnaPostJpaEntity> findByTypeOrderByCreatedAtDesc(QnaType type, Pageable pageable);

	List<QnaPostJpaEntity> findByParentIdOrderByCreatedAtAsc(Long parentId);

	boolean existsByPostId(Long postId);
}