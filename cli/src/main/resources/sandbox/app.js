'use strict';

// Display/transport adapter only. All image mathematics and exported SVG come from :core.
const state = {language: 'ru', strings: {}, defaults: null, maxUploadBytes: null, sample: '1046', file: null, fileUrl: null, result: null, svg: null, view: 'original', busy: false};
const byId = id => document.getElementById(id);
const text = key => state.strings[key] || key;
const colors = ['#285d57', '#527fc4', '#c28a52', '#bb6376', '#875aa7', '#41826c', '#809743', '#d17d45'];

function translate() {
  document.documentElement.lang = state.language;
  document.querySelectorAll('[data-i18n]').forEach(element => { element.textContent = text(element.dataset.i18n); });
  document.querySelectorAll('[data-i18n-alt]').forEach(element => { element.alt = text(element.dataset.i18nAlt); });
  document.querySelectorAll('[data-i18n-title]').forEach(element => { element.title = text(element.dataset.i18nTitle); });
  document.querySelectorAll('[data-i18n-aria]').forEach(element => { element.setAttribute('aria-label', text(element.dataset.i18nAria)); });
  document.title = `${text('app_name')} · ${text('lab_name')}`;
  if (state.result) updateResultLabels();
  updateBusy();
}

async function loadConfiguration() {
  const response = await fetch(`/api/config?lang=${encodeURIComponent(state.language)}`);
  if (!response.ok) throw new Error('config_unavailable');
  const configuration = await response.json();
  state.strings = configuration.strings;
  state.defaults = configuration.defaults;
  state.maxUploadBytes = configuration.maxUploadBytes;
  translate();
}

function defaults() {
  if (!state.defaults) return;
  ['window', 'offset', 'minArea', 'rdp'].forEach(name => { byId(name).value = state.defaults[name]; });
  updateOutputs();
}

function updateOutputs() {
  byId('offset-value').textContent = byId('offset').value;
  byId('rdp-value').textContent = new Intl.NumberFormat(state.language, {maximumFractionDigits: 1}).format(Number(byId('rdp').value));
}

function updateBusy() {
  document.body.classList.toggle('busy', state.busy);
  document.querySelectorAll('.sample').forEach(button => { button.disabled = state.busy; });
  ['window', 'offset', 'minArea', 'rdp', 'reset', 'clear-file'].forEach(id => { byId(id).disabled = state.busy; });
  byId('recalculate').disabled = state.busy || !state.defaults;
  byId('recalculate').querySelector('[data-i18n]').textContent = text(state.busy ? 'processing' : 'recalculate');
  byId('consent').disabled = state.busy;
  byId('choose-file').disabled = state.busy || !byId('consent').checked;
  byId('download').disabled = state.busy || !state.svg;
  if (state.busy) byId('result-status').textContent = text('processing');
}

function setConnection(ok) {
  const element = byId('connection');
  element.className = `connection ${ok ? 'ready' : 'offline'}`;
  element.querySelector('span').textContent = text(ok ? 'engine_ready' : 'engine_offline');
}

function showError(code) {
  const notice = byId('notice');
  const key = `error_${code}`;
  notice.textContent = state.strings[key] || text('error_generic');
  notice.hidden = false;
}

function invalidateResult() {
  state.result = null;
  state.svg = null;
  byId('vector').replaceChildren();
  const empty = document.createElement('span');
  empty.className = 'empty-preview';
  empty.textContent = text('preview_empty');
  byId('vector').appendChild(empty);
  ['strokes', 'closed', 'time', 'cycles'].forEach(id => { byId(id).textContent = '—'; });
  document.querySelectorAll('[data-view]').forEach(button => { button.disabled = button.dataset.view !== 'original'; });
}

function updateResultLabels() {
  const result = state.result;
  if (!result) return;
  byId('strokes').textContent = result.strokes;
  byId('closed').textContent = result.closedPolylines;
  byId('cycles').textContent = result.graphCycles;
  byId('time').textContent = new Intl.NumberFormat(state.language, {maximumFractionDigits: 1}).format(result.coreMs);
  byId('dimensions').textContent = `${result.width} × ${result.height}`;
  byId('result-status').textContent = text('ready_notice');
}

function renderVector(encoded) {
  const bytes = Uint8Array.from(atob(encoded), character => character.charCodeAt(0));
  state.svg = new TextDecoder().decode(bytes);
  const documentSvg = new DOMParser().parseFromString(state.svg, 'image/svg+xml');
  const root = documentSvg.documentElement;
  if (root.localName !== 'svg' || root.querySelector('parsererror')) throw new Error('processing_failed');
  // Visual stroke colors only: no new geometry or changed download contents.
  root.querySelectorAll('path').forEach((path, index) => { path.setAttribute('stroke', colors[index % colors.length]); });
  root.setAttribute('role', 'img');
  root.setAttribute('aria-label', text('result_title'));
  byId('vector').replaceChildren(document.importNode(root, true));
}

