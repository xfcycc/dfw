<script setup lang="ts">
import { computed } from 'vue';
import type { BoardEffect, BoardEffectTile, PublicPlayer, Tile } from '../../shared/types';
import { boardEffectDuration, boardEffectLabel } from '../board-effect-presentation';
import { effectSpriteKind } from '../effect-sprites';
import CharacterPawn from './CharacterPawn.vue';
import EffectSprite from './EffectSprite.vue';

const props = defineProps<{ id: string; actorId: string; effect: BoardEffect; players: PublicPlayer[]; board: Tile[]; point: (tileId: number) => { x: number; y: number }; playerPoint: (playerId: string, position: number, after?: boolean) => { x: number; y: number }; zoom: number; tileSize: number; boardWidth: number; boardHeight: number }>();
const emit = defineEmits<{ done: [id: string] }>();
const duration = computed(() => boardEffectDuration(props.effect.kind));
const label = computed(() => boardEffectLabel(props.effect.kind));
const artwork = computed(() => effectSpriteKind(props.effect.kind));
const moving = computed(() => props.effect.moves.map(move => ({ ...move, player: props.players.find(player => player.id === move.playerId), start: props.playerPoint(move.playerId, move.from), end: props.playerPoint(move.playerId, move.to, true) })));
const targets = computed(() => props.effect.beforeTiles.map(before => ({ before, after: props.effect.afterTiles.find(tile => tile.id === before.id)!, point: props.point(before.id), tile: props.board.find(tile => tile.id === before.id) })));
const participants = computed(() => props.players.filter(player => (props.effect.playerIds?.length ? props.effect.playerIds : [props.actorId]).includes(player.id)).map(player => ({ player, point: props.playerPoint(player.id, player.position) })));
const spriteSize = computed(() => Math.max(80, Math.min(220, props.tileSize * 1.8)));
/** 输入目标坐标，返回朝棋盘内侧显示的说明位置，避免边缘标签被裁切。 */
function captionStyle(point: { x: number; y: number }): { top: string; left: string; transform: string } {
  const edge = point.x < props.tileSize ? 'left' : point.x > props.boardWidth - props.tileSize ? 'right' : null;
  return { top: point.y > props.boardHeight / 2 ? '-76px' : '42px', left: edge === 'left' ? '16px' : edge === 'right' ? '-16px' : '0', transform: `translateX(${edge === 'left' ? '0' : edge === 'right' ? '-100%' : '-50%'})` };
}
/** 输入实际地产前后外观，返回等级变化或本次效果提示。 */
function propertyResult(before: BoardEffectTile, after: BoardEffectTile): string {
  switch (props.effect.kind) {
    case 'kamui': return '封入异空间';
    case 'kamuiRelease': return '地产恢复';
    case 'blackhole': return '附加效果清除';
    case 'meteor': return '陨星 +1';
    case 'tornado': if (before.level === after.level) return '力场抵挡'; break;
  }
  return before.level !== after.level ? `${before.level}级 → ${after.level}级` : before.ownerId !== after.ownerId ? '产权已转移' : '效果生效';
}
/** 输入效果前的地产外观，返回黑洞实际可吞噬的附加标记。 */
function attachments(tile: BoardEffectTile): string[] {
  return [tile.demolitionShield ? '护盾' : '', tile.treasure ? '宝藏' : '', tile.concertStacks ? '演唱会' : '', tile.barrier ? '路障' : '', tile.meteorCharges ? '陨星' : ''].filter(Boolean);
}
/** 输入动画事件，仅整段时间轴完成时通知队列，忽略子元素动画冒泡，无返回值。 */
function complete(event: AnimationEvent): void { if (event.target === event.currentTarget) emit('done', props.id); }
</script>

