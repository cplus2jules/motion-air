import test from 'node:test';
import assert from 'node:assert/strict';
import { setImmediate as settle } from 'node:timers/promises';
import { createFocusWatcher, parseFrontApp } from '../server/focus.js';

function watcher(t, options) {
  t.mock.timers.enable({ apis: ['setInterval'] });
  const focus = createFocusWatcher({ intervalMs: 100, ...options });
  t.after(() => focus.stop());
  return focus;
}

test('failed focus lookup invalidates a previously active emulator and recovers', async t => {
  let app = 'Ryujinx Motion';
  const changes = [];
  const focus = watcher(t, { readFrontApp: () => app, onChange: state => changes.push(state) });
  focus.start(); await settle();
  assert.equal(focus.last.ok, true);
  app = null; t.mock.timers.tick(100); await settle();
  assert.deepEqual(focus.last, { ok: null, app: null });
  app = 'Ryujinx Motion'; t.mock.timers.tick(100); await settle();
  assert.equal(focus.last.ok, true);
  assert.deepEqual(changes.map(state => state.ok), [true, null, true]);
});

test('new session immediately clears old focus and ignores its pending lookup', async t => {
  let resolveOld, reads = 0;
  const focus = watcher(t, { readFrontApp: () => ++reads === 1
    ? new Promise(resolve => { resolveOld = resolve; }) : 'Safari' });
  focus.start(); await settle();
  const refreshed = focus.refresh();
  assert.deepEqual(focus.last, { ok: null, app: null });
  resolveOld('Ryujinx'); await refreshed;
  assert.deepEqual(focus.last, { ok: false, app: 'Safari' });
});

test('idle watcher discards prior focus and skips OS queries', async t => {
  let active = true, reads = 0;
  const focus = watcher(t, { isActive: () => active, readFrontApp: () => { reads++; return 'Ryujinx'; } });
  focus.start(); await settle();
  active = false; t.mock.timers.tick(100); await settle();
  assert.equal(focus.last.ok, null);
  assert.equal(reads, 1);
  active = true; await focus.refresh();
  assert.equal(reads, 2);
  assert.equal(focus.last.ok, true);
});

test('stopping a watcher prevents pending results from restoring readiness', async t => {
  let finish;
  const focus = watcher(t, { readFrontApp: () => new Promise(resolve => { finish = resolve; }) });
  focus.start(); await settle(); focus.stop();
  finish('Ryujinx'); await settle();
  assert.equal(focus.last.ok, null);
});

test('lookup errors fail closed without overlapping polls', async t => {
  let finish, reads = 0;
  const focus = watcher(t, { readFrontApp: () => { reads++; return new Promise((_, reject) => { finish = reject; }); } });
  focus.start(); await settle();
  t.mock.timers.tick(500); await settle();
  assert.equal(reads, 1);
  finish(new Error('lookup failed')); await settle();
  assert.deepEqual(focus.last, { ok: null, app: null });
});

test('configured emulator names match case insensitively without repeated notifications', async t => {
  let count = 0;
  const focus = watcher(t, { match: 'dolphin', readFrontApp: () => ' DOLPHIN ', onChange: () => count++ });
  focus.start(); await settle();
  t.mock.timers.tick(100); await settle();
  assert.deepEqual(focus.last, { ok: true, app: 'DOLPHIN' });
  assert.equal(count, 1);
});

test('parseFrontApp extracts app name from modern and legacy lsappinfo output formats', () => {
  assert.equal(parseFrontApp('"Ryujinx 1.3.3+e2143d4-motion-multiplayer" ASN:0x0-0x282282: (in front)\n    bundleID=[ NULL ]'), 'Ryujinx 1.3.3+e2143d4-motion-multiplayer');
  assert.equal(parseFrontApp('"LSDisplayName"="Ryujinx"'), 'Ryujinx');
  assert.equal(parseFrontApp('"CFBundleName"="Ryujinx Motion"'), 'Ryujinx Motion');
  assert.equal(parseFrontApp('"Dia" ASN:0x0-0x281281: (in front)'), 'Dia');
  assert.equal(parseFrontApp(''), null);
  assert.equal(parseFrontApp(null), null);
});
