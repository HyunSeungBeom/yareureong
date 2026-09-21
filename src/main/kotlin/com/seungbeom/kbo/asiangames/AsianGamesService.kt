package com.seungbeom.kbo.asiangames

import com.seungbeom.kbo.game.Game
import com.seungbeom.kbo.game.GameRepository
import com.seungbeom.kbo.league.League
import com.seungbeom.kbo.team.Team
import com.seungbeom.kbo.team.TeamRepository
import org.springframework.stereotype.Service

@Service
class AsianGamesService(
    private val gameRepository: GameRepository,
    private val teamRepository: TeamRepository,
) {
    /** 대회 전체 일정. 경기와 참가국을 각각 읽어 화면용으로 합친다. */
    fun schedule(): List<AsianGameView> = views(
        gameRepository.findByLeagueOrderByGameDateAscIdAsc(League.ASIAN_GAMES),
        teamRepository.findByLeague(League.ASIAN_GAMES),
    )

    companion object {
        /**
         * 순수 함수: 경기 + 참가국 → 화면용 목록. DB 없이 테스트한다.
         * 참가국을 못 찾으면 **경기를 버리지 않고** 코드를 이름 자리에 넣는다 —
         * 팀 한 줄이 비었다고 그날 일정이 통째로 사라지면 더 이상하다.
         */
        fun views(games: List<Game>, teams: List<Team>): List<AsianGameView> {
            val byId = teams.associateBy { it.id }
            fun country(id: String): CountryView {
                val code = AsianGamesTeams.codeOf(id)
                return CountryView(id, code, byId[id]?.name ?: code)
            }
            return games.map { g ->
                AsianGameView(
                    gameId = g.id,
                    gameDate = g.gameDate,
                    startTime = g.startTime,
                    stadium = g.stadium,
                    round = g.round,
                    status = g.status,
                    home = country(g.homeTeamId),
                    away = country(g.awayTeamId),
                    homeScore = g.homeScore,
                    awayScore = g.awayScore,
                )
            }
        }
    }
}
