import { screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { TodayBoard, type StarterView, type TodayGame } from "..";
import { respondWith } from "@/tests/fetch-stub";
import { resetNavigation } from "@/tests/next-navigation-mock";
import { renderWithQuery } from "@/tests/query-provider";

vi.mock("next/navigation", async () => (await import("@/tests/next-navigation-mock")).nextNavigationModule);

/**
 * 오늘 경기 보드 — 날마다 경기 수가 2~5개로 달라진다.
 * 서버는 fetch 를 갈아끼워 흉내 내고, 화면이 «무엇을 어떻게 읽히게 하는가» 만 본다.
 */

const starter = (over: Partial<StarterView> = {}): StarterView => ({
  name: "곽빈",
  starts: 26,
  wins: 12,
  losses: 13,
  draws: 1,
  firstStart: false,
  vsOpponent: null,
  ...over,
});

const game = (over: Partial<TodayGame> = {}): TodayGame => ({
  gameId: 1,
  gameDate: "2026-09-18",
  startTime: "18:30",
  stadium: "잠실야구장",
  status: "SCHEDULED",
  homeTeamId: "OB",
  awayTeamId: "WO",
  homeScore: null,
  awayScore: null,
  homeWinProb: 0.674,
  awayWinProb: 0.326,
  home: starter(),
  away: starter({ name: "알칸타라", starts: 24, wins: 10, losses: 12, draws: 2 }),
  ...over,
});

const serve = (status: number, body: unknown) => vi.stubGlobal("fetch", respondWith(status, body));

beforeEach(resetNavigation);
afterEach(() => vi.unstubAllGlobals());

const cards = () => within(screen.getByRole("list")).getAllByRole("listitem");

describe("TDY 001: 그날 경기 수만큼 카드를 그린다", () => {
  it.each([2, 4, 5])("%i경기", async (count) => {
    serve(200, Array.from({ length: count }, (_, i) => game({ gameId: i + 1 })));
    renderWithQuery(<TodayBoard />);

    await screen.findByText(`${count}경기 · 팀 승률과 선발 투수 기록으로 계산`);
    expect(cards()).toHaveLength(count);
  });

  it("시간·구장·팀·확률을 보여준다", async () => {
    serve(200, [game()]);
    renderWithQuery(<TodayBoard />);

    const card = (await screen.findAllByRole("listitem"))[0];
    expect(within(card).getByText("18:30 · 잠실야구장")).toBeInTheDocument();
    expect(within(card).getByText("두산 베어스")).toBeInTheDocument();
    expect(within(card).getByText("67.4%")).toBeInTheDocument();
    expect(within(card).getByText("32.6%")).toBeInTheDocument();
  });

  it("확률 막대를 문장으로도 읽어준다 — 막대는 스크린 리더가 못 읽는다", async () => {
    serve(200, [game()]);
    renderWithQuery(<TodayBoard />);

    expect(
      await screen.findByRole("img", { name: "승리 확률 — 두산 베어스 67.4%, 키움 히어로즈 32.6%" }),
    ).toBeInTheDocument();
  });
});

describe("TDY 002: 선발 정보를 표본과 함께 보여준다", () => {
  it("이름과 등판 기록", async () => {
    serve(200, [game()]);
    renderWithQuery(<TodayBoard />);

    expect(await screen.findByText("곽빈")).toBeInTheDocument();
    expect(screen.getByText(/등판 26경기 · 팀 12승 13패 1무/)).toBeInTheDocument();
  });

  it("첫 선발은 계산 방식을 알린다", async () => {
    serve(200, [
      game({ home: starter({ name: "신인", starts: 0, wins: 0, losses: 0, draws: 0, firstStart: true }) }),
    ]);
    renderWithQuery(<TodayBoard />);

    expect(await screen.findByText(/첫 선발 — 팀 평균으로 계산/)).toBeInTheDocument();
  });

  it("표본이 적으면 경고를 붙인다", async () => {
    serve(200, [game({ home: starter({ starts: 3, wins: 2, losses: 1, draws: 0 }) })]);
    renderWithQuery(<TodayBoard />);

    expect(await screen.findByText(/표본 적음/)).toBeInTheDocument();
  });

  it("상대전적은 상대 팀 이름과 함께 표시한다", async () => {
    serve(200, [
      game({ home: starter({ vsOpponent: { starts: 2, wins: 1, losses: 1, draws: 0, firstStart: false } }) }),
    ]);
    renderWithQuery(<TodayBoard />);

    expect(await screen.findByText("키움 히어로즈 상대 2등판 1승 1패")).toBeInTheDocument();
  });

  it("선발이 발표되지 않았으면 그렇게 적는다", async () => {
    serve(200, [game({ home: starter({ name: null }) })]);
    renderWithQuery(<TodayBoard />);

    expect((await screen.findAllByText("선발 미발표")).length).toBeGreaterThan(0);
  });
});

describe("TDY 003: 끝난 경기는 점수를 보여준다", () => {
  it("종료 표시와 점수", async () => {
    serve(200, [game({ status: "FINAL", homeScore: 7, awayScore: 3 })]);
    renderWithQuery(<TodayBoard />);

    expect(await screen.findByText("종료 3 : 7")).toBeInTheDocument();
    expect(screen.queryByText("예정")).not.toBeInTheDocument();
  });
});

describe("TDY 004: 경기가 없는 날", () => {
  it("두 팀 비교 위젯으로 대신한다 — 빈 화면을 남기지 않는다", async () => {
    serve(200, []);
    renderWithQuery(<TodayBoard />);

    expect(await screen.findByText(/오늘은 KBO 경기가 없어요/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "예측" })).toBeInTheDocument();
    expect(screen.queryByRole("list")).not.toBeInTheDocument();
  });
});

describe("TDY 005: 실패", () => {
  it("불러오지 못하면 알린다", async () => {
    serve(500, "boom");
    renderWithQuery(<TodayBoard />);

    expect(await screen.findByText(/경기를 불러오지 못했어요/)).toBeInTheDocument();
  });
});
