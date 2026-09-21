/** queryKey 팩토리 — 리터럴 배열을 흩뿌리지 않는다. */
export const asianGamesKeys = {
  all: ["asian-games"] as const,
  schedule: () => [...asianGamesKeys.all, "schedule"] as const,
};
