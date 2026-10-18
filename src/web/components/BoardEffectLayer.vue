<script setup lang="ts">
import { computed } from 'vue';
import type { BoardEffect, BoardEffectTile, PublicPlayer, Tile } from '../../shared/types';
import { boardEffectDuration, boardEffectLabel } from '../board-effect-presentation';
import { effectSpriteKind } from '../effect-sprites';
import CharacterPawn from './CharacterPawn.vue';
import EffectSprite from './EffectSprite.vue';

const props = defineProps<{ id: string; effect: BoardEffect; players: PublicPlayer[]; board: Tile[]; point: (tileId: number) => { x: number; y: number }; zoom: number }>();
const emit = defineEmits<{ done: [id: string] }>();
const duration = computed(() => boardEffectDuration(props.effect.kind));
const label = computed(() => boardEffectLabel(props.effect.kind));
const artwork = computed(() => effectSpriteKind(props.effect.kind));
const moving = computed(() => props.effect.moves.map(move => ({ ...move, player: props.players.find(player => player.id === move.playerId), start: props.point(move.from), end: props.point(move.to) })));
const targets = computed(() => props.effect.beforeTiles.map(before => ({ before, after: props.effect.afterTiles.find(tile => tile.id === before.id)!, point: props.point(before.id), tile: props.board.find(tile => tile.id === before.id) })));
const participants = computed(() => props.players.filter(player => props.effect.playerIds?.includes(player.id)).map(player => ({ player, point: props.point(player.position) })));
const affected = computed(() => targets.value.length ? targets.value.map(target => target.tile?.name).join(' · ') : [...new Set([...moving.value.map(move => move.player?.name), ...participants.value.map(value => value.player.name)])].filter(Boolean).join(' · '));
/** 输入实际地产前后外观，返回等级变化或本次效果提示。 */
function propertyResult(before: BoardEffectTile, after: BoardEffectTile): string { return before.level !== after.level ? `${before.level}级 → ${after.level}级` : before.ownerId !== after.ownerId ? '产权已转移' : label.value; }
/** 输入效果前的地产外观，返回黑洞实际可吞噬的附加标记。 */
function attachments(tile: BoardEffectTile): string[] {
  return [tile.demolitionShield ? '护盾' : '', tile.treasure ? '宝藏' : '', tile.concertStacks ? '演唱会' : '', tile.barrier ? '路障' : '', tile.meteorCharges ? '陨星' : ''].filter(Boolean);
}
/** 输入动画事件，仅整段时间轴完成时通知队列，忽略子元素动画冒泡，无返回值。 */
function complete(event: AnimationEvent): void { if (event.target === event.currentTarget) emit('done', props.id); }
</script>

