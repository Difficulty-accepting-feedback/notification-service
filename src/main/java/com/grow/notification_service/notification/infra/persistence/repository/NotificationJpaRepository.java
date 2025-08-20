package com.grow.notification_service.notification.infra.persistence.repository;

import java.time.LocalDateTime;

import com.grow.notification_service.notification.application.dto.NotificationListItemResponse;
import com.grow.notification_service.notification.infra.persistence.entity.NotificationJpaEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {

	/**
	 * 알림 목록 조회
	 * @param memberId
	 * @param pageable
	 * @return
	 */
	@Query("""
        select new com.grow.notification_service.notification.application.dto.NotificationListItemResponse(
            n.notificationId, n.notificationType, n.content, n.isRead, n.createdAt
        )
        from NotificationJpaEntity n
        where n.memberId = :memberId
        order by n.isRead asc, n.createdAt desc
    """)
	Page<NotificationListItemResponse> findPage(@Param("memberId") Long memberId, Pageable pageable);

	/**
	 * 읽지 않은 알림 개수 조회
	 * @param memberId
	 * @return
	 */
	@Query("""
        select count(n)
        from NotificationJpaEntity n
        where n.memberId = :memberId and n.isRead = false
    """)
	long countUnread(@Param("memberId") Long memberId);

	/**
	 * 모든 알림을 읽음 처리
	 * @param memberId
	 * @return
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
        update NotificationJpaEntity n
        set n.isRead = true
        where n.memberId = :memberId and n.isRead = false
    """)
	int markAllRead(@Param("memberId") Long memberId);

	/**
	 * 특정 알림을 읽음 처리
	 * @param memberId
	 * @param id
	 * @return
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
        update NotificationJpaEntity n
        set n.isRead = true
        where n.notificationId = :id and n.memberId = :memberId and n.isRead = false
    """)
	int markOneRead(@Param("memberId") Long memberId, @Param("id") Long id);

	/**
	 * 특정 알림 삭제
	 * @param id
	 * @param memberId
	 * @return
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	int deleteByNotificationIdAndMemberId(Long id, Long memberId);

	/**
	 * 오래된 알림 삭제
	 * @param memberId
	 * @param before
	 * @return
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
        delete from NotificationJpaEntity n
        where n.memberId = :memberId and n.createdAt < :before
    """)
	int deleteOld(@Param("memberId") Long memberId, @Param("before") LocalDateTime before);

	/**
	 * 읽지 않은 알림 목록 조회 (5개)
	 * @param memberId
	 * @param pageable
	 * @return
	 */
	@Query("""
    select new com.grow.notification_service.notification.application.dto.NotificationListItemResponse(
        n.notificationId, n.notificationType, n.content, n.isRead, n.createdAt
    )
    from NotificationJpaEntity n
    where n.memberId = :memberId and n.isRead = false
    order by n.createdAt desc
""")
	Page<NotificationListItemResponse> findTopUnread(
		@Param("memberId") Long memberId,
		org.springframework.data.domain.Pageable pageable
	);
}