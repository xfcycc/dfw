<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted } from 'vue';
import CharacterPawn from './CharacterPawn.vue';
import CardSpellProp from './CardSpellProp.vue';
import BatchSpellStage from './BatchSpellStage.vue';
import type { RegularSpellSample } from '../regular-spell-preview';

const props = defineProps<{ sample: RegularSpellSample; actor: { x: number; y: number }; target: { x: number; y: number }; path: { x: number; y: number }[]; tileSize: number; width: number; height: number; reduced: boolean; showName: boolean }>();
const emit = defineEmits<{ done: [] }>();
let timer: ReturnType<typeof setTimeout> | undefined;
const focus = computed(() => ({ x: Math.min(props.width - props.tileSize * 1.2, props.target.x), y: props.target.y }));
const styles = computed(() => ({ '--duration': `${props.reduced ? 800 : props.sample.duration}ms`, '--tile': `${props.tileSize}px`, '--size': `${props.tileSize * 1.9}px`, '--dx': `${props.target.x - props.actor.x}px`, '--dy': `${props.target.y - props.actor.y}px` }));
/** 输入棋盘坐标，返回图层的绝对位置。 */
function at(point: { x: number; y: number }): { left: string; top: string } { return { left: `${point.x}px`, top: `${point.y}px` }; }
/** 无入参，按当前样板时长通知演出完成。 */
onMounted(() => { timer = setTimeout(() => emit('done'), props.reduced ? 800 : props.sample.duration); });
/** 无入参，取消旧样板的完成通知。 */
onBeforeUnmount(() => clearTimeout(timer));
</script>

<template>
  <div class="regular-spell-layer" :class="[sample.theme, { reduced }]" :style="styles" role="status" :aria-label="showName ? `${sample.name}演出` : '招式演出'">
    <svg class="connections" :viewBox="`0 0 ${width} ${height}`" aria-hidden="true"><path v-if="sample.theme === 'slowBeam'" class="beam" :d="`M${actor.x} ${actor.y}L${target.x} ${target.y}`" pathLength="1" /><circle :cx="target.x" :cy="target.y" :r="tileSize * .5" class="target-ring" /><path v-if="sample.theme === 'thunder'" class="jump-trace" :d="`M${actor.x} ${actor.y}Q${width*.5} ${height*.25} ${target.x} ${target.y}`" /></svg>
    <div class="caption" :style="{ left: `${width * .5}px`, top: `${height * .22}px` }"><b>{{ showName ? sample.name : '招式发动' }}</b><small>{{ reduced ? '静态演示' : sample.rarity }}</small></div>
    <BatchSpellStage v-if="sample.target" :sample="sample" :actor="actor" :target="target" :path="path" :tile-size="tileSize" :width="width" :height="height" :reduced="reduced" />

    <div v-if="sample.theme === 'concrete'" class="anchor" :style="at(focus)"><div class="prop concrete-prop"><CardSpellProp kind="concrete" /></div><div class="dust"></div><span class="impact-word">咚!</span></div>
    <template v-else-if="sample.theme === 'slowBeam'"><div class="anchor" :style="at(actor)"><i v-for="i in 4" :key="i" class="slow-ring" :style="{ '--lag': `${i*65}ms` }"></i></div><div class="anchor" :style="at(target)"><div class="pawn slow-echo"><CharacterPawn variant="pawn" avatar-id="cat" color="#d8978b" /></div><div class="pawn slow-pawn"><CharacterPawn variant="pawn" avatar-id="cat" color="#d8978b" /></div><span class="slow-word">慢…</span></div></template>
    <div v-else-if="sample.theme === 'snatch'" class="anchor" :style="at(actor)"><div class="hand-flight"><div class="card-back">✦</div><div class="prop hand-prop"><CardSpellProp kind="snatch" /></div></div></div>
    <div v-else-if="sample.theme === 'repair'" class="anchor" :style="at(focus)"><div class="building-copy"></div><i v-for="i in 6" :key="i" class="brick" :style="{ '--bx': `${(i%2 ? -1:1)*(35+i*5)}px`, '--by': `${15+i*8}px`, '--turn': `${i*37}deg` }"></i><div class="prop wand"><CardSpellProp kind="repair" /></div><span class="upgrade-star">✦</span><span class="level-tag">2 → 3 级</span></div>
    <div v-else-if="sample.theme === 'coupon'" class="anchor" :style="at({x:width*.5,y:height*.49})"><div class="bill"><div class="bill-half bill-top">账单<br /><b>¥2,000</b></div><div class="bill-half bill-bottom">本次费用<br />────────</div></div><div class="prop scissors"><CardSpellProp kind="coupon" /></div><strong class="discount">¥1,000</strong></div>
    <template v-else-if="sample.theme === 'thunder'"><div v-for="(point,i) in [actor,target]" :key="i" class="anchor" :style="at(point)"><div class="jump-seal">✧</div><div class="prop kunai"><CardSpellProp kind="thunder" /></div><div class="jump-flash"></div></div><div class="anchor" :style="at(actor)"><div class="pawn traveler"><CharacterPawn variant="pawn" avatar-id="explorer" color="#739d81" /></div></div><div class="anchor" :style="at(target)"><div class="pawn traveler reverse"><CharacterPawn variant="pawn" avatar-id="cat" color="#d8978b" /></div></div></template>
  </div>
