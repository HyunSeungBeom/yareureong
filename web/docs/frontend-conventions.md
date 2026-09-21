# 프론트 구조를 어떻게 나눌까

> 새 파일을 만들기 전에 읽는다. `tests/conventions.test.ts` 가 여기 적힌 규칙을 검사한다.
> 규칙을 바꾸고 싶으면 **이 문서의 «왜» 가 아직 참인지** 부터 본다.

## 한눈에

```
web/
  app/                    라우트만. 도메인 배럴에서 UI 를 가져와 조립한다
  components/ds/          도메인과 무관한 공용 부품 (Card)
  lib/
    api/                  HTTP 코어 — fetch 는 여기서만
    games/                ┐
    teams/                │ 도메인. 백엔드 패키지와 1:1
    standings/            │   (game · team · standings · simulation ·
    simulation/           │    prediction · asiangames)
    prediction/           │
    asianGames/           ┘
      index.ts            입구(배럴). 밖에서는 이것만 import
      model/  api/  hooks/  store/  ui/   ← 슬롯 (필요한 것만)
      __tests__/
  tests/                  테스트 지원 모듈 + 레포 전체 규칙 검사
```

## 왜 이렇게 나누나

### 축은 도메인이다 — 기술이 아니라

`types/` · `hooks/` · `utils/` 처럼 **기술로 나누면 같이 바뀌는 것이 흩어진다.**
경기 필터에 조건 하나를 더한다고 하자. 타입(`GameFilter`) · 조합 규칙(`normalize`) · URL 변환
(`parseGameFilter`) · 입력 UI(`GameFilterBar`) 를 같이 고쳐야 한다. 기술 축이면 폴더 넷을 열고
그중 하나를 빠뜨리기 쉽다 — "입력칸은 생겼는데 URL 에 안 실린다" 같은 버그가 에러 없이 남는다.
도메인 축이면 `lib/games/` 하나만 열면 된다.

**도메인 이름은 백엔드 패키지와 같다.** `GET /api/games` ↔ `lib/games` 처럼 API 와 화면 코드의
대응이 이름만으로 보인다. 백엔드에 패키지가 생기면 프론트 도메인도 같은 이름으로 만든다.

### Feature-Sliced Design 에서 두 가지만 가져왔다

1. **slice(도메인) 안을 segment(슬롯)로 나눈다**
2. **slice 는 public API(`index.ts`)로만 들어간다**

FSD 의 6층(`app` · `pages` · `widgets` · `features` · `entities` · `shared`)은 **쓰지 않는다.**
이 앱은 화면이 두 개뿐이라, 층을 얹으면 «어느 층인가» 와 «어느 도메인인가» 라는 판단이 둘이 되고
얻는 것이 없다. FSD 문서를 읽고 온 사람이 층이 없는 것에 놀라지 않도록 적어 둔다.

## 규칙

### 1. `lib/` 바로 아래에는 도메인과 코어만 둔다 — ARCH 001

`lib/utils` · `lib/types` · `lib/hooks` · `lib/constants` · `lib/helpers` · `lib/components` 등은
만들지 않는다. 같은 이름이라도 **도메인 안의 슬롯이면 괜찮다**(`lib/teams/store/`) — 자기 도메인
옆에 있기 때문이다.

코어는 지금 `lib/api` 하나다. 코어는 도메인이 아니라서 슬롯으로 나누지 않고 평평하게 둔다.

### 2. 도메인은 입구(`index.ts`)를 갖고, 밖에서는 그것만 import 한다 — ARCH 002 · 003

```ts
import { ScheduleList } from "@/lib/games";                 // ✅
import { ScheduleList } from "@/lib/games/ui/ScheduleList"; // ❌ 속을 직접 찌름
import { teamName } from "../../teams/model/teams";         // ❌ 상대 경로도 같다
```

입구로만 들어오면 **안을 쪼개고 합쳐도 밖이 안 깨진다.** 예를 들어 `lib/games/model/` 의 파일을
나누거나 합쳐도 `@/lib/games` 에서 가져오는 쪽은 그대로다.

**같은 도메인 안에서는 형제 파일을 직접 본다.** 자기 배럴을 거치면 순환이 된다.

### 3. 슬롯은 고정 어휘다 — ARCH 004

| 슬롯 | 담는 것 | 답하는 질문 |
| ---- | ------- | ----------- |
| `model/` | 타입 · 순수 규칙 · 파생 계산 | 이게 **무엇인가** |
| `schemas/` | zod 스키마 (자연어 검색의 LLM 응답 검증 때 생긴다) | 들어온 값이 **유효한가** |
| `mocks/` | 가짜 데이터 · 가짜 서비스 | (서버 없이) 어디서 오나 |
| `api/` | 서버 호출 함수 · queryKey 팩토리 | 어디서 오나 |
| `store/` | 클라이언트가 들고 있는 상태 | 앱이 **무엇을 기억하나** |
| `hooks/` | 화면이 쓰는 훅 | 화면이 **어떻게 쓰나** |
| `ui/` | 이 도메인 전용 React 컴포넌트 | — |

