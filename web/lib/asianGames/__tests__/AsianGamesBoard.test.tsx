import { screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AsianGamesBoard, type AsianGame } from "..";
import { respondWith } from "@/tests/fetch-stub";
import { renderWithQuery } from "@/tests/query-provider";

/**
 * 아시안게임 보드 — 1년 중 2주만 의미 있는 섹션이라 **없을 때 사라지는지**가 핵심이다.
 * 날짜를 고정해 «오늘» 이 흔들리지 않게 한다.
 */

const TODAY = new Date("2026-09-21T10:00:00+09:00");

const game = (over: Partial<AsianGame> = {}): AsianGame => ({
  gameId: 1,
  gameDate: "2026-09-21",
  startTime: "18:30",
  stadium: "오카자키",
  round: "GROUP",
  status: "SCHEDULED",
  home: { id: "AG-KR", code: "KR", name: "대한민국" },
  away: { id: "AG-TW", code: "TW", name: "차이니스 타이베이" },
  homeScore: null,
  awayScore: null,
  ...over,
});

const serve = (status: number, body: unknown) => vi.stubGlobal("fetch", respondWith(status, body));

beforeEach(() => vi.setSystemTime(TODAY));
afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

const cards = () => within(screen.getByRole("list")).getAllByRole("listitem");

describe("AGB 001: 오늘 경기를 카드로 그린다", () => {
  it("나라·시간·구장·라운드를 보여준다", async () => {
    serve(200, [game()]);
    renderWithQuery(<AsianGamesBoard />);

    await screen.findByRole("heading", { name: "아시안게임" });
    const card = cards()[0];
    expect(within(card).getByText("대한민국")).toBeInTheDocument();
    expect(within(card).getByText("차이니스 타이베이")).toBeInTheDocument();
    expect(within(card).getByText(/18:30 · 오카자키 · 예선/)).toBeInTheDocument();
  });

  it("종료 경기는 점수를, 예정 경기는 «예정» 을 보여준다", async () => {
    serve(200, [
      game({ gameId: 1, status: "FINAL", homeScore: 5, awayScore: 2 }),
      game({ gameId: 2 }),
    ]);
    renderWithQuery(<AsianGamesBoard />);

    await screen.findByText("종료 2 : 5");
    expect(screen.getByText("예정")).toBeInTheDocument();
  });

  it("승리 확률은 그리지 않는다 — 국가대표는 계산할 재료가 없다", async () => {
    serve(200, [game()]);
    renderWithQuery(<AsianGamesBoard />);

    await screen.findByRole("heading", { name: "아시안게임" });
    expect(screen.queryByText(/%/)).not.toBeInTheDocument();
    expect(screen.getByText(/국가대표라 예측은 하지 않아요/)).toBeInTheDocument();
  });
});

describe("AGB 002: 오늘 경기가 없을 때", () => {
  it("쉬는 날이면 다음 경기일을 날짜와 함께 보여준다", async () => {
    serve(200, [game({ gameDate: "2026-09-23" })]);
    renderWithQuery(<AsianGamesBoard />);

    await screen.findByText(/9월 23일 \(수\) · 1경기/);
  });

  it("대회가 끝났으면 섹션 자체가 없다", async () => {
    // 나머지 50주 동안 «경기가 없어요» 가 대시보드에 남아 있으면 군더더기다.
    serve(200, [game({ gameDate: "2026-09-19" })]);
    const { container } = renderWithQuery(<AsianGamesBoard />);

    await vi.waitFor(() => expect(container).toBeEmptyDOMElement());
  });

  it("일정이 비어도 조용히 사라진다", async () => {
    serve(200, []);
    const { container } = renderWithQuery(<AsianGamesBoard />);

    await vi.waitFor(() => expect(container).toBeEmptyDOMElement());
  });

  it("백엔드가 죽어도 KBO 화면을 방해하지 않는다", async () => {
    serve(500, { detail: "boom" });
    const { container } = renderWithQuery(<AsianGamesBoard />);

    await vi.waitFor(() => expect(container).toBeEmptyDOMElement());
  });
});
