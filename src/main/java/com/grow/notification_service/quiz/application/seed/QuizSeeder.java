package com.grow.notification_service.quiz.application.seed;

import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grow.notification_service.quiz.infra.persistence.entity.QuizJpaEntity;
import com.grow.notification_service.quiz.infra.persistence.enums.QuizLevel;
import com.grow.notification_service.quiz.infra.persistence.repository.QuizJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizSeeder implements CommandLineRunner {

	private final QuizJpaRepository quizRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	// 프로퍼티로 on/off 제어
	@Value("${grow.seed.enabled:true}")
	private boolean seedEnabled;

	// 파일 경로
	@Value("${grow.seed.files:seed/quiz_db_ko_cat3.json}")
	private String seedFilesProp;

	/**
	 * 애플리케이션 시작 시 퀴즈 데이터베이스 시딩
	 */
	@Override
	public void run(String... args) throws Exception {
		if (!seedEnabled) {
			log.info("[QUIZ][SEED][스킵] 시딩 비활성화됨 (grow.seed.enabled=false)");
			return;
		}
		String[] files = seedFilesProp.split(",");
		for (String path : files) {
			seedFromFile(path.trim());
		}
	}

	/**
	 * 단일 파일에서 퀴즈 데이터를 읽어 데이터베이스에 삽입
	 * @param classpath 클래스패스 내 JSON 파일 경로
	 */
	private void seedFromFile(String classpath) {
		try (InputStream is = new ClassPathResource(classpath).getInputStream()) {
			List<QuizSeedDto> items = objectMapper.readValue(is, new TypeReference<List<QuizSeedDto>>() {});
			int total = items.size();
			int inserted = 0, skipped = 0, failed = 0;

			log.info("[QUIZ][SEED][시도] 파일='{}' 총 {}건 로드됨", classpath, total);

			for (QuizSeedDto dto : items) {
				try {
					// 최소 검증, 정답이 보기 중 하나인지
					if (dto.choices() == null || dto.choices().isEmpty() || !dto.choices().contains(dto.answer())) {
						log.warn("[QUIZ][SEED][거절] 잘못된 문제: question='{}' (정답이 보기에 없음)", dto.question());
						failed++;
						continue;
					}

					// 중복 방지
					if (quizRepository.existsByCategoryIdAndQuestion(dto.categoryId(), dto.question())) {
						log.debug("[QUIZ][SEED][스킵] 중복된 문제: question='{}'", dto.question());
						skipped++;
						continue;
					}

					// 엔티티 생성 및 저장
					QuizJpaEntity entity = QuizJpaEntity.builder()
						.question(dto.question())
						.choices(dto.choices())
						.answer(dto.answer())
						.explain(dto.explain())
						.level(QuizLevel.valueOf(dto.level().toUpperCase())) // "EASY|NORMAL|HARD"
						.categoryId(dto.categoryId())
						.build();

					quizRepository.save(entity);
					inserted++;
				} catch (Exception e) {
					failed++;
					log.error("[QUIZ][SEED][실패] 문제 삽입 실패: question='{}'", dto.question(), e);
				}
			}
			log.info("[QUIZ][SEED][완료] 파일='{}' 총 {}건, 성공={}, 중복스킵={}, 실패={}",
				classpath, total, inserted, skipped, failed);
		} catch (Exception e) {
			log.error("[QUIZ][SEED][실패] 파일 읽기 불가: {}", classpath, e);
		}
	}
}