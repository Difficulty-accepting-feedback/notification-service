# MSA: notification service
### 자기 계발 및 취미 매칭 웹 애플리케이션, GROW 🌳

**GROW Notification Service**는 플랫폼 전체의 **실시간 알림·쪽지·공지·QnA 이벤트 허브** 역할을 수행하는 핵심 마이크로서비스입니다.  
**AI 분석 및 퀴즈 출제/채점** 흐름과 멤버 도메인을 긴밀히 연동하여, 학습 피드백·포인트/업적 반영까지 **끝단 알림**을 책임집니다.  
SSE 기반 실시간 스트림과 Kafka 이벤트 표준을 통해 멤버·퀴즈·결제 등과 **낮은 결합도**로 통신합니다.

---

|                                           장무영                                           |                                           최지선                                           |
|:---------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------:|
| <img src="https://avatars.githubusercontent.com/u/136911104?v=4" alt="장무영" width="200"> | <img src="https://avatars.githubusercontent.com/u/192316487?v=4" alt="최지선" width="200"> | 
|                           [GitHub](https://github.com/wkdan)                            |                        [GitHub](https://github.com/wesawth3sun)                         |

---

## 🏗️ 아키텍처 개요

서비스는 **DDD(Domain-Driven Design)** 원칙을 중심으로 **Hexagonal Architecture (Ports & Adapters)** 구조로 설계되어,  
도메인 로직과 인프라 의존성을 명확히 분리하고 **높은 응집도와 낮은 결합도**를 유지합니다.  
핵심 비즈니스 규칙은 `domain` 계층에서 관리되며,  
외부 연동(예: Kafka, Redis, JPA, 외부 API)은 `infra` 어댑터를 통해 주입됩니다.  
`application` 계층에서는 트랜잭션 단위의 유스케이스(예: 알림 생성/전송, 퀴즈 채점 결과 연동, 공지/QnA/쪽지 전파)를 조합하고,  
`presentation` 계층에서는 REST/SSE API를 통해 게이트웨이와 통신합니다.

---

## 🧩 운영 구조

모든 마이크로서비스(member, payment, quiz, analysis 등)는 Kubernetes 환경에서 컨테이너 단위로 배포되며,
게이트웨이와 데이터 계층, 외부 연동 API까지 완전한 클라우드 네이티브 구조로 설계되었습니다.

<img width="1541" height="1241" alt="image" src="https://github.com/user-attachments/assets/8a40b1c6-0bdb-4414-86b4-eee9f298d6ca" />

서비스 간 연결은 **Gateway, Kafka, Redis, Kubernetes**를 중심으로 구성되어 있습니다.

| 구성 요소 | 역할 |
|------------|------|
| 🧭 **Gateway (Spring Cloud Gateway)** | JWT 기반 인증 검증 및 요청 라우팅. SSE 실시간 알림 스트림 엔드포인트 제공 |
| 🧵 **Kafka (Event Bus)** | AI 분석/퀴즈 출제·채점/QnA/공지/쪽지 등 이벤트 발행·구독 및 표준화 |
| 💾 **Redis (Cache & Lock)** | 배지 카운트·세션 캐시, 멱등 처리(Idempotency Key) 및 분산 락으로 중복 전송 방지 |
| 🗃️ **MySQL (Primary DB)** | 알림/쪽지/공지/QnA, 전송 이력 및 실패 보상 로그의 영속 저장소 |
| ☸️ **Kubernetes** | 서비스 배포·스케일링·롤링 업데이트 자동화로 고가용성(HA) 확보 |
| 📊 **Prometheus + Grafana** | SSE 연결 수, 알림 전송률, AI/퀴즈 이벤트 지연, 오류율 등 실시간 모니터링 |

---

## 🧩 주요 기능 요약

| 구분 | 기능 설명 |
|------|------------|
| 🔔 **실시간 알림(SSE)** | 사용자별 SSE 스트림 제공, 자동 재연결·하트비트, 타입·우선순위 기반 전파 |
| 🧠 **AI 분석 연동** | Analysis/LLM 결과 수신 → 학습 리마인더·개인화 피드 알림 발행 |
| 🧩 **퀴즈 출제/채점 연동** | LLM 기반 퀴즈 생성/제출/채점 이벤트를 구독하여 결과 알림 및 멤버 포인트/업적 연계 |
| 📮 **쪽지(Note)** | 1:1 쪽지 송수신·읽음 처리, 신규 수신 시 즉시 푸시 |
| 📢 **공지(Notice)** | 운영 공지 작성·수정·삭제, 대상자 범위 전파 및 읽음 집계 |
| 💬 **QnA** | 질문/답변 등록·갱신 시 구독자·작성자에게 알림 전송 |
| 🧱 **데이터 무결성** | Redis 멱등키·분산락·중복 억제로 동일 이벤트 다중 전송 방지 |
| ♻️ **보상 트랜잭션** | 외부 채널 실패 시 재시도·보상 로직 수행 및 관리자 경보(Slack 등) |

---

## 🛠️ 기술 스택

### FrontEnd
<div> 
  <img src="https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white"/>
  <img src="https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=next.js&logoColor=white"/>
</div>

### BackEnd
<div> 
  <img src="https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white"/>
  <img src="https://img.shields.io/badge/SpringBoot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black"/>
</div>

### Database
<div> 
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white"/>
  <img src="https://img.shields.io/badge/redis-%23DD0031.svg?style=for-the-badge&logo=redis&logoColor=white"/>
</div>

### IDLE&Tool
<div> 
  <img src="https://img.shields.io/badge/IntelliJ%20IDEA-000000?style=for-the-badge&logo=intellijidea&logoColor=white"/>
  <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white"/>
</div>

### OPEN API
<div>
  <img src="https://img.shields.io/badge/Gemini-4285F4?style=for-the-badge&logo=google&logoColor=white"/>
  <img src="https://img.shields.io/badge/Slack-4A154B?style=for-the-badge&logo=slack&logoColor=white"/>
</div>

### Event Bus / Messaging
<div>
  <img src="https://img.shields.io/badge/Apache%20Kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white"/>
</div>

### Infra
<div>
  <img src="https://img.shields.io/badge/Linux-FCC624?style=for-the-badge&logo=linux&logoColor=black"/>
  <img src="https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonwebservices&logoColor=white"/>
  <img src="https://img.shields.io/badge/Vercel-000000?style=for-the-badge&logo=vercel&logoColor=white"/>
</div>

### Container & Orchestration
<div>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"/>
  <img src="https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white"/>
</div>

### Monitoring
<div>
  <img src="https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white"/>
  <img src="https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white"/>
</div>

### CI/CD
<div>
  <img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white"/>
</div>

---


## Conventional Commits 규칙

| 이모지 | 타입      | 설명                                               | 예시 커밋 메시지                                   |
|--------|-----------|--------------------------------------------------|--------------------------------------------------|
| ✨     | feat      | 새로운 기능 추가                                    | feat: 로그인 기능 추가                             |
| 🐛     | fix       | 버그 수정                                          | fix: 회원가입 시 이메일 중복 체크 오류 수정         |
| 📝     | docs      | 문서 수정                                          | docs: README 오타 수정                            |
| 💄     | style     | 코드 포맷, 세미콜론 누락 등 스타일 변경 (기능 변경 없음) | style: 코드 정렬 및 세미콜론 추가                  |
| ♻️     | refactor  | 코드 리팩토링 (기능 변경 없음)                     | refactor: 중복 코드 함수로 분리                    |
| ⚡     | perf      | 성능 개선                                          | perf: 이미지 로딩 속도 개선                       |
| ✅     | test      | 테스트 코드 추가/수정                              | test: 유저 API 테스트 코드 추가                    |
| 🛠️     | build     | 빌드 시스템 관련 변경                              | build: 배포 스크립트 수정                         |
| 🔧     | ci        | CI 설정 변경                                      | ci: GitHub Actions 워크플로우 수정                |

```text
타입(범위): 간결한 설명 (50자 이내, 한글 작성)

(필요시) 변경 이유/상세 내용
```
- 하나의 커밋에는 하나의 목적만 담기

    → 여러 변경 사항을 한 커밋에 몰아넣지 않기
- 제목 끝에 마침표(.)를 붙이지 않기
- 본문(Body)은 선택 사항이지만, 변경 이유나 상세 설명이 필요할 때 작성

    → 72자 단위로 줄바꿈, 제목과 본문 사이에 한 줄 띄우기
- 작업 중간 저장은 WIP(Work In Progress)로 표시할 것

  → 예) WIP: 회원가입 로직 구현 중

