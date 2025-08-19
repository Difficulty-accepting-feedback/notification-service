package com.grow.notification_service.notification.application.service.impl;

import com.grow.notification_service.notification.application.event.NotificationSavedEvent;
import com.grow.notification_service.notification.application.service.NotificationService;
import com.grow.notification_service.notification.domain.model.Notification;
import com.grow.notification_service.notification.domain.repository.NotificationRepository;
import com.grow.notification_service.notification.presentation.dto.NotificationRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
/**
 * <h2>알림 서비스의 구현 클래스</h2>
 * 알림 요청을 받아 데이터베이스에 저장하고, 저장 후 이벤트를 발행하여
 * 실시간 알림(예: SSE 푸시)을 트리거하는 역할을 합니다.
 *
 * <p>이 클래스는 @Async를 사용하여 비동기적으로 동작하며, @Transactional로
 * 데이터베이스 작업의 일관성을 보장합니다. ApplicationEventPublisher를 통해
 * NotificationSavedEvent를 발행하여 다른 서비스(SSE 알림 전송)와 연계됩니다.
 *
 * <p><b>주요 기능:</b>
 * <ul>
 *     <li>알림 DTO를 엔티티로 변환하여 DB 저장</li>
 *     <li>저장 후 이벤트 발행으로 알림 푸시 트리거</li>
 *     <li>로그 기록을 통한 작업 추적</li>
 * </ul>
 *
 * @since 25.07.30
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher publisher; // 이벤트 발행

    /**
     * 알림을 처리하는 엔트리 포인트입니다. DB 저장 후 이벤트를 발행합니다.
     * 이 메서드는 비동기(@Async)로 실행되며, 트랜잭션(@Transactional) 내에서 동작합니다.
     *
     * <p>처리 순서:
     * <ol>
     *     <li>saveNotification 메서드를 호출하여 알림을 DB에 저장합니다.</li>
     *     <li>NotificationSavedEvent를 발행하여 다른 컴포넌트(예: SSE 서비스)에서 알림을 처리할 수 있도록 합니다.</li>
     *     <li>INFO 레벨 로그를 기록합니다.</li>
     * </ol>
     *
     * @param request 알림 요청 DTO. memberId, content, notificationType 등의 필드를 포함해야 합니다.
     */
    @Async
    @Override
    @Transactional
    public void processNotification(NotificationRequestDto request) {
        // 1. DB에 알림 저장
        saveNotification(request);
        // 2. 푸시 알림 전송 이벤트 발행
        publisher.publishEvent(new NotificationSavedEvent(this, request)); // SSE 트리거 이벤트 발행
        log.info("[Matching Notification] 매칭 알림 이벤트 발행 완료 - memberId: {}, content: {}",
                request.getMemberId(), request.getContent());
    }

    /**
     * 알림 요청 DTO를 Notification 엔티티로 변환하여 데이터베이스에 저장합니다.
     * 저장 후 INFO 레벨 로그를 기록합니다.
     *
     * <p>이 메서드는 processNotification 내에서 호출되며, Clock.systemDefaultZone()을
     * 사용하여 현재 시간을 알림 생성 시점으로 설정합니다.
     *
     * @param request 알림 요청 DTO. 저장에 필요한 필드를 포함합니다.
     */
    private void saveNotification(NotificationRequestDto request) {
        notificationRepository.save(
                Notification.create(
                        request.getMemberId(),
                        request.getContent(),
                        Clock.systemDefaultZone(),
                        request.getNotificationType()
                ));
        log.info("[Matching Notification] 매칭 알림 저장 완료 - memberId: {}, content: {}",
                request.getMemberId(), request.getContent());
    }
}