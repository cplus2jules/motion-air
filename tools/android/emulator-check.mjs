// Runs the installed Android test APK against a disposable pinned-TLS bridge
// and a real UDP receiver. FORCE_LOG prevents native keyboard injection.
import { spawn, execFileSync } from 'node:child_process';
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { once } from 'node:events';
import { setTimeout as sleep } from 'node:timers/promises';
import dgram from 'node:dgram';
import net from 'node:net';
import assert from 'node:assert/strict';
import { startPairingServer } from '../../server/pairing.js';
import { encodeDataRequest, decodeResponse, MSG } from '../../server/dsu/packets.js';

const root = fileURLToPath(new URL('../../', import.meta.url));
async function freePort(udp = false) {
  const socket = udp ? dgram.createSocket('udp4') : net.createServer();
  if (udp) socket.bind(0, '127.0.0.1'); else socket.listen(0, '127.0.0.1');
  await once(socket, 'listening');
  const port = socket.address().port;
  await new Promise(resolve => socket.close(resolve));
  return port;
}
async function run(command, args, options = {}) {
  const child = spawn(command, args, { stdio: 'inherit', ...options });
  const [code] = await once(child, 'exit');
  assert.equal(code, 0, `${command} failed`);
}

// Compile before creating the one-use QR so a cold Gradle build cannot
// consume the invitation's five-minute lifetime.
const gradle = process.env.MOTION_AIR_GRADLE || resolve(root, 'android/gradlew');
await run(gradle, ['--no-daemon', 'assembleDebug', 'assembleDebugAndroidTest'], { cwd: join(root, 'android') });
const identity = await mkdtemp(join(tmpdir(), 'motion-air-android-'));
const port = await freePort(), dsuPort = await freePort(true);
let bridge, pairing, receiver, subscription;
let log = '';
try {
  bridge = spawn(process.execPath, ['server/index.js'], {
    cwd: root,
    env: { ...process.env, FORCE_LOG: '1', JOYPAD_LANG: 'en', JOYPAD_BIND_HOST: '127.0.0.1',
      PORT: String(port), DSU_HOST: '127.0.0.1', DSU_PORT: String(dsuPort), JOYPAD_STRICT_PORTS: '1' },
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  bridge.stdout.on('data', data => { log += data; });
  bridge.stderr.on('data', data => { log += data; });
  for (let attempt = 0; !log.includes('server running'); attempt++) {
    assert.ok(attempt < 150 && bridge.exitCode === null, 'Test bridge did not start');
    await sleep(100);
  }
  pairing = await startPairingServer({ directory: identity, httpsPort: 0, setupPort: 0,
    upstreamPort: port, hosts: ['10.0.2.2'], advertise: false });
  const { invitation } = await (await fetch(`${pairing.setupURL}api/state`)).json();
  receiver = dgram.createSocket('udp4');
  const packets = [];
  receiver.on('message', bytes => {
    const packet = decodeResponse(bytes);
    if (packet?.type === MSG.DATA) packets.push(packet);
  });
  receiver.connect(dsuPort, '127.0.0.1');
  await once(receiver, 'connect');
  subscription = setInterval(() => receiver.send(encodeDataRequest(0, 0)), 30);
  await run(gradle, ['--no-daemon', 'connectedDebugAndroidTest',
    '-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true',
    `-Pandroid.testInstrumentationRunnerArguments.pairingInvitation=${invitation}`], {
    cwd: join(root, 'android'),
  });
  await sleep(400);
  assert.ok(packets.length >= 10, `Only ${packets.length} DSU frames arrived`);
  assert.ok(packets.every(packet => packet.crcOk), 'Invalid DSU checksum');
  assert.ok(packets.every((packet, index) => !index || packet.tsUs > packets[index - 1].tsUs),
    'Motion timestamps must increase across background and reconnect');
  const status = await (await fetch(`http://127.0.0.1:${port}/status`)).json();
  assert.equal(status.dsu.slots[0], null, 'Backgrounded app left live motion behind');
  console.log(`Android pairing, motion, pause, background and reconnect passed: ${packets.length} ordered DSU frames.`);
} finally {
  const screenshots = join(root, 'android/app/build/reports/emulator-screenshots');
  await mkdir(screenshots, { recursive: true });
  const adb = process.env.ANDROID_HOME ? join(process.env.ANDROID_HOME, 'platform-tools/adb') : 'adb';
  for (const name of ['welcome', 'practice', 'motion-preview', 'controller-offline', 'welcome-es', 'controller', 'dance-lock']) {
    try {
      const png = execFileSync(adb, ['exec-out', 'run-as', 'com.motionair.controller.debug', 'cat', `files/qa-${name}.png`],
        { timeout: 5000, maxBuffer: 8 * 1024 * 1024, stdio: ['ignore', 'pipe', 'ignore'] });
      if (png.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))) await writeFile(join(screenshots, `${name}.png`), png);
    } catch { /* A test that failed before this screen has no screenshot. */ }
  }
  clearInterval(subscription);
  receiver?.close();
  if (pairing) await pairing.close();
  if (bridge && bridge.exitCode === null) {
    const exited = once(bridge, 'exit');
    bridge.kill('SIGTERM');
    await exited;
  }
  await rm(identity, { recursive: true, force: true });
}
