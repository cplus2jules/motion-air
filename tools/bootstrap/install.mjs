import { createHash } from 'node:crypto';
import { spawn } from 'node:child_process';
import { mkdir, readFile, rm, writeFile, rename } from 'node:fs/promises';
import { join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../../', import.meta.url));
const modules = ['express', 'ws', 'qrcode', 'selfsigned', '@nut-tree-fork/nut-js'];

export function dependencyFingerprint(manifest, lock, { platform = process.platform, arch = process.arch, abi = process.versions.modules } = {}) {
  return createHash('sha256').update(JSON.stringify([manifest, lock, platform, arch, abi])).digest('hex');
}

export function run(program, args, { cwd, quiet = false } = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(program, args, { cwd, stdio: quiet ? 'ignore' : 'inherit', windowsHide: true });
    let interrupted = false;
    const signals = process.platform === 'win32' ? ['SIGINT', 'SIGTERM'] : ['SIGINT', 'SIGTERM', 'SIGHUP'];
    const handlers = signals.map(signal => {
      const handler = () => { interrupted = true; child.kill(signal); };
      process.on(signal, handler);
      return [signal, handler];
    });
    const cleanup = () => handlers.forEach(([signal, handler]) => process.off(signal, handler));
    child.once('error', error => { cleanup(); reject(error); });
    child.once('exit', (code, signal) => {
      cleanup();
      if (code === 0 && !interrupted) resolve();
      else reject(Object.assign(new Error(`Setup command stopped (${signal || `exit ${code}`}).`), { interrupted }));
    });
  });
}

async function healthy(directory) {
  try {
    await run(process.execPath, ['--input-type=module', '--eval',
      `await Promise.all(${JSON.stringify(modules)}.map(name => import(name)))`], { cwd: directory, quiet: true });
    return true;
  } catch (error) {
    if (error.interrupted) throw error;
    return false;
  }
}

// Called under the platform launcher's OS file lock. A successful import alone
// cannot detect an outdated lockfile or native modules copied from another PC.
export async function installDependencies({ directory = root, npmCLI = process.env.MOTION_AIR_NPM_CLI,
  execute = run, probe = healthy, log = console.log, identity } = {}) {
  const manifest = await readFile(join(directory, 'package.json'), 'utf8');
  const lock = await readFile(join(directory, 'package-lock.json'), 'utf8');
  const fingerprint = dependencyFingerprint(manifest, lock, identity);
  const stamp = join(directory, 'node_modules', '.motion-air-install.json');
  let previous;
  try { previous = JSON.parse(await readFile(stamp, 'utf8')); } catch { /* First run or interrupted setup. */ }
  if (previous?.fingerprint === fingerprint && await probe(directory)) {
    log('Motion Air is ready. Using the dependencies already installed in this folder.');
    return false;
  }
  if (!npmCLI) throw new Error('Open Motion Air.command on Mac or Motion Air.cmd on Windows to finish setup.');
  log('Installing Motion Air dependencies. Please keep this window open.');
  await rm(stamp, { force: true });
  await execute(process.execPath, [npmCLI, 'ci', '--no-audit', '--no-fund', '--cache', join(directory, '.local', 'npm-cache')], { cwd: directory });
  if (!await probe(directory)) throw new Error('The computer control library could not load. Check your supported system version in README.md, then open Motion Air again.');
  await mkdir(join(directory, 'node_modules'), { recursive: true });
  const temporary = `${stamp}.tmp`;
  await writeFile(temporary, JSON.stringify({ fingerprint, node: process.versions.node }, null, 2) + '\n');
  await rename(temporary, stamp);
  log('Setup complete. Motion Air is ready.');
  return true;
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  installDependencies().catch(error => {
    console.error(`\n${error.message}\nCheck your internet connection and keep Motion Air in an extracted folder you can write to. Open the launcher again to retry.`);
    process.exitCode = 1;
  });
}
