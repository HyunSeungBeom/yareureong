package com.seungbeom.kbo.asiangames

import com.seungbeom.kbo.game.GameStatus
import com.seungbeom.kbo.game.Round
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
