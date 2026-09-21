import { describe, expect, it } from "vitest";
import { dayLabel, flagEmoji, isKoreaGame, pickDay, roundLabel, scoreLabel, seoulToday, type AsianGame } from "..";

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

describe("AGS 001: 라운드 이름", () => {
  it.each([
    ["GROUP", "예선"],
    ["SUPER_ROUND", "슈퍼라운드"],
    ["FINAL", "결승"],
  ])("%s → %s", (code, label) => {
    expect(roundLabel(code)).toBe(label);
  });

  it("모르는 코드와 null 은 감춘다 — 화면에 영어가 튀는 게 더 나쁘다", () => {
    expect(roundLabel("agbaseball_wat")).toBeNull();
    expect(roundLabel(null)).toBeNull();
  });
});

describe("AGS 002: 국기 이모지", () => {
  it("두 글자 코드를 국기로 바꾼다", () => {
    expect(flagEmoji("KR")).toBe("🇰🇷");
    expect(flagEmoji("JP")).toBe("🇯🇵");
  });

  it("출처 코드가 ISO 와 다르면 바로잡는다", () => {
    // 네이버는 팔레스타인에 PT 를 쓴다. ISO 로는 포르투갈이라 그대로 두면 다른 나라 국기가 뜬다.
    expect(flagEmoji("PT")).toBe("🇵🇸");
    expect(flagEmoji("PT")).not.toBe("🇵🇹");
  });

  it("두 글자가 아니면 null — 이름만 보여주고 넘어간다", () => {
    for (const code of ["", "K", "KOR", "A1", "12"]) expect(flagEmoji(code)).toBeNull();
  });
});

describe("AGS 003: 보여줄 하루 고르기", () => {
  const days = [
    game({ gameId: 1, gameDate: "2026-09-21" }),
    game({ gameId: 2, gameDate: "2026-09-21" }),
    game({ gameId: 3, gameDate: "2026-09-23" }),
  ];

  it("오늘 경기가 있으면 오늘만 준다", () => {
    const day = pickDay(days, "2026-09-21");
    expect(day).toMatchObject({ date: "2026-09-21", isToday: true });
    expect(day?.games.map((g) => g.gameId)).toEqual([1, 2]);
  });

  it("오늘 쉬는 날이면 가장 빠른 다음 경기일을 준다", () => {
    // 대회 중 쉬는 날이 있다. 그날 화면이 통째로 비면 «대회가 끝났나» 싶다.
    expect(pickDay(days, "2026-09-22")).toMatchObject({ date: "2026-09-23", isToday: false });
  });

  it("대회가 끝났거나 일정이 없으면 null — 섹션이 사라진다", () => {
    expect(pickDay(days, "2026-09-24")).toBeNull();
    expect(pickDay([], "2026-09-21")).toBeNull();
  });
});

describe("AGS 004: 날짜·점수·한국 경기 표기", () => {
  it("연도 없이 요일까지 보여준다", () => {
    expect(dayLabel("2026-09-21")).toBe("9월 21일 (월)");
    expect(dayLabel("2026-09-27")).toBe("9월 27일 (일)");
  });

  it("종료 경기만 점수를 보여준다 — 예정 경기 0대0 은 무승부로 읽힌다", () => {
    expect(scoreLabel(game({ status: "FINAL", homeScore: 5, awayScore: 2 }))).toBe("2 : 5");
    expect(scoreLabel(game({ homeScore: 0, awayScore: 0 }))).toBeNull();
    expect(scoreLabel(game({ status: "FINAL", homeScore: null, awayScore: null }))).toBeNull();
  });

  it("한국 경기는 홈·원정 어느 쪽이어도 찾는다", () => {
    expect(isKoreaGame(game())).toBe(true);
    expect(
      isKoreaGame(
        game({
          home: { id: "AG-JP", code: "JP", name: "일본" },
          away: { id: "AG-KR", code: "KR", name: "대한민국" },
        }),
      ),
    ).toBe(true);
    expect(
      isKoreaGame(
        game({
          home: { id: "AG-JP", code: "JP", name: "일본" },
          away: { id: "AG-CN", code: "CN", name: "중국" },
        }),
      ),
    ).toBe(false);
  });

  it("오늘은 브라우저 시간대가 아니라 한국 날짜다", () => {
    // 2026-09-21 09:00 UTC = 18:00 KST 같은 날, 2026-09-21 16:00 UTC = 22일 01:00 KST
    expect(seoulToday(new Date("2026-09-21T09:00:00Z"))).toBe("2026-09-21");
    expect(seoulToday(new Date("2026-09-21T16:00:00Z"))).toBe("2026-09-22");
  });
});
