"use client";

import { dayLabel } from "../model/date";
import { useMemo } from "react";
import { Card } from "@/components/ds";
import { ApiError } from "@/lib/api";
import { TeamLogo, teamName } from "@/lib/teams";
import { useGameFilter } from "../hooks/useGameFilter";
import { useGames } from "../hooks/useGames";
import { recordOf, resultFor } from "../model/result";
import type { GameResult } from "../model/types";
import { GameFilterBar } from "./GameFilterBar";

const RESULT_BADGE: Record<GameResult, { label: string; className: string }> = {
  WIN: { label: "승", className: "text-blue-600" },
  LOSS: { label: "패", className: "text-red-600" },
  DRAW: { label: "무", className: "text-slate-500" },
};

/** 경기 일정 + 조건 검색. 필터의 원천은 URL 쿼리스트링이다(`useGameFilter`). */
export function ScheduleList() {
  const { filter, setFilter } = useGameFilter();
  const { data, isLoading, error } = useGames(filter);

  const record = useMemo(
    () => (data && filter.team ? recordOf(data, filter.team) : null),
    [data, filter.team],
  );

  return (
    <Card>
      <h2 className="mb-3 text-base font-semibold">경기 일정</h2>

      <div className="mb-4 border-b border-slate-100 pb-4">
        <GameFilterBar filter={filter} onChange={setFilter} />
      </div>

      {isLoading && <p className="text-sm text-slate-500">불러오는 중…</p>}

      {error && (
        <div className="text-sm text-red-600">
          {error instanceof ApiError && error.errors.length > 0 ? (
            <>
              <p>검색 조건을 확인해 주세요.</p>
              <ul className="mt-1 list-inside list-disc">
                {error.errors.map((msg) => (
                  <li key={msg}>{msg}</li>
                ))}
              </ul>
            </>
          ) : (
            <p>일정을 불러오지 못했어요. 백엔드가 켜져 있나요?</p>
          )}
        </div>
      )}

      {data && (
        <p className="mb-2 text-xs text-slate-500">
          {data.length}경기
          {record && filter.team && (
            <>
              {" · "}
              {teamName(filter.team)} {record.WIN}승 {record.LOSS}패 {record.DRAW}무
            </>
          )}
        </p>
      )}

      {data && data.length === 0 && <p className="text-sm text-slate-500">조건에 맞는 경기가 없어요.</p>}

      {data && data.length > 0 && (
        <ul className="divide-y divide-slate-100">
          {data.map((g) => {
            const result = filter.team ? resultFor(g, filter.team) : null;
            const badge = result
              ? RESULT_BADGE[result]
              : g.status === "FINAL"
                ? { label: "종료", className: "text-slate-400" }
                : { label: "예정", className: "text-emerald-600" };
            return (
              <li key={g.id} className="flex items-center gap-2 py-2 text-sm">
                <span className="w-24 text-slate-500">{dayLabel(g.gameDate)}</span>
                <span
                  className={`flex flex-1 items-center justify-end gap-1.5 ${
                    g.homeTeamId === filter.team ? "font-semibold" : ""
                  }`}
                >
                  <TeamLogo teamId={g.homeTeamId} size={18} />
                  {teamName(g.homeTeamId)}
                </span>
                <span className="w-16 text-center font-medium tabular-nums">
                  {g.status === "FINAL" ? `${g.homeScore} : ${g.awayScore}` : "vs"}
                </span>
                <span
                  className={`flex flex-1 items-center gap-1.5 ${
                    g.awayTeamId === filter.team ? "font-semibold" : ""
                  }`}
                >
                  <TeamLogo teamId={g.awayTeamId} size={18} />
                  {teamName(g.awayTeamId)}
                </span>
                <span className={`w-12 text-right text-xs font-medium ${badge.className}`}>
                  {badge.label}
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </Card>
  );
}
