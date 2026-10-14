<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { boardDimensions, DISTRICTS, districtName, rentFor, ruleMoney, tileCoordinates } from '../../shared/rules';
import type { PublicRoomState, Tile } from '../../shared/types';
import City from './City.vue';
const props = defineProps<{ room: PublicRoomState; spectator?: boolean; compact?: boolean }>();
const emit = defineEmits<{ select: [tile: Tile] }>();
const container = ref<HTMLElement>();
const viewport = ref<HTMLElement>();
const size = ref(700);
const zoom = ref(1);
const animatedPosition = ref<{ id: string; position: number } | null>(null);
let animationGeneration = 0;
let observer: ResizeObserver | undefined;
let lastWidth = 0;
let lastHeight = 0;
let lastLength = 0;
const districts = computed(() => DISTRICTS.map((fallback, side) => { const tile = props.room.board.find(tile => tile.district === side); return tile ? districtName(tile) : fallback; }));
const startIncome = computed(() => ruleMoney(props.room.economy, 'startIncome', 1000));
const dimensions = computed(() => boardDimensions(props.room.board.length));
const height = computed(() => size.value * dimensions.value.rows / dimensions.value.columns);
const current = computed(() => props.room.players.find(player => player.id === (props.room.status === 'finished' ? props.room.winnerId : props.room.turnOrder[props.room.turnIndex])));
const faceDots: Record<number, number[]> = { 0: [], 1: [5], 2: [1, 9], 3: [1, 5, 9], 4: [1, 3, 7, 9], 5: [1, 3, 5, 7, 9], 6: [1, 3, 4, 6, 7, 9] };
const icons = { start: '➜', chance: '?', fortune: '✦', jail: 'Ⅱ', property: '⌂' };
/** 输入地块，返回其所有者的显示颜色。 */
function ownerColor(tile: Tile): string | undefined { return props.room.players.find(player => player.id === tile.ownerId)?.color; }
/** 输入地块索引，返回当前可见的棋子列表。 */
function occupants(id: number) {
  return props.room.players.filter(player => !player.bankrupt && (animatedPosition.value?.id === player.id ? animatedPosition.value.position : player.position) === id);
}
/** 根据容器宽度和地图规模设置可读尺寸，返回全图默认比例；历史预览沿用高度限制。 */
function resizeBoard(): void {
  const width = container.value?.clientWidth ?? 700;
  if (width <= 0) return;
  if (width === lastWidth && window.innerHeight === lastHeight && props.room.board.length === lastLength) return;
  lastWidth = width; lastHeight = window.innerHeight; lastLength = props.room.board.length;
  size.value = props.compact ? Math.max(760, dimensions.value.columns * 64, Math.min(1280, width)) : Math.max(560, Math.min(900, width, window.innerHeight - 280));
  zoom.value = Math.max(0.3, Math.min(1, (width - 6) / size.value));
}
/** 无入参，将滚动视野移到当前行动者；只改变本地棋盘视野。 */
function locateCurrent(): void {
  if (!current.value || !viewport.value) return;
  const cell = tileCoordinates(current.value.position, props.room.board.length);
  viewport.value.scrollTo({ left: (cell.column - 0.5) * size.value * zoom.value / dimensions.value.columns - viewport.value.clientWidth / 2, top: (cell.row - 0.5) * height.value * zoom.value / dimensions.value.rows - viewport.value.clientHeight / 2, behavior: 'smooth' });
}
/** 无入参，恢复能容纳整张棋盘的默认比例。 */
function fitBoard(): void { zoom.value = Math.max(0.3, Math.min(1, ((container.value?.clientWidth ?? size.value) - 6) / size.value)); }
watch(() => props.room.motion?.id, async (id, old) => {
  if (!id || id === old) return;
  const motion = props.room.motion!;
  const generation = ++animationGeneration;
  animatedPosition.value = { id: motion.playerId, position: motion.from };
  for (const position of motion.path) {
    await new Promise(resolve => setTimeout(resolve, 100));
    if (generation !== animationGeneration) return;
    animatedPosition.value = { id: motion.playerId, position };
  }
  if (generation === animationGeneration) animatedPosition.value = null;
});
onMounted(() => {
  observer = new ResizeObserver(resizeBoard);
  if (container.value) observer.observe(container.value);
  window.addEventListener('resize', resizeBoard);
});
onBeforeUnmount(() => { observer?.disconnect(); window.removeEventListener('resize', resizeBoard); animationGeneration++; });
</script>
<template>
  <div ref="container" class="board-container" :class="{ 'game-board': compact, 'dense-board': compact && dimensions.columns >= 14 }">
    <div class="board-tools"><span class="eyebrow">{{ room.board.length }} 格城市 <span class="board-direction">· 顺时针前进 ↻</span></span><div class="board-controls"><button v-if="compact" class="board-control" @click="fitBoard">全图</button><button v-if="compact" class="board-control" @click="locateCurrent">定位棋子</button><label>缩放 <input v-model.number="zoom" aria-label="地图缩放" type="range" min="0.3" max="1.3" step="0.01" /></label></div></div>
    <div ref="viewport" class="board-viewport">
      <div class="board-sizer" :style="{ width: `${size * zoom}px`, height: `${height * zoom}px` }">
        <div class="board" :style="{ width: `${size}px`, height: `${height}px`, transform: `scale(${zoom})`, gridTemplateColumns: `repeat(${dimensions.columns}, 1fr)`, gridTemplateRows: `repeat(${dimensions.rows}, 1fr)` }">
          <div class="board-center" :style="{ gridRow: `2 / span ${dimensions.rows - 2}`, gridColumn: `2 / span ${dimensions.columns - 2}` }">
            <span class="center-label"><span class="status-dot"></span>{{ spectator ? '公共观战席' : '好友财富派对' }}</span>
            <h2>{{ compact ? '把城市，' : '一步好运' }}<br /><span>{{ compact ? '变成你的主场。' : '一城财富' }}</span></h2>
            <City />
            <div class="turn-display"><span :style="{ background: current?.color }" class="avatar">{{ current?.name.slice(0, 1) }}</span><div><small>{{ room.status === 'finished' ? '本局财富赢家' : `第 ${room.turnNumber} 次行动` }}</small><strong>{{ current?.name }}{{ room.status === 'finished' ? '赢下了城市' : '的回合' }}</strong></div><div class="die" :class="{ fresh: room.dice !== null, 'movement-value': room.dice !== null && room.dice <= 0 }" :key="`${room.turnNumber}-${room.dice}`" :aria-label="room.dice === null ? '等待掷骰' : room.dice < 0 ? `本次后退 ${-room.dice} 格` : room.dice === 0 ? '原地重新结算' : `骰子 ${room.dice} 点`"><strong v-if="room.dice !== null && room.dice <= 0">{{ room.dice }}</strong><template v-else><span v-for="dot in 9" :key="dot" :class="{ filled: faceDots[room.dice ?? 1]?.includes(dot) }"></span></template></div></div>
            <div v-if="$slots.controls" class="board-center-controls"><slot name="controls" /></div>
            <div class="turn-order"><span v-for="id in room.turnOrder" :key="id" :class="{ active: id === current?.id, eliminated: room.players.find(p => p.id === id)?.bankrupt }" :style="{ '--player-color': room.players.find(p => p.id === id)?.color }">{{ room.players.find(p => p.id === id)?.name }}</span></div>
          </div>
          <button v-for="tile in room.board" :key="tile.id" class="tile" :class="[tile.type, `district-${tile.district}`, { owned: tile.ownerId, mortgaged: tile.mortgaged, 'current-position': compact && current?.position === tile.id }]" :style="{ gridRow: tileCoordinates(tile.id, room.board.length).row, gridColumn: tileCoordinates(tile.id, room.board.length).column, '--owner': ownerColor(tile) }" :aria-label="`${tile.id}号 ${tile.name}${tile.type === 'property' ? `，价格${tile.price}，租金${rentFor(tile, room.economy)}，${room.players.find(player => player.id === tile.ownerId)?.name ?? '待购入'}，${tile.level}级${tile.mortgaged ? '，已抵押' : ''}` : ''}${tile.barrier ? '，路障，首位经过者停下' : ''}`" @click="emit('select', tile)">
            <span v-if="tile.ownerId" class="owner-strip"></span>
            <span v-if="tile.barrier" class="barrier-marker" title="路障">🚧</span>
            <span class="tile-icon" :class="{ built: tile.level > 0 }">{{ tile.type === 'property' && tile.level > 0 ? '▥' : icons[tile.type] }}</span>
            <strong>{{ tile.name }}</strong><small>{{ tile.type === 'property' ? (tile.mortgaged ? '已抵押' : `¥${tile.price.toLocaleString()}`) : ({ start: `+${startIncome.toLocaleString()}`, chance: '好运降临', fortune: '惊喜奖励', jail: '休息一轮' }[tile.type]) }}</small>
            <span v-if="compact && tile.type === 'property'" class="tile-ownership" :title="room.players.find(player => player.id === tile.ownerId)?.name ?? '待购入'"><i v-if="tile.ownerId" :style="{ background: ownerColor(tile) }"></i><span>{{ room.players.find(player => player.id === tile.ownerId)?.name ?? '待购入' }}</span><b v-if="tile.ownerId"> · {{ tile.level }}级</b></span>
            <span v-else-if="tile.level" class="building-level">{{ '●'.repeat(tile.level) }}</span>
            <div v-if="occupants(tile.id).length" class="pawns"><span v-for="player in occupants(tile.id)" :key="player.id" :style="{ background: player.color }" :title="player.name">{{ player.name.slice(0, 1) }}</span></div>
            <span class="tile-number">{{ tile.id }}</span>
          </button>
        </div>
      </div>
    </div>
    <div class="map-legend"><span v-for="(name, i) in districts" :key="i"><i :class="`district-${i}`"></i>{{ name }}</span></div>
    <div v-if="compact" class="board-state-legend"><span>彩色边框 · 地产主人</span><span>0级土地 · 1～3级建筑</span><span>🚧 路障 · 经过即停</span><span>▨ 条纹 · 已抵押</span></div>
  </div>
</template>
