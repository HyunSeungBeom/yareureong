package com.seungbeom.kbo.prediction

import com.seungbeom.kbo.game.Game
import com.seungbeom.kbo.game.GameRepository
import com.seungbeom.kbo.game.GameStatus
import com.seungbeom.kbo.standings.StandingsService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import com.seungbeom.kbo.league.League

/**
 * 계산된 순위(승률)와 선발 등판 이력을 읽어 승리확률을 낸다.
 * 수식 자체는 [Predictions] → Java [Log5] 로 위임 (Kotlin → Java interop).
 */
@Service
class PredictionService(
    private val standingsService: StandingsService,
    private val gameRepository: GameRepository,
) {

    /** 홈팀이 이길 확률(선발 미지정 매치업). 기록이 없는 팀은 승률 0으로 취급. */
    fun homeWinProbability(homeTeamId: String, awayTeamId: String): Double {
        val winPct = standingsService.winPctByTeam()
        val home = winPct[homeTeamId] ?: 0.0
        val away = winPct[awayTeamId] ?: 0.0
        return Predictions.homeWinProbability(home, away)
    }

    /**
     * 그날 열리는 경기들 + 예측. 기본값은 **KST 오늘** — 서버 타임존(운영은 UTC)을 따르면
     * 한국 새벽 시간대에 «어제 경기» 가 뜬다.
     */
    fun gamesOn(date: LocalDate = LocalDate.now(KST)): List<TodayGame> {
        val games = gameRepository.findByLeagueAndGameDateOrderByIdAsc(League.KBO, date)
        if (games.isEmpty()) return emptyList()

        val winPct = standingsService.winPctByTeam()
        val ratings = StarterRatings(gameRepository.findByLeagueAndStatus(League.KBO, GameStatus.FINAL))
        return games.map { toTodayGame(it, winPct, ratings) }
    }

    private fun toTodayGame(
        game: Game,
        winPct: Map<String, Double>,
        ratings: StarterRatings,
    ): TodayGame {
        val homePct = winPct[game.homeTeamId] ?: 0.0
        val awayPct = winPct[game.awayTeamId] ?: 0.0
        val homeProb = Predictions.homeWinProbability(
            homePct,
            awayPct,
            ratings.adjustment(game.homeStartPitcher, game.homeTeamId, homePct),
            ratings.adjustment(game.awayStartPitcher, game.awayTeamId, awayPct),
        )

        return TodayGame(
            gameId = game.id,
            gameDate = game.gameDate,
            startTime = game.startTime,
            stadium = game.stadium,
            status = game.status,
            homeTeamId = game.homeTeamId,
            awayTeamId = game.awayTeamId,
            homeScore = game.homeScore,
            awayScore = game.awayScore,
            homeWinProb = homeProb,
            awayWinProb = 1.0 - homeProb,
            home = starterView(ratings, game.homeStartPitcher, game.homeTeamId, game.awayTeamId),
            away = starterView(ratings, game.awayStartPitcher, game.awayTeamId, game.homeTeamId),
        )
    }

    private fun starterView(
        ratings: StarterRatings,
        pitcher: String?,
        teamId: String,
        opponentTeamId: String,
    ): StarterView {
        val record = ratings.record(pitcher, teamId)
        return StarterView(
            name = pitcher,
            starts = record.starts,
            wins = record.wins,
            losses = record.losses,
            draws = record.draws,
            firstStart = record.firstStart,
            vsOpponent = ratings.against(pitcher, teamId, opponentTeamId),
        )
    }

    companion object {
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
