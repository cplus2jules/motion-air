import { readFileSync } from 'node:fs';

// Served only by the loopback pairing service.
export const pairingPage = readFileSync(new URL('../public/pairing.html', import.meta.url), 'utf8');
