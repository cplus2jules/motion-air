// Run against an isolated paired bridge. Requires Playwright on NODE_PATH and a Chromium executable.
import { createRequire } from 'node:module';
import { mkdir, writeFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
const require = createRequire(import.meta.url);
const { chromium } = require('playwright');
const origin = process.env.JOYPAD_UI_ORIGIN || 'http://127.0.0.1:3544';
const directory = '.local/ui-verification';
await mkdir(directory, { recursive: true });
const browser = await chromium.launch({ executablePath: process.env.JOYPAD_UI_BROWSER, headless: true });
const anchor = await browser.newPage();
const results = [], errors = [];
async function check(name, run) {
  try { await run(); results.push({ name, passed: true }); console.log('PASS', name); }
  catch (error) { results.push({ name, passed: false, error: error.stack }); console.error('FAIL', name, error.stack); }
}
async function inspect(page) {
  return page.evaluate(() => {
    const visible = element => !!element.getClientRects().length && getComputedStyle(element).visibility !== 'hidden';
    return {
      overflow: document.documentElement.scrollWidth > innerWidth,
      smallControls: [...document.querySelectorAll('button, select, summary')].filter(visible).filter(e => e.getBoundingClientRect().height < 44).map(e => e.id || e.textContent),
      unnamedControls: [...document.querySelectorAll('button, select, textarea')].filter(visible).filter(e => !e.getAttribute('aria-label') && !e.textContent.trim() && !e.labels?.length).map(e => e.id),
      brokenImages: [...document.images].filter(visible).filter(e => !e.complete || !e.naturalWidth).map(e => e.id),
      h1: document.querySelectorAll('h1').length,
      lang: document.documentElement.lang,
    };
  });
}
const watch = page => page.on('pageerror', error => errors.push(error.message));
try {
  for (const width of [320, 375, 390, 430, 768, 1024, 1440]) {
    for (const colorScheme of ['light', 'dark']) {
      for (const language of ['en', 'es']) {
        await check(`${width}px ${colorScheme} ${language}`, async () => {
          const page = anchor;
          await page.setViewportSize({ width, height: 900 }); await page.emulateMedia({ colorScheme });
          try {
            await page.goto(`${origin}/?lang=${language}`);
            await page.waitForFunction(() => !document.getElementById('copy').disabled);
            await page.locator('#qr').evaluate(image => image.decode());
            // Let disabled-state opacity settle before capturing.
            await page.waitForTimeout(160);
            const audit = await inspect(page);
            assert.equal(audit.overflow, false); assert.deepEqual(audit.smallControls, []);
            assert.deepEqual(audit.unnamedControls, []); assert.deepEqual(audit.brokenImages, []);
            assert.equal(audit.h1, 1); assert.equal(audit.lang, language);
            if ([320, 390, 768, 1440].includes(width)) await page.screenshot({ path: `${directory}/web-${width}-${colorScheme}-${language}.png`, fullPage: true });
          } finally { /* Keep one context: Helium exits when an incognito context closes. */ }
        });
      }
    }
  }
  const context = anchor.context(); await context.grantPermissions(['clipboard-read', 'clipboard-write']);
  const page = anchor; await page.setViewportSize({ width: 390, height: 844 }); await page.emulateMedia({ colorScheme: 'light' }); watch(page);
  await page.goto(origin + '/?lang=en'); await page.waitForFunction(() => !document.getElementById('copy').disabled);
  await check('Copy, keyboard feedback, language persistence', async () => {
    await page.locator('#copy').focus(); await page.keyboard.press('Enter');
    await page.waitForFunction(() => document.getElementById('feedback').textContent.length > 0);
    assert.match(await page.locator('#feedback').innerText(), /Copied/);
    assert.equal(await page.evaluate(() => navigator.clipboard.readText()), await page.locator('#payload').inputValue());
    assert.equal(await page.locator('#copy .t-icon-swap').getAttribute('data-state'), 'b');
    assert.equal(await page.locator('#copy .t-icon').first().evaluate(e => getComputedStyle(e).transitionDuration), '0s');
    await page.selectOption('#language', 'es'); assert.match(await page.locator('#feedback').innerText(), /Copiado/);
    await page.reload(); await page.waitForFunction(() => !document.getElementById('copy').disabled);
    assert.equal(await page.locator('html').getAttribute('lang'), 'es');
    await page.selectOption('#language', 'en');
  });
  await check('Renew invitation through real local API', async () => {
    const old = await page.locator('#payload').inputValue();
    await page.locator('#renew').click(); await page.waitForFunction(old => document.getElementById('payload').value !== old, old);
    await page.waitForFunction(() => document.getElementById('feedback').textContent.includes('New code ready'));
  });
  const base = await (await page.request.get(`${origin}/api/state`)).json();
  let scenario = structuredClone(base), failRead = false, failMutation = false, revocations = 0;
  await page.route('**/api/state', route => failRead ? route.fulfill({ status: 503, body: '{}' }) : route.fulfill({ json: scenario }));
  await page.route('**/api/revoke', async route => {
    revocations++;
    if (failMutation) return route.fulfill({ status: 503, body: '{}' });
    scenario.paired = scenario.paired.filter(phone => phone.id !== route.request().postDataJSON().id);
    await route.fulfill({ json: { ok: true } });
  });
  await check('Long names, connected/saved devices, safe DOM content', async () => {
    scenario.name = 'The living room Mac with a very long name';
    scenario.paired = [{ id: 'qa-one', name: '<img src=x onerror=alert(1)> Jordan’s iPhone with a very long name', connected: true }, { id: 'qa-two', name: 'Second player', connected: false }];
    await page.reload(); await page.waitForFunction(() => document.querySelectorAll('.device').length === 2);
    assert.equal(await page.locator('#connection').getAttribute('data-state'), 'connected');
    assert.equal(await page.locator('#devices img').count(), 0);
    assert.equal((await inspect(page)).overflow, false);
    await page.screenshot({ path: `${directory}/web-connected-long-names.png`, fullPage: true });
  });
  await check('Remove dialog cancel, Escape, focus trap and confirmed revoke', async () => {
    const remove = page.locator('[data-phone-id="qa-one"]');
    await remove.click(); assert.equal(await page.locator('[autofocus]').evaluate(e => e === document.activeElement), true);
    await page.keyboard.press('Shift+Tab'); assert.equal(await page.evaluate(() => document.activeElement === document.body || document.getElementById('remove-dialog').contains(document.activeElement)), true, 'Focus may move to browser chrome but never to the page behind the modal');
    await page.keyboard.press('Escape'); assert.equal(revocations, 0);
    await page.waitForFunction(() => !document.getElementById('remove-dialog').open);
    assert.equal(await remove.evaluate(e => e === document.activeElement), true, 'Escape restores initiating button');
    await remove.click(); await page.locator('#remove-dialog [value="cancel"]').click(); assert.equal(revocations, 0);
    await remove.click(); await page.locator('#confirm-remove').click();
    await page.waitForFunction(() => document.querySelectorAll('.device').length === 1);
    assert.equal(revocations, 1); assert.equal(await page.locator('#devices-title').evaluate(e => e === document.activeElement), true);
    await page.locator('[data-phone-id="qa-two"]').click(); await page.keyboard.press('Escape');
    await page.waitForFunction(() => !document.getElementById('remove-dialog').open);
    assert.equal(revocations, 1, 'Escape after an earlier confirmation must not remove another phone');
  });
  await check('Failed removal preserves phone and translates error', async () => {
    failMutation = true; await page.locator('[data-phone-id="qa-two"]').click(); await page.locator('#confirm-remove').click();
    await page.waitForFunction(() => !document.getElementById('error-box').hidden);
    assert.equal(await page.locator('.device').count(), 1);
    await page.selectOption('#language', 'es'); assert.match(await page.locator('#error').innerText(), /No se pudo/);
    await page.selectOption('#language', 'en'); failMutation = false;
  });
  await check('Offline status hides stale QR, disables copying, retry recovers', async () => {
    failRead = true; await page.reload(); await page.waitForFunction(() => !document.getElementById('error-box').hidden);
    assert.equal(await page.locator('#qr').isVisible(), false); assert.equal(await page.locator('#copy').isDisabled(), true);
    assert.equal(await page.locator('#connection').getAttribute('data-state'), 'offline');
    await page.screenshot({ path: `${directory}/web-offline.png`, fullPage: true });
    failRead = false; await page.locator('#retry').click(); await page.waitForFunction(() => !document.getElementById('copy').disabled);
    assert.equal(await page.locator('#error-box').isVisible(), false);
  });
  await check('Expired invitations cannot be copied or scanned', async () => {
    scenario.expiresAt = Date.now() - 1000; await page.reload();
    await page.waitForFunction(() => document.getElementById('expiry').textContent.includes('expired'));
    assert.equal(await page.locator('#qr').isVisible(), false); assert.equal(await page.locator('#copy').isDisabled(), true);
    scenario.expiresAt = Date.now() + 300000;
  });
  await check('Clipboard denial reveals and selects manual code', async () => {
    await page.reload(); await page.waitForFunction(() => !document.getElementById('copy').disabled);
    await page.evaluate(() => Object.defineProperty(navigator.clipboard, 'writeText', { value: async () => { throw Error('denied'); } }));
    await page.locator('#copy').click(); await page.waitForFunction(() => document.querySelector('details').open);
    assert.equal(await page.locator('#payload').evaluate(e => e === document.activeElement && e.selectionEnd === e.value.length), true);
  });
  await check('Reduced motion and 200 percent text', async () => {
    await page.emulateMedia({ reducedMotion: 'reduce' });
    assert.equal(await page.locator('.t-icon').first().evaluate(e => getComputedStyle(e).transitionDuration), '0s');
    await page.setViewportSize({ width: 768, height: 900 });
    await page.evaluate(() => {
      const sizes = [...document.querySelectorAll('body *')].map(e => [e, getComputedStyle(e).fontSize]);
      for (const [e, size] of sizes) e.style.fontSize = `${parseFloat(size) * 2}px`;
    });
    assert.equal((await inspect(page)).overflow, false);
    await page.screenshot({ path: `${directory}/web-200-percent-text.png`, fullPage: true });
  });
  await context.close();
  await check('No uncaught browser errors', async () => assert.deepEqual(errors, []));
} finally {
  await writeFile(`${directory}/web-results.json`, JSON.stringify({ results, errors }, null, 2));
  await browser.close();
}
if (results.some(result => !result.passed)) process.exitCode = 1;
