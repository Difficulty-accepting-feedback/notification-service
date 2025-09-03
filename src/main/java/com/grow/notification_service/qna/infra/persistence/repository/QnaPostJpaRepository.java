package com.grow.notification_service.qna.infra.persistence.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.grow.notification_service.qna.domain.model.enums.QnaType;
import com.grow.notification_service.qna.infra.persistence.entity.QnaPostJpaEntity;

public interface QnaPostJpaRepository extends JpaRepository<QnaPostJpaEntity, Long> {

	Page<QnaPostJpaEntity> findByTypeOrderByCreatedAtDesc(QnaType type, Pageable pageable);

	List<QnaPostJpaEntity> findByParentIdOrderByCreatedAtAsc(Long parentId);

	Page<QnaPostJpaEntity> findByTypeAndMemberIdOrderByCreatedAtDesc(QnaType type, Long memberId, Pageable pageable);

	Page<QnaPostJpaEntity> findByTypeAndParentIdIsNullOrderByCreatedAtDesc(QnaType type, Pageable pageable);

	Page<QnaPostJpaEntity> findByTypeAndMemberIdAndParentIdIsNullOrderByCreatedAtDesc(QnaType type, Long memberId,
		Pageable pageable);

	/**
	 * rootId(QUESTION)부터 시작해 parent-child 연결을 따라 모든 하위 노드를 재귀적으로 조회한다.
	 * 루트 포함 전체 서브트리를 플랫 리스트로 반환한다.
	 */
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

	/**
	 * startId에서 parent_id를 따라 상향으로 올라가 parent_id가 NULL이고 type이 QUESTION인 루트의 post_id를 조회한다.
	 * 조회된 루트의 post_id를 한 건 반환한다. 없으면 null을 반환한다.
	 */
	@Query(value = """
		WITH RECURSIVE up (id, parent_id, ptype) AS (
		  SELECT post_id, parent_id, type
		  FROM qna_post
		  WHERE post_id = :startId
		  UNION ALL
		  SELECT p.post_id, p.parent_id, p.type
		  FROM qna_post p
		  JOIN up u ON p.post_id = u.parent_id
		)
		SELECT id
		FROM up
		WHERE parent_id IS NULL AND ptype = 'QUESTION'
		FETCH FIRST 1 ROWS ONLY
		""", nativeQuery = true)
	Long findRootIdFromAny(@Param("startId") Long startId);

	/**
	 * 지정한 post_id 한 건의 status와 updated_at을 갱신한다.
	 * 변경 대상은 전달된 rootId와 일치하는 행 한 건
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = false)
	@Query(value = """
		UPDATE qna_post
		SET status = :status,
		    updated_at = CURRENT_TIMESTAMP
		WHERE post_id = :rootId
		""", nativeQuery = true)
	int updateStatusById(@Param("rootId") Long rootId, @Param("status") String status);

	boolean existsByPostId(Long postId);
}