- **어휘를 열어 두지 않는다.** 열어 두면 어디 넣을지 몰라 새로 만들게 되고, 그렇게 `hook/`·`hooks/`
  · `util/`·`utils/` 가 공존하게 된다. 새 파일은 항상 이 일곱 중 하나에 들어간다.
- **빈 슬롯은 만들지 않는다.** `teams` 에는 `api/` 가 없다 — 서버에서 팀을 받지 않기 때문이고,
  **없는 것 자체가 정보다.**
- **슬롯에는 배럴을 두지 않는다.** 도메인 `index.ts` 가 슬롯 파일을 직접 재수출한다. 두 겹이면
  심볼 하나를 찾는 데 파일 셋을 거친다.
- **도메인 루트에는 `index.ts` 만 둔다.** 나머지는 전부 슬롯에 들어간다.

### 4. 깊이는 도메인 + 슬롯 + 파일까지다 — ARCH 005

`lib/games/model/types.ts` 가 끝이다. 슬롯 안에 폴더를 또 파지 않는다. 파고 싶어지면 도메인을
나눌 때가 됐다는 신호다.

### 5. 서버 호출은 한 경로로만 간다 — ARCH 006

```
ui/ScheduleList  →  hooks/useGames  →  api/gamesApi.getGames  →  lib/api.get  →  fetch
```

- `fetch` 는 `lib/api` 만 부른다. 주소 조립(`API_BASE`)과 실패 해석(`ApiError`)이 한 곳에 있어야
  고칠 때도 한 곳만 고친다
- 코어의 `get` 은 도메인의 `api/` 슬롯만 쓴다. 훅과 화면은 `getGames` 같은 도메인 함수를 거친다
- `ApiError` 는 화면이 써도 된다 — 호출 자체만 한 층에 모은다

### 6. 서버 상태는 TanStack Query 가, queryKey 는 팩토리가 갖는다 — ARCH 007

```ts
useQuery({ queryKey: gameKeys.list(filter), queryFn: () => getGames(filter) }); // ✅
useQuery({ queryKey: ["games", filter], queryFn });                            // ❌
```

키를 리터럴로 흩뿌리면 **무효화할 때 한 곳을 빠뜨려도 에러가 안 난다** — 화면이 조용히 낡은
데이터를 보여줄 뿐이다. 팩토리는 도메인의 `api/keys.ts` 에 두고, 앞단은 도메인 이름이다.

서버에서 온 데이터를 `useState` 나 store 에 복사해 두지 않는다. 복사하면 두 곳이 서로 다른
시점의 값을 들고 있게 된다.

### 7. 목록의 필터는 URL 이 갖는다

필터를 `useState` 로 들면 새로고침 · 뒤로가기 · 링크 공유에서 조건이 사라진다.

| 항목 | 정한 것 | 왜 |
| ---- | ------- | -- |
| 읽기·쓰기 | `model/urlFilter` 의 **순수 함수** + `hooks/useGameFilter` 의 얇은 훅 | 규칙은 라우터 없이 테스트된다 |
| 모르는 값 | **버린다** (`team=XX` → 조건 없음) | 주소는 사용자가 고칠 수 있는 외부 입력이다 |
| 말이 안 되는 조합 | 읽을 때 정리한다 (`normalize`) | 백엔드가 400 으로 거부할 상태를 화면이 갖지 않게 |
| 키 순서 | 고정 (`GAME_FILTER_KEYS`) | 같은 조건은 같은 주소 = 같은 캐시 키 |
| 이동 | `router.replace` + `scroll: false` | 필터를 만질 때마다 뒤로가기 기록이 쌓이지 않고 목록이 튀지 않는다 |

불가능한 조합은 가능하면 **UI 에서 아예 못 만들게** 한다. 예: «예정» 과 «승» 을 따로 고르게 하지
않고 «결과» 선택지 하나로 합쳤다(`Outcome`). 그래도 백엔드 검증은 남긴다 — 주소를 직접 고치거나
자연어 검색이 이상한 값을 만들 수 있다.

### 8. 도메인끼리 순환하지 않는다 — ARCH 008

지금 방향: `games` · `standings` · `simulation` · `prediction` → `teams` → (없음).
배럴 순환은 import 순서에 따라 값이 `undefined` 가 되는 버그를 만든다. 양쪽이 서로 필요해지면
공통 부분을 아래 도메인으로 내린다.

