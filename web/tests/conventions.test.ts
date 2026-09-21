import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { join, relative } from "node:path";
import { describe, expect, it } from "vitest";
import {
  CORES,
  MAX_DEPTH,
  SLOTS,
  TECH_FOLDERS,
  callsServerDirectly,
  findCycles,
  importSpecs,
  importedValueNames,
  literalQueryKeys,
  moduleOf,
  resolveSpec,
  slotOf,
  stripComments,
} from "./conventions-detectors";

/**
 * ARCH — 프론트 구조 규약. 근거는 `docs/frontend-conventions.md` 에 있고 여기서 강제한다.
 *
 * 문서만 두면 지켜지지 않는다. 그래서 규약마다 검사가 딸려 있다.
 *
 * **KNOWN 과 정확히 일치해야 통과한다.** 새 위반이 생겨도 빨개지고, 고쳤는데 목록에 남은
 * 항목도 빨개진다. 예외가 정말 필요하면 KNOWN 에 **이유와 함께** 올린다 — 이유 없이 목록만
 * 늘리는 것은 검사를 끄는 것과 같다.
 */

const ROOT = process.cwd();

interface Source {
  path: string;
  body: string;
}

/** 검사 대상 = 제품 코드. 테스트는 대상이 아니다(HYG 가 따로 본다) */
function productSources(): Source[] {
  const out: Source[] = [];
  const walk = (dir: string) => {
    for (const name of readdirSync(dir)) {
      const full = join(dir, name);
      if (statSync(full).isDirectory()) {
        if (name !== "__tests__" && name !== "node_modules") walk(full);
      } else if (/\.tsx?$/.test(name)) {
        out.push({ path: relative(ROOT, full), body: stripComments(readFileSync(full, "utf-8")) });
      }
    }
  };
  for (const dir of ["app", "components", "lib"]) walk(join(ROOT, dir));
  return out;
}

const FILES = productSources();
const LIB = FILES.filter((f) => moduleOf(f.path));
const isCore = (module: string) => (CORES as readonly string[]).includes(module);

function expectMatchesKnown(hits: string[], known: Record<string, string>, message: string) {
  expect([...new Set(hits)].sort(), message).toEqual(Object.keys(known).sort());
}

describe("ARCH 000: 검사 대상이 실제로 있다", () => {
  it("파일을 못 모으면 아래 검사가 전부 빈 목록끼리 통과한다", () => {
    expect(LIB.length).toBeGreaterThan(20);
    expect(FILES.some((f) => f.path.startsWith("app/"))).toBe(true);
  });
});

describe("ARCH 001: lib/ 바로 아래는 도메인이나 코어다 — 기술 이름 폴더를 만들지 않는다", () => {
  const KNOWN: Record<string, string> = {};

  it("lib/utils · lib/types · lib/hooks 같은 폴더가 없다", () => {
    /* 기술로 나누면 같이 바뀌는 것(타입·상수·그걸 읽는 함수)이 폴더 셋에 흩어져 한쪽만 고치게 된다 */
    const hits = LIB.map((f) => moduleOf(f.path)!).filter((m) => (TECH_FOLDERS as readonly string[]).includes(m));
    expectMatchesKnown(hits.map((m) => `lib/${m}`), KNOWN, "기술 이름 폴더다 — 도메인 폴더 안의 슬롯으로 둔다");
  });
});

describe("ARCH 002: 도메인(과 코어)은 입구 index.ts 를 갖는다", () => {
  const KNOWN: Record<string, string> = {};

  it("입구가 없으면 모듈이 아니라 잡동사니 폴더다", () => {
    const modules = new Set(LIB.map((f) => moduleOf(f.path)!).filter((m) => existsSync(join(ROOT, "lib", m)) && statSync(join(ROOT, "lib", m)).isDirectory()));
    const missing = [...modules].filter((m) => !existsSync(join(ROOT, "lib", m, "index.ts"))).map((m) => `lib/${m}`);
    expectMatchesKnown(missing, KNOWN, "입구(index.ts)가 없다");
  });
});

describe("ARCH 003: 밖에서는 배럴로만 들어간다", () => {
  const KNOWN: Record<string, string> = {};

  it("다른 모듈의 속 파일을 직접 import 하지 않는다 — 별칭·상대 경로 모두", () => {
    /* 속을 직접 찌르면 파일을 쪼개거나 합칠 때마다 소비자가 깨진다. 같은 도메인 안은 형제를
       직접 본다(배럴을 거치면 순환이 된다) */
    const hits: string[] = [];
    for (const f of FILES) {
      for (const spec of importSpecs(f.body)) {
        const target = resolveSpec(f.path, spec);
        const m = target && /^lib\/([^/]+)\/(.+)$/.exec(target);
        if (!m || m[2] === "index" || moduleOf(f.path) === m[1]) continue;
        hits.push(`${f.path} → ${spec}`);
      }
    }
    expectMatchesKnown(hits, KNOWN, "모듈 속을 직접 찔렀다 — 배럴(index)로 들어간다");
  });
});

