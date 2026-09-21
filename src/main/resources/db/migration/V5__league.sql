-- 국제대회(아시안게임 등) 경기를 같은 테이블에 담기 위한 리그 구분.
--
-- ⚠️ 이 컬럼이 없으면 «대한민국 vs 대만» 이 KBO 순위표와 몬테카를로 시뮬레이션에 섞여 들어간다
--    (에러 없이 숫자만 틀린다). 기존 조회는 전부 league = 'KBO' 로 좁힌다.
ALTER TABLE team ADD COLUMN league VARCHAR(20) NOT NULL DEFAULT 'KBO';
ALTER TABLE game ADD COLUMN league VARCHAR(20) NOT NULL DEFAULT 'KBO';

CREATE INDEX idx_game_league_date ON game (league, game_date);

-- 팀 id 는 리그 안에서만 유일하면 된다(KBO 의 'KT' 와 국가코드가 겹칠 일은 없지만,
-- 리그가 늘어나면 언제든 겹칠 수 있다). 지금은 team.id 가 전역 PK 라 그대로 두고,
-- 국가팀 id 는 접두사를 붙여 넣는다(예: 'AG-KR').
