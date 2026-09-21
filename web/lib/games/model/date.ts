/**
 * 경기 날짜 표기. **경기 날짜를 소유한 도메인이 여기라서** 다른 도메인도 이걸 쓴다
 * (`lib/asiangames`). 화면마다 제 나름대로 포맷하면 같은 날짜가 다르게 보인다.
 */

/** KST 기준 오늘(YYYY-MM-DD). 브라우저가 어느 시간대에 있든 한국 날짜로 본다. */
export function seoulToday(now: Date = new Date()): string {
  /* 로케일 형식(`en-CA` 가 "2026-09-21" 을 준다)에 기대지 않는다 — 로케일 데이터가 바뀌면
     조용히 깨진다. 조각을 직접 꺼내 조립한다. */
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(now);
  const at = (type: Intl.DateTimeFormatPartTypes) => parts.find((p) => p.type === type)!.value;
  return `${at("year")}-${at("month")}-${at("day")}`;
}

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

/** "9월 21일 (월)" — 같은 시즌 안에서만 쓰므로 연도는 군더더기다. */
export function dayLabel(date: string): string {
  const [, month, day] = date.split("-").map(Number);
  /* 자정을 UTC 로 읽고 UTC 요일을 쓴다. 한쪽만 시간대를 붙이면 날짜가 하루 밀린다
     (`T00:00:00+09:00` 은 UTC 로 전날 15시다 — 실제로 하루 어긋났다). */
  const weekday = WEEKDAYS[new Date(`${date}T00:00:00Z`).getUTCDay()];
  return `${month}월 ${day}일 (${weekday})`;
}
