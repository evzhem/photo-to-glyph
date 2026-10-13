'use strict';
// M2 UI/transport only. Pixel crop, rotation, page detection, homography and B/W are Kotlin functions.
const prep = {strings:{}, language:'ru', file:null, source:null, quad:null, result:null, busy:false, dragging:-1, maxBytes:0};
const element = id => document.getElementById(id);
const label = key => prep.strings[key] || key;
const canvas = element('source-canvas');
const context = canvas.getContext('2d');
let sourceBitmap = null;
function translate() {
  document.documentElement.lang = prep.language;
  document.querySelectorAll('[data-i18n]').forEach(node => node.textContent = label(node.dataset.i18n));
  document.querySelectorAll('[data-i18n-alt]').forEach(node => node.alt = label(node.dataset.i18nAlt));
  document.title = `${label('app_name')} · ${label('prep_badge')}`;
}
async function configuration() {
  const response = await fetch(`/api/config?lang=${encodeURIComponent(prep.language)}`);
  if (!response.ok) throw new Error('generic');
  const data = await response.json(); prep.strings = data.strings; prep.maxBytes = data.maxUploadBytes;
  translate();
}
function busy(value) {
  prep.busy = value;
  document.body.classList.toggle('busy',value);
  ['sample','detect','rotate','crop','window','offset','a4','process','clear-file','consent'].forEach(id => element(id).disabled = value);
  element('choose-file').disabled = value || !element('consent').checked;
  element('process').textContent = label(value ? 'processing' : 'prep_process');
  element('download').disabled = value || !prep.result;
}
function error(code) { const node=element('notice');node.textContent=label(`error_${code}`);node.hidden=false; }
function invalidate() {
  prep.result=null;
  ['download','show-binary','show-rectified'].forEach(id => element(id).disabled=true);
  element('output').hidden=true;element('empty').hidden=false;element('result-info').textContent='';element('output-dimensions').textContent='';
}
async function drawSource() {
  if (!prep.source) return;
  sourceBitmap = new Image();sourceBitmap.src=`data:image/png;base64,${prep.source.sourceBase64}`;
  await sourceBitmap.decode();
  canvas.width=prep.source.width;canvas.height=prep.source.height;
  element('dimensions').textContent=`${canvas.width} × ${canvas.height}`;
  draw();
}
function draw() {
  if (!sourceBitmap) return;
  context.clearRect(0,0,canvas.width,canvas.height);context.drawImage(sourceBitmap,0,0);
  if (!prep.quad) return;
  context.strokeStyle='#218a70';context.fillStyle='#218a70';context.lineWidth=Math.max(canvas.width/220,2);
  context.beginPath();prep.quad.forEach(([x,y],index) => index ? context.lineTo(x,y) : context.moveTo(x,y));context.closePath();context.stroke();
  prep.quad.forEach(([x,y],index) => {context.beginPath();context.arc(x,y,Math.max(canvas.width/80,6),0,Math.PI*2);context.fill();context.fillStyle='#fff';context.font=`${Math.max(canvas.width/60,11)}px sans-serif`;context.fillText(String(index+1),x+10,y+3);context.fillStyle='#218a70';});
}
function point(event) {const rect=canvas.getBoundingClientRect();return [(event.clientX-rect.left)*canvas.width/rect.width,(event.clientY-rect.top)*canvas.height/rect.height];}
canvas.addEventListener('pointerdown',event => {
  if (prep.busy || !prep.quad) return;
  const [x,y]=point(event);const distances=prep.quad.map(([qx,qy]) => Math.hypot(qx-x,qy-y));
  prep.dragging=distances.indexOf(Math.min(...distances));canvas.setPointerCapture(event.pointerId);
});
canvas.addEventListener('pointermove',event => {
  if (prep.dragging<0) return;
  const [x,y]=point(event);prep.quad[prep.dragging]=[Math.max(0,Math.min(canvas.width,x)),Math.max(0,Math.min(canvas.height,y))];invalidate();draw();
});
canvas.addEventListener('pointerup',() => prep.dragging=-1);
canvas.addEventListener('pointercancel',() => prep.dragging=-1);
async function request(operation='detect',extras={}) {
  if (prep.busy) return;
  busy(true);element('notice').hidden=true;
  const query=new URLSearchParams({op:operation,window:element('window').value,offset:element('offset').value,a4:String(element('a4').checked),...extras});
  if (!prep.file) query.set('sample','page');
  if (operation==='prepare' && prep.quad) query.set('quad',prep.quad.flat().join(','));
  try {
    // Once crop/rotation has produced a new source, upload that PNG for later operations.
    const body=prep.source && prep.source.edited ? Uint8Array.from(atob(prep.source.sourceBase64),c=>c.charCodeAt(0)) : prep.file || undefined;
    if (body) query.delete('sample');
    const response=await fetch(`/api/prepare?${query}`,{method:'POST',headers:{'Content-Type':'application/octet-stream'},body});
    const data=await response.json();
    if (!response.ok) {error(data.error||'generic');return;}
    const edited=operation==='rotate'||operation==='crop'||Boolean(prep.source?.edited);
    prep.source={...data,edited};prep.quad=data.quad;
    await drawSource();
    if (operation==='prepare') {
      prep.result=data;element('empty').hidden=true;element('output').hidden=false;
      element('output').src=`data:image/png;base64,${data.binaryBase64}`;
      element('output-dimensions').textContent=`${data.preparedWidth} × ${data.preparedHeight}`;
      element('result-info').textContent=`${label('ready_notice')} · ${new Intl.NumberFormat(prep.language,{maximumFractionDigits:1}).format(data.prepareMs)} ${label('time_unit')}. ${label('prep_no_recognition')}`;
      ['download','show-binary','show-rectified'].forEach(id=>element(id).disabled=false);
    } else {invalidate();if(!data.detected) element('result-info').textContent=label('prep_corner_not_found');}
  } catch (failure) {error('generic');}
  finally {busy(false);}
}
element('sample').addEventListener('click',() => {prep.file=null;prep.source=null;element('file').value='';element('filename').textContent='';element('clear-file').hidden=true;element('source-kind').textContent=label('synthetic_notice');request();});
element('detect').addEventListener('click',()=>request());
element('rotate').addEventListener('click',()=>request('rotate'));
element('crop').addEventListener('click',()=>{
  if(!prep.quad)return;
  const left=Math.floor(Math.min(...prep.quad.map(p=>p[0]))),top=Math.floor(Math.min(...prep.quad.map(p=>p[1])));
  const right=Math.ceil(Math.max(...prep.quad.map(p=>p[0]))),bottom=Math.ceil(Math.max(...prep.quad.map(p=>p[1])));
  request('crop',{left:String(left),top:String(top),width:String(right-left),height:String(bottom-top)});
});
element('form').addEventListener('submit',event=>{event.preventDefault();request('prepare');});
element('offset').addEventListener('input',()=>{element('offset-value').textContent=element('offset').value;invalidate();});
element('window').addEventListener('change',invalidate);element('a4').addEventListener('change',invalidate);
element('show-binary').addEventListener('click',()=>element('output').src=`data:image/png;base64,${prep.result.binaryBase64}`);
element('show-rectified').addEventListener('click',()=>element('output').src=`data:image/png;base64,${prep.result.rectifiedBase64}`);
element('download').addEventListener('click',()=>{
  if(!prep.result)return;
  const bytes=Uint8Array.from(atob(prep.result.binaryBase64),c=>c.charCodeAt(0));const url=URL.createObjectURL(new Blob([bytes],{type:'image/png'}));
  const link=document.createElement('a');link.href=url;link.download='photoglyph-prepared.png';link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
});
element('consent').addEventListener('change',()=>{if(!element('consent').checked&&prep.file)element('sample').click();busy(false);});
element('choose-file').addEventListener('click',()=>{if(element('consent').checked)element('file').click();});
element('file').addEventListener('change',()=>{
  const file=element('file').files[0];if(!file||!element('consent').checked||prep.busy)return;
  if(file.size>prep.maxBytes){error('too_large');return;}
  prep.file=file;prep.source=null;element('a4').checked=false;element('filename').textContent=file.name;element('clear-file').hidden=false;
  element('source-kind').textContent=label('uploaded_notice');request();
});
element('clear-file').addEventListener('click',()=>element('sample').click());
element('language').addEventListener('change',async event=>{prep.language=event.target.value;try{await configuration();busy(false);}catch(failure){error('generic');}});
(async()=>{try{await configuration();await request();}catch(failure){error('generic');}})();
