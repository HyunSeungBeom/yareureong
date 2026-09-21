package com.seungbeom.kbo.league

/**
 * 대회 구분.
 *
 * 한 테이블에 여러 대회를 담되 **집계는 절대 섞이지 않아야 한다** — KBO 순위·우승확률에
 * 국제대회 경기가 들어가면 에러 없이 숫자만 틀린다. 그래서 조회마다 리그를 명시한다.
 *
 * **문자열이 아니라 타입인 이유**: 예전에는 `"KBO"` 상수였는데, 그러면 엉뚱한 리그로 조회해도
 * 컴파일이 통과하고 «빈 결과 + 0승 0패» 가 조용히 나온다(실제로 변형 테스트에서 살아남았다).
 * 타입이면 애초에 못 쓴다.
 *
 * [code] 는 DB 에 저장되는 값이다. 이름과 따로 두어 `ASIAN_GAMES` 라는 읽히는 이름을 지키면서
 * 컬럼에는 짧은 `AG` 를 넣는다(이미 그렇게 저장돼 있어 마이그레이션도 필요 없다).
 */
enum class League(val code: String) {
    KBO("KBO"),

    /** 아시안게임 야구. 국가대표 경기라 승률·선발 기록이 쌓이지 않아 예측 대상이 아니다. */
    ASIAN_GAMES("AG"),

    ;

    companion object {
        private val BY_CODE = entries.associateBy { it.code }

        /** 모르는 값이면 예외. DB 에 우리가 안 쓰는 리그가 들어왔다는 뜻이라 조용히 넘기면 안 된다. */
        fun of(code: String): League =
            BY_CODE[code] ?: throw IllegalArgumentException("알 수 없는 리그 코드: $code")
    }
}
