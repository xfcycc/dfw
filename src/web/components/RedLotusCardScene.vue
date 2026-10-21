<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import sceneSources from '../ur-card-art.json';

const canvas = ref<HTMLCanvasElement | null>(null);
const ready = ref(false);
const sources = sceneSources.fortune_crossover_endlessClouds;
const size = 640;
const duration = 9000;
let frame = 0;
let disposed = false;
let observer: IntersectionObserver | undefined;
type Layer = HTMLImageElement | HTMLCanvasElement;

/** 输入起止时间与当前时间，返回零至一的平滑过渡。 */
function ease(start: number, end: number, value: number): number {
  const progress = Math.max(0, Math.min(1, (value - start) / (end - start)));
  return progress * progress * (3 - 2 * progress);
}

/** 输入OSS图片地址，返回解码完成的图层；不读取像素，沿用现有跨域图片加载方式。 */
async function loadImage(source: string): Promise<HTMLImageElement> {
  const image = new Image();
  image.src = source;
  await image.decode();
  return image;
}

/** 输入图层、透明度起止位置及方向，返回用于前后景遮挡的渐变透明图层。 */
function maskedLayer(image: Layer, start: number, end: number, reverse = false): HTMLCanvasElement {
  const layer = document.createElement('canvas');
  layer.width = size; layer.height = size;
  const context = layer.getContext('2d')!;
  context.drawImage(image, 0, 0, size, size);
  const mask = context.createLinearGradient(0, size * start, 0, size * end);
  mask.addColorStop(0, reverse ? '#fff' : 'transparent');
  mask.addColorStop(1, reverse ? 'transparent' : '#fff');
  context.globalCompositeOperation = 'destination-in';
  context.fillStyle = mask; context.fillRect(0, 0, size, size);
  return layer;
}

/** 无入参；分层加载成功后播放已确认的九秒红莲演出，失败时保留下层静态原图。 */
onMounted(async () => {
  const element = canvas.value;
  const context = element?.getContext('2d');
  if (!element || !context) return;
  try {
    const [background, originalHealer, lotus] = await Promise.all([sources.background, sources.foreground, sources.lotus].map(loadImage));
    if (disposed) return;
    const healer = maskedLayer(originalHealer, .72, .94, true);
    const lotusFront = maskedLayer(lotus, .61, .82);
    const reduced = matchMedia('(prefers-reduced-motion: reduce)');
    let visible = true, elapsed = 3600, last = performance.now(), painted = 0;
    observer = new IntersectionObserver(entries => { visible = entries[0]?.isIntersecting ?? false; });
    observer.observe(element);
    /** 输入图层、缩放、透明度和垂直位移，以莲心为锚点绘制一层。 */
    function layer(image: Layer, scale = 1, opacity = 1, dy = 0): void {
      context!.save(); context!.globalAlpha = opacity;
      context!.translate(size * .5, size * .79 + dy); context!.scale(scale, scale);
      context!.drawImage(image, -size * .5, -size * .79, size, size); context!.restore();
    }
    /** 输入浏览器帧时间，绘制聚光、莲瓣展开、扩散光波与飞散花瓣；暂停时保持当前进度。 */
    function tick(now: number): void {
      const delta = Math.min(now - last, 80); last = now;
      if (visible && !reduced.matches && !document.hidden && !element!.closest('.paused')) {
        elapsed = (elapsed + delta) % duration;
        if (now - painted >= 32) {
          painted = now;
          const t = elapsed / duration;
          const bloom = ease(.08, .37, t), energy = bloom * (1 - ease(.81, 1, t)), scale = .22 + bloom * .79;
          context!.clearRect(0, 0, size, size); context!.drawImage(background, 0, 0, size, size);
          const glow = context!.createRadialGradient(size * .5, size * .79, 0, size * .5, size * .79, size * .54);
          glow.addColorStop(0, `rgba(255,198,162,${.48 * energy})`); glow.addColorStop(.43, `rgba(235,57,71,${.17 * energy})`); glow.addColorStop(1, 'rgba(232,48,67,0)');
          context!.fillStyle = glow; context!.fillRect(0, 0, size, size);
          layer(lotus, scale, energy);
          context!.save(); context!.translate(-size * .035, size * .045 - Math.sin(Math.PI * t) * size * .007); layer(healer, .92); context!.restore();
          layer(lotusFront, scale, energy);
          for (let i = 0; i < 3; i++) {
            const progress = Math.max(0, (t - .2 - i * .095) / .57);
            if (progress <= 0 || progress >= 1) continue;
            context!.save(); context!.globalCompositeOperation = 'screen'; context!.globalAlpha = Math.sin(Math.PI * progress) * .62;
            context!.strokeStyle = '#ffd5bf'; context!.lineWidth = 1.2; context!.shadowBlur = 9; context!.shadowColor = '#ff576b';
            context!.beginPath(); context!.ellipse(size * .5, size * .84, size * (.08 + .6 * progress), size * (.025 + .13 * progress), -.04, 0, Math.PI * 2); context!.stroke(); context!.restore();
          }
          for (let i = 0; i < 25; i++) {
            const progress = Math.max(0, Math.min(1, (t - .16 - (i % 5) * .018) / .78));
            const angle = i * 2.399 + progress * 1.65, radius = size * (.08 + .43 * progress);
            context!.save(); context!.globalAlpha = Math.sin(Math.PI * progress) * .76;
            context!.translate(size * .5 + Math.cos(angle) * radius, size * .8 + Math.sin(angle) * radius * .26 - size * progress * (.27 + (i % 4) * .09)); context!.rotate(angle + progress * 3);
            const length = size * (.0045 + (i % 4) * .0025); context!.scale(length, length * (.55 + .3 * Math.sin(angle)));
            context!.fillStyle = i % 4 ? '#e95765' : '#ffe9cf'; context!.beginPath(); context!.moveTo(0, -1.5); context!.bezierCurveTo(1.4, -.3, .75, 1.1, 0, 1.5); context!.bezierCurveTo(-.7, .4, -.8, -.6, 0, -1.5); context!.fill(); context!.restore();
          }
          ready.value = true;
        }
      }
      frame = requestAnimationFrame(tick);
    }
    frame = requestAnimationFrame(tick);
  } catch {
    ready.value = false;
  }
});

/** 无入参；组件卸载后取消绘制和可见性观察，不再持有页面节点。 */
onBeforeUnmount(() => { disposed = true; cancelAnimationFrame(frame); observer?.disconnect(); });
</script>

<template>
  <div class="ur-scene red-lotus-scene" :class="{ ready }" aria-hidden="true"><canvas ref="canvas" :width="size" :height="size"></canvas></div>
</template>

<style scoped>
.red-lotus-scene { position: absolute; inset: 0; overflow: hidden; pointer-events: none; opacity: 0; }
.red-lotus-scene.ready { opacity: 1; }
canvas { display: block; width: 100%; height: 100%; }
@media (prefers-reduced-motion: reduce) { .red-lotus-scene { display: none; } }
</style>
