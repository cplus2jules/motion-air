const strings={
  "en": {
    "tag": "YOUR MAC, READY TO PLAY",
    "title": "Game night starts here.",
    "loading": "Finding your Mac…",
    "scan": "Bring your iPhone along.",
    "instructions": "Pair your iPhone once. Next time, choose this Mac in Motion Air and jump back in.",
    "copy": "Copy pairing code",
    "renew": "New QR code",
    "manual": "Paste a code instead",
    "devices": "Your players",
    "privacy": "Your controller stays on this network. Saved connections are encrypted. Keep the launcher’s Terminal window open while you play.",
    "language": "Language",
    "empty": "A spot for your iPhone.",
    "connected": "Connected",
    "saved": "Saved",
    "remove": "Remove",
    "expires": "New code in",
    "copied": "Copied. Paste it in Motion Air on your iPhone.",
    "selectCopy": "Select and copy the code below.",
    "readError": "Can’t reach the launcher. Open Motion Air.command on this Mac, then try again.",
    "updateError": "That didn’t go through. Check the launcher and try again.",
    "qrAlt": "One-time pairing QR code",
    "payloadLabel": "Pairing code",
    "emptyDetail": "Pair a phone to save it here for next time.",
    "ready": "Ready to pair",
    "live": "Phone connected",
    "offline": "Launcher unavailable",
    "expired": "Code expired. Get a new QR code.",
    "retry": "Try again",
    "step1": "Open Motion Air on iPhone",
    "step1Detail": "Keep your iPhone and Mac on the same Wi-Fi or Personal Hotspot.",
    "step2": "Scan this QR code",
    "step2Detail": "Choose Pair a Mac, then Scan Mac QR code. Check the Mac’s name and confirm.",
    "step3": "Make your move",
    "step3Detail": "Open your game on the Mac. Enable Motion on iPhone when you’re ready to dance.",
    "qrLabel": "SCAN TO JOIN",
    "macLabel": "THIS MAC",
    "keepOpen": "Keep this window handy.",
    "local": "Local connection",
    "phoneCount": "phones saved",
    "phoneCountOne": "phone saved",
    "removeTitle": "Forget this phone?",
    "removeDetail": "It will disconnect and need to scan a new code to play again.",
    "cancel": "Keep phone",
    "confirmRemove": "Forget phone",
    "savedDetail": "Ready to reconnect from iPhone",
    "connectedDetail": "Connected to this Mac",
    "newCode": "New code ready. Scan it with your iPhone.",
    "footer": "Made for one more round.",
    "credits": "Built on Joypad Air by David García.",
    "pairedTitle": "You’re in. Pick your game.",
    "pairedSubtitle": "Your phone is connected. Keep Motion Air open on iPhone while you play."
  },
  "es": {
    "tag": "TU MAC, LISTO PARA JUGAR",
    "title": "Aquí empieza la partida.",
    "loading": "Buscando tu Mac…",
    "scan": "Trae tu iPhone.",
    "instructions": "Empareja el iPhone una vez. La próxima vez, elige este Mac en Motion Air y sigue jugando.",
    "copy": "Copiar código de conexión",
    "renew": "Nuevo código QR",
    "manual": "Pegar un código",
    "devices": "Tus jugadores",
    "privacy": "El mando permanece en esta red. Las conexiones guardadas están cifradas. Mantén abierta la ventana de Terminal del lanzador mientras juegas.",
    "language": "Idioma",
    "empty": "Un sitio para tu iPhone.",
    "connected": "Conectado",
    "saved": "Guardado",
    "remove": "Eliminar",
    "expires": "Nuevo código en",
    "copied": "Copiado. Pégalo en Motion Air en tu iPhone.",
    "selectCopy": "Selecciona y copia el código de abajo.",
    "readError": "No se puede conectar con el lanzador. Abre Motion Air.command en este Mac y vuelve a intentarlo.",
    "updateError": "No se pudo completar la acción. Revisa el lanzador y vuelve a intentarlo.",
    "qrAlt": "Código QR de un solo uso",
    "payloadLabel": "Código de conexión",
    "emptyDetail": "Empareja un teléfono para guardarlo aquí.",
    "ready": "Listo para emparejar",
    "live": "Teléfono conectado",
    "offline": "Lanzador no disponible",
    "expired": "El código venció. Genera un nuevo QR.",
    "retry": "Reintentar",
    "step1": "Abre Motion Air en el iPhone",
    "step1Detail": "Conecta el iPhone y el Mac a la misma Wi-Fi o a un punto de acceso personal.",
    "step2": "Escanea este código QR",
    "step2Detail": "Elige Pair a Mac y luego Scan Mac QR code. Revisa el nombre del Mac y confirma.",
    "step3": "Haz tu jugada",
    "step3Detail": "Abre el juego en el Mac. Activa Enable Motion en el iPhone cuando quieras bailar.",
    "qrLabel": "ESCANEA PARA UNIRTE",
    "macLabel": "ESTE MAC",
    "keepOpen": "Ten esta ventana a mano.",
    "local": "Conexión local",
    "phoneCount": "teléfonos guardados",
    "phoneCountOne": "teléfono guardado",
    "removeTitle": "¿Olvidar este teléfono?",
    "removeDetail": "Se desconectará y tendrá que escanear un nuevo código para volver a jugar.",
    "cancel": "Conservar teléfono",
    "confirmRemove": "Olvidar teléfono",
    "savedDetail": "Listo para reconectar desde el iPhone",
    "connectedDetail": "Conectado a este Mac",
    "newCode": "Nuevo código listo. Escanéalo con tu iPhone.",
    "footer": "Siempre hay otra partida.",
    "credits": "Basado en Joypad Air de David García.",
    "pairedTitle": "Ya estás dentro. Elige un juego.",
    "pairedSubtitle": "El teléfono está conectado. Mantén Motion Air abierto en el iPhone mientras juegas."
  }
};
const $ = id => document.getElementById(id);
let language = 'en', state, online = false, loaded = false, requestID = 0, refreshing = false, mutating = false;
let feedbackKey = '', errorKey = '', pendingRemoval, copyTimer, deviceSignature = '';
try { language = localStorage.getItem('joypad-air-language') || navigator.language || 'en'; } catch { language = navigator.language || 'en'; }
language = new URL(location.href).searchParams.get('lang') || language;
language = /^es/i.test(language) ? 'es' : 'en';
const text = key => strings[language][key];
const phoneIcon = '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="6" y="2" width="12" height="20" rx="3"/><path d="M10 5h4m-3 14h2"/></svg>';
function setError(key = '') { errorKey = key; $('error').textContent = key ? text(key) : ''; $('error-box').hidden = !key; }
function feedback(key = '') { feedbackKey = key; $('feedback').textContent = key ? text(key) : ''; }
function updateExpiry() {
  const remaining = Math.max(0, Math.ceil(((state?.expiresAt || 0) - Date.now()) / 1000));
  $('expiry').textContent = !state || !online ? '' : remaining > 0 ? text('expires') + ' ' + Math.floor(remaining / 60) + ':' + String(remaining % 60).padStart(2, '0') : text('expired');
  $('qr').hidden = !online || !state || remaining === 0;
  $('qr-placeholder').hidden = !$('qr').hidden;
  $('copy').disabled = !online || !state || remaining === 0 || mutating;
  $('renew').disabled = !online || mutating;
  document.querySelectorAll('[data-phone-id]').forEach(button => { button.disabled = !online || mutating; });
}
function renderDevices() {
  const paired = state?.paired || [];
  const signature = JSON.stringify([language, paired]);
  // Preserve focus and open controls while status polls return unchanged data.
  if (signature === deviceSignature) return;
  deviceSignature = signature;
  const list = $('devices'); list.replaceChildren();
  $('phone-count').textContent = paired.length ? paired.length + ' ' + text(paired.length === 1 ? 'phoneCountOne' : 'phoneCount') : '';
  if (!paired.length) {
    const empty = document.createElement('div'); empty.className = 'empty';
    empty.innerHTML = phoneIcon;
    const description = document.createElement('div'), title = document.createElement('strong'), detail = document.createElement('p');
    title.textContent = text('empty'); detail.textContent = text('emptyDetail');
    description.append(title, detail); empty.append(description); list.append(empty);
  }
  for (const phone of paired) {
    const row = document.createElement('div'); row.className = 'device';
    const icon = document.createElement('span'); icon.className = 'device-icon'; icon.innerHTML = phoneIcon;
    const info = document.createElement('div'); info.className = 'device-info';
    const name = document.createElement('strong'); name.textContent = phone.name;
    const detail = document.createElement('p'); detail.textContent = text(phone.connected ? 'connectedDetail' : 'savedDetail');
    info.append(name, detail);
    const button = document.createElement('button'); button.className = 'secondary'; button.textContent = text('remove');
    button.setAttribute('aria-label', text('remove') + ' ' + phone.name);
    button.dataset.phoneId = phone.id;
    button.onclick = () => { pendingRemoval = phone; $('remove-dialog').returnValue = ''; $('remove-name').textContent = phone.name; $('remove-dialog').showModal(); };
    row.append(icon, info, button); list.append(row);
  }
}
function render() {
  document.documentElement.lang = language; document.title = 'Motion Air · ' + text('title'); $('language').value = language;
  document.querySelectorAll('[data-i18n]').forEach(element => element.textContent = text(element.dataset.i18n));
  $('pair-card').setAttribute('aria-label', text('qrLabel')); $('qr').alt = text('qrAlt'); $('payload').setAttribute('aria-label', text('payloadLabel'));
  const connected = state?.paired.some(phone => phone.connected) && online;
  $('connection').dataset.state = !online ? 'offline' : connected ? 'connected' : 'ready';
  $('connection-label').textContent = text(!loaded ? 'loading' : !online ? 'offline' : connected ? 'live' : 'ready');
  $('mac').textContent = state?.name || text('loading');
  $('joined').hidden = !connected;
  if (state) { if ($('qr').getAttribute('src') !== state.qr) $('qr').src = state.qr; if ($('payload').value !== state.invitation) $('payload').value = state.invitation; }
  renderDevices(); updateExpiry(); feedback(feedbackKey); setError(errorKey);
}
async function refresh() {
  if (refreshing) return;
  refreshing = true;
  const id = ++requestID;
  try {
    const response = await fetch('/api/state', { signal: AbortSignal.timeout(4500), cache: 'no-store' });
    if (!response.ok) throw Error();
    const next = await response.json();
    if (!next || typeof next.name !== 'string' || !Array.isArray(next.paired) || typeof next.invitation !== 'string' || typeof next.qr !== 'string' || !Number.isFinite(next.expiresAt)) throw Error();
    if (id !== requestID) return;
    if (state?.invitation !== next.invitation) { feedback(''); $('copy').querySelector('.t-icon-swap').dataset.state = 'a'; }
    state = next; online = true; loaded = true; if (errorKey === 'readError') setError(); render();
  } catch { if (id === requestID) { online = false; loaded = true; setError('readError'); render(); } }
  finally { if (id === requestID) refreshing = false; }
}
async function mutate(url, body = {}) {
  if (mutating) return false;
  mutating = true; updateExpiry();
  try {
    const response = await fetch(url, { method: 'POST', headers: { 'content-type': 'application/json', 'x-joypad-pairing': '1' }, body: JSON.stringify(body), signal: AbortSignal.timeout(4500) });
    if (!response.ok) throw Error();
    // Invalidate a status read started before the mutation; its QR may be consumed.
    ++requestID; refreshing = false; await refresh(); setError(online ? '' : 'readError');
    return online;
  } catch { setError('updateError'); return false; }
  finally { mutating = false; updateExpiry(); }
}
$('language').onchange = event => {
  language = event.target.value; const url = new URL(location.href); url.searchParams.set('lang', language); history.replaceState(null, '', url);
  try { localStorage.setItem('joypad-air-language', language); } catch {}
  render();
};
$('copy').onclick = async event => {
  if (!state || !online) return;
  const button = event.currentTarget;
  button.classList.toggle('keyboard-activate', event.detail === 0);
  try {
    await navigator.clipboard.writeText(state.invitation); feedback('copied');
    button.querySelector('.t-icon-swap').dataset.state = 'b';
    clearTimeout(copyTimer); copyTimer = setTimeout(() => { button.querySelector('.t-icon-swap').dataset.state = 'a'; }, 3000);
  } catch {
    $('payload').closest('details').open = true; $('payload').focus(); $('payload').select(); feedback('selectCopy');
  }
};
$('renew').onclick = async () => { if (await mutate('/api/renew')) feedback('newCode'); };
$('retry').onclick = refresh;
$('remove-dialog').addEventListener('close', async () => {
  const phone = pendingRemoval; pendingRemoval = undefined;
  if ($('remove-dialog').returnValue !== 'confirm' || !phone) return;
  if (await mutate('/api/revoke', { id: phone.id })) $('devices-title').focus({ preventScroll: true });
});
$('devices-title').tabIndex = -1;
document.addEventListener('keydown', event => { if ((event.key === ' ' || event.key === 'Enter') && event.target instanceof HTMLButtonElement) event.target.classList.add('keyboard-activate'); });
document.addEventListener('pointerdown', event => event.target.closest('button')?.classList.remove('keyboard-activate'));
document.addEventListener('visibilitychange', () => { if (!document.hidden) refresh(); });
render(); refresh();
setInterval(() => { updateExpiry(); if (!document.hidden) refresh(); }, 5000);
