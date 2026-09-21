# 🔴 야르렁 (야구르지렁)

[![CI/CD](https://github.com/HyunSeungBeom/yareureong/actions/workflows/ci.yml/badge.svg)](https://github.com/HyunSeungBeom/yareureong/actions/workflows/ci.yml)

**오늘 경기 승부 예보 · KBO 팬 게시판.**

이름은 «야구르지렁» 의 줄임말이다 — 야구팬은 화가 많아 으르렁거린다는 데서 왔다.
로고는 «야구르지렁» 을 쓰되 **야·르·렁만 붉게** 칠해, 빨간 글자만 읽으면 «야르렁» 이 된다.

선발 투수 기록까지 반영한 승리 확률, 실시간 순위, 가을야구 진출·우승 확률, 팀별 팬 게시판,
아시안게임 야구 일정·결과.
내 팀(응원팀)을 설정하면 해당 팀 관점으로 하이라이트한다.

> 🔗 **웹: https://yareureong.vercel.app** · **API: https://kbo-predictor-api.onrender.com/actuator/health**
>
> 무료 인스턴스라 15분간 요청이 없으면 잠든다 — **첫 접속은 서버가 깨어나는 1분가량이 걸린다.**

---

## 왜 이 스택인가 (설계 의도)

- **백엔드: Spring Boot 4 + Java · Kotlin 공존** — 이 프로젝트는 "레거시 Java → 모던 Kotlin 전환"을
  축소 재현한다. 계산 코어([`Log5.java`](src/main/java/com/seungbeom/kbo/prediction/Log5.java))는
  **Java**로 두어 기본기를 드러내고, 이를 호출하는 서비스·엔티티·API는 **Kotlin**으로 작성해
  **Java ↔ Kotlin 상호운용**을 보여준다.
- **영속성: Spring Data JPA + PostgreSQL**, 스키마는 **Flyway**로 버전 관리.
- **프론트: Next.js + TypeScript + TanStack Query + Recharts** — 같은 레포의 `web/`. 도메인 폴더 구조와
  그 이유는 [web/docs/frontend-conventions.md](web/docs/frontend-conventions.md).

## 확률 모델

| 예측 | 방법 | 상태 |
|------|------|------|
| 오늘 경기 승리확률 | **log5**(승률) + 홈 어드밴티지 + **선발 투수 보정**(등판 수에 따라 팀 평균으로 수축) | ✅ 구현 |
| 가을야구 진출확률 / 우승확률 | **몬테카를로 시뮬레이션** (잔여 일정 N회 가상 플레이 + 계단식 포스트시즌) | ✅ 구현 |
| 승률 대신 Elo·선발투수 반영 | 레이팅 시스템 | 🔮 v2 |

## 실행 방법

```bash
# 1) DB 띄우기
docker compose up -d db

# 2) 앱 실행 (로컬)
./gradlew bootRun

# 또는 앱까지 컨테이너로 한 방에
docker compose up --build
```

- 일정:  `GET http://localhost:8080/api/schedule`  (특정일: `?date=2026-07-20`)
- 검색:  `GET http://localhost:8080/api/games?team=OB&opponent=LG&venue=HOME&result=WIN&from=2026-08-01&to=2026-08-31`
  (모든 파라미터 선택·AND 결합. `opponent`/`venue`/`result`는 `team` 관점이라 `team`과 함께만 쓸 수 있고,
  말이 안 되는 조합은 400 + `errors` 목록으로 거부 — 이후 자연어 검색에서 LLM이 만든 조건을 같은 규칙으로 검증한다)
- 오늘 경기: `GET http://localhost:8080/api/predict/today` (날짜 지정: `?date=2026-09-18`) — 그날 경기 + 선발 반영 예측
- 예측:  `GET http://localhost:8080/api/predict?home=OB&away=LG` (선발 미지정 매치업)
- 순위:  `GET http://localhost:8080/api/standings`  (game 결과에서 실시간 집계)
- 확률:  `GET http://localhost:8080/api/simulation?iterations=10000`  (몬테카를로 진출/우승 확률)
- 헬스:  `GET http://localhost:8080/actuator/health`
- 수집(수동): `POST http://localhost:8080/api/admin/ingest?month=2026-07` + 헤더 `X-Admin-Token` (`app.admin.token` 과 같은 값)

### 데이터 수집 (Phase 0)

출처를 [`KboScheduleSource`](src/main/kotlin/com/seungbeom/kbo/ingest/KboScheduleSource.kt) 인터페이스로
추상화해, "가져오기(취약)"와 "upsert(안정)"를 분리했다.

- **기본(개발)**: [`SeedKboScheduleSource`](src/main/kotlin/com/seungbeom/kbo/ingest/SeedKboScheduleSource.kt)
  가 샘플 경기를 넣어 파이프라인이 바로 동작한다. `POST /api/admin/ingest?month=2026-07` 로 넣고
  `GET /api/schedule` 로 확인.
- **라이브**: [`DaumKboScheduleSource`](src/main/kotlin/com/seungbeom/kbo/ingest/DaumKboScheduleSource.kt)
  (프로필 `live`). 다음 스포츠 JSON은 공개 문서가 없어, 브라우저 DevTools>Network 에서 실제
  `schedule.json` 요청을 확인해 엔드포인트/매핑만 채우면 된다(구조는 완성). 그전까진 seed 로 개발.
- 매일 06:00(KST) [`ScheduleIngestScheduler`](src/main/kotlin/com/seungbeom/kbo/ingest/ScheduleIngestScheduler.kt)
  가 이번 달을 자동 갱신. upsert 라 여러 번 돌려도 중복이 생기지 않는다.

## 테스트

```bash
./gradlew test
```
계산 코어는 Java·Kotlin 양쪽 테스트로 검증한다
([`Log5Test.java`](src/test/java/com/seungbeom/kbo/prediction/Log5Test.java),
[`PredictionsTest.kt`](src/test/kotlin/com/seungbeom/kbo/prediction/PredictionsTest.kt)).
DB가 필요한 통합 테스트(Testcontainers)는 Phase 2에서 추가 예정.

## 프론트엔드 (`web/`)

Next.js(App Router) + TypeScript + Tailwind + TanStack Query + Recharts. 백엔드 API를 소비하는 대시보드.

```bash
cd web
npm install
npm run dev      # http://localhost:3000  (백엔드가 :8080 에서 떠 있어야 함)
npm run verify   # type-check · test · lint (CI 와 같다)
```
- 대시보드: 순위표 · 우승확률 차트(몬테카를로) · 오늘 경기 예측
- 일정 페이지: 팀 · 상대팀 · 홈/원정 · 결과 · 기간 조건 검색. 조건은 URL 에 담겨 새로고침·공유해도 유지
- 팬 게시판(팀별 10개): 읽기는 누구나, 글쓰기·수정·삭제는 카카오 로그인한 본인만
- 내 팀 설정: localStorage, 순위·차트에서 하이라이트
- API 주소는 `web/.env.local`의 `NEXT_PUBLIC_API_BASE`(기본 `http://localhost:8080`). 백엔드는 `app.cors.allowed-origins`로 오리진 허용.
- 구조 규약: [web/docs/frontend-conventions.md](web/docs/frontend-conventions.md) · 테스트: [web/docs/testing.md](web/docs/testing.md)

## 데이터 출처

KBO는 공식 오픈 API가 없어, 일정·결과는 공개 출처(KBO 공식 / 다음 스포츠 등)를
**하루 1회 스크래핑 → DB upsert**하는 방식으로 수집한다(예정, Phase 0). 원본 사이트를
직접 호출하지 않고 항상 DB만 조회하며, 스크래퍼는 요청 간격을 두고 robots.txt를 존중한다.
비상업·학습 목적.

## 로드맵

- [x] 프로젝트 뼈대 (Spring Boot 4 · Java/Kotlin · JPA · Flyway · Docker · CI)
- [x] log5 단일 경기 승리확률 + 테스트
- [x] **Phase 0** — 수집 파이프라인(출처 추상화 + upsert + 스케줄러 + 수동 트리거). seed 로 동작 / 라이브 어댑터는 엔드포인트 확인만 남음
- [x] **Phase 1** — 순위(승/패/무) 계산 API. `game`(FINAL)에서 실시간 집계(승률·게임차·순위), 단일 진실원천
- [x] **Phase 2** — 몬테카를로 진출/우승 확률 (계단식 포스트시즌 시뮬, Random 주입 → 테스트 가능). 캐싱은 후속
- [x] **Phase 3** — Next.js 프론트(`web/`): 순위표·우승확률 차트·경기예측·일정·내 팀 설정
- [x] **Phase 4** — 배포: GitHub Actions CI/CD → GHCR · Render(API) · Neon(DB, 싱가포르) · Vercel(웹). main push 시 검증 통과분만 자동 배포하고, 매일 06:00 KST 수집 ([DEPLOY.md](DEPLOY.md))

## 기술 스택

`Kotlin 2.3` · `Java 17` · `Spring Boot 4.1` · `Spring Data JPA` · `Flyway` ·
`PostgreSQL 16` · `Gradle (Kotlin DSL)` · `Docker` · `GitHub Actions`
