@AGENTS.md

# web/ — 프론트

> 레포 전체 안내는 루트 `CLAUDE.md`. 여기는 프론트에서 일할 때의 규칙이다.

## 구조 — 축은 도메인

```
app/              라우트만 (도메인 배럴에서 UI 를 가져와 조립)
components/ds/    도메인과 무관한 공용 부품
lib/api/          HTTP 코어 — fetch 는 여기서만
lib/{도메인}/      games · teams · standings · simulation · prediction · asiangames  (= 백엔드 패키지)
  index.ts        입구. 밖에서는 이것만 import
  model/ schemas/ mocks/ api/ store/ hooks/ ui/   슬롯 — 고정 어휘, 필요한 것만
  __tests__/
```

- **다른 도메인은 배럴로만** import 한다(`@/lib/games`). 같은 도메인 안은 형제 파일을 직접 본다
- **`lib/` 바로 아래에 `utils` · `types` · `hooks` 같은 기술 폴더를 만들지 않는다**
- **서버 호출 경로는 하나**: `ui → hooks → api/ 슬롯 → lib/api.get → fetch`
- **queryKey 는 `api/keys.ts` 팩토리**로만. 리터럴 배열 금지
- **목록 필터는 URL 이 갖는다** — `model/urlFilter`(순수 함수) + `hooks/use*Filter`, `router.replace` + `scroll: false`
- 컴포넌트는 **이름 export**, `hooks/` · `store/` · `ui/` 파일은 `"use client"`
- **버튼 묶음을 `<label>` 로 감싸지 않는다** — `role="group"` + `aria-labelledby`
- 의존성은 **쓰는 화면이 생길 때 근거와 함께** 넣는다. RHF · zod 는 게시판 입력 화면이 생기며
  들였고(`lib/board`), zustand 는 아직 안 들였다 — 서버 상태는 Query 가, 내 팀 설정은
  `lib/teams/store` 가 들고 있어 쓸 자리가 없다

근거 · 예시 · 규칙별 검사표: **[docs/frontend-conventions.md](docs/frontend-conventions.md)**

## 테스트

- Vitest + jsdom + Testing Library. 도메인 테스트는 `lib/{도메인}/__tests__/`, 레포 전체 규칙은 `tests/`
- 제목은 `그룹 번호: 설명`(`SCH 003: …`). 새 그룹을 만들면 docs/testing.md 표에 올린다(DOC 002 가 검사)
- 셀렉터는 **`getByRole` / `getByLabelText`** — 접근성 검사를 겸한다
- **테스트를 쓰면 고친 코드를 되돌려 빨개지는지 확인한다**
- 커버리지 목표치는 없다
- **npm 으로 패키지를 추가·제거하면 lockfile 에서 플랫폼 바이너리가 지워질 수 있다** — `DEP 003` 이 잡는다.
  복구법은 docs/testing.md 함정 표

붙이는 법 · 모의 도구 · 함정: **[docs/testing.md](docs/testing.md)**

## 끝내기 전에

```bash
npm run verify   # type-check · test · lint — CI 와 같다
npm run build
```
