package com.grow.notification_service.analysis.infra.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.grow.notification_service.analysis.application.port.GroupMembershipPort;
import com.grow.notification_service.global.exception.AnalysisException;
import com.grow.notification_service.global.exception.ErrorCode;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyGroupWebClientAdapter implements GroupMembershipPort {

	private final WebClient webClient;

	@Value("${services.study.base-url}")
	private String studyServiceBaseUrl;

	@Value("${study.groups.my-joined.path:/api/v1/groups/{category}/my-joined-groups}")
	private String myJoinedGroupsPath;

	private WebClient studyClient;

	@PostConstruct
	void init() {
		this.studyClient = webClient.mutate()
			.baseUrl(studyServiceBaseUrl)
			.build();
	}

	@Override
	public List<GroupSimpleResponse> getMyJoinedGroups(Long memberId, String category) {
		RsData<List<GroupMembershipPort.GroupSimpleResponse>> rs = studyClient.get()
			.uri(uriBuilder -> uriBuilder
				.path(myJoinedGroupsPath)
				.build(category))
			.header("X-Authorization-Id", String.valueOf(memberId))
			.retrieve()
			.onStatus(HttpStatusCode::isError, resp ->
				resp.bodyToMono(String.class)
					.defaultIfEmpty("")
					.flatMap(body -> {
						int sc = resp.statusCode().value();
						log.warn("[GROUPS][HTTP_ERROR] status={}, body={}", sc, body);
						return reactor.core.publisher.Mono.error(
							new AnalysisException(ErrorCode.MEMBER_RESULT_FETCH_FAILED, new RuntimeException(body))
						);
					})
			)
			.bodyToMono(new ParameterizedTypeReference<RsData<List<GroupSimpleResponse>>>() {})
			.block();

		if (rs == null || rs.data == null) {
			throw new AnalysisException(ErrorCode.MEMBER_RESULT_FETCH_FAILED);
		}
		return rs.data;
	}

	/** WebClient 응답 래퍼 (프로젝트 컨벤션) */
	public record RsData<T>(String code, String message, T data) {}
}