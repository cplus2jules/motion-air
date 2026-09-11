import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
import { prepareWindowsDesktop, detectWindowsConfig } from './windows-desktop.mjs';
import { configureRyujinx } from '../server/ryujinx.js';
import { windowsRunning, windowsAction } from '../server/windows.js';
import { openDesktop } from './paired-desktop.mjs';

function fixture(t) {
  const root = mkdtempSync(join(tmpdir(), 'motion windows & '));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  const executable = join(root, 'Ryujinx.exe'); writeFileSync(executable, 'fixture');
  const appdata = join(root, 'AppData'); const configDir = join(appdata, 'Ryujinx');
  mkdirSync(configDir, { recursive: true });
  const config = { version: 70, game_dirs: ['D:\\Games'], graphics_backend: 'Vulkan', input_config: [] };
  writeFileSync(join(configDir, 'Config.json'), JSON.stringify(config));
  return { root, executable, configDir, env: { APPDATA: appdata }, config };
}
test('first Windows launch chooses existing emulator, backs up settings, and later launches need no picker', async t => {
  const f = fixture(t); let selections = 0;
  const options = { directory: f.root, env: f.env, log() {}, select: async () => { selections++; return f.executable; },
    configure: options => configureRyujinx(options, { isRunning: () => false }) };
  const desktop = await prepareWindowsDesktop(options);
  assert.equal(selections, 1); assert.equal(desktop.configDir, f.configDir);
  const config = JSON.parse(readFileSync(join(f.configDir, 'Config.json')));
  assert.deepEqual(config.game_dirs, f.config.game_dirs);
  assert.equal(config.input_config[0].controller_type, 'JoyconRight');
  assert.equal(config.input_config[0].motion.dsu_server_port, 26760);
  assert.deepEqual(await prepareWindowsDesktop({ ...options, select: () => assert.fail('unexpected picker'), configure: () => assert.fail('unexpected rewrite') }), desktop);
});
test('portable data takes precedence over roaming settings, even before its first config exists', t => {
  const f = fixture(t); assert.equal(detectWindowsConfig(f.executable, f.env), f.configDir);
  mkdirSync(join(f.root, 'portable'));
  assert.equal(detectWindowsConfig(f.executable, f.env), join(f.root, 'portable'));
});
test('a new motion build retains existing data and a later manual selection stays selected', async t => {
  const f = fixture(t);
  const options = { directory: f.root, env: f.env, log() {}, select: async () => f.executable,
    configure: options => configureRyujinx(options, { isRunning: () => false }) };
  await prepareWindowsDesktop(options);
  const buildDir = join(f.root, 'motion build'); mkdirSync(buildDir);
  const built = join(buildDir, 'Ryujinx.exe'); writeFileSync(built, 'fixture');
  // Windows PowerShell 5.1 writes UTF-8 JSON with a BOM.
  writeFileSync(join(f.root, 'windows-motion-build.json'), '\uFEFF' + JSON.stringify({ executable: built }));
  assert.deepEqual(await prepareWindowsDesktop({ ...options, select: () => assert.fail('unexpected picker') }),
    { executable: built, configDir: f.configDir });
  await prepareWindowsDesktop({ ...options, reset: true });
  assert.deepEqual(await prepareWindowsDesktop({ ...options, select: () => assert.fail('manual choice was forgotten') }),
    { executable: f.executable, configDir: f.configDir });
});
test('cancelled selection and an open emulator leave settings unchanged', async t => {
  const f = fixture(t); const before = readFileSync(join(f.configDir, 'Config.json'), 'utf8');
  await assert.rejects(prepareWindowsDesktop({ directory: f.root, env: f.env, select: async () => '', log() {} }), /No emulator selected/);
  await assert.rejects(prepareWindowsDesktop({ directory: f.root, env: { ...f.env, RYUJINX_EXE: f.executable }, log() {},
    configure: options => configureRyujinx(options, { isRunning: () => true }) }), /Quit Ryujinx/);
  assert.equal(readFileSync(join(f.configDir, 'Config.json'), 'utf8'), before);
});
test('Windows launch passes paths as data and opens pairing before the emulator', async () => {
  const calls = []; const executable = 'C:\\Games & Apps\\Ryujinx.exe';
  await openDesktop('http://127.0.0.1:3444/', { platform: 'win32', desktop: { executable, configDir: 'C:\\Users\\O\'Brien\\Ryujinx' },
    run: async (...args) => { calls.push(args); return { stdout: '' }; } });
  assert.equal(calls[0][1].at(-1), 'Browser'); assert.equal(calls[1][1].at(-1), 'Launch');
  assert.equal(calls[1][2].env.MOTION_AIR_EXE, executable);
  assert.ok(!calls[1][1].includes(executable));
  assert.equal(calls[1][2].windowsHide, true);
});
test('Windows process inspection fails closed on missing tools or malformed responses', () => {
  assert.equal(windowsRunning(() => 'true\r\n'), true); assert.equal(windowsRunning(() => 'false'), false);
  assert.throws(() => windowsRunning(() => '')); assert.throws(() => windowsRunning(() => { throw new Error('denied'); }));
});
test('real Windows helper can inspect processes and parse every action', { skip: process.platform !== 'win32' }, async () => {
  assert.equal(typeof windowsRunning(), 'boolean');
  assert.equal(typeof await windowsAction('Focus'), 'string');
});
