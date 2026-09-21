# 테스트를 어떻게 붙일까

> 이 문서만 보고 새 코드에 테스트를 붙일 수 있어야 한다. 안 되면 문서가 부족한 것이다.

## 원칙 셋

1. **커버리지 목표치를 세우지 않는다.** 숫자를 채우려고 쓴 테스트는 아무것도 지키지 않는다.
   대신 «버그를 고치면 그 버그가 테스트로 고정됐는가» 를 본다.
2. **테스트를 쓰면 고친 코드를 되돌려 빨개지는지 확인한다.** 안 빨개지면 그 테스트는 아무것도
   지키지 않는다. 이 레포의 테스트는 전부 이렇게 확인했다 — 순수 로직 9가지 · 구조 규칙 16가지 ·
   컴포넌트 12가지 변형.
3. **처음부터 초록인 테스트를 의심한다.** 특히 «목록이 비어 있으면 통과» 하는 검사는 대상을 못
   모아도 초록이다. 그래서 규칙 검사마다 «검사 대상이 실제로 있다» 는 테스트가 붙어 있다.

## 계층

| 계층 | 도구 | 파일 | 맡는 것 |
| ---- | ---- | ---- | ------- |
| 유닛 | Vitest | `*.test.ts` | 순수 함수 · 규칙 · 계약. DOM 이 필요 없는 전부 |
| 컴포넌트 | Vitest + jsdom + Testing Library | `*.test.tsx` | 이벤트 · 상태 전이 · 렌더 결과 · 접근성 이름 |
| e2e | (아직 없음 — Playwright 예정) | `e2e/*.spec.ts` | **jsdom 이 원리적으로 못 하는 것만** |

jsdom 은 레이아웃과 CSS 를 계산하지 않는다. 그래서 e2e 는 «모바일 폭에서 가로로 넘치지 않는다»
같은 것만 맡는다(지금 실제로 넘치는 문제가 있어 첫 e2e 후보다). **로직을 e2e 로 내리지 않는다** —
느리고, 실패 원인이 흐리고, 같은 계약을 두 곳에서 관리하게 된다.

## 어디에 두나

```
lib/{도메인}/__tests__/     도메인 하나당 한 자리. 슬롯마다 두지 않는다
lib/api/__tests__/          HTTP 코어
tests/                      특정 도메인에 속하지 않는 것만
  conventions.test.ts         구조 규칙 (ARCH)
  conventions.detect.test.ts  규칙 판정 함수 자체 (DET)
  conventions-detectors.ts    판정 함수 — 테스트 파일이 아니다
  hygiene.test.ts             테스트 위생 (HYG)
  dependencies.test.ts        의존성 정책 · lockfile (DEP)
  docs.test.ts                문서 ↔ 코드 동기 (DOC)
  team-codes.test.ts          프론트 팀 코드 ↔ 백엔드 DB 시드 계약
  fetch-stub.ts               fetch 대역
  next-navigation-mock.ts     next/navigation 모의
  query-provider.tsx          TanStack Query 래퍼
```

- **소스 옆이 아니라 `__tests__/` 에 둔다.** 소스와 테스트가 섞이면 파일 목록을 못 쓴다
- **도메인 테스트는 배럴(`from ".."`)로 들어간다.** 소비자와 같은 시점이라, 안을 옮겨도 안 깨진다
- **테스트 파일은 다른 테스트 파일을 import 하지 않는다**(HYG 001). 가져온 쪽에서 그 안의 테스트가
  다시 등록돼 두 번 돈다. 공유할 것은 `tests/` 의 비-테스트 모듈로 뺀다
- `node:fs` 로 레포를 읽는 테스트는 `tests/` 에 둔다

## 이름

테스트 제목은 `그룹 번호: 설명` 이다 — `describe("SCH 003: 필터를 만지면 주소를 바꾼다")`.
실패 목록에서 어느 영역인지 바로 보이고, 이 문서의 표와 대조된다(DOC 002 가 검사한다).

