package com.seungbeom.kbo.team

import com.seungbeom.kbo.league.League
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
    val league: League = League.KBO,
)

