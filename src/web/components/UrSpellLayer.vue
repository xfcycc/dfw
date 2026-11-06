<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted } from 'vue';
import CharacterPawn from './CharacterPawn.vue';
import { UR_SPELL_ATLAS, type UrSpellSample } from '../ur-spell-preview';

const props = defineProps<{
  sample: UrSpellSample;
  actor: { x: number; y: number };
  target: { x: number; y: number };
  targetAppearance: { name: string; level?: number; avatarId?: string; color?: string };
  width: number;
  height: number;
  tileSize: number;
  reduced: boolean;
  showName: boolean;
  /** 多目标合奏只由第一层压暗棋盘。 */
  hideBackdrop?: boolean;
}>();
const emit = defineEmits<{ done: [] }>();
let timer: ReturnType<typeof setTimeout> | undefined;
const extent = computed(() => Math.min(props.width, props.height));
const artSize = computed(() => extent.value * .42);
// 大型主体向棋盘内侧展开，光带仍准确连接真实的施术者与目标坐标。
const stage = computed(() => props.sample.theme === 'punch' ? { x: props.width * .3, y: props.actor.y } : { x: props.width * .5, y: props.height * .46 });
const portal = computed(() => ({ x: Math.max(artSize.value * .6, Math.min(props.width - artSize.value * .6, props.target.x)), y: Math.max(artSize.value * .6, Math.min(props.height - artSize.value * .6, props.target.y)) }));
const angle = computed(() => Math.atan2(props.target.y - stage.value.y, props.target.x - stage.value.x) * 180 / Math.PI);
const styles = computed(() => ({
  '--duration': `${props.reduced ? 1000 : props.sample.duration}ms`, '--art': `${artSize.value}px`,
  '--tile': `${props.tileSize}px`, '--atlas': `url("${UR_SPELL_ATLAS}")`,
  '--dx': `${props.target.x - stage.value.x}px`, '--dy': `${props.target.y - stage.value.y}px`,
  '--angle': `${angle.value}deg`, '--unit': `${extent.value / 100}px`,
  '--pull-x': `${portal.value.x - props.target.x}px`, '--pull-y': `${portal.value.y - props.target.y}px`,
  '--hit-x': `${props.target.x - stage.value.x - Math.cos(angle.value * Math.PI / 180) * artSize.value * .4}px`,
  '--hit-y': `${props.target.y - stage.value.y - Math.sin(angle.value * Math.PI / 180) * artSize.value * .4}px`,
}));
const ribbon = computed(() => `M ${props.actor.x} ${props.actor.y} Q ${props.width * .5} ${props.height * .15} ${props.target.x} ${props.target.y}`);
/** 输入棋盘坐标，返回绝对定位样式，不改变棋盘数据。 */
function at(point: { x: number; y: number }): { left: string; top: string } { return { left: `${point.x}px`, top: `${point.y}px` }; }
/** 输入花瓣序号，返回从刀刃经弧线汇聚至目标的固定轨迹参数。 */
function petalStyle(index: number): Record<string, string> {
  const phase = index * 2.399;
  return { '--px': `${Math.cos(phase) * extent.value * .33}px`, '--py': `${Math.sin(phase) * extent.value * .25}px`, '--delay': `${index * 22}ms`, '--spin': `${index * 71}deg` };
}
/** 无入参，登记整段演出结束通知；静态模式只展示一秒提示。 */
onMounted(() => { timer = setTimeout(() => emit('done'), props.reduced ? 1000 : props.sample.duration); });
/** 无入参，移除播放计时，避免跳过或切换后发出过期通知。 */
onBeforeUnmount(() => { clearTimeout(timer); });
</script>

