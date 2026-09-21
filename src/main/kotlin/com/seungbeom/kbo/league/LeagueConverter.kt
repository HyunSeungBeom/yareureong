package com.seungbeom.kbo.league

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * [League] ↔ DB 문자열. `@Enumerated(STRING)` 을 쓰면 enum **이름** 이 그대로 저장돼
 * 컬럼 값이 `ASIAN_GAMES` 가 되는데, 이미 `AG` 로 들어가 있고 짧은 쪽이 낫다.
 * 이름은 코드에서 읽히게 두고 저장 값만 [League.code] 로 바꾼다.
 */
@Converter(autoApply = true)
class LeagueConverter : AttributeConverter<League, String> {
    override fun convertToDatabaseColumn(attribute: League?): String? = attribute?.code

    override fun convertToEntityAttribute(dbData: String?): League? = dbData?.let(League::of)
}
