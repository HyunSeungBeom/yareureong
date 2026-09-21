package com.seungbeom.kbo.asiangames

import com.seungbeom.kbo.game.Game
import com.seungbeom.kbo.game.GameRepository
import com.seungbeom.kbo.game.GameStatus
import com.seungbeom.kbo.game.Round
import com.seungbeom.kbo.league.League
import com.seungbeom.kbo.team.Team
import com.seungbeom.kbo.team.TeamRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/** 아시안게임 참가국. 국가 코드([code])는 화면에서 국기를 고르는 데 쓴다. */
data class CountryView(val id: String, val code: String, val name: String)

/**
 * 아시안게임 경기 한 건. **승리 확률이 없다** — 국가대표팀은 승률·선발 이력이 쌓이지 않아
 * log5 에 넣을 재료 자체가 없다. 없는 숫자를 지어내느니 일정·결과만 보여준다.
 */
data class AsianGameView(
    val gameId: Long?,
    val gameDate: LocalDate,
    val startTime: String?,
    val stadium: String?,
    /** 예선 / 슈퍼라운드 / 결승 … */
    val round: Round?,
    val status: GameStatus,
    val home: CountryView,
    val away: CountryView,
    val homeScore: Int?,
    val awayScore: Int?,
)

@RestController
class AsianGamesController(
    private val gameRepository: GameRepository,
    private val teamRepository: TeamRepository,
) {

    /**
     * 대회 전체 일정. 한 대회가 스무 경기 남짓이라 **한 번에 다 내려준다** —
     * 화면이 «오늘 / 다음 경기» 를 고르는 데 왕복을 더 하지 않게 하려는 것.
     */
    @GetMapping("/api/asian-games")
    fun schedule(): List<AsianGameView> = views(
        gameRepository.findByLeagueOrderByGameDateAscIdAsc(League.ASIAN_GAMES),
        teamRepository.findByLeague(League.ASIAN_GAMES),
    )

    companion object {
        private const val ID_PREFIX = "AG-"

        /**
         * 순수 함수: 경기 + 참가국 → 화면용 목록. DB 없이 테스트한다.
         * 참가국을 못 찾으면 **경기를 버리지 않고** 코드를 이름 자리에 넣는다 —
         * 팀 한 줄이 비었다고 그날 일정이 통째로 사라지면 더 이상하다.
         */
        fun views(games: List<Game>, teams: List<Team>): List<AsianGameView> {
            val byId = teams.associateBy { it.id }
            fun country(id: String): CountryView {
                val code = id.removePrefix(ID_PREFIX)
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
