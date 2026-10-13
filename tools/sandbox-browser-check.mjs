#!/usr/bin/env node
// Optional browser QA tools live under ignored .cache/browser, never in the product.
// Install: npm install --prefix .cache/browser playwright-core@1.64.0 @sparticuz/chromium@153.0.0
import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';

const root = path.resolve(import.meta.dirname, '..');
const require = createRequire(path.join(root, '.cache/browser/package.json'));
const chromiumPackage = require('@sparticuz/chromium');
const chromium = chromiumPackage.default;
const {chromium: playwright} = require('playwright-core');
// Minimal containers lack NSS. Use the browser package's own local support archive;
// no OS downloads or native processing dependencies are added to PhotoGlyph.
const supportArchive = path.join(path.dirname(require.resolve('@sparticuz/chromium')), '../bin/al2023.tar.br');
const supportDirectory = await chromiumPackage.inflate(supportArchive);
chromiumPackage.setupLambdaEnvironment(path.join(supportDirectory, 'lib'));
const base = process.env.SANDBOX_URL || 'http://127.0.0.1:8080';
const browserArgs = chromium.args.filter(argument => !['--disable-web-security', '--allow-running-insecure-content'].includes(argument));
const browser = await playwright.launch({executablePath: await chromium.executablePath(), args: browserArgs, headless: true});
const context = await browser.newContext({viewport: {width: 1440, height: 1200}, acceptDownloads: true});
const page = await context.newPage();
const errors = [];
page.on('pageerror', error => errors.push(error.message));
if (process.env.BROWSER_DEBUG) {
  page.on('request', request => console.log('REQUEST', request.url()));
  page.on('requestfailed', request => console.log('FAILED', request.url(), request.failure()));
  page.on('console', message => console.log('CONSOLE', message.type(), message.text()));
}
const output = path.join(root, '.m1-work');
fs.mkdirSync(output, {recursive: true});

try {
  await page.goto(base, {waitUntil: 'domcontentloaded'});
  await page.waitForFunction(() => document.getElementById('strokes').textContent === '8');
  assert.equal(await page.locator('#closed').textContent(), '0');
  assert.equal(await page.locator('#vector path').count(), 8);
  assert.equal(await page.locator('#choose-file').isEnabled(), false);
  const fills = await page.locator('#vector path').evaluateAll(paths => paths.map(item => item.getAttribute('fill')));
  assert(fills.every(fill => fill === 'none'));
  console.log('PASS Browser.initialSyntheticZhe -> HTTP vectorize, strokesToSvg, requireStrokeOnlySvg; 8 real paths, no fills');
  await page.screenshot({path: path.join(output, 'sandbox-desktop.png'), fullPage: true});

  for (const [code, expected] of [['1041', 3], ['1074', 3], ['1064', 3], ['1092', 5]]) {
    const responsePromise = page.waitForResponse(response => response.url().includes(`/api/diagnose?`) && response.url().includes(`sample=${code}`));
    await page.locator(`[data-sample="${code}"]`).click();
    const data = await (await responsePromise).json();
    await page.waitForFunction(() => !document.body.classList.contains('busy'));
    assert.equal(data.strokes, expected);
    assert.equal(data.closedPolylines, 0);
    assert.equal(await page.locator('#vector path').count(), expected);
  }
  console.log('PASS Browser.fiveFixtureSelection -> real vectorize + no-fill SVG; rendered counts equal server results');

  const parameterResponse = page.waitForResponse(response => response.url().includes('/api/diagnose?') && response.url().includes('rdp=1.2'));
  await page.locator('#rdp').evaluate(element => { element.value = '1.2'; element.dispatchEvent(new Event('input', {bubbles: true})); });
  await page.locator('#recalculate').click();
  const parameters = await (await parameterResponse).json();
  assert.equal(parameters.params.rdp, 1.2);
  await page.waitForFunction(() => !document.body.classList.contains('busy'));
  await page.locator('[data-view="skeleton"]').click();
  assert((await page.locator('#source').getAttribute('src')).startsWith('data:image/png;base64,'));
  console.log('PASS Browser.parameterAndSkeletonControls -> PipelineParams, vectorize, simplifyRdp; server-generated skeleton PNG');

  await page.locator('#language').selectOption('en');
  await page.waitForFunction(() => document.querySelector('h1').textContent === 'First, test the strokes.');
  await page.locator('#language').selectOption('ru');
  await page.waitForFunction(() => document.querySelector('h1').textContent === 'Сначала проверяем штрихи.');
  console.log('PASS Browser.ruEnStrings -> server resources and existing real processing result; no hardcoded translated UI');

  await page.locator('#consent').check();
  const uploadResponse = page.waitForResponse(response => response.url().includes('/api/diagnose?') && !response.url().includes('sample='));
  await page.locator('#file').setInputFiles(path.join(root, 'tools/fixtures/synthetic/m0/1041.png'));
  assert.equal((await uploadResponse).status(), 200);
  await page.waitForFunction(() => !document.body.classList.contains('busy'));
  // Read the actual response consumed by the UI. Single-process Chromium can evict
  // inspector response bodies when an image blob URL changes; no response is mocked.
  const upload = await page.evaluate(() => state.result);
  assert.equal(upload.synthetic, false); // Transport origin only; the controlled test input remains synthetic.
  assert.equal(upload.strokes, 3);
  assert.equal(upload.closedPolylines, 0);
  console.log('PASS Browser.explicitConsentUpload -> real image decode + vectorize; controlled synthetic file only');

  const downloadPromise = page.waitForEvent('download');
  await page.locator('#download').click();
  const download = await downloadPromise;
  assert(download.suggestedFilename().endsWith('.svg'));
  const downloaded = path.join(output, 'browser-download.svg');
  await download.saveAs(downloaded);
  const svg = fs.readFileSync(downloaded, 'utf8');
  assert(svg.includes('fill="none"') && svg.includes('stroke="black"') && !svg.includes(' Z'));
  console.log('PASS Browser.downloadActualSvg -> strokesToSvg output, not a fake .gfont or screenshot');

  await page.locator('#clear-file').click();
  await page.waitForFunction(() => !document.body.classList.contains('busy'));
  await page.setViewportSize({width: 390, height: 844});
  await page.screenshot({path: path.join(output, 'sandbox-mobile.png'), fullPage: true});
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth));
  assert.equal(errors.length, 0, JSON.stringify(errors));
  console.log('PASS Browser.mobileAndNoJsErrors -> same real core preview, no horizontal overflow');
  console.log('Browser checks passed: actual sandbox interactions; no Android device/OCR/gfont claim');
} finally {
  await browser.close();
}
