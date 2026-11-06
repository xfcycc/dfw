<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import CharacterPawn from './CharacterPawn.vue';
import CardSpellProp from './CardSpellProp.vue';
import type { RegularSpellSample } from '../regular-spell-preview';
type Point = { x: number; y: number };
const props = defineProps<{ sample: RegularSpellSample; actor: Point; target: Point; path: Point[]; tileSize: number; width: number; height: number; reduced: boolean; actorAppearance?: { avatarId?: string; color: string }; payment?: { before: number; after: number } }>();
const traveler = ref<HTMLElement>();
let flight: Animation | undefined;
const swap = computed(() => ['cloak', 'room'].includes(props.sample.theme));
const moving = computed(() => props.sample.steps !== undefined);
const focus = computed(() => ({ x: Math.max(props.tileSize * .8, Math.min(props.width - props.tileSize * .8, props.target.x)), y: Math.max(props.tileSize * .8, Math.min(props.height - props.tileSize * .8, props.target.y)) }));
const route = computed(() => props.path.map((p, i) => `${i ? 'L' : 'M'}${p.x} ${p.y}`).join(' '));
/** 输入棋盘点，输出绝对定位样式。 */
function at(p: Point): { left: string; top: string } { return { left: `${p.x}px`, top: `${p.y}px` }; }
/** 输入路径、棋子节点及减少动画偏好，重定位时保留播放进度，不执行游戏移动。 */
watch([traveler, () => props.path, () => props.reduced], () => {
  const elapsed = Number(flight?.currentTime ?? 0);
  flight?.cancel();
  if (props.reduced || !moving.value || props.sample.theme === 'door') return;
  const frames = props.path.map((p, i) => ({ transform: `translate(${p.x - props.actor.x}px, ${p.y - props.actor.y}px)`, offset: .25 + .6 * i / (props.path.length - 1) }));
  flight = traveler.value?.animate([{ transform: 'translate(0, 0)', offset: 0 }, ...frames, { ...frames[frames.length - 1]!, offset: 1 }], { duration: props.sample.duration, fill: 'both', easing: 'linear' });
  if (flight) flight.currentTime = elapsed;
}, { flush: 'post' });
/** 无入参，取消路径动画，切换后不保留副本。 */
onBeforeUnmount(() => flight?.cancel());
</script>

