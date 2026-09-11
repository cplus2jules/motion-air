// Vigila qué app tiene el foco en macOS.
//
// Si Ryujinx no está al frente, las teclas inyectadas van a otra app — el
// bloqueador #1 histórico de este proyecto ("controla el Mac, no el juego").
// Detectamos el frontmost con `lsappinfo` (builtin de macOS, NO dispara
// prompts de TCC, a diferencia de osascript+System Events) y avisamos a los
// iPhones para que muestren un banner.
//
// Si lsappinfo falla, invalidamos el resultado anterior para pausar las
// teclas. Nunca usar osascript aquí: pediría permiso de Automation.

import { execFile } from "node:child_process";
import { windowsAction } from "./windows.js";

function getFrontApp() {
  if (process.platform === 'win32') return windowsAction('Focus').catch(() => null);
  return new Promise((resolve) => {
    execFile("lsappinfo", ["front"], { timeout: 1500 }, (err, stdout) => {
      const asn = (stdout || "").trim();
      if (err || !asn) return resolve(null);
      execFile(
        "lsappinfo",
        ["info", "-only", "name", asn],
        { timeout: 1500 },
        (err2, out2) => {
          if (err2 || !out2) return resolve(null);
          // formato esperado: "LSDisplayName"="Ryujinx"
          const m = out2.match(/=\s*"([^"]+)"/);
          resolve(m ? m[1].trim() : null);
        }
      );
    });
  });
}

// match: substring case-insensitive del nombre de la app objetivo.
// isActive: solo sondear cuando hay players conectados (no gastar CPU).
export function createFocusWatcher({ match = "ryujinx", intervalMs = 2000, isActive = () => true, onChange, readFrontApp = getFrontApp } = {}) {
  let last = null; // { ok, app } | null
  let timer = null;
  let polling = null;
  let generation = 0;
  let stopped = false;

  function publish(app) {
    const ok = app === null ? null : app.toLowerCase().includes(match.toLowerCase());
    if (!last || last.ok !== ok || last.app !== app) {
      last = { ok, app };
      onChange?.(last);
    }
  }

  function poll() {
    if (stopped || polling) return polling;
    if (!isActive()) { publish(null); return; }
    const current = generation;
    polling = Promise.resolve().then(readFrontApp).catch(() => null).then(app => {
      if (!stopped && current === generation) publish(typeof app === "string" && app.trim() ? app.trim() : null);
    }).finally(() => { polling = null; });
    return polling;
  }

  return {
    start() {
      if (!timer) {
        stopped = false;
        timer = setInterval(poll, intervalMs);
        timer.unref?.();
        poll();
      }
    },
    stop() {
      stopped = true;
      generation++;
      publish(null);
      if (timer) {
        clearInterval(timer);
        timer = null;
      }
    },
    // A newly connected controller must not inherit a result from before
    // its session. Ignore any in-flight result, then check the current app.
    refresh() {
      generation++;
      publish(null);
      return Promise.resolve(polling).then(poll);
    },
    get last() {
      return last;
    },
  };
}
