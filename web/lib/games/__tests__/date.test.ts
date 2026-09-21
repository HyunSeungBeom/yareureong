import { afterEach, describe, expect, it } from "vitest";
import { dayLabel, seoulToday } from "..";

/** 경기 날짜 표기 — 일정 화면과 아시안게임 보드가 같은 함수를 쓴다. */

describe("DATE 001: 날짜 라벨", () => {
  it("연도 없이 요일까지 보여준다", () => {
    expect(dayLabel("2026-09-21")).toBe("9월 21일 (월)");
    expect(dayLabel("2026-09-27")).toBe("9월 27일 (일)");
  });

  it("요일이 하루 밀리지 않는다", () => {
    // `T00:00:00+09:00` 으로 읽으면 UTC 로 전날 15시라 요일이 하루 당겨진다(실제로 밟았다).
    expect(dayLabel("2026-01-01")).toBe("1월 1일 (목)");
    expect(dayLabel("2026-12-31")).toBe("12월 31일 (목)");
  });
});

describe("DATE 002: 한국 기준 오늘", () => {
  it("브라우저 시간대가 아니라 한국 날짜다", () => {
    // 2026-09-21 09:00 UTC = 18:00 KST 같은 날, 16:00 UTC = 22일 01:00 KST
    expect(seoulToday(new Date("2026-09-21T09:00:00Z"))).toBe("2026-09-21");
    expect(seoulToday(new Date("2026-09-21T16:00:00Z"))).toBe("2026-09-22");
  });

  it("자정 경계에서도 한 자리 월·일을 0 으로 채운다", () => {
    // "2026-1-5" 같은 값이 나오면 날짜 비교(문자열)가 조용히 어긋난다.
    expect(seoulToday(new Date("2026-01-04T15:00:00Z"))).toBe("2026-01-05");
  });

  it("한국 밖 브라우저에서도 같은 날짜를 준다", () => {
    /* 테스트는 TZ=Asia/Seoul 로 돌아서 «시간대를 안 넘겨도» 통과해 버린다 —
       이 함수의 존재 이유가 그 경우라 여기서만 시간대를 바꿔 본다. */
    process.env.TZ = "America/New_York";

    // 뉴욕은 아직 9/21 12:00 인데, 한국은 이미 9/22 다.
    expect(seoulToday(new Date("2026-09-21T16:00:00Z"))).toBe("2026-09-22");
  });
});

afterEach(() => {
  process.env.TZ = "Asia/Seoul";
});