---
## 🕒 협업 시간 안내

팀원들이 주로 활동하는 시간대입니다.  
이 시간에 맞춰 커뮤니케이션과 코드 리뷰, 회의 등을 진행합니다.

| 요일     | 활동 시간                  |
|----------|----------------------------|
| 📅 평일  | 14:00 ~ 18:00, 20:00 ~ 23:00 |
| 📅 주말  | 14:00 ~ 18:00               |

---

## 🧐 코드 리뷰 규칙

- PR 제목과 설명을 명확하게 작성 (변경 내용, 목적, 참고 이슈 등 포함)
- Conventional Commits 규칙을 준수하여 커밋 메시지 작성
- 하나의 PR에는 하나의 기능/이슈만 포함
- 코드 스타일, 네이밍, 로직, 성능, 보안, 예외 처리 등 꼼꼼히 확인
- 리뷰 코멘트에는 반드시 답변, 필요시 추가 커밋으로 반영
- 모든 리뷰 코멘트 resolve 후 머지
- 스쿼시 머지 방식 권장, 충돌 발생 시 머지 전 해결
- 리뷰는 24시간 이내 진행, 모르는 부분은 적극적으로 질문
- 리뷰 과정에서 배운 점은 팀 문서에 공유 (트러블 슈팅 등)