<template>
  <div class="board-effect-layer" :class="`effect-${effect.kind}`" :style="{ '--effect-duration': `${duration}ms`, '--effect-zoom': zoom, '--vfx-size': `${spriteSize}px` }" role="status" :aria-label="`${label}，正在生效位置播放`" @animationend="complete">
    <div v-for="move in moving" :key="`gate-${move.playerId}`" class="effect-gate" :style="{ left: `${move.start.x}px`, top: `${move.start.y}px` }"><EffectSprite kind="blackhole" /></div>
    <div v-for="move in moving" :key="move.playerId" class="effect-traveler" :style="{ left: `${move.start.x}px`, top: `${move.start.y}px`, '--travel-x': `${move.end.x - move.start.x}px`, '--travel-y': `${move.end.y - move.start.y}px`, '--traveler-color': move.player?.color }">
      <CharacterPawn variant="pawn" :avatar-id="move.player?.avatarId" :color="move.player?.color" /><strong>{{ move.player?.name }} · {{ label }}</strong>
    </div>
    <div v-for="move in moving" :key="`arrival-${move.playerId}`" class="effect-arrival" :style="{ left: `${move.end.x}px`, top: `${move.end.y}px` }"><EffectSprite kind="blackhole" /></div>
    <div v-if="effect.kind === 'gravity' && moving[0]" class="gravity-core vfx-envelope" :style="{ left: `${moving[0].end.x}px`, top: `${moving[0].end.y}px` }"><EffectSprite kind="blackhole" /></div>
    <template v-if="!moving.length && !targets.length"><div v-for="participant in participants" :key="participant.player.id" class="participant-spell" :style="{ left: `${participant.point.x}px`, top: `${participant.point.y}px`, '--participant-color': participant.player.color }"><div class="participant-ring"></div><div class="vfx-envelope"><EffectSprite :kind="artwork" /></div><span class="effect-property-label" :style="captionStyle(participant.point)"><strong>{{ label }}</strong><small>{{ participant.player.name }}</small></span></div></template>
    <div v-for="target in targets" :key="target.before.id" class="effect-property" :style="{ left: `${target.point.x}px`, top: `${target.point.y}px` }">
      <template v-if="effect.kind === 'kamui' || effect.kind === 'kamuiRelease'"><div class="vfx-envelope"><EffectSprite kind="kamui" :reverse="effect.kind === 'kamuiRelease'" /></div></template>
      <template v-else-if="effect.kind === 'blackhole'"><div class="vfx-envelope"><EffectSprite kind="blackhole" /></div><span v-for="(attachment, index) in attachments(target.before)" :key="index" class="swallowed-mark" :style="{ '--mark-angle': `${index * 72}deg` }">{{ attachment }}</span></template>
      <template v-else-if="effect.kind === 'meteor'"><div class="falling-meteor"><EffectSprite kind="meteor" /></div><div class="meteor-impact"></div></template>
      <template v-else-if="effect.kind === 'tornado'"><div class="tornado-funnel vfx-envelope"><EffectSprite kind="tornado" /></div></template>
      <template v-else><div class="vfx-envelope"><EffectSprite :kind="artwork" /></div></template>
      <span class="effect-property-label" :style="captionStyle(target.point)"><strong>{{ label }}</strong><small>{{ target.tile?.name }} · {{ propertyResult(target.before, target.after) }}</small></span>
    </div>
  </div>
</template>

