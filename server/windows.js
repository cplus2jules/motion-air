import { execFile, execFileSync } from 'node:child_process';
import { promisify } from 'node:util';
import { fileURLToPath } from 'node:url';

const script = fileURLToPath(new URL('./windows.ps1', import.meta.url));
const args = action => ['-NoLogo', '-NoProfile', '-NonInteractive', '-STA', '-ExecutionPolicy', 'Bypass', '-File', script, action];
// All variable data goes through environment variables, never PowerShell source.
export async function windowsAction(action, env = {}, run = promisify(execFile)) {
  const result = await run('powershell.exe', args(action), {
    env: { ...process.env, ...env }, encoding: 'utf8', windowsHide: true,
    timeout: action.startsWith('Select') ? 0 : 10000,
  });
  return result.stdout.trim();
}
export function windowsRunning(run = execFileSync) {
  const output = run('powershell.exe', args('Running'), { encoding: 'utf8', windowsHide: true, timeout: 10000 });
  if (!['true', 'false'].includes(output.trim())) throw new Error('Cannot inspect Windows processes.');
  return output.trim() === 'true';
}