<template>
  <div class="ur-spell-layer" :class="[sample.theme, { reduced }]" :style="styles" role="status" :aria-label="showName ? `${sample.name}演出` : '招式演出'">
    <div v-if="!hideBackdrop" class="spell-dim"></div>
    <svg class="spell-lines" :viewBox="`0 0 ${width} ${height}`" aria-hidden="true">
      <path v-if="sample.theme === 'reaper'" class="seal-ribbon ribbon-back" :d="ribbon" pathLength="1" />
      <path v-if="sample.theme === 'reaper'" class="seal-ribbon" :d="ribbon" pathLength="1" />
      <path v-if="sample.theme === 'punch'" class="speed-line" :d="`M ${stage.x} ${stage.y} L ${target.x} ${target.y}`" pathLength="1" />
      <circle class="actor-ring" :cx="actor.x" :cy="actor.y" :r="tileSize * .32" />
      <circle class="target-ring" :cx="target.x" :cy="target.y" :r="tileSize * .44" />
    </svg>
    <div class="target-copy-anchor" :style="at(target)">
      <div class="target-copy">
        <CharacterPawn v-if="targetAppearance.avatarId" variant="pawn" :avatar-id="targetAppearance.avatarId" :color="targetAppearance.color" />
        <template v-else><span class="copy-building" :style="{ backgroundPosition: `${(targetAppearance.level ?? 0) % 2 * 100}% ${Math.floor((targetAppearance.level ?? 0) / 2) * 100}%` }"></span><b>{{ targetAppearance.level }}级</b></template>
      </div>
    </div>
    <div class="target-label" :style="{ left: `${Math.max(tileSize, Math.min(width - tileSize, target.x))}px`, top: `${target.y > height / 2 ? target.y - tileSize * .8 : target.y + tileSize * .65}px` }">{{ targetAppearance.name }}</div>
    <div v-if="sample.theme === 'kamui'" class="spell-anchor" :style="at(portal)">
      <div class="vortex"><span class="atlas-art art-kamui"></span></div>
      <div class="rift"></div>
    </div>
    <div v-if="sample.theme === 'kamui'" class="spell-anchor" :style="at(target)"><div class="seal-mark">封</div></div>
    <div v-if="sample.theme === 'punch'" class="spell-anchor" :style="at(stage)">
      <div class="fist-flight"><span class="atlas-art art-punch"></span></div>
    </div>
    <div v-if="sample.theme === 'punch'" class="spell-anchor" :style="at(target)">
      <div class="impact-burst"></div><div class="shockwave"></div><div class="shockwave second"></div>
      <i v-for="i in 9" :key="i" class="debris" :style="{ '--spin': `${i * 40}deg` }"></i>
    </div>
    <div v-if="sample.theme === 'sakura'" class="spell-anchor" :style="at(stage)">
      <div class="blade"><span class="atlas-art art-sakura"></span></div>
      <span v-for="i in 32" :key="i" class="petal-flight" :style="petalStyle(i)"><svg viewBox="0 0 24 30" aria-hidden="true"><path d="M12 28C-5 17 0 3 8 2L12 6L16 2C26 4 29 18 12 28Z" fill="#ffb8d6" stroke="#a84379" stroke-width="1.5" /><path d="M12 24L12 9" stroke="#fff1f8" stroke-width="1.5" /></svg></span>
    </div>
    <div v-if="sample.theme === 'reaper'" class="spell-anchor" :style="at({ x: Math.max(artSize * .7, actor.x), y: Math.max(artSize * .7, actor.y - artSize * .35) })">
      <div class="reaper-spirit"><span class="atlas-art art-reaper"></span></div>
    </div>
    <template v-if="sample.theme === 'reaper'"><div v-for="(point, i) in [actor, target]" :key="i" class="spell-anchor" :style="at(point)"><div class="seal-mark">封</div></div></template>
    <div v-if="showName || reduced" class="spell-title"><small>{{ reduced ? '静态演示 · 减少动态效果' : 'UR · 秘技' }}</small><strong v-if="showName">{{ sample.name }}</strong></div>
  </div>
</template>