### 9. 컴포넌트 위치

- **한 도메인만 쓰면** `lib/{도메인}/ui/`
- **도메인과 무관하면** `components/ds/` (지금은 `Card`)
- "나중에 다른 데서도 쓸 것 같다" 면 **지금 쓰는 곳에 만든다.** 두 번째 소비자가 실제로 생길 때
  `components/ds/` 로 옮긴다. 미리 공용으로 만들면 아무도 안 쓰는 옵션이 붙는다

컴포넌트는 **이름 export** 다(`export function ScheduleList`). 배럴의 `export *` 는 기본 export 를
재수출하지 않는다. 라우트 파일(`app/**/page.tsx`)만 Next 규약대로 기본 export 다.

`hooks/` · `store/` · `ui/` 파일은 `"use client"` 로 시작한다. 서버 컴포넌트인 페이지가 도메인
배럴을 import 하면 배럴에 딸린 모듈이 서버 쪽 모듈 그래프에 들어오기 때문이다.

### 10. 접근성 이름

입력 하나는 `<label>` 로 감싸 이름을 연결한다. **버튼 여러 개를 `<label>` 로 감싸지 않는다** —
label 은 첫 버튼 하나에만 연결돼서, 이름 글자를 누르면 그 버튼이 눌린다(홈/원정 필터에서 실제로
선택이 풀렸다). 버튼 묶음은 `role="group"` + `aria-labelledby` 로 이름을 붙인다.

## 아직 들이지 않은 것

**의존성은 쓰는 화면이 생길 때 근거와 함께 넣는다.** "나중에 쓸 것 같아서" 넣지 않는다.

| 후보 | 지금 안 넣은 이유 | 넣을 때 |
| ---- | ----------------- | ------- |
| react-hook-form | 제출이 있는 폼이 없다. 검색창·필터는 폼이 아니다(«취소하면 되돌아가야 하는가» 로 가른다) | 제출 폼이 생길 때 |
| zustand | 전역 상태가 «내 팀» 하나이고 `useSyncExternalStore` + localStorage 로 충분하다 | 두 화면 이상이 읽는 전역 상태가 더 생길 때 |
| zod | 검증할 외부 응답이 없다 | 자연어 검색에서 LLM 응답을 검증할 때 (`schemas/` 슬롯) |

금지 의존성(axios · ky · node-fetch · swr)과 이유는 `tests/dependencies.test.ts` 에 있다.

## 검사

```bash
npm run verify   # type-check · test · lint — CI 도 이것을 돌린다
```

| 검사 | 막는 것 |
| ---- | ------- |
| ARCH 000 | 검사 대상 파일을 못 모아서 아래가 전부 빈 채로 통과하는 것 |
| ARCH 001 | `lib/` 바로 아래 기술 이름 폴더 |
| ARCH 002 | 입구(`index.ts`) 없는 도메인·코어 |
| ARCH 003 | 다른 모듈 속 파일 직접 import (`@/` 별칭 · 상대 경로 모두) |
| ARCH 004 | 어휘 밖 슬롯 · 도메인 루트에 흩어진 파일 · 코어 안의 폴더 |
| ARCH 005 | 도메인 + 슬롯 + 파일보다 깊은 중첩 |
| ARCH 006 | 코어 밖 `fetch`·axios · `api/` 슬롯 밖 `get` |
| ARCH 007 | queryKey 리터럴 (인라인 · 변수로 뺀 것) |
| ARCH 008 | 도메인 간 순환 의존 |

### KNOWN — 예외를 올리는 법

각 검사는 **위반 목록이 `KNOWN` 과 정확히 일치해야** 통과한다. 그래서

- 새 위반이 생기면 빨개지고
- 위반을 고쳤는데 `KNOWN` 에 남아 있어도 빨개진다(유령 항목)

규칙을 정말 벗어나야 하면 `KNOWN` 에 **이유를 적어** 올린다. 이유 없이 목록만 늘리는 것은 검사를
끄는 것과 같다. 지금은 모든 검사의 `KNOWN` 이 0건이다.

### 검사의 한계

소스를 문자열로 훑는 **걸림쇠**지 증명이 아니다. 무엇을 잡고 무엇을 못 잡는지는
`tests/conventions.detect.test.ts`(DET) 에 예시로 고정돼 있다 — 예를 들어 다른 파일에서 가져온
배열을 queryKey 로 쓰면 못 잡는다. 판정 함수는 `tests/conventions-detectors.ts` 에 있고, 테스트
파일이 아니어야 한다(테스트 파일을 import 하면 그 안의 테스트가 두 번 돈다).
