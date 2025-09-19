package com.grow.notification_service.quiz.infra.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.global.exception.QuizException;
import com.grow.notification_service.quiz.application.port.SubscriptionPort;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSubscriptionWebClientAdapter implements SubscriptionPort {

	private final WebClient webClient;

	@Value("${services.payment.base-url}")
	private String paymentServiceBaseUrl;

	@Value("${payment.subscription.ai-review.path:/api/v1/payment/internal/subscriptions/ai-review}")
	private String aiReviewPath;

	private WebClient paymentClient;

	@PostConstruct
	void init() {
		this.paymentClient = webClient.mutate()
			.baseUrl(paymentServiceBaseUrl)
			.build();
	}

	@Override
	public boolean canGenerateAiReview(Long memberId) {
		RsData<AiReviewSubscriptionResponse> rs = paymentClient.get()
			.uri(aiReviewPath)
			.header("X-Authorization-Id", String.valueOf(memberId))
			.retrieve()
			.onStatus(HttpStatusCode::isError, resp ->
				resp.bodyToMono(String.class)
					.defaultIfEmpty("")
					.flatMap(body -> {
						int sc = resp.statusCode().value();
						log.warn("[SUBSCRIPTION][HTTP_ERROR] status={}, body={}", sc, body);
						return reactor.core.publisher.Mono.error(
							new QuizException(ErrorCode.MEMBER_RESULT_FETCH_FAILED, new RuntimeException(body))
						);
					})
			)
			.bodyToMono(new ParameterizedTypeReference<RsData<AiReviewSubscriptionResponse>>() {})
			.block();

		if (rs == null || rs.data == null) {
			throw new QuizException(ErrorCode.MEMBER_RESULT_FETCH_FAILED);
		}
		return rs.data.aiReviewAllowed();
	}

	/** payment_service가 RsData로 내려주는 페이로드 DTO */
	public record AiReviewSubscriptionResponse(Long memberId, boolean aiReviewAllowed) {}

	/** WebClient 응답 래퍼 (프로젝트 컨벤션) */
	public record RsData<T>(String code, String message, T data) {}
}