import { existsSync, readFileSync, writeFileSync, mkdirSync, statSync, renameSync } from 'node:fs';
import { dirname, join, basename, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { configureRyujinx, inspectRyujinx } from '../server/ryujinx.js';
import { windowsAction } from '../server/windows.js';

const root = fileURLToPath(new URL('../', import.meta.url));
export function detectWindowsConfig(executable, env = process.env) {
  const portable = join(dirname(executable), 'portable');
  // Ryujinx chooses portable even when that folder has no config yet.
  if (existsSync(portable)) return portable;
  return env.APPDATA ? join(env.APPDATA, 'Ryujinx') : null;
}
export async function prepareWindowsDesktop({
  directory = join(root, '.local'), env = process.env, select = windowsAction,
  configure = configureRyujinx, dsuPort = 26760, reset = false, log = console.log,
} = {}) {
  const path = join(directory, 'windows-launcher.json');
  let saved = {};
  if (existsSync(path) && !reset) {
    try { saved = JSON.parse(readFileSync(path, 'utf8')); }
    catch { throw new Error('Saved Windows setup is unreadable. Open Motion Air.cmd --setup to choose Ryujinx again.'); }
  }
  const buildPath = join(directory, 'windows-motion-build.json');
  let motionBuild;
  if (existsSync(buildPath)) {
    try { motionBuild = JSON.parse(readFileSync(buildPath, 'utf8').replace(/^\uFEFF/, '')).executable; }
    catch { if (!reset) throw new Error('Saved motion build is unreadable. Open Motion Air.cmd --setup to select your emulator.'); }
  }
  // Offer a newly built emulator once; an explicit later selection must stick.
  const newMotionBuild = !reset && motionBuild !== saved.motionBuildSeen ? motionBuild : undefined;
  let executable = env.RYUJINX_EXE || newMotionBuild || saved.executable;
  if (!executable || !existsSync(executable)) {
    log('Choose your Ryujinx.exe in the file picker. Motion Air will remember it.');
    executable = await select('SelectExe');
  }
  if (!executable) throw new Error('No emulator selected. Open Motion Air again when ready.');
  executable = resolve(executable);
  if (!/^Ryujinx(?:\.Avalonia)?\.exe$/i.test(basename(executable)) || !statSync(executable).isFile()) {
    throw new Error('Select Ryujinx.exe or Ryujinx.Avalonia.exe.');
  }
  let configDir = env.RYUJINX_CONFIG_DIR || ((saved.executable === executable || executable === newMotionBuild) && saved.configDir) || detectWindowsConfig(executable, env);
  if (!configDir || !existsSync(join(configDir, 'Config.json'))) {
    log('Choose Config.json from File > Open Ryujinx Folder. Open Ryujinx once and quit it if no file exists yet.');
    const configFile = await select('SelectConfig');
    if (!configFile) throw new Error('No settings selected. Open Ryujinx once, quit it, then open Motion Air again.');
    configDir = dirname(configFile);
  }
  configDir = resolve(configDir);
  if (!inspectRyujinx(configDir, { preset: 'just-dance', dsuPort }).synced) {
    const result = configure({ configDir, preset: 'just-dance', dsuPort });
    log(`Controller profile ready. Previous settings saved to ${join(configDir, result.backup)}`);
  }
  mkdirSync(directory, { recursive: true });
  const temporary = `${path}.tmp`;
  writeFileSync(temporary, JSON.stringify({ executable, configDir, motionBuildSeen: motionBuild || saved.motionBuildSeen }, null, 2));
  renameSync(temporary, path);
  log('For motion, choose a Ryujinx build with the keyboard + CemuHook patch. Stock Ryujinx supports buttons only.');
  log('Allow Node.js on your private network if Windows Firewall asks. Keep Ryujinx focused while playing.');
  return { executable, configDir };
}
