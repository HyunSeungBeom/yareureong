package com.seungbeom.kbo.simulation

import com.seungbeom.kbo.game.GameRepository
import com.seungbeom.kbo.game.GameStatus
import com.seungbeom.kbo.standings.StandingsService
import org.springframework.stereotype.Service
import kotlin.random.Random
import com.seungbeom.kbo.team.League

/**
 * 현재 순위 + 남은 경기로 시즌을 몬테카를로 시뮬레이션한다.
 * (반복 횟수가 커서 매 요청 재계산은 무겁다 → 캐싱은 후속 과제.)
 */
@Service
class SimulationService(
    private val standingsService: StandingsService,
    private val gameRepository: GameRepository,
) {
    fun simulate(iterations: Int): List<SimulationResult> {
        val n = iterations.coerceIn(MIN_ITERATIONS, MAX_ITERATIONS)
        val standings = standingsService.standings()
        val remaining = gameRepository.findByLeagueAndStatus(League.KBO, GameStatus.SCHEDULED)
        return SeasonSimulator.simulate(standings, remaining, n, Random.Default)
    }

    companion object {
        const val MIN_ITERATIONS = 1_000
        const val MAX_ITERATIONS = 100_000
    }
}
