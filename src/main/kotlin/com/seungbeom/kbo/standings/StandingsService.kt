package com.seungbeom.kbo.standings

import com.seungbeom.kbo.game.GameRepository
import com.seungbeom.kbo.game.GameStatus
import com.seungbeom.kbo.team.TeamRepository
import org.springframework.stereotype.Service
import com.seungbeom.kbo.team.League

/**
 * 순위표를 만든다: 팀 목록 + FINAL 경기 → [Standings.compute].
 * (경기 수가 적어 매 요청 계산해도 충분. 커지면 캐싱 = Phase 2.)
 */
@Service
class StandingsService(
    private val teamRepository: TeamRepository,
    private val gameRepository: GameRepository,
) {
    fun standings(): List<TeamStanding> {
        val teams = teamRepository.findByLeague(League.KBO)
        /* 국제대회 경기가 섞이면 순위가 조용히 틀어진다 — 리그를 반드시 좁힌다 */
        val finals = gameRepository.findByLeagueAndStatus(League.KBO, GameStatus.FINAL)
        return Standings.compute(teams, finals)
    }

    /** 예측용: 팀 id → 승률 맵. */
    fun winPctByTeam(): Map<String, Double> =
        standings().associate { it.teamId to it.winPct }
}
