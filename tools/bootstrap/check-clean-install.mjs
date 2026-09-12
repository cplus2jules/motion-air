// Runs the actual user launcher in an empty folder. No emulator, pairing state,
// developer Node installation or global npm cache is used by the launcher.
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { cp, mkdir, mkdtemp, readFile, rm, stat, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../../', import.meta.url));
const directory = await mkdtemp(join(tmpdir(), 'Motion Air [á] '));
const windows = process.platform === 'win32';
assert.ok(windows || process.platform === 'darwin', 'Run on macOS or Windows.');
const env = { ...process.env, PATH: windows
  ? `${process.env.SystemRoot}\\System32;${process.env.SystemRoot}\\System32\\WindowsPowerShell\\v1.0;${process.env.SystemRoot}`
  : '/usr/bin:/bin:/usr/sbin:/sbin' };

function launch(extraEnv = {}) {
  return new Promise((resolve, reject) => {
    const program = windows ? process.env.ComSpec : '/bin/bash';
    const args = windows ? ['/d', '/s', '/c', '""Motion Air.cmd" --install-only"'] : ['Motion Air.command', '--install-only'];
    const child = spawn(program, args, { cwd: directory, env: { ...env, ...extraEnv },
      windowsVerbatimArguments: windows, stdio: ['ignore', 'pipe', 'pipe'] });
    let output = '';
    child.stdout.on('data', data => { output += data; });
    child.stderr.on('data', data => { output += data; });
    child.once('error', reject);
    child.once('exit', code => code === 0 ? resolve(output) : reject(new Error(`Launcher exited ${code}:\n${output}`)));
  });
}

try {
  await mkdir(join(directory, 'tools'), { recursive: true });
  for (const name of ['package.json', 'package-lock.json', 'Motion Air.command', 'Motion Air.cmd']) {
    await cp(join(root, name), join(directory, name));
  }
  await cp(join(root, 'tools', 'bootstrap'), join(directory, 'tools', 'bootstrap'), { recursive: true });
  await mkdir(join(directory, '.local'), { recursive: true });
  await writeFile(join(directory, '.local', 'saved-pairing-fixture'), 'unchanged');
  console.log('Checking that a runtime with the wrong checksum is rejected...');
  const manifestPath = join(directory, 'tools', 'bootstrap', 'node-runtimes.csv');
  const manifest = await readFile(manifestPath, 'utf8');
  await writeFile(manifestPath, manifest.replace(/,[a-f0-9]{64},/g, `,${'0'.repeat(64)},`));
  await assert.rejects(launch(), /download verification failed/);
  await assert.rejects(stat(join(directory, 'node_modules')), { code: 'ENOENT' });
  await writeFile(manifestPath, manifest);
  console.log('Checking two simultaneous first launches without Node.js on PATH...');
  const attempts = await Promise.allSettled([launch(), launch()]);
  for (const attempt of attempts) if (attempt.status === 'rejected') throw attempt.reason;
  const first = attempts.map(attempt => attempt.value);
  assert.equal(first.filter(output => output.includes('Setup complete.')).length, 1, first.join('\n'));
  assert.equal(first.filter(output => output.includes('Using the dependencies already installed')).length, 1, first.join('\n'));
  const stamp = join(directory, 'node_modules', '.motion-air-install.json');
  const installedAt = (await stat(stamp)).mtimeMs;
  assert.ok(JSON.parse(await readFile(stamp, 'utf8')).fingerprint);
  const offline = await launch({ HTTPS_PROXY: 'http://127.0.0.1:1', HTTP_PROXY: 'http://127.0.0.1:1',
    npm_config_registry: 'http://127.0.0.1:1', npm_config_offline: 'true' });
  assert.match(offline, /Using the dependencies already installed/);
  assert.equal((await stat(stamp)).mtimeMs, installedAt);
  console.log('Cached relaunch passed with downloads blocked. Checking dependency repair...');
  await rm(join(directory, 'node_modules', 'ws'), { recursive: true });
  assert.match(await launch(), /Setup complete\./);
  assert.equal(await readFile(join(directory, '.local', 'saved-pairing-fixture'), 'utf8'), 'unchanged');
  console.log('PASS: checksum rejection, first install, simultaneous launch, offline relaunch, dependency repair and saved data preservation.');
} finally {
  await rm(directory, { recursive: true, force: true });
}
