import type { GameStatus } from "@/lib/games";

/** 참가국. [code] 는 접두사 없는 국가 코드("KR") — 국기를 고르는 데 쓴다. */
export interface Country {
  id: string;
  code: string;
  name: string;
}

/**
 * 백엔드 `GET /api/asian-games` 응답 한 건.
 *
 * **승리 확률이 없다.** 국가대표팀은 승률·선발 이력이 쌓이지 않아 예측의 재료가 없다.
 * 타입에 확률 자리를 만들지 않음으로써 "언젠가 채우자"는 유혹도 같이 막는다.
 */
export interface AsianGame {
  gameId: number | null;
  gameDate: string;
  startTime: string | null;
  stadium: string | null;
  round: string | null;
  status: GameStatus;
  home: Country;
  away: Country;
  homeScore: number | null;
  awayScore: number | null;
}
