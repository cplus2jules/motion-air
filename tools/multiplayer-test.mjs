import test from 'node:test';
import assert from 'node:assert/strict';
import { once } from 'node:events';
import { mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawn } from 'node:child_process';
import net from 'node:net';
import dgram from 'node:dgram';
import { setTimeout as sleep } from 'node:timers/promises';
import WebSocket from 'ws';
import { startPairingServer } from '../server/pairing.js';
import { decodeResponse, encodeDataRequest } from '../server/dsu/packets.js';
import { motionEndpoint } from '../server/players.js';

async function until(fn, label) {
  for (let i = 0; i < 500; i++) { const result = await fn(); if (result) return result; await sleep(10); }
  throw new Error(`Timed out: ${label}`);
}
async function ports() {
  const tcp = net.createServer(); tcp.listen(0, '127.0.0.1'); await once(tcp, 'listening');
  const upstream = tcp.address().port; await new Promise(r => tcp.close(r));
  for (;;) {
    const a = dgram.createSocket('udp4'), b = dgram.createSocket('udp4');
    a.bind(0, '127.0.0.1'); await once(a, 'listening'); const base = a.address().port;
    if (base === 65535) { a.close(); continue; }
    try { b.bind(base + 1, '127.0.0.1'); await once(b, 'listening'); a.close(); b.close(); return { upstream, base }; }
    catch { a.close(); b.close(); }
  }
}
test('six authenticated phones feed separate Joy-Cons; a seventh never evicts a player', { timeout: 30000 }, async t => {
  const { upstream, base } = await ports();
  const directory = await mkdtemp(join(tmpdir(), 'motion-air-six-'));
  const phones = [], receivers = []; let relay; let logs = '';
  const child = spawn(process.execPath, ['server/index.js'], { cwd: new URL('../', import.meta.url),
    env: { ...process.env, FORCE_LOG: '1', PORT: String(upstream), DSU_PORT: String(base), DSU_CONTROLS: '1', JOYPAD_BIND_HOST: '127.0.0.1', JOYPAD_QUIET_STARTUP: '1', JOYPAD_STRICT_PORTS: '1' }, stdio: ['ignore', 'pipe', 'pipe'] });
  child.stdout.on('data', d => { logs += d; }); child.stderr.on('data', d => { logs += d; });
  t.after(async () => {
    for (const p of phones) p.ws.terminate();
    for (const r of receivers) { clearInterval(r.tick); r.socket.close(); }
    await relay?.close();
    const exited = once(child, 'exit'); child.kill('SIGTERM'); await exited;
    await rm(directory, { recursive: true, force: true });
  });
  await until(() => logs.includes('Internal bridge listening'), 'bridge startup');
  relay = await startPairingServer({ directory, httpsPort: 0, setupPort: 0, upstreamPort: upstream, advertise: false, hosts: ['127.0.0.1'] });
  const status = async () => (await fetch(`http://127.0.0.1:${upstream}/status`)).json();
  function connect(token) {
    const ws = new WebSocket(`wss://127.0.0.1:${relay.httpsPort}/controller`, { ca: relay.identity.cert, checkServerIdentity: () => undefined, headers: { authorization: `Bearer ${token}` } });
    const phone = { ws, messages: [], code: null, token }; phones.push(phone);
    ws.on('message', d => phone.messages.push(JSON.parse(d))); ws.on('close', code => { phone.code = code; }); ws.on('error', () => {});
    return phone;
  }
  const send = (p, message) => p.ws.send(JSON.stringify(message));
  for (let n = 1; n <= 6; n++) {
    const claim = relay.authority.claim(relay.authority.current().code, `Phone ${n}`, `test-${n}`);
    assert.equal(claim.status, 200);
    const p = connect(claim.body.token);
    const hello = await until(() => p.messages.find(m => m.t === 'hello'), `player ${n}`);
    assert.equal(hello.player, n); assert.equal(hello.maxPlayers, 6); assert.equal(hello.inputBackend, 'dsu');
    assert.deepEqual(hello.motionEndpoint, motionEndpoint(n, base));
    send(p, { t: 'config', motionProfile: 'just-dance', orientation: 'portrait', motion: true });
    await until(() => p.messages.some(m => m.t === 'config-ack' && m.motion === true), `ack ${n}`);
    const endpoint = motionEndpoint(n, base), socket = dgram.createSocket('udp4');
    socket.connect(endpoint.port, '127.0.0.1'); await once(socket, 'connect');
    const receiver = { socket, frames: [], tick: setInterval(() => socket.send(encodeDataRequest(n, endpoint.slot)), 100) };
    socket.on('message', data => { const decoded = decodeResponse(data); if (decoded?.packetId) receiver.frames.push({ raw: data, ...decoded }); });
    receivers.push(receiver);
  }
  await until(async () => Object.values((await status()).dsu.receivers).filter(n => n === 1).length === 6, 'six independent DSU subscribers');
  const buttons = ['a', 'b', 'x', 'y', 'sl', 'sr'];
  for (let i = 0; i < 6; i++) {
    send(phones[i], { t: 'btn', k: buttons[i], d: true });
    send(phones[i], { t: 'stick', s: 'R', x: (i + 1) / 6, y: 0 });
    send(phones[i], { t: 'motion', ax: (i + 1) / 10, ay: 0, az: -1, gx: 10 + i, gy: 0, gz: 0, ts: 1000000, seq: 1 });
  }
  const expectedButtons = [32, 64, 16, 128, 0, 0];
  for (let i = 0; i < 6; i++) {
    const frame = await until(() => receivers[i].frames.find(f => f.raw[37] === expectedButtons[i] && f.raw[103] === (i < 4 ? 0 : i - 3) && Math.abs(f.ax - (i + 1) / 10) < 0.001), `isolated controls and motion ${i + 1}`);
    assert.equal(frame.slot, i % 4); assert.equal(frame.crcOk, true); assert.equal(frame.raw[42], Math.round(128 + (i + 1) / 6 * 127));
  }
  const seventhClaim = relay.authority.claim(relay.authority.current().code, 'Seventh phone', 'test-7');
  const seventh = connect(seventhClaim.body.token);
  await until(() => seventh.code, 'full server rejection'); assert.equal(seventh.code, 4003);
  assert.ok(phones.slice(0, 6).every(p => p.ws.readyState === WebSocket.OPEN));
  const duplicate = connect(phones[0].token);
  await until(() => duplicate.code, 'duplicate phone rejection'); assert.equal(duplicate.code, 4004);
  assert.equal(phones[0].ws.readyState, WebSocket.OPEN);
  const oldToken = phones[2].token; const closed = once(phones[2].ws, 'close'); phones[2].ws.close(); await closed;
  await until(async () => !(await status()).players[3].connected, 'player 3 released');
  await until(() => receivers[2].frames.at(-1)?.connected === 0 && receivers[2].frames.at(-1)?.raw[37] === 0, 'disconnect neutralizes only player 3');
  assert.equal(receivers[0].frames.at(-1).raw[37], 32);
  const rejoined = connect(oldToken);
  const hello = await until(() => rejoined.messages.find(m => m.t === 'hello'), 'player 3 rejoin'); assert.equal(hello.player, 3);
  assert.equal((await status()).players[3].motion, false);
  assert.equal((await status()).players[1].connected, true);
  const state = await (await fetch(relay.setupURL + 'api/state')).json();
  assert.deepEqual(state.paired.filter(p => p.connected).map(p => p.player).sort(), [1, 2, 3, 4, 5, 6]);
});
