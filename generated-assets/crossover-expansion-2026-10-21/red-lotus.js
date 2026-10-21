/** 加载本地分层及冻结文案，播放红莲试作；不请求游戏接口或改动卡牌规则。 */
(async function () {
  const canvas = document.querySelector('canvas');
  const ctx = canvas.getContext('2d');
  const W = canvas.width;
  const duration = 9000;
  const ui = Object.fromEntries(['phase', 'replay', 'pause', 'static', 'snapshot', 'record', 'time', 'status'].map(id => [id, document.getElementById(id)]));
  const slider = document.querySelector('input[type=range]');
  const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches;
  let elapsed = 3600, last = performance.now(), paused = reduced, recording = false, staticMode = false;
  const assets = {};
  /** 输入图片地址，返回已解码图片；失败交给现有静态图与状态提示。 */
  async function load(src) { const img = new Image(); img.src = src; await img.decode(); return img; }
  /** 输入起止值与当前值，返回平滑的零至一过渡进度。 */
  function ease(a, b, x) { const u = Math.max(0, Math.min(1, (x - a) / (b - a))); return u * u * (3 - 2 * u); }
  /** 输入图像、缩放、透明度及垂直偏移，以莲心为锚点绘制一层。 */
  function layer(img, scale = 1, opacity = 1, dy = 0) { ctx.save(); ctx.globalAlpha = opacity; ctx.translate(W * .5, W * .79 + dy); ctx.scale(scale, scale); ctx.drawImage(img, -W * .5, -W * .79, W, W); ctx.restore(); }
  /** 输入循环内毫秒数，按聚光、开莲、光波与散瓣顺序绘制一帧。 */
  function draw(ms) {
    const t = ms / duration;
    const bloom = ease(.08, .37, t);
    const fade = 1 - ease(.81, 1, t);
    const energy = bloom * fade;
    const scale = .22 + bloom * .79;
    ctx.clearRect(0, 0, W, W);
    ctx.drawImage(assets.background, 0, 0, W, W);
    const glow = ctx.createRadialGradient(W * .5, W * .79, 0, W * .5, W * .79, W * .54);
    glow.addColorStop(0, `rgba(255,198,162,${.48 * energy})`); glow.addColorStop(.43, `rgba(235,57,71,${.17 * energy})`); glow.addColorStop(1, 'rgba(232,48,67,0)');
    ctx.fillStyle = glow; ctx.fillRect(0, 0, W, W);
    layer(assets.lotus, scale, energy);
    ctx.save(); ctx.translate(-W * .035, W * .045 - Math.sin(Math.PI * t) * W * .007); layer(assets.healer, .92); ctx.restore();
    layer(assets.lotusFront, scale, energy);
    for (let i = 0; i < 3; i++) {
      const u = Math.max(0, (t - .2 - i * .095) / .57);
      if (u <= 0 || u >= 1) continue;
      ctx.save(); ctx.globalCompositeOperation = 'screen'; ctx.globalAlpha = Math.sin(Math.PI * u) * .62;
      ctx.strokeStyle = '#ffd5bf'; ctx.lineWidth = 1.8; ctx.shadowBlur = 14; ctx.shadowColor = '#ff576b'; ctx.beginPath(); ctx.ellipse(W * .5, W * .84, W * (.08 + .6 * u), W * (.025 + .13 * u), -.04, 0, Math.PI * 2); ctx.stroke(); ctx.restore();
    }
    for (let i = 0; i < 25; i++) {
      const u = Math.max(0, Math.min(1, (t - .16 - (i % 5) * .018) / .78));
      const a = i * 2.399 + u * 1.65, r = W * (.08 + .43 * u);
      ctx.save(); ctx.globalAlpha = Math.sin(Math.PI * u) * .76;
      ctx.translate(W * .5 + Math.cos(a) * r, W * .8 + Math.sin(a) * r * .26 - W * u * (.27 + (i % 4) * .09)); ctx.rotate(a + u * 3);
      const s = W * (.0045 + (i % 4) * .0025); ctx.scale(s, s * (.55 + .3 * Math.sin(a)));
      ctx.fillStyle = i % 4 ? '#e95765' : '#ffe9cf'; ctx.shadowBlur = 3; ctx.shadowColor = '#ffb5a6'; ctx.beginPath(); ctx.moveTo(0, -1.5); ctx.bezierCurveTo(1.4, -.3, .75, 1.1, 0, 1.5); ctx.bezierCurveTo(-.7, .4, -.8, -.6, 0, -1.5); ctx.fill(); ctx.restore();
    }
    const phase = t < .17 ? 0 : t < .4 ? 1 : t < .78 ? 2 : 3;
    ui.phase.textContent = ['凝光 · 聚气', '红莲 · 层层展开', '治疗光波 · 花瓣回旋', '余韵 · 花瓣散去'][phase];
    document.querySelectorAll('.phases span').forEach((el, i) => el.classList.toggle('active', i === phase));
    slider.value = (ms / 1000).toFixed(2); ui.time.textContent = `${(ms / 1000).toFixed(1)} / 9.0 秒`;
  }
  /** 输入浏览器帧时间，推进或暂停演出，返回下一帧调度。 */
  function tick(now) { if (!paused && !staticMode) elapsed = (elapsed + Math.min(now - last, 80)) % duration; last = now; if (!staticMode) draw(elapsed); requestAnimationFrame(tick); }
  /** 输入导出文件的Blob及名称，通过本地下载提供审阅文件。 */
  function download(blob, name) { const url = URL.createObjectURL(blob); const a = document.getElementById('download'); a.href = url; a.download = name; a.click(); setTimeout(() => URL.revokeObjectURL(url), 30000); }
  /** 输入是否暂停，更新播放按钮与状态，返回空。 */
  function setPaused(value) { paused = value; ui.pause.textContent = paused ? '继续' : '暂停'; }
  try {
    const manifest = await (await fetch('manifest.json', { cache: 'no-store' })).json();
    const card = manifest.entries.find(c => c.code === 'C14');
    document.getElementById('description').textContent = card.description;
    document.getElementById('effect').textContent = card.effect;
    document.getElementById('quote').textContent = `「${card.quote}」`;
    await Promise.all(['background', 'healer', 'lotus'].map(async key => { assets[key] = await load(`art/C14-${key}.png`); }));
    // 前景莲瓣使用渐变透明遮罩，人物与后景莲瓣之间保持柔和遮挡。
    assets.lotusFront = document.createElement('canvas'); assets.lotusFront.width = W; assets.lotusFront.height = W;
    const front = assets.lotusFront.getContext('2d'); front.drawImage(assets.lotus, 0, 0, W, W);
    const mask = front.createLinearGradient(0, W * .61, 0, W * .82); mask.addColorStop(0, 'transparent'); mask.addColorStop(1, '#fff');
    front.globalCompositeOperation = 'destination-in'; front.fillStyle = mask; front.fillRect(0, 0, W, W);
    const figure = document.createElement('canvas'); figure.width = W; figure.height = W;
    const figureCtx = figure.getContext('2d'); figureCtx.drawImage(assets.healer, 0, 0, W, W);
    const hem = figureCtx.createLinearGradient(0, W * .72, 0, W * .94); hem.addColorStop(0, '#fff'); hem.addColorStop(1, 'transparent');
    figureCtx.globalCompositeOperation = 'destination-in'; figureCtx.fillStyle = hem; figureCtx.fillRect(0, 0, W, W); assets.healer = figure;
    document.body.classList.add('ready'); Object.values(ui).filter(el => el.tagName === 'BUTTON').forEach(b => { b.disabled = false; });
    setPaused(reduced); draw(elapsed); requestAnimationFrame(tick);
    ui.replay.onclick = () => { staticMode = false; document.body.classList.remove('static'); ui.static.textContent = '查看静态图'; elapsed = 0; last = performance.now(); setPaused(false); };
    ui.pause.onclick = () => setPaused(!paused);
    ui.static.onclick = () => { staticMode = !staticMode; document.body.classList.toggle('static', staticMode); ui.static.textContent = staticMode ? '返回动态' : '查看静态图'; };
    slider.oninput = () => { setPaused(true); elapsed = Number(slider.value) * 1000; draw(elapsed); };
    ui.snapshot.onclick = async () => { if (staticMode) download(await (await fetch('art/C14.png')).blob(), '白云无尽-红莲静态稿.png'); else canvas.toBlob(blob => { if (blob) download(blob, '白云无尽-红莲试作.png'); }); };
    ui.record.onclick = () => {
      if (recording) return;
      if (!canvas.captureStream || !window.MediaRecorder) { ui.status.textContent = '当前浏览器不支持录制，可使用保存画面。'; return; }
      recording = true; ui.record.disabled = true; ui.status.textContent = '正在保存 9 秒演出…'; ui.replay.click();
      const type = ['video/webm;codecs=vp9', 'video/webm'].find(t => MediaRecorder.isTypeSupported(t));
      const stream = canvas.captureStream(30); const recorder = new MediaRecorder(stream, type ? { mimeType: type, videoBitsPerSecond: 7000000 } : undefined); const chunks = [];
      recorder.ondataavailable = e => { if (e.data.size) chunks.push(e.data); };
      recorder.onstop = () => { download(new Blob(chunks, { type: recorder.mimeType }), '白云无尽-红莲试作.webm'); stream.getTracks().forEach(track => track.stop()); recording = false; ui.record.disabled = false; ui.status.textContent = '演示已保存，未上传。'; };
      recorder.start(); setTimeout(() => recorder.stop(), duration);
    };
  } catch (error) { ui.status.textContent = '动态素材未能加载，当前展示完整静态插画。'; ui.phase.textContent = '静态预览'; console.error(error); }
})();
