package com.grow.notification_service.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;

public class KafkaTopicConfig {
	@Bean
	public NewTopic qnaNotificationRequested() {
		return TopicBuilder.name("qna.notification.requested")
			.partitions(3)
			.replicas(3)
			.build();
	}

	@Bean
	public NewTopic noteNotificationRequested() {
		return TopicBuilder.name("note.notification.requested")
			.partitions(3)
			.replicas(3)
			.build();
	}
}