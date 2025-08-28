package com.grow.notification_service.note.infra.persistence.repository;

import com.grow.notification_service.note.infra.persistence.entity.NoteJpaEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteJpaRepository extends JpaRepository<NoteJpaEntity, Long> {

	// 보낸 쪽지함: 발신자 기준 + 발신자 미삭제
	Page<NoteJpaEntity> findBySenderIdAndSenderDeletedFalseOrderByCreatedAtDesc(Long senderId, Pageable pageable);

	// 받은 쪽지함: 수신자 기준 + 수신자 미삭제
	Page<NoteJpaEntity> findByRecipientIdAndRecipientDeletedFalseOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

	// 안읽은 쪽지 개수: 수신자 기준 + 수신자 미삭제 + 안읽음
	long countByRecipientIdAndRecipientDeletedFalseAndIsReadFalse(Long recipientId);
}