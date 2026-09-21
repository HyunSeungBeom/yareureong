package com.seungbeom.kbo.game

import com.seungbeom.kbo.prediction.PredictionService
import com.seungbeom.kbo.prediction.TodayGame
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import com.seungbeom.kbo.league.League

@RestController
@RequestMapping("/api")
class GameController(
    private val gameRepository: GameRepository,
    private val predictionService: PredictionService,
    private val gameSearchService: GameSearchService,
) {

    /** 전체 일정 또는 특정 날짜(?date=2026-07-20)의 경기 목록. */
    @GetMapping("/schedule")
    fun schedule(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,
    ): List<Game> =
        if (date != null) gameRepository.findByLeagueAndGameDateOrderByIdAsc(League.KBO, date)
        else gameRepository.findByLeagueOrderByGameDateAscIdAsc(League.KBO)

    /**
     * 조건 검색. 모든 파라미터 선택, 조건끼리는 AND. venue·result·opponent 는 team 관점.
     * 예: /api/games?team=OB&opponent=LG&venue=HOME&result=WIN&from=2026-08-01&to=2026-08-31
     */
    @GetMapping("/games")
    fun games(
        @RequestParam(required = false) team: String?,
        @RequestParam(required = false) opponent: String?,
        @RequestParam(required = false) venue: Venue?,
        @RequestParam(required = false) result: GameResult?,
        @RequestParam(required = false) status: GameStatus?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): List<Game> =
        gameSearchService.search(GameFilter(team, opponent, venue, result, status, from, to))

    /** 검증 실패는 400 + 위반 목록(errors). */
    @ExceptionHandler(InvalidGameFilterException::class)
    fun invalidFilter(e: InvalidGameFilterException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "검색 조건이 올바르지 않습니다").apply {
            setProperty("errors", e.errors)
        }

    /**
     * 그날 열리는 경기 + 선발 반영 예측. `/api/predict/today?date=2026-09-18`
     * date 를 생략하면 **KST 오늘**.
     */
    @GetMapping("/predict/today")
    fun today(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,
    ): List<TodayGame> =
        if (date != null) predictionService.gamesOn(date) else predictionService.gamesOn()

    /** 두 팀 매치업 예측(선발 미지정): /api/predict?home=OB&away=LG */
    @GetMapping("/predict")
    fun predict(
        @RequestParam home: String,
        @RequestParam away: String,
    ): Map<String, Any> {
        val homeWin = predictionService.homeWinProbability(home, away)
        return mapOf(
            "home" to home,
            "away" to away,
            "homeWinProb" to homeWin,
            "awayWinProb" to 1.0 - homeWin,
        )
    }
}
