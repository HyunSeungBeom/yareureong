/**
 * HTTP 코어 — 서버 호출은 전부 여기를 지난다.
 *
 * 도메인의 `api/` 슬롯이 [get] 을 쓰고, 훅·화면은 fetch 를 직접 부르지 않는다.
 * 주소 조립과 실패 해석이 한 곳에 있어야 고칠 때도 한 곳만 고친다.
 */

/**
 * API 주소. 기본은 **빈 문자열 = 같은 사이트**다 — `next.config.ts` 의 프록시가 `/api/*` 를
 * 실제 API 서버로 넘긴다. 세션 쿠키가 1차 쿠키로 취급되려면 같은 사이트여야 한다.
 */
const configuredBase = process.env.NEXT_PUBLIC_API_BASE ?? "";

/**
 * 절대 주소(`https://…`)만 API 주소로 인정한다.
 *
 * 그 외 값은 **경로로 붙어** `/무언가/api/...` 를 부르게 되어 전부 404 가 된다. 실제로 Vercel 의
 * «민감값» 환경변수가 `[SENSITIVE]` 라는 문자열로 내려와 그 사고가 났다 — 조용히 깨지는 대신
 * 같은 사이트 호출(기본값)로 되돌린다.
 */
export const API_BASE = /^https?:\/\//.test(configuredBase) ? configuredBase : "";

/**
 * API 실패. 검증 실패(400)면 백엔드 ProblemDetail 의 `errors`(위반 사유 목록)를 담는다.
 * 사유를 화면에 그대로 보여주고, 나중엔 자연어 검색 재시도에도 쓴다.
 */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly errors: string[] = [],
  ) {
    super(`API ${status}`);
  }
}

const isStringArray = (v: unknown): v is string[] =>
  Array.isArray(v) && v.every((item) => typeof item === "string");

export /** 모든 요청에 세션 쿠키를 함께 보낸다 — 로그인 상태가 유지되는 유일한 통로다. */
const withCookies: RequestInit = { credentials: "include" };

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, { ...withCookies, ...init });
  if (!res.ok) {
    const body: unknown = await res.json().catch(() => null);
    const errors =
      body && typeof body === "object" && "errors" in body && isStringArray(body.errors)
        ? body.errors
        : [];
    throw new ApiError(res.status, errors);
  }
  /* 204 No Content (로그아웃·삭제) 는 본문이 없다 */
  return (res.status === 204 ? undefined : await res.json()) as T;
}

export const get = <T>(path: string) => request<T>(path);

export const post = <T>(path: string, body?: unknown) =>
  request<T>(path, {
    method: "POST",
    headers: body === undefined ? undefined : { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

export const put = <T>(path: string, body: unknown) =>
  request<T>(path, { method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });

export const del = <T>(path: string) => request<T>(path, { method: "DELETE" });