<template>
  <div class="board-effect-layer" :class="`effect-${effect.kind}`" :style="{ '--effect-duration': `${duration}ms`, '--effect-zoom': zoom }" aria-hidden="true" @animationend="complete">
    <div v-for="move in moving" :key="`gate-${move.playerId}`" class="effect-gate" :style="{ left: `${move.start.x}px`, top: `${move.start.y}px` }"><EffectSprite kind="blackhole" /></div>
    <div v-for="move in moving" :key="move.playerId" class="effect-traveler" :style="{ left: `${move.start.x}px`, top: `${move.start.y}px`, '--travel-x': `${move.end.x - move.start.x}px`, '--travel-y': `${move.end.y - move.start.y}px`, '--traveler-color': move.player?.color }">
      <CharacterPawn variant="pawn" :avatar-id="move.player?.avatarId" :color="move.player?.color" /><strong>{{ move.player?.name }}</strong>
    </div>
    <div v-if="effect.kind === 'gravity' && moving[0]" class="gravity-core vfx-envelope" :style="{ left: `${moving[0].end.x}px`, top: `${moving[0].end.y}px` }"><EffectSprite kind="blackhole" /></div>
    <template v-if="!moving.length && !targets.length"><div v-for="participant in participants" :key="participant.player.id" class="participant-spell vfx-envelope" :style="{ left: `${participant.point.x}px`, top: `${participant.point.y}px` }"><EffectSprite :kind="artwork" /></div></template>
    <div v-for="target in targets" :key="target.before.id" class="effect-property" :style="{ left: `${target.point.x}px`, top: `${target.point.y}px` }">
      <template v-if="effect.kind === 'kamui' || effect.kind === 'kamuiRelease'"><div class="vfx-envelope"><EffectSprite kind="kamui" :reverse="effect.kind === 'kamuiRelease'" /></div><span class="effect-property-label">{{ effect.kind === 'kamui' ? '封入异空间' : '地产恢复' }}</span></template>
      <template v-else-if="effect.kind === 'blackhole'"><div class="vfx-envelope"><EffectSprite kind="blackhole" /></div><span v-for="(attachment, index) in attachments(target.before)" :key="index" class="swallowed-mark" :style="{ '--mark-angle': `${index * 72}deg` }">{{ attachment }}</span><span class="effect-property-label">附加效果清除</span></template>
      <template v-else-if="effect.kind === 'meteor'"><div class="falling-meteor"><EffectSprite kind="meteor" /></div><div class="meteor-impact"></div><span class="effect-property-label">陨星 +1</span></template>
      <template v-else-if="effect.kind === 'tornado'"><div class="tornado-funnel vfx-envelope"><EffectSprite kind="tornado" /></div><span class="effect-property-label">{{ target.before.level === target.after.level ? '力场抵挡' : `${target.before.level}级 → ${target.after.level}级` }}</span></template>
      <template v-else><div class="vfx-envelope"><EffectSprite :kind="artwork" /></div><span class="effect-property-label">{{ propertyResult(target.before, target.after) }}</span></template>
    </div>
    <Teleport to="body">
      <div class="effect-cinematic" :class="`cinematic-${effect.kind}`" :style="{ '--effect-duration': `${duration}ms` }" role="status" aria-live="polite">
        <div class="effect-cinematic-shade"></div>
        <div class="effect-cinematic-art"><EffectSprite :kind="artwork" :reverse="effect.kind === 'kamuiRelease'" /></div>
        <div class="effect-cinematic-title"><small>道 具 · 事 件 发 动</small><strong>{{ label }}</strong><span>{{ effect.kind === 'tornado' && !targets.length ? '当前没有可受影响的建筑' : affected }}</span></div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.board-effect-layer { position:absolute; inset:0; z-index:12; pointer-events:none; overflow:visible; --effect-duration:1800ms; animation:effect-timeline var(--effect-duration) linear both; }
