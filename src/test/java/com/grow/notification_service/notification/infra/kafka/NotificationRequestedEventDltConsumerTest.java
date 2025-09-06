package com.grow.notification_service.notification.infra.kafka;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EmbeddedKafka(
	partitions = 1,
	topics = { "member.notification.requested", "member.notification.requested.dlt" }
)
@DirtiesContext
@TestPropertySource(properties = {
	"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
	// 재시도 즉시 종료 → 바로 DLT로
	"grow.kafka.retry.attempts=1",
	"grow.kafka.retry.delay-ms=0",
	"grow.kafka.retry.max-delay-ms=0"
})
class NotificationRequestedEventDltConsumerTest {

	@Autowired EmbeddedKafkaBroker broker;
	@Autowired KafkaTemplate<String, String> kafkaTemplate;

	private Consumer<String, String> dltConsumer() {
		Map<String, Object> props = KafkaTestUtils.consumerProps(
			"dlt-smoke-" + UUID.randomUUID(), "true", broker);
		Consumer<String, String> c = new org.apache.kafka.clients.consumer.KafkaConsumer<>(
			props, new StringDeserializer(), new StringDeserializer());
		c.subscribe(java.util.List.of("member.notification.requested.dlt"));
		return c;
	}

	@Test
	@Timeout(20)
	void badJson_goes_to_dlt() {
		// given
		String key = "k1";
		String badPayload = "{ not-a-json ";

		// when
		kafkaTemplate.send("member.notification.requested", key, badPayload);

		// then
		try (Consumer<String, String> consumer = dltConsumer()) {
			ConsumerRecord<String, String> rec =
				KafkaTestUtils.getSingleRecord(consumer,
					"member.notification.requested.dlt",
					Duration.ofSeconds(30));

			assertEquals(key, rec.key());
			assertEquals(badPayload, rec.value());
		}
	}
}