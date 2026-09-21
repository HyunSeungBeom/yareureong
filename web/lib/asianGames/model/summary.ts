import type { AsianGame } from "./types";

/** 우리가 응원하는 쪽. 한국 경기는 카드에서 강조한다. */
export const KOREA = "KR";

const ROUND_LABELS: Record<string, string> = {
  GROUP: "예선",
  SUPER_ROUND: "슈퍼라운드",
  PLACEMENT: "순위결정전",
  BRONZE: "동메달 결정전",
  FINAL: "결승",
};

/** 모르는 라운드 코드는 **감추고 넘어간다** — 출처가 새 코드를 주면 화면에 영어가 튀는 게 더 나쁘다. */
export const roundLabel = (round: string | null): string | null =>
  round ? (ROUND_LABELS[round] ?? null) : null;

/**
 * 출처 코드 중 ISO 3166-1 과 다른 것. 그대로 국기로 바꾸면 **다른 나라 국기**가 뜬다
 * (PT 는 ISO 로 포르투갈인데 네이버는 팔레스타인에 쓴다).
 */
const ISO_OVERRIDES: Record<string, string> = { PT: "PS" };

/**
 * 국가 코드 → 국기 이모지. 이미지 대신 이모지를 쓰는 이유는 외부 CDN 에 의존하지 않기 위해서다
 * (핫링크는 출처가 막으면 그날로 깨지고, 우리 서버로 받아 두면 저작권 문제가 붙는다).
 * 두 글자 코드가 아니면 null — 화면은 이름만 보여주고 넘어간다.
 */
export function flagEmoji(code: string): string | null {
  const iso = ISO_OVERRIDES[code] ?? code;
  if (!/^[A-Za-z]{2}$/.test(iso)) return null;
  return [...iso.toUpperCase()].map((c) => String.fromCodePoint(0x1f1e6 + c.charCodeAt(0) - 65)).join("");
}

/** "9월 21일 (월)" — 대회가 2주짜리라 연도는 군더더기다. */
export function dayLabel(date: string): string {
  const [, month, day] = date.split("-").map(Number);
  /* 자정을 UTC 로 읽고 UTC 요일을 쓴다. 한쪽만 시간대를 붙이면 날짜가 하루 밀린다
     (`T00:00:00+09:00` 은 UTC 로 전날 15시다 — 실제로 하루 어긋났다). */
  const weekday = ["일", "월", "화", "수", "목", "금", "토"][new Date(`${date}T00:00:00Z`).getUTCDay()];
  return `${month}월 ${day}일 (${weekday})`;
}

/** KST 기준 오늘(YYYY-MM-DD). 브라우저가 어느 시간대에 있든 한국 날짜로 본다. */
export const seoulToday = (now: Date = new Date()): string =>
  new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Seoul", dateStyle: "short" }).format(now);

export interface GameDay {
  date: string;
  games: AsianGame[];
  isToday: boolean;
}

/**
 * 보여줄 하루를 고른다 — **오늘 경기, 없으면 가장 빠른 다음 경기일**.
 *
 * 대회는 2주 남짓이고 쉬는 날이 있다. 오늘만 보여주면 그 날은 화면이 통째로 비고,
 * 전부 보여주면 스무 경기가 대시보드를 잡아먹는다. 대회가 끝났으면 null — 섹션이 사라진다.
 */
export function pickDay(games: AsianGame[], today: string = seoulToday()): GameDay | null {
  const upcoming = games.filter((g) => g.gameDate >= today).map((g) => g.gameDate);
  if (upcoming.length === 0) return null;

  const date = upcoming.reduce((a, b) => (a < b ? a : b));
  return { date, games: games.filter((g) => g.gameDate === date), isToday: date === today };
}

/** 경기 결과 한 줄. 예정이면 시간, 종료면 점수. */
export const scoreLabel = (game: AsianGame): string | null =>
  game.status === "FINAL" && game.homeScore != null && game.awayScore != null
    ? `${game.awayScore} : ${game.homeScore}`
    : null;

export const isKoreaGame = (game: AsianGame): boolean =>
  game.home.code === KOREA || game.away.code === KOREA;