<style scoped>
.board-effect-layer { position:absolute; inset:0; z-index:12; pointer-events:none; overflow:visible; --effect-duration:1800ms; animation:effect-timeline var(--effect-duration) linear both; }
.effect-traveler,.effect-gate,.effect-arrival,.effect-property,.participant-spell,.gravity-core { position:absolute; width:0; height:0; }
.effect-traveler { z-index:3; animation:effect-travel var(--effect-duration) cubic-bezier(.45,0,.2,1) both; }
.effect-traveler > .character-pawn { position:absolute; width:64px; height:70px; left:-32px; top:-52px; scale:clamp(.65,var(--effect-zoom),1); filter:drop-shadow(0 0 9px var(--traveler-color)); }
.effect-traveler > strong { position:absolute; top:11px; left:0; transform:translateX(-50%); padding:3px 8px; border:1px solid #dec69b; border-radius:10px; background:#fff7e9f2; color:#805e4c; font-size:11px; white-space:nowrap; }
.effect-gate { animation:effect-gate var(--effect-duration) ease-in-out both; filter:hue-rotate(330deg); }
.effect-property { z-index:2; }
.effect-arrival { animation:effect-arrival var(--effect-duration) ease-in-out both; }
.effect-shuffle .effect-traveler { animation-name:effect-shuffle; }
.effect-gravity .effect-traveler { animation-name:effect-attract; }
.gravity-core { z-index:4; }
.participant-ring { position:absolute; left:-25px; top:-20px; width:50px; height:40px; border:2px solid var(--participant-color); border-radius:50%; box-shadow:0 0 18px var(--participant-color); animation:effect-impact var(--effect-duration) ease-out both; }
.vfx-envelope { position:absolute; width:0; height:0; animation:effect-bloom var(--effect-duration) ease-in-out both; filter:drop-shadow(0 3px 12px #71503c40); }
.effect-kamuiRelease .vfx-envelope { animation-direction:reverse; }
.effect-property-label { position:absolute; z-index:5; left:0; transform:translateX(-50%); padding:4px 9px; border:1px solid #d9ba8c; border-radius:10px; color:#825b49; background:#fff3dcef; box-shadow:0 3px 10px #69473125; font-size:11px; font-weight:800; white-space:nowrap; animation:effect-result var(--effect-duration) ease both; }
.effect-property-label strong,.effect-property-label small { display:block; text-align:center; }
.effect-property-label small { margin-top:3px; font-size:10px; font-weight:500; }
.effect-property .effect-property-label small { animation:effect-outcome var(--effect-duration) linear both; }
.swallowed-mark { position:absolute; z-index:2; left:-15px; top:-15px; display:grid; place-items:center; min-width:32px; height:25px; padding:0 5px; border:1px solid #eedba4; color:#fff2cf; background:#75607ee6; border-radius:7px; font-size:10px; white-space:nowrap; animation:effect-swallow var(--effect-duration) ease-in both; }
.falling-meteor { position:absolute; width:0; height:0; animation:effect-meteor var(--effect-duration) ease-in both; }
.meteor-impact { position:absolute; left:-33px; top:-20px; width:66px; height:40px; border:4px solid #e0ad6b; border-radius:50%; box-shadow:0 0 24px #ffd27c; animation:effect-impact var(--effect-duration) ease-out both; }
.tornado-funnel { top:-20px; }
@keyframes effect-timeline { from { opacity:1; } to { opacity:1; } }
@keyframes effect-travel { 0%,20% { transform:translate(0,0) scale(1); } 40% { transform:translate(0,0) scale(.15); } 45% { transform:translate(var(--travel-x),var(--travel-y)) scale(.15); } 75%,100% { transform:translate(var(--travel-x),var(--travel-y)) scale(1); } }
@keyframes effect-shuffle { 0%,15% { transform:translate(0,0) rotate(0); } 45% { transform:translate(calc(var(--travel-x) * .5),calc(var(--travel-y) * .5)) rotate(180deg) scale(.5); } 80%,100% { transform:translate(var(--travel-x),var(--travel-y)) rotate(360deg) scale(1); } }
@keyframes effect-attract { 0%,25% { transform:translate(0,0) scale(1); } 65% { transform:translate(var(--travel-x),var(--travel-y)) scale(.25); } 85%,100% { transform:translate(var(--travel-x),var(--travel-y)) scale(1); } }
@keyframes effect-gate { 0%,100% { opacity:0; scale:.2; } 20%,65% { opacity:1; scale:1.15; } }
@keyframes effect-bloom { 0% { opacity:0; scale:.15; } 20% { opacity:1; scale:1.12; } 65% { opacity:1; scale:1; } 100% { opacity:0; scale:.1; } }
@keyframes effect-swallow { 0%,20% { opacity:0; transform:rotate(var(--mark-angle)) translateX(67px) rotate(calc(-1 * var(--mark-angle))) scale(1); } 30% { opacity:1; } 70%,100% { opacity:0; transform:rotate(calc(var(--mark-angle) + 240deg)) translateX(0) scale(0); } }
@keyframes effect-meteor { 0%,15% { opacity:0; transform:translate(-110px,-170px) scale(.5); } 20% { opacity:1; } 48% { opacity:1; transform:translate(0,0) scale(1); } 70%,100% { opacity:0; transform:translate(0,0) scale(.6); } }
@keyframes effect-impact { 0%,45% { opacity:0; scale:.1; } 55% { opacity:1; scale:1; } 85%,100% { opacity:0; scale:1.8; } }
@keyframes effect-result { 0%,12% { opacity:0; } 25%,90% { opacity:1; } 100% { opacity:0; } }
@keyframes effect-outcome { 0%,58% { opacity:0; } 70%,100% { opacity:1; } }
@keyframes effect-arrival { 0%,45%,100% { opacity:0; scale:.2; } 65%,80% { opacity:1; scale:1; } }
@media (prefers-reduced-motion:reduce) { .board-effect-layer { display:none; } }
</style>
