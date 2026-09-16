#!/usr/bin/env node
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { setTimeout as sleep } from 'node:timers/promises';
import { startPairingServer } from '../server/pairing.js';
import { inspectRyujinx } from '../server/ryujinx.js';
import { createLauncherUI } from './launcher-ui.mjs';
import { checkDesktopLaunch, runningPairing, openDesktop } from './paired-desktop.mjs';
import { prepareWindowsDesktop } from './windows-desktop.mjs';

const root = fileURLToPath(new URL('../', import.meta.url));
let configDir = process.env.RYUJINX_CONFIG_DIR || fileURLToPath(new URL('../.local/ryujinx-motion-data', import.meta.url));
let desktop;
const spanish = /^es(?:[-_]|$)/i.test(process.env.JOYPAD_LANG || '');
const say = (en, es) => console.log(spanish ? es : en);
const launchDesktop = process.argv.includes('--launch');
const launcherUI = createLauncherUI({ spanish });
if (launchDesktop) launcherUI.start();
function port(name, fallback) {
  const value = Number(process.env[name] || fallback);
  if (!Number.isInteger(value) || value < 1024 || value > 65535) throw new Error(`${name}: expected port 1024–65535`);
  return value;
}
let child, pairing, stopped = false;
async function stop(code = 0) {
  if (stopped) return;
  stopped = true;
  await pairing?.close();
  if (child && child.exitCode === null) {
    child.kill('SIGTERM');
    await Promise.race([new Promise(resolve => child.once('exit', resolve)), sleep(2000)]);
    if (child.exitCode === null) child.kill('SIGKILL');
  }
  if (launchDesktop && code === 0) launcherUI.stopped();
  process.exitCode = code;
}
process.once('SIGINT', () => void stop());
process.once('SIGTERM', () => void stop());
process.once('SIGHUP', () => void stop());

try {
  if (launchDesktop) launcherUI.step('Checking the local dance profile…', 'Revisando el perfil local de baile…');
  const upstreamPort = port('PAIRING_BRIDGE_PORT', 3001);
  const dsuPort = port('PAIRING_DSU_PORT', 26760);
  if (dsuPort > 65534) throw new Error('DSU needs two consecutive UDP ports, at most 65534–65535.');
  const httpsPort = port('PAIRING_HTTPS_PORT', 3443);
  const setupPort = port('PAIRING_SETUP_PORT', 3444);
  if (new Set([upstreamPort, httpsPort, setupPort]).size !== 3) throw new Error('HTTP, setup and internal bridge ports must differ.');
  if (launchDesktop && process.platform === 'win32') {
    desktop = await prepareWindowsDesktop({ dsuPort, reset: process.argv.includes('--setup') });
    configDir = desktop.configDir;
  }
  if (process.env.FORCE_LOG !== '1' && !inspectRyujinx(configDir, { preset:'just-dance', dsuPort, playerCount: 6, controllerInput: true }).synced) {
    throw new Error(spanish ? 'Falta el perfil local de Just Dance. Consulta docs/motion-implementation-status.md.' : 'The isolated Just Dance profile is missing or changed. See docs/motion-implementation-status.md.');
  }
  if (launchDesktop) {
    await checkDesktopLaunch();
    if (stopped) throw new Error('Startup was interrupted.');
    if (await runningPairing({ setupPort, upstreamPort, httpsPort, dsuPort })) {
      if (stopped) throw new Error('Startup was interrupted.');
      say('Motion Air is already running. Reusing the bridge in its original launcher window.',
        'Motion Air ya está activo. Se usará el puente de su ventana original del lanzador.');
      await openDesktop(`http://127.0.0.1:${setupPort}/`, { desktop });
      launcherUI.ready(`http://127.0.0.1:${setupPort}/`, true);
      process.exit(0);
    }
    if (stopped) throw new Error('Startup was interrupted.');
  }
  if (launchDesktop) launcherUI.step('Starting your controller bridge…', 'Iniciando el puente del mando…');
  let ready = false, startupTail = '';
  child = spawn(process.execPath, ['server/index.js'], {
    cwd: root,
    env: { ...process.env, PORT:String(upstreamPort), DSU_PORT:String(dsuPort), DSU_HOST:'127.0.0.1', DSU_OFF:'0', DSU_CONTROLS:'1',
      JOYPAD_PARENT_PID:String(process.pid), JOYPAD_BIND_HOST:'127.0.0.1', JOYPAD_QUIET_STARTUP:'1', JOYPAD_STRICT_PORTS:'1', RYUJINX_CONFIG_DIR:configDir },
    stdio: ['ignore','pipe','pipe'],
  });
  child.on('error', error => { console.error(error.message); void stop(1); });
  child.stdout.on('data', bytes => {
    const message = bytes.toString();
    startupTail = (startupTail + message).slice(-4096);
    if (startupTail.includes(`Internal bridge listening on 127.0.0.1:${upstreamPort}`)) ready = true;
    process.stdout.write(bytes);
  });
  child.stderr.pipe(process.stderr);
  child.on('exit', code => { if (!stopped) { console.error(`[pairing] Internal bridge stopped (${code}).`); void stop(1); } });
  for (let i=0; i<100 && !ready && !stopped; i++) await sleep(100);
  if (!ready || stopped) throw new Error('Internal bridge could not start. Stop the previous Motion Air server before starting paired mode.');
  // Check the exact child-selected port; never relay to an unrelated old server.
  const status = await (await fetch(`http://127.0.0.1:${upstreamPort}/status`, {signal:AbortSignal.timeout(3000)})).json();
  if (status.app !== 'joypad-air' || !status.dsu?.listening) throw new Error('Internal motion bridge is not ready.');
  if (launchDesktop) launcherUI.step('Preparing secure phone pairing…', 'Preparando la conexión segura del teléfono…');
  pairing = await startPairingServer({
    directory: process.env.JOYPAD_PAIRING_DIR || fileURLToPath(new URL('../.local/pairing', import.meta.url)),
    httpsPort, setupPort, upstreamPort, advertise: process.env.JOYPAD_BONJOUR !== '0',
  });
  if (stopped) { await pairing.close(); throw new Error('Pairing startup was interrupted.'); }
  say(`Open pairing on this computer: ${pairing.setupURL}`, `Abre el enlace de emparejamiento en este equipo: ${pairing.setupURL}`);
  say('Scan its QR in Motion Air on your phone. Saved phones reconnect securely. Leave this window open.',
    'Escanea el QR desde Motion Air en el teléfono. Los teléfonos guardados se reconectan de forma segura. Deja esta ventana abierta.');
  if (launchDesktop) {
    launcherUI.step('Opening pairing and Ryujinx…', 'Abriendo el emparejamiento y Ryujinx…');
    await openDesktop(pairing.setupURL, { desktop });
    launcherUI.ready(pairing.setupURL);
  }
} catch (error) {
  if (launchDesktop) launcherUI.error(error.message);
  else console.error(`[pairing] ${error.message}`);
  await stop(1);
}
