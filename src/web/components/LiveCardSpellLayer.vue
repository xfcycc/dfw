<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted } from 'vue';
import type { MotionSegment, PublicPlayer, Tile } from '../../shared/types';
import { cardSpellFor } from '../card-spell-presentation';
import BatchSpellStage from './BatchSpellStage.vue';
import CardSpellProp from './CardSpellProp.vue';
import CharacterPawn from './CharacterPawn.vue';
import UrSpellLayer from './UrSpellLayer.vue';

type Point = { x: number; y: number };
const props = defineProps<{ segment: MotionSegment; players: PublicPlayer[]; board: Tile[]; point: (id: number) => Point; playerPoint: (id: string, position: number, after?: boolean) => Point; tileSize: number; width: number; height: number }>();
const emit = defineEmits<{ done: [id: string] }>();
const sample = computed(() => cardSpellFor(props.segment)!);
const effect = computed(() => props.segment.boardEffect!);
const actor = computed(() => props.players.find(player => player.id === props.segment.playerId)!);
const origin = computed(() => props.playerPoint(props.segment.playerId, props.segment.from));
const moving = computed(() => effect.value.moves.map(move => ({ ...move, player: props.players.find(player => player.id === move.playerId)!, start: props.playerPoint(move.playerId, move.from), end: props.playerPoint(move.playerId, move.to, true) })));
const targets = computed(() => effect.value.beforeTiles.map(before => ({ before, tile: props.board.find(tile => tile.id === before.id)!, point: props.point(before.id) })));
const path = computed(() => [origin.value, ...props.segment.path.map(id => props.point(id))]);
const target = computed(() => targets.value[0]?.point ?? moving.value[0]?.end ?? origin.value);
let timer: ReturnType<typeof setTimeout> | undefined;
/** 无入参，通知原队列应用服务端结果；不提交业务操作。 */
function finish(): void { emit('done', props.segment.id); }
/** 无入参，启动共用整段时长，不等待子动画的冒泡事件。 */
onMounted(() => { timer = setTimeout(finish, sample.value.duration); });
/** 无入参，取消离页或快照重置后的旧回调。 */
onBeforeUnmount(() => clearTimeout(timer));
</script>

<template>
  <div class="live-card-spell" :style="{ '--duration': `${sample.duration}ms`, '--u': `${tileSize}px` }" role="status" :aria-label="sample.name">
    <template v-if="sample.theme === 'kamui' && !('rarity' in sample)">
      <UrSpellLayer v-for="(t,i) in targets" :key="t.before.id" :sample="sample" :actor="origin" :target="t.point" :target-appearance="{ name:t.tile.name, level:t.before.level }" :width="width" :height="height" :tile-size="tileSize" :reduced="false" :show-name="false" :hide-backdrop="i>0" />
    </template>
    <template v-else-if="sample.theme === 'thunder'">
      <div v-for="move in moving" :key="move.playerId" class="jump-origin" :style="{ left:`${move.start.x}px`,top:`${move.start.y}px`,'--dx':`${move.end.x-move.start.x}px`,'--dy':`${move.end.y-move.start.y}px` }">
        <div class="kunai"><CardSpellProp kind="thunder"/></div><div class="seal">✧</div>
        <div class="traveler"><CharacterPawn variant="pawn" :avatar-id="move.player.avatarId" :color="move.player.color"/></div>
      </div>
    </template>
    <BatchSpellStage v-else-if="'rarity' in sample" :sample="sample" :actor="origin" :target="target" :path="path" :tile-size="tileSize" :width="width" :height="height" :reduced="false" :actor-appearance="actor" :payment="effect.paymentBefore != null && effect.paymentAfter != null ? { before:effect.paymentBefore,after:effect.paymentAfter } : undefined" />
    <svg class="targets" :viewBox="`0 0 ${width} ${height}`"><circle v-for="t in targets" :key="t.before.id" :cx="t.point.x" :cy="t.point.y" :r="tileSize*.45"/></svg>
  </div>
</template>

<style scoped>
.live-card-spell { position:absolute; inset:0; z-index:20; overflow:hidden; pointer-events:none; border-radius:12px; }
.targets { position:absolute; inset:0; width:100%; height:100%; fill:none; stroke:#d9aa66; stroke-width:2; stroke-dasharray:5 4; }
.jump-origin { position:absolute; width:0; height:0; }.traveler { position:absolute; width:calc(var(--u)*.8); height:calc(var(--u)*.8); left:calc(var(--u)*-.4); top:calc(var(--u)*-.4); display:grid; place-items:center; animation:jump var(--duration) both; }.traveler :deep(.character-pawn) { width:100%; height:100%; }.kunai { position:absolute; width:var(--u); height:var(--u); left:calc(var(--u)*-.5); top:calc(var(--u)*-.7); animation:kunai var(--duration) both; }.seal { position:absolute; width:var(--u); height:var(--u); left:calc(var(--u)*-.5); top:calc(var(--u)*-.5); display:grid; place-items:center; border:3px double #ce943b; border-radius:50%; color:#a97529; background:#ffe49777; font-size:calc(var(--u)*.7); animation:seal var(--duration) both; }
@keyframes jump { 0%,44% { opacity:1; transform:translate(0,0) scale(1); } 49% { opacity:0; transform:translate(0,0) scale(.2); } 51% { opacity:0; transform:translate(var(--dx),var(--dy)) scale(.2); } 60%,100% { opacity:1; transform:translate(var(--dx),var(--dy)) scale(1); } }
@keyframes kunai { 0% { opacity:0; transform:translateY(calc(var(--u)*-.7)) rotate(-50deg); } 30%,50% { opacity:1; transform:rotate(15deg); } 75%,100% { opacity:0; transform:rotate(15deg); } }
@keyframes seal { 0%,20% { opacity:0; scale:.3; } 45%,80% { opacity:1; scale:1; } 100% { opacity:0; scale:1.2; } }
</style>