<template>
  <div class="batch-stage" :class="sample.theme" :style="{ '--u': `${tileSize}px` }">
    <div v-if="reduced" class="static-token" :style="at({x:width*.5,y:height*.52})"><CardSpellProp :kind="sample.theme"/><small>招式提示</small></div>
    <template v-else>
      <svg v-if="moving" class="route" :viewBox="`0 0 ${width} ${height}`"><path :d="route"/><circle :cx="target.x" :cy="target.y" :r="tileSize*.35"/></svg>
      <div v-if="sample.theme === 'room'" class="domain"><CardSpellProp kind="room" /></div>
      <template v-if="swap">
        <div v-for="(p,i) in [actor,target]" :key="i" class="anchor" :style="at(p)">
          <div v-if="sample.theme === 'cloak'" class="art empty-cloak"><CardSpellProp kind="cloak"/></div>
          <div class="pawn swap-copy" :style="i ? {'--dx':`${actor.x-target.x}px`,'--dy':`${actor.y-target.y}px`} : {}"><CharacterPawn variant="pawn" :avatar-id="i ? 'cat' : 'explorer'" :color="i ? '#d8978b' : '#739d81'"/></div>
        </div>
      </template>
      <template v-else-if="moving">
        <template v-if="sample.theme === 'door'"><div v-for="(p,i) in [actor,target]" :key="i" class="anchor" :style="at(p)"><div class="art doorway"><CardSpellProp kind="door"/></div></div></template>
        <div v-if="sample.theme === 'banana'" class="anchor" :style="at(actor)"><div class="art peel"><CardSpellProp kind="banana"/></div></div>
        <div class="anchor" :style="at(actor)"><div ref="traveler" :class="{ 'door-travel':sample.theme === 'door' }"><div class="rider" :class="sample.theme"><div class="pawn"><CharacterPawn variant="pawn" :avatar-id="actorAppearance?.avatarId ?? 'explorer'" :color="actorAppearance?.color ?? '#739d81'"/></div><div v-if="['cloud','copter'].includes(sample.theme)" class="art vehicle"><CardSpellProp :kind="sample.theme"/></div></div></div></div>
      </template>
      <div v-else-if="sample.theme === 'groupon'" class="anchor" :style="at({x:width*.5,y:height*.5})"><div class="art phone"><CardSpellProp kind="groupon"/></div><div class="receipt">待付费用 <s>¥{{ (payment?.before ?? 2000).toLocaleString() }}</s><strong>¥{{ (payment?.after ?? 1000).toLocaleString() }}</strong></div><b class="stamp">已核销</b></div>
      <div v-else class="anchor" :style="at(focus)">
        <template v-if="sample.theme === 'sap' || sample.theme === 'staff'"><div class="art weapon" :class="sample.theme"><CardSpellProp :kind="sample.theme"/></div><div class="hit">✦</div><div v-if="sample.theme === 'sap'" class="dizzy"><i v-for="i in 3" :key="i" :style="{ '--i':i }">★</i></div><div v-else class="chips"><i v-for="i in 5" :key="i" :style="{ '--i':i }"></i></div></template>
        <template v-else-if="sample.theme === 'atField'"><div v-for="i in 3" :key="i" class="art wall" :style="{ '--i':i }"><CardSpellProp kind="atField"/></div><div class="wall-strike"></div></template>
        <template v-else-if="sample.theme === 'vines'"><div class="art vine-growth"><CardSpellProp kind="vines"/></div><i v-for="i in 4" :key="i" class="dew" :style="{ '--i':i }">✦</i></template>
        <template v-else-if="sample.theme === 'substitute'"><div class="art log-token"><CardSpellProp kind="substitute"/></div><i v-for="i in 5" :key="i" class="smoke" :style="{ '--i':i }"></i><span class="waiver">下次免付</span></template>
      </div>
    </template>
  </div>
</template>

