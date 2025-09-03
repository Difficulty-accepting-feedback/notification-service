package com.grow.notification_service.qna.infra.persistence.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.grow.notification_service.qna.domain.model.enums.QnaType;
import com.grow.notification_service.qna.infra.persistence.entity.QnaPostJpaEntity;

public interface QnaPostJpaRepository extends JpaRepository<QnaPostJpaEntity, Long> {

	Page<QnaPostJpaEntity> findByTypeOrderByCreatedAtDesc(QnaType type, Pageable pageable);

	List<QnaPostJpaEntity> findByParentIdOrderByCreatedAtAsc(Long parentId);

	Page<QnaPostJpaEntity> findByTypeAndMemberIdOrderByCreatedAtDesc(QnaType type, Long memberId, Pageable pageable);

	Page<QnaPostJpaEntity> findByTypeAndParentIdIsNullOrderByCreatedAtDesc(QnaType type, Pageable pageable);

	Page<QnaPostJpaEntity> findByTypeAndMemberIdAndParentIdIsNullOrderByCreatedAtDesc(QnaType type, Long memberId, Pageable pageable);

	@Query(value = """
    WITH RECURSIVE tree (
      post_id, type, parent_id, member_id, content, status, created_at, updated_at
    ) AS (
      SELECT
        post_id, type, parent_id, member_id, content, status, created_at, updated_at
      FROM qna_post
      WHERE post_id = :rootId
      UNION ALL
      SELECT
        c.post_id, c.type, c.parent_id, c.member_id, c.content, c.status, c.created_at, c.updated_at
      FROM qna_post c
      JOIN tree t ON c.parent_id = t.post_id
    )
    SELECT
      post_id, type, parent_id, member_id, content, status, created_at, updated_at
    FROM tree
    """, nativeQuery = true)
	List<QnaPostJpaEntity> findTreeByRootId(@Param("rootId") Long rootId);

	boolean existsByPostId(Long postId);
}