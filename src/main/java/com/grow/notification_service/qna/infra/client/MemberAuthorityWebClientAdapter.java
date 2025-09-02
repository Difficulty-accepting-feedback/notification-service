package com.grow.notification_service.qna.infra.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.global.exception.QnaException;
import com.grow.notification_service.qna.application.port.AuthorityCheckerPort;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberAuthorityWebClientAdapter implements AuthorityCheckerPort {

	private final WebClient webClient; // 공용 설정 주입

	@Value("${services.member.base-url}")
	private String baseUrl;

	@PostConstruct
	void init() {
		this.client = webClient.mutate().baseUrl(baseUrl).build();
	}

	private WebClient client;

	@Override
	public boolean isAdmin(Long memberId) {
		RsData<Boolean> rs = client.get()
			.uri("/api/v1/admin/members/{memberId}/is-admin", memberId)
			.retrieve()
			.bodyToMono(new ParameterizedTypeReference<RsData<Boolean>>() {})
			.block();
		if (rs == null || rs.getData() == null) {
			throw new QnaException(ErrorCode.MEMBER_CHECK_FAILED);
		}
		return rs.getData();
	}

	@Getter
	static class RsData<T> { String code; String message; T data; }
}