function renderSource() {
  const original = state.fileUrl || `/api/fixtures/${encodeURIComponent(state.sample)}.png`;
  const data = state.result;
  const source = state.view === 'binary' && data ? `data:image/png;base64,${data.binaryBase64}` :
    state.view === 'skeleton' && data ? `data:image/png;base64,${data.skeletonBase64}` : original;
  byId('source').src = source;
  document.querySelectorAll('[data-view]').forEach(button => { button.classList.toggle('active', button.dataset.view === state.view); });
}

async function processImage() {
  if (state.busy || !state.defaults) return;
  state.busy = true;
  byId('notice').hidden = true;
  updateBusy();
  const query = new URLSearchParams();
  ['window', 'offset', 'minArea', 'rdp'].forEach(name => { query.set(name, byId(name).value); });
  query.set('pruning', String(state.defaults.pruning));
  if (!state.file) query.set('sample', state.sample);
  try {
    const response = await fetch(`/api/diagnose?${query.toString()}`, {
      method: 'POST',
      headers: {'Content-Type': 'application/octet-stream'},
      body: state.file || undefined
    });
    const data = await response.json();
    if (!response.ok) {
      invalidateResult();
      showError(data.error || 'generic');
      return;
    }
    state.result = data;
    renderVector(data.svgBase64);
    updateResultLabels();
    renderSource();
    document.querySelectorAll('[data-view]').forEach(button => { button.disabled = false; });
    setConnection(true);
  } catch (error) {
    invalidateResult();
    showError(error.message === 'processing_failed' ? error.message : 'generic');
  } finally {
    state.busy = false;
    updateBusy();
    if (state.result) updateResultLabels();
  }
}

function clearFileUrl() {
  if (state.fileUrl) URL.revokeObjectURL(state.fileUrl);
  state.fileUrl = null;
  state.file = null;
  byId('file').value = '';
  byId('file-name').textContent = '';
  byId('clear-file').hidden = true;
}

document.querySelectorAll('[data-sample]').forEach(button => {
  button.addEventListener('click', () => {
    if (state.busy) return;
    clearFileUrl();
    state.sample = button.dataset.sample;
    state.view = 'original';
    document.querySelectorAll('[data-sample]').forEach(other => { other.classList.toggle('active', other === button); });
    invalidateResult();
    renderSource();
    processImage();
  });
});

document.querySelectorAll('[data-view]').forEach(button => {
  button.addEventListener('click', () => { state.view = button.dataset.view; renderSource(); });
});
byId('offset').addEventListener('input', updateOutputs);
byId('rdp').addEventListener('input', updateOutputs);
byId('parameter-form').addEventListener('submit', event => { event.preventDefault(); processImage(); });
byId('reset').addEventListener('click', () => { defaults(); processImage(); });
byId('consent').addEventListener('change', () => {
  if (!byId('consent').checked && state.file) document.querySelector(`[data-sample="${state.sample}"]`).click();
  updateBusy();
});
byId('choose-file').addEventListener('click', () => { if (byId('consent').checked) byId('file').click(); });
byId('file').addEventListener('change', () => {
  const file = byId('file').files[0];
  if (!file || !byId('consent').checked || state.busy) return;
  if (file.size > state.maxUploadBytes) { showError('too_large'); return; }
  clearFileUrl();
  state.file = file;
  state.fileUrl = URL.createObjectURL(file);
  state.view = 'original';
  byId('file-name').textContent = `${file.name} · ${text('uploaded_notice')}`;
  byId('clear-file').hidden = false;
  document.querySelectorAll('[data-sample]').forEach(button => { button.classList.remove('active'); });
  invalidateResult();
  renderSource();
  processImage();
});
byId('clear-file').addEventListener('click', () => { document.querySelector(`[data-sample="${state.sample}"]`).click(); });
byId('download').addEventListener('click', () => {
  if (!state.svg || state.busy) return;
  const url = URL.createObjectURL(new Blob([state.svg], {type: 'image/svg+xml;charset=utf-8'}));
  const link = document.createElement('a');
  link.href = url;
  link.download = `photoglyph-lab-${state.file ? 'upload' : state.sample}.svg`;
  link.click();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
});
byId('language').addEventListener('change', async event => {
  state.language = event.target.value;
  try { await loadConfiguration(); updateOutputs(); setConnection(true); }
  catch (error) { setConnection(false); showError('generic'); }
});
window.addEventListener('beforeunload', clearFileUrl);

(async () => {
  try {
    await loadConfiguration();
    defaults();
    const health = await fetch('/api/health');
    if (!health.ok) throw new Error('core_unavailable');
    setConnection(true);
    await processImage();
  } catch (error) {
    setConnection(false);
    showError('generic');
  }
})();
