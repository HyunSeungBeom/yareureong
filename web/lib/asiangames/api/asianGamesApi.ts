import { get } from "@/lib/api";
import type { AsianGame } from "../model/types";

/** 대회 전체 일정. 스무 경기 남짓이라 한 번에 받고 화면에서 «오늘/다음» 을 고른다. */
export const getAsianGames = () => get<AsianGame[]>("/api/asian-games");
