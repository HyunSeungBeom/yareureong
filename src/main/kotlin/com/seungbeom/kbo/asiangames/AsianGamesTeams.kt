package com.seungbeom.kbo.asiangames

/**
 * 아시안게임 참가국의 팀 id 규칙.
 *
 * 국가 코드를 그대로 쓰면 KBO 구단 코드(`KT`·`LG`)와 부딪히므로 대회 접두사를 붙인다.
 * **수집(`ingest`)과 화면(`asiangames`)이 같은 규칙을 써야 하므로 여기 한 곳에서만 만든다** —
 * 양쪽에 접두사를 복제해 두면 한쪽만 바뀌는 날 경기가 팀을 못 찾는다.
 */
object AsianGamesTeams {
    const val ID_PREFIX = "AG-"

    fun id(countryCode: String): String = ID_PREFIX + countryCode

    fun codeOf(teamId: String): String = teamId.removePrefix(ID_PREFIX)
}
