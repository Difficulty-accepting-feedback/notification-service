package com.grow.notification_service.chatbot.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.grow.notification_service.chatbot.infra.persistence.entity.ChatbotJpaEntity;

public interface ChatbotJpaRepository
	extends JpaRepository<ChatbotJpaEntity, Long> {
}