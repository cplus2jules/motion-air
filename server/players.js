export const MAX_PLAYERS = 6;
export const PLAYER_NUMBERS = Object.freeze(Array.from({ length: MAX_PLAYERS }, (_, i) => i + 1));
export const DSU_SLOTS_PER_PORT = 4;

export function motionEndpoint(player, basePort = 26760) {
  if (!PLAYER_NUMBERS.includes(player)) throw new RangeError('Player must be 1–6.');
  return { port: basePort + Math.floor((player - 1) / DSU_SLOTS_PER_PORT), slot: (player - 1) % DSU_SLOTS_PER_PORT };
}
