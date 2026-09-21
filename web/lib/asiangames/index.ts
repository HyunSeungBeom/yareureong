/**
 * 아시안게임 도메인의 입구 — 백엔드 `asiangames` 패키지(`/api/asian-games`)와 1:1.
 *
 * **쓰는 화면** — 대시보드(`/`)의 아시안게임 섹션
 *
 * KBO 와 한 테이블을 쓰지만 리그가 달라 집계가 섞이지 않는다(백엔드 `League`).
 * 이 도메인에는 승리 확률이 없다 — 국가대표는 승률·선발 이력이 쌓이지 않는다.
 */

export * from "./model/types";
export * from "./model/summary";
export * from "./api/asianGamesApi";
export * from "./api/keys";
export * from "./hooks/useAsianGames";
export * from "./ui/AsianGameCard";
export * from "./ui/AsianGamesBoard";
