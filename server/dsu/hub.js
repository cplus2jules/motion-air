import { createDsuServer } from './server.js';
import { MAX_PLAYERS, DSU_SLOTS_PER_PORT } from '../players.js';

// One bridge serves six phones. Standard DSU endpoints each have four slots.
export function createDsuHub({ port = 26760, ...options } = {}) {
  const endpoints = Array.from({ length: Math.ceil(MAX_PLAYERS / DSU_SLOTS_PER_PORT) },
    (_, i) => createDsuServer({ ...options, port: port + i }));
  const route = (slot, method, ...args) => {
    if (!Number.isInteger(slot) || slot < 0 || slot >= MAX_PLAYERS) return;
    return endpoints[Math.floor(slot / DSU_SLOTS_PER_PORT)][method](slot % DSU_SLOTS_PER_PORT, ...args);
  };
  return {
    updateSlot: (...args) => route(args[0], 'updateSlot', ...args.slice(1)),
    updateControls: (...args) => route(args[0], 'updateControls', ...args.slice(1)),
    quiesceSlot: slot => route(slot, 'quiesceSlot'),
    clearSlot: slot => route(slot, 'clearSlot'),
    status() {
      const states = endpoints.map(endpoint => endpoint.status());
      return { ...states[0], listening: states.every(s => s.listening), endpoints: states,
        subscribers: states.reduce((sum, s) => sum + s.subscribers, 0),
        slots: Object.fromEntries(states.flatMap((s, i) => Object.entries(s.slots)
          .map(([slot, value]) => [Number(slot) + i * DSU_SLOTS_PER_PORT, value]))),
        receivers: Object.fromEntries(states.flatMap((s, i) => Object.entries(s.receivers)
          .map(([slot, value]) => [Number(slot) + i * DSU_SLOTS_PER_PORT, value]))),
      };
    },
    close() { endpoints.forEach(endpoint => endpoint.close()); },
  };
}
