"use client";

import { Card } from "@/components/ds";
import { useTodayGames } from "../hooks/useTodayGames";
import { GameCard } from "./GameCard";
import { PredictWidget } from "./PredictWidget";

/**
 * 오늘 열리는 **KBO** 경기 + 예측. 국제대회는 예측 대상이 아니라 다른 섹션(`lib/asianGames`)이 맡는다 —
 * 그래서 빈 상태 문구도 «KBO 경기» 라고 못 박는다. 아시안게임 기간엔 KBO 가 쉬는데
 * 그냥 «경기가 없어요» 라고 하면 바로 위 카드들과 모순돼 보인다. 경기 수는 날마다 2~5경기로 달라지므로 **개수에 맞춰 흐르는 그리드**다.
 *
 * 경기가 없는 날(월요일·우천 취소·비시즌)에는 두 팀을 골라 비교하는 위젯을 대신 보여준다 —
 * 빈 화면보다 낫고, 그 위젯의 유일한 소비자이기도 하다.
 */
export function TodayBoard({ date }: { date?: string }) {
  const { data, isLoading, error } = useTodayGames(date);

  if (isLoading) {
    return (
      <Card>
        <h2 className="text-base font-semibold">오늘 경기</h2>
        <p className="mt-2 text-sm text-slate-500">불러오는 중…</p>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <h2 className="text-base font-semibold">오늘 경기</h2>
        <p className="mt-2 text-sm text-red-600">경기를 불러오지 못했어요. 백엔드가 켜져 있나요?</p>
      </Card>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div>
        <p className="mb-3 text-sm text-slate-500">오늘은 KBO 경기가 없어요. 두 팀을 골라 비교해 보세요.</p>
        <PredictWidget />
      </div>
    );
  }

  return (
    <section>
      <div className="mb-3 flex items-baseline gap-2">
        <h2 className="text-base font-semibold">오늘 경기</h2>
        <span className="text-xs text-slate-500">
          {data.length}경기 · 팀 승률과 선발 투수 기록으로 계산
        </span>
      </div>
      <ul className="grid gap-3 md:grid-cols-2">
        {data.map((game) => (
          <GameCard key={game.gameId ?? `${game.awayTeamId}-${game.homeTeamId}`} game={game} />
        ))}
      </ul>
    </section>
  );
}
