"use client";

import { dayLabel } from "@/lib/games";
import { useAsianGames } from "../hooks/useAsianGames";
import { pickDay } from "../model/summary";
import { AsianGameCard } from "./AsianGameCard";

/**
 * 아시안게임 경기 보드.
 *
 * **대회 기간이 아니면 아무것도 그리지 않는다.** 1년 중 2주만 의미 있는 섹션이라
 * 빈 상태("경기가 없어요")를 남겨 두면 나머지 50주 동안 대시보드에 군더더기가 된다.
 * 같은 이유로 로딩·에러도 조용히 넘어간다 — 이 섹션이 없다고 KBO 화면이 아쉬울 게 없다.
 */
export function AsianGamesBoard() {
  const { data } = useAsianGames();
  const day = data ? pickDay(data) : null;
  if (!day) return null;

  return (
    <section>
      <div className="mb-3 flex items-baseline gap-2">
        <h2 className="text-base font-semibold">아시안게임</h2>
        <span className="text-xs text-slate-500">
          {day.isToday ? "오늘" : dayLabel(day.date)} · {day.games.length}경기 · 국가대표라 예측은 하지 않아요
        </span>
      </div>
      <ul className="grid gap-3 md:grid-cols-2">
        {day.games.map((game) => (
          <AsianGameCard key={game.gameId ?? `${game.away.id}-${game.home.id}`} game={game} />
        ))}
      </ul>
    </section>
  );
}
