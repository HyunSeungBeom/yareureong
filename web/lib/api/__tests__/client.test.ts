import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE, ApiError, get, post } from "..";
import { respondWith } from "@/tests/fetch-stub";

/**
 * HTTP 코어 — 가짜 클라이언트가 아니라 **fetch 를 갈아끼운다.** 주소 조립과 실패 해석이
 * 실제 코드 경로를 그대로 타게 하기 위해서다.
 */

afterEach(() => vi.unstubAllGlobals());

describe("API 001: 성공하면 본문을 돌려준다", () => {
  it("API_BASE 뒤에 경로를 붙여 부른다", async () => {
    const fetch = respondWith(200, [{ id: 1 }]);
    vi.stubGlobal("fetch", fetch);

    await expect(get("/api/standings")).resolves.toEqual([{ id: 1 }]);
    expect(fetch).toHaveBeenCalledWith(
      `${API_BASE}/api/standings`,
      /* 세션 쿠키를 함께 보내야 로그인 상태가 유지된다 */
      expect.objectContaining({ credentials: "include" }),
    );
  });
});

describe("API 001-2: 본문 없는 응답(204)도 처리한다", () => {
  it("로그아웃·삭제는 204 라 JSON 파싱을 시도하면 안 된다", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => new Response(null, { status: 204 })));

    await expect(post("/api/auth/logout")).resolves.toBeUndefined();
  });
});

describe("API 002: 검증 실패는 사유 목록을 담아 던진다", () => {
  it("ProblemDetail 의 errors 를 그대로 옮긴다", async () => {
    vi.stubGlobal(
      "fetch",
      respondWith(400, { status: 400, detail: "검색 조건이 올바르지 않습니다", errors: ["opponent 는 team 과 함께 써야 합니다"] }),
    );

    const error = await get("/api/games").catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 400, errors: ["opponent 는 team 과 함께 써야 합니다"] });
  });

  it.each([
    ["본문이 JSON 이 아님", 502, "Bad Gateway"],
    ["errors 가 없음 (스프링 기본 400)", 400, { status: 400, error: "Bad Request" }],
    ["errors 가 문자열 배열이 아님", 400, { errors: [{ message: "x" }] }],
  ])("%s → 빈 목록 — 화면이 이상한 값을 사유로 그리지 않는다", async (_, status, body) => {
    vi.stubGlobal("fetch", respondWith(status, body));

    await expect(get("/api/games")).rejects.toMatchObject({ status, errors: [] });
  });
});

describe("API 003: 이상한 API 주소 설정은 무시하고 같은 사이트로 부른다", () => {
  it("절대 주소가 아니면 경로로 붙어 전부 404 가 된다 — 그럴 바엔 기본값(같은 사이트)이 낫다", () => {
    /* Vercel 의 «민감값» 환경변수는 실제 값 대신 [SENSITIVE] 문자열로 내려온다(실제로 겪은 사고) */
    expect(API_BASE).not.toContain("[SENSITIVE]");
    expect(API_BASE === "" || /^https?:\/\//.test(API_BASE)).toBe(true);
  });
});
