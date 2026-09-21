-- 대회 라운드(예선 / 슈퍼라운드 / 결승 …). KBO 는 페넌트레이스라 라운드 개념이 없어 NULL 이고,
-- 아시안게임은 이 값이 없으면 "9월 27일 한국 vs 일본" 이 결승인지 순위결정전인지 알 수 없다.
ALTER TABLE game ADD COLUMN round VARCHAR(20);