| 그룹 | 대상 | 파일 |
| ---- | ---- | ---- |
| `URL` | 주소 ↔ 필터 변환 | `lib/games/__tests__/filter.test.ts` |
| `NRM` | 필터 조합 규칙 | 〃 |
| `OUT` | «결과» 선택지 전환 | 〃 |
| `RES` | 팀 기준 승패 · 합계 | 〃 |
| `GAPI` | 경기 API 주소 조립 | `lib/games/__tests__/gamesApi.test.ts` |
| `GFB` | 필터 바 컴포넌트 | `lib/games/__tests__/GameFilterBar.test.tsx` |
| `SIM` | 진출·우승 확률 차트 데이터 정렬·환산 | `lib/simulation/__tests__/chart.test.ts` |
| `SUM` | 선발 기록 표시 문구(표본 수 · 첫 선발 · 상대전적) | `lib/prediction/__tests__/summary.test.ts` |
| `TDY` | 오늘 경기 보드 | `lib/prediction/__tests__/TodayBoard.test.tsx` |
| `AGS` | 아시안게임 표시 규칙(라운드 · 국기 · 보여줄 하루) | `lib/asianGames/__tests__/summary.test.ts` |
| `AGB` | 아시안게임 보드(대회 아니면 사라짐 · 확률 없음) | `lib/asianGames/__tests__/AsianGamesBoard.test.tsx` |
| `SCH` | 일정 목록 화면 | `lib/games/__tests__/ScheduleList.test.tsx` |
| `BRD` | 게시판 화면(로그인 분기 · 내 글만 수정·삭제) | `lib/board/__tests__/TeamBoard.test.tsx` |
| `FMT` | 글 시간 표기 | `lib/board/__tests__/format.test.ts` |
| `PSC` | 글 입력 규칙(zod) | `lib/board/__tests__/postSchema.test.ts` |
| `TEAM` | 팀 코드 좁히기 · 표시 · 로고 주소 · 백엔드 시드 계약 | `lib/teams/__tests__/teams.test.ts` · `tests/team-codes.test.ts` |
| `TLG` | 구단 로고(실패 시 배지 대체 · 접근성) | `lib/teams/__tests__/TeamLogo.test.tsx` |
| `SITE` | 사이트 메타 · 링크 미리보기 값 | `lib/site/__tests__/site.test.ts` |
| `API` | HTTP 코어 성공 · 실패 해석 | `lib/api/__tests__/client.test.ts` |
| `ARCH` | 구조 규칙 | `tests/conventions.test.ts` |
| `DET` | 규칙 판정 함수 | `tests/conventions.detect.test.ts` |
| `HYG` | 테스트 위생 | `tests/hygiene.test.ts` |
| `DEP` | 의존성 정책 · lockfile | `tests/dependencies.test.ts` |
| `DOC` | 문서 ↔ 코드 동기 | `tests/docs.test.ts` |

## 셀렉터

**`getByRole` / `getByLabelText` 를 쓴다.** 역할과 이름으로 잡으면 접근성 검사를 겸한다.

```ts
screen.getByLabelText("상대팀");
within(screen.getByRole("group", { name: "홈/원정" })).getByRole("button", { name: "홈" });
```

실제로 이 방식이 버그를 잡았다 — 홈/원정 버튼 묶음이 `<label>` 에 싸여 있어서 «전체» 버튼 이름이
"홈/원정 홈 원정" 으로 읽혔고, 그대로는 역할로 잡을 수 없었다. `data-testid` 는 역할·이름으로
정말 구분이 안 될 때만 쓴다(지금은 0개).

## 자주 쓰는 틀

**서버** — 가짜 클라이언트가 아니라 `fetch` 를 갈아끼운다. 주소 조립과 실패 해석이 실제 경로를 탄다.

```ts
import { respondWith } from "@/tests/fetch-stub";
vi.stubGlobal("fetch", respondWith(400, { errors: ["..."] }));
afterEach(() => vi.unstubAllGlobals());
```