<style scoped>
.ur-spell-layer { position:absolute; inset:0; z-index:20; overflow:hidden; pointer-events:none; isolation:isolate; border-radius:12px; --ink:#c4abff; }
.spell-dim { position:absolute; inset:0; background:radial-gradient(ellipse at center,#1c143d99,#100e25df); animation:dim var(--duration) both; }
.punch { --ink:#ffd7a1; }.sakura { --ink:#ffc1e1; }.reaper { --ink:#b5eeed; }
.spell-lines { position:absolute; inset:0; width:100%; height:100%; overflow:visible; fill:none; stroke:var(--ink); }
.actor-ring { stroke-width:2; opacity:.7; }.target-ring { stroke-width:3; stroke-dasharray:6 5; }
.spell-anchor,.target-copy-anchor { position:absolute; width:0; height:0; }
.atlas-art { display:block; width:100%; height:100%; background-image:var(--atlas); background-size:200% 200%; background-repeat:no-repeat; }
.art-kamui { background-position:0 0; }.art-punch { background-position:100% 0; }.art-sakura { background-position:0 100%; }.art-reaper { background-position:100% 100%; }
.target-copy { position:absolute; width:var(--tile); height:var(--tile); left:calc(var(--tile) / -2); top:calc(var(--tile) / -2); display:flex; align-items:center; justify-content:center; flex-direction:column; border:2px solid #ead6b5; border-radius:12px; background:#f3e3c5; color:#775841; box-shadow:0 0 28px #f6d99666; }
.target-copy b { position:absolute; bottom:2px; font-size:10px; }.copy-building { width:90%; height:90%; background:var(--property-levels) 100% 100% / 200% 200% no-repeat; }.target-copy :deep(.character-pawn) { width:70%; height:70%; }
.target-label { position:absolute; transform:translateX(-50%); white-space:nowrap; color:#fff2df; text-shadow:0 2px 5px #000; font-size:clamp(10px,calc(var(--unit) * 1.8),14px); }
.spell-title { position:absolute; top:13%; left:12%; right:12%; text-align:center; color:#fff0d7; text-shadow:0 2px 12px #000; animation:title var(--duration) both; }
.spell-title small { display:block; font-size:10px; letter-spacing:4px; }.spell-title strong { display:block; margin-top:7px; font-size:clamp(20px,calc(var(--unit) * 4.5),38px); letter-spacing:6px; }
.vortex,.fist-flight,.blade,.reaper-spirit { width:var(--art); height:var(--art); position:absolute; left:calc(var(--art) / -2); top:calc(var(--art) / -2); }
.vortex { animation:vortex var(--duration) both; filter:drop-shadow(0 0 12px #8366f7); }
.rift { position:absolute; width:calc(var(--tile) * .5); height:calc(var(--tile) * .5); left:calc(var(--tile) * -.25); top:calc(var(--tile) * -.25); border-radius:50%; background:#12091b; animation:rift var(--duration) both; }
.kamui .target-copy { animation:absorb var(--duration) both; }
.seal-mark { position:absolute; width:calc(var(--tile) * .8); height:calc(var(--tile) * .8); left:calc(var(--tile) * -.4); top:calc(var(--tile) * -.4); border:3px double var(--ink); color:var(--ink); border-radius:50%; display:grid; place-items:center; font-family:serif; font-weight:bold; font-size:calc(var(--tile) * .38); background:#211736dd; box-shadow:0 0 20px #be9afc88,inset 0 0 12px #be9afc55; animation:seal var(--duration) both; }
.fist-flight { animation:punch var(--duration) both; filter:drop-shadow(-12px 0 10px #ff9e4666); }
.speed-line { stroke:#fff0c4; stroke-width:calc(var(--unit) * 2); stroke-dasharray:1; animation:speed var(--duration) both; }
.impact-burst { width:calc(var(--art) * .7); height:calc(var(--art) * .7); position:absolute; left:calc(var(--art) * -.35); top:calc(var(--art) * -.35); background:#fff3c3; clip-path:polygon(50% 0,58% 32%,85% 7%,72% 40%,100% 50%,70% 58%,90% 90%,60% 72%,50% 100%,40% 70%,5% 90%,30% 58%,0 50%,30% 38%,10% 10%,42% 28%); animation:impact var(--duration) both; }
.shockwave { position:absolute; width:var(--tile); height:var(--tile); left:calc(var(--tile) / -2); top:calc(var(--tile) / -2); border:5px solid #fff0c4; border-radius:50%; animation:shock var(--duration) both; }.shockwave.second { animation-delay:110ms; border-width:2px; }
.debris { position:absolute; width:calc(var(--unit) * 1.7); height:calc(var(--unit) * 1.3); border:1px solid #805e56; background:#b7a092; animation:debris var(--duration) both; }
.punch .target-copy { animation:hit var(--duration) both; }
.blade { animation:blade var(--duration) both; filter:drop-shadow(0 0 10px #e98bbf); }
.petal-flight { position:absolute; width:calc(var(--unit) * 2.5); height:calc(var(--unit) * 3.2); left:0; top:0; animation:petal calc(var(--duration) - 700ms) var(--delay) both; filter:drop-shadow(0 0 3px #ffbadb); }.petal-flight svg { width:100%; height:100%; }
.reaper-spirit { width:calc(var(--art) * 1.3); height:calc(var(--art) * 1.3); left:calc(var(--art) * -.65); top:calc(var(--art) * -.65); animation:spirit var(--duration) both; filter:drop-shadow(0 0 18px #bb8be4); }
.seal-ribbon { stroke:#d6fbec; stroke-width:3; stroke-dasharray:1; animation:ribbon var(--duration) both; }.ribbon-back { stroke:#9b83d8; stroke-width:12; opacity:.5; }
@keyframes dim { 0%,100% { opacity:0; } 10%,88% { opacity:1; } }
@keyframes title { 0% { opacity:0; transform:translateY(10px); } 12%,30% { opacity:1; transform:translateY(0); } 47%,100% { opacity:0; transform:translateY(-8px); } }
@keyframes vortex { 0%,8% { opacity:0; transform:scale(.05) rotate(-90deg); } 25% { opacity:1; transform:scale(.8) rotate(0); } 60% { opacity:1; transform:scale(1.15) rotate(210deg); } 82%,100% { opacity:0; transform:scale(.02) rotate(400deg); } }
@keyframes rift { 0%,28%,83%,100% { opacity:0; transform:scale(0); } 47%,65% { opacity:1; transform:scale(1); } }
@keyframes absorb { 0%,28% { transform:translate(0,0) rotate(0) scale(1); opacity:1; } 66%,100% { transform:translate(var(--pull-x),var(--pull-y)) rotate(260deg) scale(0); opacity:0; } }
@keyframes seal { 0%,65% { opacity:0; transform:scale(2) rotate(-30deg); } 80%,95% { opacity:1; transform:scale(1) rotate(0); } 100% { opacity:0; transform:scale(1); } }
@keyframes punch { 0% { opacity:0; transform:translate(calc(var(--dx) * -.15),calc(var(--dy) * -.15)) rotate(var(--angle)) scale(.5); } 16%,30% { opacity:1; transform:translate(calc(var(--dx) * -.2),calc(var(--dy) * -.2)) rotate(var(--angle)) scale(.7); } 45%,51% { opacity:1; transform:translate(var(--hit-x),var(--hit-y)) rotate(var(--angle)) scale(1.1); } 65%,100% { opacity:0; transform:translate(var(--hit-x),var(--hit-y)) rotate(var(--angle)) scale(.95); } }
@keyframes speed { 0%,30% { opacity:0; stroke-dashoffset:1; } 44%,53% { opacity:1; stroke-dashoffset:0; } 65%,100% { opacity:0; stroke-dashoffset:0; } }
@keyframes impact { 0%,43% { opacity:0; transform:scale(.1) rotate(-20deg); } 46%,51% { opacity:1; transform:scale(1) rotate(0); } 66%,100% { opacity:0; transform:scale(1.6) rotate(10deg); } }
@keyframes shock { 0%,48% { opacity:0; transform:scale(.2); } 53% { opacity:1; transform:scale(1); } 88%,100% { opacity:0; transform:scale(8); } }
@keyframes hit { 0%,44% { transform:scale(1); opacity:1; } 49% { transform:scale(.8) rotate(-8deg); } 55% { transform:scale(1.08) rotate(5deg); } 66%,100% { transform:scale(.3) translateY(20px); opacity:0; } }
@keyframes debris { 0%,48% { opacity:0; transform:rotate(var(--spin)) translateX(0); } 52% { opacity:1; } 85%,100% { opacity:0; transform:rotate(var(--spin)) translateX(calc(var(--art) * .7)) rotate(120deg); } }
@keyframes blade { 0% { opacity:0; transform:translateY(-20px) scale(.8); } 15%,27% { opacity:1; transform:translateY(0) scale(.85); } 45%,100% { opacity:0; transform:translateY(35px) scale(.75); } }
@keyframes petal { 0%,23% { opacity:0; transform:translate(0,0) rotate(0) scale(0); } 40% { opacity:1; transform:translate(var(--px),var(--py)) rotate(var(--spin)) scale(1.1); } 66% { opacity:1; transform:translate(calc(var(--dx) + var(--px) * .28),calc(var(--dy) + var(--py) * .28)) rotate(calc(var(--spin) + 180deg)) scale(1); } 78% { opacity:1; transform:translate(var(--dx),var(--dy)) rotate(calc(var(--spin) + 260deg)) scale(.8); } 100% { opacity:0; transform:translate(calc(var(--dx) + var(--px) * .5),calc(var(--dy) + var(--py) * .4 + 40px)) rotate(calc(var(--spin) + 360deg)) scale(.4); } }
@keyframes spirit { 0%,5% { opacity:0; transform:translateY(40px) scale(.7); } 25%,62% { opacity:.95; transform:translateY(0) scale(1); } 90%,100% { opacity:0; transform:translateY(-20px) scale(1.05); } }
@keyframes ribbon { 0%,25% { stroke-dashoffset:1; opacity:0; } 40%,64% { stroke-dashoffset:0; opacity:1; } 82%,100% { stroke-dashoffset:-1; opacity:0; } }
.reduced * { animation:none!important; }.reduced .spell-dim { opacity:.8; }.reduced .spell-anchor { display:none; }.reduced .spell-title { opacity:1; }.reduced .speed-line,.reduced .seal-ribbon { display:none; }
</style>