describe("ARCH 004: 도메인 안은 고정된 슬롯으로 나눈다", () => {
  const KNOWN_SLOTS: Record<string, string> = {};
  const KNOWN_LOOSE: Record<string, string> = {};

  it(`슬롯 이름은 ${SLOTS.join(" · ")} 중 하나다`, () => {
    const hits = LIB.map((f) => ({ f, slot: slotOf(f.path) }))
      .filter(({ slot }) => slot && !(SLOTS as readonly string[]).includes(slot))
      .map(({ f, slot }) => `lib/${moduleOf(f.path)}/${slot}`);
    expectMatchesKnown(hits, KNOWN_SLOTS, "슬롯 어휘 밖이다 — 어디 넣을지 몰라 새로 만들면 갈린다");
  });

  it("도메인 루트에는 index.ts 만 둔다 — 나머지는 슬롯에 들어간다", () => {
    const hits = LIB.filter((f) => !isCore(moduleOf(f.path)!))
      .filter((f) => f.path.split("/").length === 3 && !f.path.endsWith("/index.ts"))
      .map((f) => f.path);
    expectMatchesKnown(hits, KNOWN_LOOSE, "도메인 루트에 흩어진 파일이다");
  });

  it("코어는 평평하다 — 슬롯으로 나누지 않는다", () => {
    const hits = LIB.filter((f) => isCore(moduleOf(f.path)!) && f.path.split("/").length > 3).map((f) => f.path);
    expectMatchesKnown(hits, {}, "코어 안에 폴더를 팠다");
  });
});

describe("ARCH 005: 깊이는 도메인 + 슬롯 + 파일까지다", () => {
  const KNOWN: Record<string, string> = {};

  it(`lib 경로가 ${MAX_DEPTH} 조각을 넘지 않는다 — 슬롯 안에 폴더를 또 파지 않는다`, () => {
    const hits = LIB.filter((f) => f.path.split("/").length > MAX_DEPTH).map((f) => f.path);
    expectMatchesKnown(hits, KNOWN, "중첩이 깊어진다");
  });
});

describe("ARCH 006: 서버 호출은 한 경로로만 간다", () => {
  const KNOWN_FETCH: Record<string, string> = {};
  const KNOWN_GET: Record<string, string> = {};

  it("fetch·axios 는 HTTP 코어(lib/api)만 쓴다", () => {
    const hits = FILES.filter((f) => moduleOf(f.path) !== "api" && callsServerDirectly(f.body)).map((f) => f.path);
    expectMatchesKnown(hits, KNOWN_FETCH, "코어 밖에서 서버를 직접 부른다");
  });

  it("코어의 get 은 도메인의 api/ 슬롯만 쓴다 — 훅·화면은 api 함수를 거친다", () => {
    /* ApiError 같은 타입·클래스는 화면이 써도 된다. 호출 자체만 한 층에 모은다 */
    const hits = FILES.filter((f) => slotOf(f.path) !== "api")
      .filter((f) => importedValueNames(f.body, (spec) => resolveSpec(f.path, spec) === "lib/api").includes("get"))
      .map((f) => f.path);
    expectMatchesKnown(hits, KNOWN_GET, "api/ 슬롯 밖에서 get 을 부른다");
  });
});

describe("ARCH 007: queryKey 는 팩토리를 거친다", () => {
  const KNOWN: Record<string, string> = {};

  it("리터럴 배열(인라인·변수로 뺀 것)을 queryKey 로 쓰지 않는다 — 무효화가 조용히 빗나간다", () => {
    const hits = FILES.filter((f) => literalQueryKeys(f.body).length > 0).map((f) => f.path);
    expectMatchesKnown(hits, KNOWN, "queryKey 리터럴 — api/keys.ts 팩토리를 쓴다");
  });
});

describe("ARCH 009: 도메인 폴더 이름은 소문자다", () => {
  const KNOWN: Record<string, string> = {};

  it("백엔드 패키지와 1:1 이라 표기가 갈리면 짝이 안 보인다", () => {
    /* 백엔드 패키지는 소문자만 쓴다(`asiangames`). 한쪽만 카멜이면 «같은 도메인» 이라는 게
       이름으로 드러나지 않고, import 경로 오타도 잦아진다 */
    const hits = [...new Set(LIB.map((f) => moduleOf(f.path)!))]
      .filter((m) => m !== m.toLowerCase())
      .map((m) => `lib/${m}`);
    expectMatchesKnown(hits, KNOWN, "도메인 폴더 이름에 대문자가 있다");
  });
});

describe("ARCH 008: 도메인끼리 순환 의존하지 않는다", () => {
  it("배럴 순환은 import 순서에 따라 undefined 가 되는 버그를 만든다", () => {
    const edges: Record<string, Set<string>> = {};
    for (const f of LIB) {
      const from = moduleOf(f.path)!;
      for (const spec of importSpecs(f.body)) {
        const to = moduleOf(resolveSpec(f.path, spec) ?? "");
        if (to && to !== from) (edges[from] ??= new Set()).add(to);
      }
    }
    const graph = Object.fromEntries(Object.entries(edges).map(([k, v]) => [k, [...v]]));
    expect(findCycles(graph), "도메인 순환 — 공통 부분을 아래 도메인으로 내린다").toEqual([]);
  });
});