**라우터** — `vi.mock` 은 파일 맨 위로 끌어올려지므로 팩토리 안에서 지연 import 한다.

```ts
vi.mock("next/navigation", async () => (await import("@/tests/next-navigation-mock")).nextNavigationModule);
beforeEach(() => { resetNavigation(); setLocation("/schedule", "?team=OB"); });
// 읽기: 주소를 정하고 화면을 본다 / 쓰기: router.replace 에 무엇이 넘어갔는지 본다
```

**Query** — `renderWithQuery(<ScheduleList />)`. 테스트마다 새 클라이언트, 재시도 없음.

**controlled 컴포넌트** — prop 을 주고 `onChange` 에 무엇이 올라왔는지만 본다(`GameFilterBar`).

## 실행

```bash
npm test              # 전부 (몇 초)
npm run test:watch
npm run verify        # type-check · test · lint — PR 전에, CI 도 이것
```

## 새 테스트를 붙일 때

1. **순수 함수로 뺄 수 있나?** 뺄 수 있으면 `model/` 로 빼고 유닛으로. 화면 안의 계산은 보통
   이게 최고의 선택이다(`recordOf` 가 `ScheduleList` 안의 `useMemo` 에서 나왔다)
2. **사용자 동작이 얽히나?** 컴포넌트 테스트 + `userEvent`
3. **레이아웃·CSS 가 계산돼야 하나?** 그때만 e2e
4. **되돌려서 빨개지는지 확인한다**

## 함정 (전부 이 레포에서 실제로 밟았다)

| 함정 | 대응 |
| ---- | ---- |
| **npm 이 lockfile 에서 플랫폼 바이너리를 지운다** (npm/cli#4828). 기존 lockfile 위에서 `npm install <pkg>`·`npm uninstall <pkg>` 를 하면 rolldown · lightningcss 의 OS별 바이너리 항목이 사라진다. 내 머신은 멀쩡하고 CI 에서 "Cannot find native binding" 으로 테스트가 기동조차 안 된다 | `DEP 003` 이 잡는다. 고칠 때 lockfile 을 통째로 재생성하지 않는다(운영 의존성이 같이 올라간다). npm 명령 전 lockfile 을 복사해 두고, 명령 후 사라진 `@rolldown/binding-*` · `lightningcss-*` · `fsevents` 항목만 되살린다 |
| **주석을 `/* globals` 로 시작하면** ESLint 가 전역 변수 선언 지시문으로 읽는다 | 경고 13개가 났다. `//` 주석을 쓰거나 다른 단어로 시작한다 |
| **`Response` 본문은 한 번만 읽힌다.** 같은 객체를 두 번 돌려주면 "Body has already been read" | `respondWith` 는 호출마다 새 `Response` 를 만든다 |
| **`id in OBJECT` 는 프로토타입까지 본다.** `"toString" in TEAMS` 가 true | `Object.hasOwn` 을 쓴다(`isTeamId` 에서 실제로 났다) |
| **타임존** — 날짜 코드는 머신 타임존에 따라 결과가 달라진다 | `vitest.config.mts` 가 `TZ=Asia/Seoul` 로 고정한다. 지우면 `HYG 002` 가 빨개진다 |
| **헤드리스 크롬 스크린샷에서 우승확률 차트가 비어 보인다** | recharts 애니메이션 도중에 찍힌 것이다. 버그가 아니다. 막대 존재는 DOM 의 `recharts-bar-rectangle` 개수로 확인한다 |
| **Turbopack 은 심볼릭 링크 `node_modules` 를 거부한다** | 비교 빌드용 작업 복사본(worktree)에는 `npm ci` 로 실제 설치한다 |
| **변형 확인 때 여는 태그만 바꾸면** JSX 문법 오류로 테스트가 아예 안 돈다 — «실패» 가 아니라 «실행 안 됨» 이다 | 변형은 실행 가능한 코드로 만든다. 결과에 테스트 개수가 찍히는지 본다 |
