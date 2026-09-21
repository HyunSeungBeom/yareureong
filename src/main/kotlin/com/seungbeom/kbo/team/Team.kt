package com.seungbeom.kbo.team

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * KBO 구단. id 는 약칭 코드(예: "OB" 두산).
 * 승/패/무는 저장하지 않는다 — 순위는 game(FINAL)에서 계산(single source of truth).
 * @see com.seungbeom.kbo.standings.Standings
 */
@Entity
@Table(name = "team")
class Team(
    @Id
    val id: String,
    val name: String,
    /** 소속 리그. KBO 순위표에는 KBO 팀만 오른다. */
    val league: String = League.KBO,
)

/**
 * 대회 구분.
 *
 * 한 테이블에 여러 대회를 담되 **집계는 절대 섞이지 않아야 한다** — KBO 순위·우승확률에
 * 국제대회 경기가 들어가면 에러 없이 숫자만 틀린다. 그래서 조회마다 리그를 명시한다.
 */
object League {
    const val KBO = "KBO"

    /** 아시안게임 야구. 국가대표 경기라 승률·선발 기록이 쌓이지 않아 예측 대상이 아니다. */
    const val ASIAN_GAMES = "AG"
}