.effect-traveler,.effect-gate,.effect-property,.gravity-core { position:absolute; width:0; height:0; }
.effect-traveler { z-index:3; animation:effect-travel var(--effect-duration) cubic-bezier(.45,0,.2,1) both; }
.effect-traveler > .character-pawn { position:absolute; width:64px; height:70px; left:-32px; top:-52px; scale:clamp(.65,var(--effect-zoom),1); filter:drop-shadow(0 0 9px var(--traveler-color)); }
.effect-traveler > strong { position:absolute; top:11px; left:0; transform:translateX(-50%); padding:3px 8px; border:1px solid #dec69b; border-radius:10px; background:#fff7e9f2; color:#805e4c; font-size:11px; white-space:nowrap; }
.effect-gate { --vfx-size:120px; animation:effect-gate var(--effect-duration) ease-in-out both; filter:hue-rotate(330deg); }
.effect-property { transform:scale(clamp(.65,var(--effect-zoom),1)); }
.effect-shuffle .effect-traveler { animation-name:effect-shuffle; }
.effect-gravity .effect-traveler { animation-name:effect-attract; }
.gravity-core { z-index:4; --vfx-size:210px; }
.participant-spell { --vfx-size:115px; }
.vfx-envelope { position:absolute; width:0; height:0; animation:effect-bloom var(--effect-duration) ease-in-out both; filter:drop-shadow(0 3px 12px #71503c40); }
.effect-kamuiRelease .vfx-envelope { animation-direction:reverse; }
.effect-property-label { position:absolute; z-index:5; top:45px; left:0; transform:translateX(-50%); padding:4px 9px; border:1px solid #d9ba8c; border-radius:10px; color:#825b49; background:#fff3dcef; box-shadow:0 3px 10px #69473125; font-size:11px; font-weight:800; white-space:nowrap; animation:effect-result var(--effect-duration) ease both; }
.swallowed-mark { position:absolute; z-index:2; left:-15px; top:-15px; display:grid; place-items:center; min-width:32px; height:25px; padding:0 5px; border:1px solid #eedba4; color:#fff2cf; background:#75607ee6; border-radius:7px; font-size:10px; white-space:nowrap; animation:effect-swallow var(--effect-duration) ease-in both; }
.falling-meteor { position:absolute; width:0; height:0; --vfx-size:150px; animation:effect-meteor var(--effect-duration) ease-in both; }
.meteor-impact { position:absolute; left:-33px; top:-20px; width:66px; height:40px; border:4px solid #e0ad6b; border-radius:50%; box-shadow:0 0 24px #ffd27c; animation:effect-impact var(--effect-duration) ease-out both; }
.tornado-funnel { top:-38px; --vfx-size:190px; }
.effect-cinematic { position:fixed; z-index:185; inset:0; pointer-events:none; --vfx-size:clamp(240px,34vw,370px); animation:effect-cinema var(--effect-duration) ease both; }
.effect-cinematic-shade { position:absolute; inset:0; background:radial-gradient(ellipse at 50% 44%,#33284385 0%,#3f344a29 35%,transparent 72%); }
.effect-cinematic-art { position:absolute; left:50%; top:43%; width:0; height:0; animation:effect-hero var(--effect-duration) cubic-bezier(.2,.7,.25,1) both; filter:drop-shadow(0 12px 22px #2c163f4d); }
.cinematic-meteor .effect-cinematic-art { rotate:-10deg; }.cinematic-tornado .effect-cinematic-art { --vfx-size:clamp(280px,39vw,420px); }
.effect-cinematic-title { position:absolute; left:50%; top:calc(43% + clamp(125px,18vw,195px)); transform:translateX(-50%); width:max-content; max-width:calc(100vw - 28px); display:grid; justify-items:center; gap:7px; padding:12px 26px 15px; border-top:1px solid #e5c98cba; border-bottom:1px solid #e5c98c70; background:linear-gradient(90deg,transparent,#463345d9 15%,#463345ed 85%,transparent); color:#fff2ce; text-align:center; text-shadow:0 2px 7px #382332; }
.effect-cinematic-title small { font-size:9px; color:#ead7b1; letter-spacing:3px; }.effect-cinematic-title strong { font-size:clamp(23px,3vw,33px); letter-spacing:3px; }.effect-cinematic-title > span { max-width:75vw; font-size:11px; line-height:1.5; color:#eddbbd; }
@keyframes effect-timeline { from { opacity:1; } to { opacity:1; } }
@keyframes effect-travel { 0%,20% { transform:translate(0,0) scale(1); } 40% { transform:translate(0,0) scale(.15); } 45% { transform:translate(var(--travel-x),var(--travel-y)) scale(.15); } 75%,100% { transform:translate(var(--travel-x),var(--travel-y)) scale(1); } }
@keyframes effect-shuffle { 0%,15% { transform:translate(0,0) rotate(0); } 45% { transform:translate(calc(var(--travel-x) * .5),calc(var(--travel-y) * .5)) rotate(180deg) scale(.5); } 80%,100% { transform:translate(var(--travel-x),var(--travel-y)) rotate(360deg) scale(1); } }
@keyframes effect-attract { 0%,25% { transform:translate(0,0) scale(1); } 65% { transform:translate(var(--travel-x),var(--travel-y)) scale(.25); } 85%,100% { transform:translate(var(--travel-x),var(--travel-y)) scale(1); } }
@keyframes effect-gate { 0%,100% { opacity:0; scale:.2; } 20%,65% { opacity:1; scale:1.15; } }
@keyframes effect-bloom { 0% { opacity:0; scale:.15; } 20% { opacity:1; scale:1.12; } 65% { opacity:1; scale:1; } 100% { opacity:0; scale:.1; } }
@keyframes effect-swallow { 0%,20% { opacity:0; transform:rotate(var(--mark-angle)) translateX(67px) rotate(calc(-1 * var(--mark-angle))) scale(1); } 30% { opacity:1; } 70%,100% { opacity:0; transform:rotate(calc(var(--mark-angle) + 240deg)) translateX(0) scale(0); } }
@keyframes effect-meteor { 0%,15% { opacity:0; transform:translate(-110px,-170px) scale(.5); } 20% { opacity:1; } 48% { opacity:1; transform:translate(0,0) scale(1); } 70%,100% { opacity:0; transform:translate(0,0) scale(.6); } }
@keyframes effect-impact { 0%,45% { opacity:0; scale:.1; } 55% { opacity:1; scale:1; } 85%,100% { opacity:0; scale:1.8; } }
@keyframes effect-result { 0%,60% { opacity:0; } 75%,100% { opacity:1; } }
@keyframes effect-cinema { 0%,100% { opacity:0; } 15%,78% { opacity:1; } }
@keyframes effect-hero { 0% { scale:.35; translate:0 30px; } 25% { scale:1.08; translate:0 0; } 65% { scale:1; } 100% { scale:.6; translate:0 -15px; } }
@media (prefers-reduced-motion:reduce) { .board-effect-layer,.effect-cinematic { display:none; } }
</style>
