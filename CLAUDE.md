# CLAUDE.md

> 야르렁(야구르지렁) — KBO 승부 예보 · 순위 · 가을야구 진출/우승 확률 · 팀별 팬 게시판.
> 서비스 이름·태그라인·로고 워드마크는 `web/lib/site` 한 곳에서 관리한다.
> 이직 포트폴리오라 **기능 개수보다 완성도**(테스트 · 설계 이유 · 배포 링크)가 우선이다.

## 레포 구성

| 경로 | 무엇 | 스택 |
| ---- | ---- | ---- |
| `src/` | 백엔드 API | Spring Boot 4 · Kotlin + Java · JPA · Flyway · PostgreSQL |
| `web/` | 프론트 | Next.js 16 (App Router) · React 19 · TanStack Query · Tailwind · Recharts |
| `.github/workflows/ci.yml` | CI/CD | PR = 검증만 · main push = 검증 → 이미지(GHCR) → Render → Vercel |
| `.github/workflows/ingest.yml` | 수집 | 매일 06:00 KST 운영 API 에 수집 요청 |
| `DEPLOY.md` | 배포 | Render(API) · Neon(DB) · Vercel(웹) · GHCR, 전부 무료 티어. 설정·비밀값·롤백 |

프론트 작업 규칙은 **`web/CLAUDE.md`** 에 있다(그 폴더에서 일하면 함께 읽힌다).

## 실행

```bash
docker compose up -d db                 # PostgreSQL :5432
./gradlew bootRun                       # API :8080 — 기본은 seed 데이터
./gradlew bootRun --args='--spring.profiles.active=live --app.admin.token=dev'   # 다음 스포츠 실데이터
curl -X POST -H "X-Admin-Token: dev" "localhost:8080/api/admin/ingest?month=2026-08"  # 한 달치 수집
./gradlew test

cd web && npm run dev                   # :3000
cd web && npm run verify                # type-check · test · lint
```

포트가 겹치면 DB 는 `DB_PORT=5544`, 프론트는 `next dev -p <포트>` 로 바꾸고, 백엔드에
`--app.cors.allowed-origins=http://localhost:<포트>` 를 준다(안 주면 브라우저가 CORS 로 막는다).

## 백엔드 규칙

- **계산 코어는 순수 함수**로 두고 DB 없이 테스트한다 — `Log5`(Java) · `Standings` · `SeasonSimulator` ·
  `GameFilter` · `ScheduleIngestService.reconcile`
- **Java 는 계산 코어(`Log5.java`)만, 나머지는 Kotlin.** «레거시 Java → Kotlin 전환» 을 작게 보여주는 의도다
- **순위는 저장하지 않는다.** `game`(FINAL)에서 매번 집계한다 — 진실은 한 곳(`V2__drop_team_record.sql`)
- 스키마는 **Flyway** 가 소유하고 JPA 는 `ddl-auto: validate` 로 검증만 한다. 스키마 변경 = 새 마이그레이션
- 수집 출처는 `KboScheduleSource` 로 추상화 — `SeedKboScheduleSource`(기본) / `DaumKboScheduleSource`(`live`
  프로필, 문서 없는 비공식 JSON). 매일 06:00 KST 스케줄러가 이번 달을 upsert 한다
- 검색 조건이 말이 안 되면 **조용히 무시하지 않고 400 + `errors` 목록**으로 거부한다(`GameFilter.validate`)
- **선발 투수는 «팀 평균 대비 보정» 으로만 반영한다**(`StarterRatings`). 등판 수가 적을수록 팀 평균으로
  수축시키고, 등판 0(첫 선발)이면 보정 0 = 팀 승률만 쓴 예측과 같다. 상대전적은 표본이 1~3등판이라
  **표시만** 하고 계산에 넣지 않는다
- upsert 키는 **출처 경기 id**(`external_id`)다. (날짜+홈+원정)은 더블헤더에서 두 경기가 한 건으로 덮어써진다
- 수집 API 는 `X-Admin-Token` 이 `app.admin.token` 과 같아야 한다. **토큰 설정이 비면 항상 403** — 설정을 빠뜨려도 열리지 않게
- **리그가 다르면 절대 섞지 않는다.** `team`·`game` 에 `league` 컬럼이 있고(`KBO` / `AG` = 아시안게임),
  순위·시뮬레이션·예측·일정은 전부 `League.KBO` 로 좁혀서 조회한다. 국가대표 경기는 승률·선발 기록이
  쌓이지 않으므로 **예측 대상이 아니다** — 일정·결과만 보여준다.
  `findAll()` 같은 리그 없는 조회는 `LeagueQueryConventionTest` 가 소스에서 찾아 막는다