<style scoped>
.batch-stage { position:absolute; inset:0; --ink:#59463e; }.anchor { position:absolute; width:0; height:0; }.art { position:absolute; width:calc(var(--u)*1.4); height:calc(var(--u)*1.4); left:calc(var(--u)*-.7); top:calc(var(--u)*-.7); filter:drop-shadow(0 3px 2px #59463e25); }.pawn { position:absolute; width:calc(var(--u)*.72); height:calc(var(--u)*.72); left:calc(var(--u)*-.36); top:calc(var(--u)*-.36); display:grid; place-items:center; }.pawn :deep(.character-pawn) { width:100%; height:100%; }
.route { position:absolute; inset:0; width:100%; height:100%; fill:none; stroke:#9cba9b; stroke-width:2; stroke-dasharray:4 5; opacity:.8; }.static-token { position:absolute; width:calc(var(--u)*1.6); transform:translate(-50%,-50%); text-align:center; }.static-token small { display:block; color:#76543e; background:#fff8e8; border-radius:8px; padding:4px; }
.domain { position:absolute; inset:0; animation:domain var(--duration) both; }.domain :deep(svg) { stroke-width:1; }.swap-copy,.door-travel { animation:relocate var(--duration) both; }.empty-cloak { width:var(--u); height:var(--u); left:calc(var(--u)*-.5); top:calc(var(--u)*-.5); animation:shed var(--duration) both; }
.doorway { width:calc(var(--u)*.9); height:var(--u); left:calc(var(--u)*-.45); top:calc(var(--u)*-.5); animation:appear var(--duration) both; }.doorway :deep(.door-leaf) { transform-box:fill-box; transform-origin:left; animation:open-door var(--duration) both; }
.rider { position:absolute; }.rider.copter { animation:lift var(--duration) both; }.rider.cloud { animation:tumble var(--duration) both; }.rider.banana { animation:slip var(--duration) both; }.vehicle { width:var(--u); height:var(--u); left:calc(var(--u)*-.5); }.copter .vehicle { top:calc(var(--u)*-.95); }.cloud .vehicle { top:calc(var(--u)*-.1); }.copter :deep(.rotor) { transform-box:fill-box; transform-origin:center; animation:rotor 100ms linear infinite; }.peel { width:calc(var(--u)*.65); height:calc(var(--u)*.65); left:calc(var(--u)*-.1); top:calc(var(--u)*-.05); animation:peel var(--duration) both; }
.phone { left:calc(var(--u)*-1.2); animation:appear var(--duration) both; }.phone :deep(.voucher) { transform-box:fill-box; transform-origin:center; animation:voucher var(--duration) both; }.receipt { position:absolute; left:calc(var(--u)*.15); top:calc(var(--u)*-.5); width:calc(var(--u)*1.35); background:#fff6de; border:2px dashed #c4a576; border-radius:8px; padding:8px; color:#876647; font-size:calc(var(--u)*.18); }.receipt s,.receipt strong { display:block; margin-top:5px; }.receipt strong { color:#49876f; font-size:calc(var(--u)*.28); animation:late var(--duration) both; }.stamp { position:absolute; white-space:nowrap; left:calc(var(--u)*-.6); top:calc(var(--u)*.1); border:3px double #c76559; color:#b44e46; background:#fff0dccc; padding:5px; border-radius:5px; font-size:calc(var(--u)*.22); animation:stamp var(--duration) both; }
.weapon { transform-origin:20% 80%; animation:swing var(--duration) both; }.weapon.sap { top:calc(var(--u)*-1); left:calc(var(--u)*-.8); }.weapon.staff { width:calc(var(--u)*2); left:calc(var(--u)*-1.5); animation:extend-swing var(--duration) both; }.hit { position:absolute; left:calc(var(--u)*-.35); top:calc(var(--u)*-.6); font-size:var(--u); color:#ffde87; animation:hit var(--duration) both; }.dizzy { position:absolute; top:calc(var(--u)*-.7); animation:late var(--duration) both; }.dizzy i { position:absolute; color:#e5b747; font-size:calc(var(--u)*.3); animation:orbit 900ms linear infinite; animation-delay:calc(var(--i)*-300ms); }.chips i { position:absolute; width:calc(var(--u)*.18); height:calc(var(--u)*.12); background:#bd8b70; border:1px solid var(--ink); animation:chips var(--duration) both; }
.wall { animation:wall var(--duration) both; transform:scale(calc(.6 + var(--i)*.18)); animation-delay:calc(var(--i)*40ms); }.wall-strike { position:absolute; width:calc(var(--u)*.7); height:calc(var(--u)*.2); border-radius:50%; background:#ffefb9; box-shadow:0 0 8px #eaa04d; animation:repel var(--duration) both; }.vine-growth { animation:grow var(--duration) both; }.dew { position:absolute; color:#d5ffff; left:calc((var(--i) - 2.5)*var(--u)*.2); top:calc((var(--i) - 3)*var(--u)*.2); animation:late var(--duration) both; }.log-token { animation:token var(--duration) both; }.smoke { position:absolute; width:calc(var(--u)*.55); height:calc(var(--u)*.55); border:2px solid #d3d9cc; border-radius:50%; background:#fffcef; animation:smoke var(--duration) both; }.waiver { position:absolute; white-space:nowrap; left:calc(var(--u)*-.55); top:calc(var(--u)*.55); padding:4px 8px; background:#fff3cd; border:1px solid #d4b46f; border-radius:10px; color:#876647; font-size:calc(var(--u)*.17); animation:late var(--duration) both; }
@keyframes appear { 0% { opacity:0; scale:.3; } 20%,85% { opacity:1; scale:1; } 100% { opacity:0; scale:.9; } }
@keyframes relocate { 0%,32% { opacity:1; transform:translate(0,0) scale(1); } 42% { opacity:0; transform:translate(0,0) scale(.15); } 57% { opacity:0; transform:translate(var(--dx),var(--dy)) scale(.15); } 70%,100% { opacity:1; transform:translate(var(--dx),var(--dy)) scale(1); } }
@keyframes shed { 0% { opacity:0; } 20%,43% { opacity:1; transform:scale(1); } 75% { opacity:1; transform:translateY(calc(var(--u)*.35)) scale(1.2,.15); } 100% { opacity:0; transform:translateY(calc(var(--u)*.35)) scale(1.2,.15); } }
@keyframes open-door { 0%,17%,90%,100% { transform:scaleX(1); } 33%,75% { transform:scaleX(.18) skewY(-10deg); } }
@keyframes lift { 0%,100% { transform:translateY(0); } 23%,84% { transform:translateY(calc(var(--u)*-.22)); } }
@keyframes tumble { 0%,100% { transform:translateY(0) rotate(0); } 24% { transform:translateY(calc(var(--u)*-.2)) rotate(-12deg); } 44% { transform:translateY(calc(var(--u)*-.2)) rotate(12deg); } 65% { transform:translateY(calc(var(--u)*-.2)) rotate(-10deg); } 83% { transform:translateY(calc(var(--u)*-.2)) rotate(8deg); } }
@keyframes rotor { 50% { transform:scaleX(.2); } }
@keyframes slip { 0%,15%,100% { transform:rotate(0); } 27%,72% { transform:rotate(-55deg); } 86% { transform:rotate(16deg); } }
@keyframes peel { 0% { opacity:0; transform:translateX(var(--u)); } 20%,70% { opacity:1; transform:translateX(0) rotate(15deg); } 100% { opacity:0; } }
@keyframes voucher { 0%,20% { transform:scale(.6); } 35%,75% { transform:scale(1.2); } 100% { transform:scale(1); } }
@keyframes late { 0%,52% { opacity:0; } 66%,100% { opacity:1; } }
@keyframes stamp { 0%,43% { opacity:0; transform:scale(2) rotate(-18deg); } 56%,100% { opacity:1; transform:scale(1) rotate(-12deg); } }
@keyframes swing { 0% { opacity:0; transform:rotate(-70deg); } 25% { opacity:1; transform:rotate(-70deg); } 45%,53% { opacity:1; transform:rotate(30deg); } 80%,100% { opacity:0; transform:rotate(-30deg); } }
@keyframes extend-swing { 0% { opacity:0; transform:rotate(-40deg) scaleX(.08); } 20% { opacity:1; transform:rotate(-40deg) scaleX(.08); } 38% { transform:rotate(-40deg) scaleX(1); } 48%,56% { opacity:1; transform:rotate(25deg) scaleX(1); } 90%,100% { opacity:0; transform:rotate(25deg) scaleX(.08); } }
@keyframes hit { 0%,44%,70%,100% { opacity:0; scale:.2; } 49%,55% { opacity:1; scale:1; } }
@keyframes orbit { from { transform:rotate(0) translateX(calc(var(--u)*.45)) rotate(0); } to { transform:rotate(360deg) translateX(calc(var(--u)*.45)) rotate(-360deg); } }
@keyframes chips { 0%,48% { opacity:0; transform:translate(0,0); } 54% { opacity:1; } 95%,100% { opacity:0; transform:translate(calc((var(--i) - 3)*var(--u)*.22),calc(var(--u)*.5)) rotate(80deg); } }
@keyframes wall { 0% { opacity:0; scale:.1; } 40% { opacity:1; scale:1; } 55% { opacity:.5; scale:1.07; } 70%,100% { opacity:.85; scale:1; } }
@keyframes repel { 0%,30% { opacity:0; transform:translateX(var(--u)); } 48% { opacity:1; transform:translateX(0); } 75%,100% { opacity:0; transform:translateX(var(--u)) scale(.1); } }
@keyframes grow { 0% { clip-path:inset(100% 0 0); transform:scaleX(1.2); } 65% { clip-path:inset(0); transform:scaleX(1.1); } 80%,100% { clip-path:inset(0); transform:scaleX(.85); } }
@keyframes token { 0%,15% { opacity:0; transform:scale(.3); } 45%,80% { opacity:1; transform:scale(1); } 100% { opacity:1; transform:scale(.65); } }
@keyframes smoke { 0% { opacity:0; transform:scale(.2); } 25% { opacity:1; } 70%,100% { opacity:0; transform:translate(calc((var(--i) - 3)*var(--u)*.3),calc(var(--u)*-.5)) scale(1.5); } }
@keyframes domain { 0% { opacity:0; transform:scale(.05); } 28%,78% { opacity:.85; transform:scale(1); } 100% { opacity:0; transform:scale(.1); } }
</style>
