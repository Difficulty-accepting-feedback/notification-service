package com.grow.notification_service.note.infra.client;

import com.grow.notification_service.note.application.port.MemberPort;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.global.exception.NoteException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberWebClientAdapter implements MemberPort {

	private final WebClient webClient;

	@Value("${services.member.base-url}")
	private String memberServiceBaseUrl;

	@Value("${member.name.path}")
	private String memberNamePath;

	@Value("${member.resolve.path}")
	private String memberResolvePath;

	private WebClient memberClient;

	@PostConstruct
	void init() {
		this.memberClient = webClient.mutate()
			.baseUrl(memberServiceBaseUrl)
			.build();
	}

	/**
	 * 멤버 ID로 멤버 닉네임 조회
	 */
	@Override
	public String getMemberName(Long memberId) {
		return memberClient.get()
			.uri(memberNamePath, memberId)
			.retrieve()
			.onStatus(HttpStatusCode::isError,
				resp -> resp.bodyToMono(String.class)
					.defaultIfEmpty("")
					.flatMap(body -> {
						int sc = resp.statusCode().value();
						// 찾는 멤버가 없는 경우
						if (sc == 404) {
							return reactor.core.publisher.Mono.error(
								new NoteException(ErrorCode.MEMBER_NOT_FOUND, new RuntimeException(body)));
						}
						// 그 외 에러
						return reactor.core.publisher.Mono.error(
							new NoteException(ErrorCode.MEMBER_RESOLVE_FAILED, new RuntimeException(body)));
					})
			)
			.bodyToMono(String.class)
			.block();
	}

	/**
	 * 닉네임으로 멤버 ID 조회
	 */
	@Override
	public ResolveResult resolveByNickname(String nickname) {
		String q = nickname == null ? "" : nickname.trim();

		RsData<ResolveMemberResponse> rs = memberClient.get()
			.uri(uri -> uri.path(memberResolvePath)
				.queryParam("nickname", q)
				.build())
			.retrieve()
			.onStatus(HttpStatusCode::isError,
				resp -> resp.bodyToMono(String.class)
					.defaultIfEmpty("")
					.flatMap(body -> {
						int sc = resp.statusCode().value();
						// 찾는 멤버가 없는 경우
						if (sc == 404) {
							return reactor.core.publisher.Mono.error(
								new NoteException(ErrorCode.MEMBER_NOT_FOUND, new RuntimeException(body)));
						}
						// 그 외 에러
						return reactor.core.publisher.Mono.error(
							new NoteException(ErrorCode.MEMBER_RESOLVE_FAILED, new RuntimeException(body)));
					})
			)
			.bodyToMono(new ParameterizedTypeReference<RsData<ResolveMemberResponse>>() {})
			.block();

		// 응답이 비어있는 경우
		if (rs == null || rs.data == null) {
			throw new NoteException(ErrorCode.MEMBER_RESOLVE_EMPTY);
		}
		return new ResolveResult(rs.data.memberId(), rs.data.nickname());
	}

	/* 멤버 서비스의 공통 RsData 래핑 DTO */
	public record RsData<T>(String code, String message, T data) {}
	public record ResolveMemberResponse(Long memberId, String nickname) {}
}