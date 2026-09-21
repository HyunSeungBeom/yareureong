package com.seungbeom.kbo.asiangames

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class AsianGamesController(
    private val asianGamesService: AsianGamesService,
) {
    /**
     * 대회 전체 일정. GET /api/asian-games
     *
     * 한 대회가 스무 경기 남짓이라 **한 번에 다 내려준다** — 화면이 «오늘 / 다음 경기» 를
     * 고르는 데 왕복을 더 하지 않게 하려는 것.
     */
    @GetMapping("/asian-games")
    fun schedule(): List<AsianGameView> = asianGamesService.schedule()
}
