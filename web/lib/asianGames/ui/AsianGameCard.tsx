"use client";

import { flagEmoji, isKoreaGame, roundLabel, scoreLabel } from "../model/summary";
import type { AsianGame, Country } from "../model/types";

function Side({ country, align }: { country: Country; align: "left" | "right" }) {
  const flag = flagEmoji(country.code);
  return (
    <div className={`min-w-0 flex-1 ${align === "right" ? "text-right" : ""}`}>
      <p
        className={`flex items-center gap-1.5 text-sm font-semibold break-keep ${
          align === "right" ? "justify-end" : ""
        }`}
      >
        {/* 이모지는 그 자체로 «국기» 라고 읽히지 않는다 — 이름이 바로 옆에 있으니 장식으로 숨긴다 */}
        {flag && (
          <span aria-hidden className="text-lg leading-none">
            {flag}
          </span>
        )}
        {country.name}
      </p>
    </div>
  );
}

/**
 * 아시안게임 경기 한 건. KBO 카드와 달리 **확률 막대가 없다** —
 * 국가대표는 승률·선발 이력이 없어 계산할 재료가 없다.
 */
export function AsianGameCard({ game }: { game: AsianGame }) {
  const score = scoreLabel(game);
  const round = roundLabel(game.round);
  const korea = isKoreaGame(game);

  return (
    <li
      className={`rounded-xl border bg-white p-4 shadow-sm ${
        korea ? "border-red-200 ring-1 ring-red-100" : "border-slate-200"
      }`}
    >
      <div className="mb-3 flex flex-wrap items-center justify-between gap-1 text-xs text-slate-500">
        <span>
          {game.startTime ?? "시간 미정"}
          {game.stadium && ` · ${game.stadium}`}
          {round && ` · ${round}`}
        </span>
        {score ? (
          <span className="font-medium text-slate-400">종료 {score}</span>
        ) : (
          <span className="font-medium text-emerald-600">예정</span>
        )}
      </div>

      <div className="flex items-center justify-between gap-2 sm:gap-4">
        <Side country={game.away} align="left" />
        <span className="shrink-0 text-xs text-slate-400">vs</span>
        <Side country={game.home} align="right" />
      </div>
    </li>
  );
}