- ⚠️ **`live` 프로필 없이 수집을 부르면 샘플 출처가 실제 결과를 가짜 점수로 덮어쓴다**(upsert 라 에러 없음).
  실DB 에 붙은 앱은 반드시 `live` 로 띄운다. 운영은 `/actuator/info` 의 `ingestSource` 를 배포 파이프라인이 확인한다

## 로그인 · 게시판 규칙

- **소셜 로그인만 쓴다(지금은 카카오).** 비밀번호를 보관하지 않으므로 해싱·재설정·유출 대응이
  통째로 없다. 받는 정보도 **닉네임·프로필 이미지뿐** — 이메일·전화번호는 요청하지 않는다
- **토큰은 서버 밖으로 안 나간다.** 인가 코드 교환·프로필 조회를 서버가 하고 브라우저에는
  **HttpOnly 쿠키**만 준다. 프론트 코드에 토큰이 없으니 XSS 로도 세션을 못 훔친다
- 세션은 **서버가 들고 있고 DB 에는 해시만** 저장한다(JWT 아님) — 로그아웃·차단이 즉시 먹고,
  DB 가 새도 그 값으로 로그인할 수 없다
- **프론트와 API 는 같은 사이트여야 한다.** `web/next.config.ts` 가 `/api/*` 를 API 서버로
  프록시한다. 안 그러면 세션 쿠키가 제3자 쿠키가 되어 사파리·아이폰에서 로그인이 끊긴다
- 게시판은 **읽기 누구나 · 쓰기·수정·삭제는 글쓴이 본인만**(`PostRules.canModify`). 화면의 `mine`
  은 버튼 노출용일 뿐 권한 판정이 아니다 — 판정은 서버가 쿠키로 한다
- 글은 **평문만** 저장하고 평문으로 그린다(HTML·마크다운 없음) — 태그를 허용하면 XSS 를 막을
  책임이 생기는데 응원 글에 그만한 값이 없다
- 지운 글은 `deleted_at` 으로 남긴다(신고·분쟁 확인, 실수 복구). 조회는 전부 이 조건을 건다

## 프론트 ↔ 백엔드 계약

한 레포에 있으니 어긋나면 테스트가 잡게 해 두었다.

| 계약 | 백엔드 | 프론트 | 검사 |
| ---- | ------ | ------ | ---- |
| 팀 코드·이름 | `V1__init.sql` 시드 | `web/lib/teams/model/teams.ts` | `web/tests/team-codes.test.ts` (TEAM 003) |
| 경기 검색 조건 | `GameFilter.kt` (validate) | `web/lib/games/model/` (normalize) | 양쪽 각자 테스트 — 규칙을 바꾸면 **둘 다** 고친다 |
| 검증 실패 응답 | `ProblemDetail` + `errors: string[]` | `ApiError.errors` | `web/lib/api/__tests__/client.test.ts` (API 002) |
| 글 입력 규칙 | `PostRules`(길이·공백) | `web/lib/board/model/postSchema.ts`(zod) | 양쪽 각자 테스트 — **한쪽만 고치면 사용자가 헷갈린다** |

### 세 번째 당사자 — 앱

RN 앱이 [yareureong-app](https://github.com/HyunSeungBeom/yareureong-app) 레포에서 **같은 API**
를 본다. 레포가 달라 테스트로 묶을 수 없으니 앱이 `model/`·`api/` 파일을 **복제**하고, 앱 쪽
COPY 검사가 (웹 레포가 옆에 체크아웃돼 있으면) 바이트 단위로 대조한다.

**위 표의 계약을 고치면 앱도 고쳐야 한다.** 복제 목록은 앱 레포의 백엔드 계약 문서에 있다.
앱 때문에 드러난 기존 구멍 하나: `WebConfig.kt` 의 CORS `allowedMethods` 에 `PUT`·`DELETE` 가
빠져 있다(웹은 프록시로 우회 중이라 안 보인다). 앱에서 게시판 수정·삭제를 붙일 때 같이 고친다.

## 배포

main 에 push → 테스트 통과 → 자동 배포. 배포된 커밋은 `/actuator/info` 의 `app.commit` 으로 확인한다.
배포 파이프라인을 바꾸면 커밋 전에 `docker run --rm -v "$PWD":/repo -w /repo rhysd/actionlint:latest` 로 검사한다.

## 문서

- 프론트 구조와 그 이유: [web/docs/frontend-conventions.md](web/docs/frontend-conventions.md)
- 테스트 붙이는 법 · 밟은 함정: [web/docs/testing.md](web/docs/testing.md)
- 배포: [DEPLOY.md](DEPLOY.md)
