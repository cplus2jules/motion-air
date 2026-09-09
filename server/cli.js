#!/usr/bin/env node
// Punto de entrada del paquete (npx motion-air / npx github:cplus2jules/motion-air).
//
//   motion-air                      → arranca el server (mando + DSU + /setup)
//   motion-air ryujinx-setup [...]  → configura Ryujinx (perfiles + Config.json)
//                                     flags: --check --restore --sideways --motion --patched

const [cmd, ...rest] = process.argv.slice(2);

if (cmd === "ryujinx-setup") {
  process.argv = [process.argv[0], process.argv[1], ...rest];
  import("../tools/ryujinx-setup.mjs");
} else {
  import("./index.js");
}
