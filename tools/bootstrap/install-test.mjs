import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, readFile, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { dependencyFingerprint, installDependencies } from './install.mjs';

async function fixture(t) {
  const directory = await mkdtemp(join(tmpdir(), 'Motion Air [á] '));
  t.after(() => rm(directory, { recursive: true, force: true }));
  await writeFile(join(directory, 'package.json'), '{"name":"fixture"}');
  await writeFile(join(directory, 'package-lock.json'), '{"lockfileVersion":3}');
  await mkdir(join(directory, '.local'), { recursive: true });
  await writeFile(join(directory, '.local', 'pairing.json'), 'keep saved pairing');
  let calls = 0;
  const options = { directory, npmCLI: join(directory, 'runtime', 'npm-cli.js'), probe: async () => true,
    log: () => {}, execute: async (program, args, options) => {
      calls++;
      assert.equal(program, process.execPath);
      assert.equal(options.cwd, directory);
      assert.equal(args[0], join(directory, 'runtime', 'npm-cli.js'));
      assert.equal(args[1], 'ci');
      assert.equal(args.at(-1), join(directory, '.local', 'npm-cache'));
    } };
  return { directory, options, calls: () => calls };
}

test('unchanged setup skips npm; an updated lockfile reinstalls even when imports work', async t => {
  const f = await fixture(t);
  assert.equal(await installDependencies(f.options), true);
  assert.equal(await installDependencies(f.options), false);
  await writeFile(join(f.directory, 'package-lock.json'), '{"lockfileVersion":3,"changed":true}');
  assert.equal(await installDependencies(f.options), true);
  assert.equal(f.calls(), 2);
  assert.equal(await readFile(join(f.directory, '.local', 'pairing.json'), 'utf8'), 'keep saved pairing');
});

test('moving a folder to another OS, CPU or Node ABI requires matching native modules', () => {
  const original = { platform: 'darwin', arch: 'arm64', abi: '127' };
  const baseline = dependencyFingerprint('manifest', 'lock', original);
  for (const changed of [{ platform: 'win32' }, { arch: 'x64' }, { abi: '137' }]) {
    assert.notEqual(dependencyFingerprint('manifest', 'lock', { ...original, ...changed }), baseline);
  }
  assert.notEqual(dependencyFingerprint('new manifest', 'lock', original), baseline);
});

test('missing modules are repaired even when the installation stamp matches', async t => {
  const f = await fixture(t);
  await installDependencies(f.options);
  let probes = 0;
  assert.equal(await installDependencies({ ...f.options, probe: async () => ++probes > 1 }), true);
  assert.equal(f.calls(), 2);
});

test('failed npm install clears the success stamp and can be retried', async t => {
  const f = await fixture(t);
  await installDependencies(f.options);
  await writeFile(join(f.directory, 'package-lock.json'), 'updated lock');
  await assert.rejects(installDependencies({ ...f.options, execute: async () => { throw new Error('network interrupted'); } }), /network interrupted/);
  await assert.rejects(readFile(join(f.directory, 'node_modules', '.motion-air-install.json')), { code: 'ENOENT' });
  assert.equal(await installDependencies(f.options), true);
});

test('a native library load failure never marks setup successful', async t => {
  const f = await fixture(t);
  await assert.rejects(installDependencies({ ...f.options, probe: async () => false }), /control library could not load/);
  await assert.rejects(readFile(join(f.directory, 'node_modules', '.motion-air-install.json')), { code: 'ENOENT' });
  assert.equal(await installDependencies(f.options), true);
});
