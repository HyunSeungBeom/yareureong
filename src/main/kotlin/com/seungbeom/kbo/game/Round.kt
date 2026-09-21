package com.seungbeom.kbo.game

/**
 * 대회 라운드. 페넌트레이스인 KBO 는 라운드 개념이 없어 null 이고, 국제대회는 이 값이 없으면
 * "9월 27일 한국 vs 일본" 이 결승인지 순위결정전인지 알 수 없다.
 *
 * **출처 코드(`agbaseball_group` …)를 그대로 쓰지 않는다** — 출처가 바뀌어도 DB·화면은 그대로여야
 * 하고, 모르는 코드는 null 로 흘려보내 화면이 안 깨지게 한다.
 *
 * 이름이 그대로 저장되므로([jakarta.persistence.Enumerated]) 이름을 바꾸면 마이그레이션이 필요하다.
 */
enum class Round {
    GROUP,
    SUPER_ROUND,
    PLACEMENT,
    BRONZE,
    FINAL,
}
