package com.grow.notification_service.quiz.infra.client;

import com.grow.notification_service.quiz.application.MemberQuizResultPort;
import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.global.exception.QuizException;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberQuizResultWebClientAdapter implements MemberQuizResultPort {

	private final WebClient webClient;

	@Value("${services.member.base-url}")
	private String memberServiceBaseUrl;

	@Value("${member.quiz.path}")
	private String correctIdsPath;

	private WebClient memberClient;

	// WebClient 초기화
	@PostConstruct
	void init() {
		this.memberClient = webClient.mutate()
			.baseUrl(memberServiceBaseUrl)
			.build();
	}

	/**
	 * 멤버의 맞은 퀴즈 ID 목록 조회
	 */
	@Override
	public List<Long> findCorrectQuizIds(Long memberId) {
		RsData<List<Long>> rs = memberClient.get()
			.uri(correctIdsPath, memberId)
			.retrieve()
			.onStatus(HttpStatusCode::isError,
				resp -> resp.bodyToMono(String.class)
					.defaultIfEmpty("")
					.flatMap(body -> {
						int sc = resp.statusCode().value();
						// 찾는 멤버가 없는 경우
						if (sc == 404) {
							return reactor.core.publisher.Mono.error(
								new QuizException(ErrorCode.MEMBER_NOT_FOUND, new RuntimeException(body)));
						}
						// 그 외 에러
						return reactor.core.publisher.Mono.error(
							new QuizException(ErrorCode.MEMBER_RESULT_FETCH_FAILED, new RuntimeException(body)));
					})
			)
			.bodyToMono(new ParameterizedTypeReference<RsData<List<Long>>>() {})
			.block();

		if (rs == null || rs.data == null) {
			throw new QuizException(ErrorCode.MEMBER_RESULT_FETCH_FAILED);
		}
		return rs.data;
	}

	/* 멤버 서비스의 공통 RsData 래핑 DTO */
	public record RsData<T>(String code, String message, T data) {}
}