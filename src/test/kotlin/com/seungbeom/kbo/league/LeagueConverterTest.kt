package com.seungbeom.kbo.league

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * 리그 ↔ DB 값 변환. 목으로 돌리는 서비스 테스트는 이 경로를 절대 지나지 않아서,
 * 여기가 틀리면 **뜨고 나서** 순위표가 비는 식으로 드러난다.
 */
class LeagueConverterTest {

    private val converter = LeagueConverter()

    @Test
    fun `DB 에는 짧은 코드가 저장된다`() {
        // 이미 'KBO'·'AG' 로 들어가 있다. 이름(ASIAN_GAMES)이 저장되면 기존 행을 못 읽는다.
        assertEquals("KBO", converter.convertToDatabaseColumn(League.KBO))
        assertEquals("AG", converter.convertToDatabaseColumn(League.ASIAN_GAMES))
    }

    @Test
    fun `저장한 값을 그대로 되읽는다`() {
        for (league in League.entries) {
            assertEquals(league, converter.convertToEntityAttribute(converter.convertToDatabaseColumn(league)))
        }
    }

    @Test
    fun `null 은 null 로 흘려보낸다`() {
        assertNull(converter.convertToDatabaseColumn(null))
        assertNull(converter.convertToEntityAttribute(null))
    }

    @Test
    fun `모르는 코드는 터뜨린다`() {
        // 우리가 안 쓰는 리그가 DB 에 있다는 뜻이다. 조용히 null·KBO 로 떨어뜨리면
        // 그 경기가 KBO 집계에 섞이거나 사라진 이유를 못 찾는다.
        assertFailsWith<IllegalArgumentException> { converter.convertToEntityAttribute("WBC") }
        assertFailsWith<IllegalArgumentException> { League.of("ASIAN_GAMES") }
    }
}
