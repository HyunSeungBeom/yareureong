import { AsianGamesBoard } from "@/lib/asiangames";
import { TodayBoard } from "@/lib/prediction";
import { ProbabilityChart } from "@/lib/simulation";
import { StandingsTable } from "@/lib/standings";

export default function Home() {
  return (
    <div className="grid gap-6 md:grid-cols-2">
      {/* 대회 기간이 아니면 스스로 사라진다. 그 기간엔 KBO 가 쉬어서 여기가 «오늘 경기» 다. */}
      <div className="md:col-span-2 empty:hidden">
        <AsianGamesBoard />
      </div>
      <div className="md:col-span-2">
        <TodayBoard />
      </div>
      <StandingsTable />
      <ProbabilityChart />
    </div>
  );
}