</template>

<style scoped>
.regular-spell-layer { position:absolute; inset:0; z-index:20; overflow:hidden; pointer-events:none; border-radius:12px; }
.caption { z-index:2; }
.connections { position:absolute; inset:0; width:100%; height:100%; fill:none; }.target-ring { stroke:#e2b870; stroke-width:3; stroke-dasharray:5 4; }.anchor { position:absolute; width:0; height:0; }.prop { position:absolute; width:var(--size); height:var(--size); left:calc(var(--size)/-2); top:calc(var(--size)/-2); filter:drop-shadow(0 4px 2px #59463e30); }
.caption { position:absolute; transform:translateX(-50%); background:#fff9eced; border:1px solid #e2c7a0; border-radius:12px; padding:8px 18px; text-align:center; color:#76543e; white-space:nowrap; }.caption b { display:block; font-size:clamp(12px,2vw,18px); }.caption small { font-size:10px; color:#a88365; }
.concrete-prop { animation:drop var(--duration) both; }.dust { position:absolute; width:var(--size); height:calc(var(--tile)*.3); left:calc(var(--size)/-2); top:calc(var(--tile)*.6); border:8px dotted #cfb998; border-radius:50%; animation:dust var(--duration) both; }.impact-word { position:absolute; top:calc(var(--tile)*-1.3); left:calc(var(--tile)*-.4); font-size:calc(var(--tile)*.42); font-weight:900; color:#8d6243; transform:rotate(-12deg); animation:pop var(--duration) both; }
.beam { stroke:#e88abb; stroke-width:8; stroke-dasharray:1; animation:beam var(--duration) both; }.slow-ring { position:absolute; width:calc(var(--tile)*.4); height:calc(var(--tile)*.65); left:calc(var(--tile)*-.2); top:calc(var(--tile)*-.325); border:4px solid #eea0d6; border-radius:50%; background:#ffd9f044; animation:ray calc(var(--duration) - 300ms) var(--lag) both; }
.pawn { position:absolute; width:calc(var(--tile)*.85); height:calc(var(--tile)*.85); left:calc(var(--tile)*-.425); top:calc(var(--tile)*-.425); display:grid; place-items:center; }.pawn :deep(.character-pawn) { width:100%; height:100%; }.slow-pawn { animation:slow var(--duration) both; }.slow-echo { animation:echo var(--duration) both; filter:sepia(1) hue-rotate(250deg); }.slow-word { position:absolute; top:calc(var(--tile)*-.85); left:calc(var(--tile)*-.7); font-size:calc(var(--tile)*.35); color:#b65a97; font-weight:900; animation:pop var(--duration) both; }
.hand-flight { position:absolute; animation:grab var(--duration) both; }.hand-prop { width:calc(var(--tile)*1.6); height:calc(var(--tile)*1.6); left:calc(var(--tile)*-1.2); top:calc(var(--tile)*-.65); }.card-back { position:absolute; width:calc(var(--tile)*.6); height:calc(var(--tile)*.85); left:calc(var(--tile)*-.2); top:calc(var(--tile)*-.6); border:3px double #f7d791; border-radius:7px; background:#687b99; color:#f7d791; display:grid; place-items:center; font-size:calc(var(--tile)*.3); animation:card-grab var(--duration) both; }
.building-copy { position:absolute; width:var(--size); height:var(--size); left:calc(var(--size)/-2); top:calc(var(--size)/-2); background:var(--property-levels) 100% 100% / 200% 200% no-repeat; animation:build var(--duration) both; }.wand { transform-origin:20% 80%; animation:wand var(--duration) both; left:calc(var(--size)*-.85); top:calc(var(--size)*-.85); }.brick { position:absolute; width:calc(var(--tile)*.25); height:calc(var(--tile)*.16); border:2px solid #986a50; border-radius:2px; background:#dfa37a; animation:brick var(--duration) both; }.upgrade-star { position:absolute; color:#eeb948; font-size:var(--tile); left:calc(var(--tile)*-.5); top:calc(var(--tile)*-.8); animation:spark var(--duration) both; }.level-tag { position:absolute; top:calc(var(--tile)*.8); left:calc(var(--tile)*-.6); white-space:nowrap; background:#fff3c7; padding:3px 8px; border-radius:8px; color:#8c693b; font-size:12px; animation:pop var(--duration) both; }
.bill { position:absolute; width:calc(var(--tile)*1.65); left:calc(var(--tile)*-.825); top:calc(var(--tile)*-.9); filter:drop-shadow(0 3px 3px #0002); }.bill-half { padding:10px 5px; background:#fff9e8; border:2px solid #c9ad81; text-align:center; color:#886b48; font-size:clamp(10px,1.5vw,15px); }.bill-top { border-radius:8px 8px 0 0; border-bottom-style:dashed; animation:bill-top var(--duration) both; }.bill-bottom { border-top:0; border-radius:0 0 8px 8px; animation:bill-away var(--duration) both; }.scissors { animation:cut var(--duration) both; }.discount { position:absolute; white-space:nowrap; color:#44806a; font-size:calc(var(--tile)*.45); left:calc(var(--tile)*-.8); top:calc(var(--tile)*.25); animation:discount var(--duration) both; background:#fff6dd; border-radius:8px; padding:5px 12px; }
.kunai { width:var(--tile); height:var(--tile); left:calc(var(--tile)*-.5); top:calc(var(--tile)*-.8); animation:kunai var(--duration) both; }.jump-seal { position:absolute; width:var(--tile); height:var(--tile); left:calc(var(--tile)*-.5); top:calc(var(--tile)*-.5); border:3px double #ce943b; border-radius:50%; color:#a97529; display:grid; place-items:center; font-size:calc(var(--tile)*.7); background:#ffe49777; animation:seal var(--duration) both; }.jump-flash { position:absolute; width:var(--tile); height:var(--tile); left:calc(var(--tile)*-.5); top:calc(var(--tile)*-.5); background:#fff4bb; border-radius:50%; animation:flash var(--duration) both; }.jump-trace { stroke:#edc362; stroke-width:3; stroke-dasharray:8 8; animation:flash var(--duration) both; }.traveler { animation:jump var(--duration) both; }.traveler.reverse { animation-name:jump-back; }
@keyframes drop { 0% { opacity:0; transform:translateY(-120px) rotate(-12deg); } 15% { opacity:1; } 42% { transform:translateY(0) scale(1.12,.88); } 55% { transform:translateY(-10px) scale(.98,1.02); } 67%,100% { opacity:1; transform:translateY(0); } }
@keyframes dust { 0%,40% { opacity:0; transform:scale(.3); } 48% { opacity:1; } 88%,100% { opacity:0; transform:scale(1.7); } }
@keyframes pop { 0%,45% { opacity:0; scale:.5; } 60%,90% { opacity:1; scale:1; } 100% { opacity:0; scale:1; } }
@keyframes beam { 0% { opacity:0; stroke-dashoffset:1; } 30%,60% { opacity:.65; stroke-dashoffset:0; } 85%,100% { opacity:0; stroke-dashoffset:0; } }
@keyframes ray { 0% { opacity:0; transform:translate(0,0) scale(.4); } 15% { opacity:1; } 62%,100% { opacity:0; transform:translate(var(--dx),var(--dy)) scale(1.1); } }
@keyframes slow { 0%,35% { transform:translateX(0); } 80%,100% { transform:translateX(-8px) rotate(-8deg); } }
@keyframes echo { 0%,35% { opacity:0; transform:translateX(0); } 55% { opacity:.5; } 100% { opacity:0; transform:translateX(-24px) rotate(-8deg); } }
@keyframes grab { 0% { opacity:0; transform:translate(0,0); } 15% { opacity:1; } 42%,55% { transform:translate(var(--dx),var(--dy)); } 85%,100% { opacity:1; transform:translate(0,0); } }
@keyframes card-grab { 0%,40% { opacity:0; transform:rotate(15deg); } 48%,100% { opacity:1; transform:rotate(-10deg); } }
@keyframes wand { 0% { opacity:0; transform:rotate(-35deg) scale(.65); } 15% { opacity:1; } 35% { transform:rotate(10deg) scale(.7); } 60%,100% { opacity:0; transform:rotate(-15deg) scale(.7); } }
@keyframes brick { 0% { opacity:0; transform:translate(var(--bx),var(--by)) rotate(var(--turn)); } 25% { opacity:1; } 65%,100% { opacity:0; transform:translate(0,0) rotate(0); } }
@keyframes build { 0%,35% { opacity:0; transform:scale(.5) translateY(20px); clip-path:inset(100% 0 0); } 75%,100% { opacity:1; transform:scale(1) translateY(0); clip-path:inset(0); } }
@keyframes spark { 0%,50% { opacity:0; transform:scale(.1); } 65% { opacity:1; } 90%,100% { opacity:0; transform:scale(1.5); } }
@keyframes cut { 0% { opacity:0; transform:translateX(-70px) rotate(-25deg) scale(.65); } 25% { opacity:1; } 45%,55% { transform:translateX(5px) rotate(10deg) scale(.65); } 75%,100% { opacity:0; transform:translateX(70px) rotate(-10deg) scale(.65); } }
@keyframes bill-away { 0%,48% { opacity:1; transform:translate(0,0) rotate(0); } 90%,100% { opacity:0; transform:translate(60px,70px) rotate(35deg); } }
@keyframes bill-top { 0%,50% { opacity:1; } 65%,100% { opacity:.3; } }
@keyframes discount { 0%,55% { opacity:0; transform:translateY(15px) scale(.8); } 80%,100% { opacity:1; transform:translateY(0) scale(1); } }
@keyframes kunai { 0% { opacity:0; transform:translateY(-60px) rotate(-50deg); } 30%,50% { opacity:1; transform:translateY(0) rotate(15deg); } 75%,100% { opacity:0; transform:translateY(0) rotate(15deg); } }
@keyframes seal { 0%,20% { opacity:0; transform:scale(.3) rotate(-40deg); } 45%,80% { opacity:1; transform:scale(1) rotate(0); } 100% { opacity:0; transform:scale(1.2); } }
@keyframes flash { 0%,43%,70%,100% { opacity:0; } 51%,56% { opacity:1; } }
@keyframes jump { 0%,44% { opacity:1; transform:translate(0,0) scale(1); } 49% { opacity:0; transform:translate(0,0) scale(.2); } 51% { opacity:0; transform:translate(var(--dx),var(--dy)) scale(.2); } 60%,100% { opacity:1; transform:translate(var(--dx),var(--dy)) scale(1); } }
@keyframes jump-back { 0%,44% { opacity:1; transform:translate(0,0) scale(1); } 49% { opacity:0; transform:translate(0,0) scale(.2); } 51% { opacity:0; transform:translate(calc(-1*var(--dx)),calc(-1*var(--dy))) scale(.2); } 60%,100% { opacity:1; transform:translate(calc(-1*var(--dx)),calc(-1*var(--dy))) scale(1); } }
.reduced * { animation:none!important; }.reduced .anchor,.reduced .connections { display:none; }
</style>
