"use client";

import { useQuery } from "@tanstack/react-query";
import { getAsianGames } from "../api/asianGamesApi";
import { asianGamesKeys } from "../api/keys";

export function useAsianGames() {
  return useQuery({
    queryKey: asianGamesKeys.schedule(),
    queryFn: getAsianGames,
  });
}
