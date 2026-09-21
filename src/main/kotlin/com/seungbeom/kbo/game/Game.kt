package com.seungbeom.kbo.game

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import com.seungbeom.kbo.team.League
import java.time.LocalDate

enum class GameStatus { SCHEDULED, FINAL }

/**
 * 한 경기. 예정(SCHEDULED)일 땐 점수가 null, 종료(FINAL)면 채워진다.
 * 스크래퍼가 매일 이 테이블을 upsert 하고, 프론트/시뮬레이터는 여기만 읽는다.
 */
@Entity
@Table(name = "game")
class Game(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val gameDate: LocalDate,
    val homeTeamId: String,
    val awayTeamId: String,

    var homeScore: Int? = null,
    var awayScore: Int? = null,

    @Enumerated(EnumType.STRING)
    var status: GameStatus = GameStatus.SCHEDULED,

    /** 선발 투수. 예정 경기는 발표 전이라 null 일 수 있고, 경기 당일 바뀌기도 한다. */
    var homeStartPitcher: String? = null,
    var awayStartPitcher: String? = null,

    /** "18:30" — 오늘 경기 카드에 표시. */
    var startTime: String? = null,
    var stadium: String? = null,

    /** 출처 경기 id. upsert 키 — 더블헤더를 서로 다른 경기로 구분한다. */
    var externalId: String? = null,

    /**
     * 어느 대회의 경기인가. KBO 순위·우승확률 집계는 이 값이 [League.KBO] 인 것만 본다 —
     * 국제대회 경기가 섞이면 에러 없이 숫자만 틀어진다.
     *
     * 기존 호출부를 건드리지 않으려고 **맨 뒤에** 둔다(위치 인자로 쓰는 코드가 많다).
     */
    val league: String = League.KBO,
)
