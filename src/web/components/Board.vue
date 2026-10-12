<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { DISTRICTS, rentFor, tileCoordinates } from '../../shared/rules';
import type { PublicRoomState, Tile } from '../../shared/types';
import City from './City.vue';
const props = defineProps<{ room: PublicRoomState; spectator?: boolean }>();
const emit = defineEmits<{ select: [tile: Tile] }>();
const container = ref<HTMLElement>();
const size = ref(700);
const zoom = ref(1);
const animatedPosition = ref<{ id: string; position: number } | null>(null);
let animationGeneration = 0;
let observer: ResizeObserver | undefined;
let smallScreen = true;
const quarter = computed(() => props.room.board.length / 4);
const current = computed(() => props.room.players.find(player => player.id === (props.room.status === 'finished' ? props.room.winnerId : props.room.turnOrder[props.room.turnIndex])));
const faceDots: Record<number, number[]> = { 0: [], 1: [5], 2: [1, 9], 3: [1, 5, 9], 4: [1, 3, 7, 9], 5: [1, 3, 5, 7, 9], 6: [1, 3, 4, 6, 7, 9] };
const icons = { start: '➜', chance: '?', fortune: '✦', jail: 'Ⅱ', property: '⌂' };
/** 输入地块，返回其所有者的显示颜色。 */
function ownerColor(tile: Tile): string | undefined { return props.room.players.find(player => player.id === tile.ownerId)?.color; }
/** 输入地块索引，返回当前可见的棋子列表。 */
function occupants(id: number) {
  return props.room.players.filter(player => !player.bankrupt && (animatedPosition.value?.id === player.id ? animatedPosition.value.position : player.position) === id);
}
/** 根据可用宽度和屏幕高度调整棋盘，手机默认缩小至能完整看到地图。 */
function resizeBoard(): void {
  const width = container.value?.clientWidth ?? 700;
  if (width <= 0) return;
  size.value = Math.max(560, Math.min(900, width, window.innerHeight - 280));
  if (width < 560) zoom.value = Math.max(0.5, Math.min(1, width / 560));
  else if (smallScreen) zoom.value = 1;
  smallScreen = width < 560;
}
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
  <div ref="container" class="board-container">
    <div class="board-tools"><span class="eyebrow">{{ room.board.length }} 格随机城市 · 顺时针前进</span><label>缩放 <input v-model.number="zoom" aria-label="地图缩放" type="range" min="0.5" max="1.3" step="0.05" /></label></div>
    <div class="board-viewport">
      <div class="board-sizer" :style="{ width: `${size * zoom}px`, height: `${size * zoom}px` }">
        <div class="board" :style="{ width: `${size}px`, height: `${size}px`, transform: `scale(${zoom})`, gridTemplateColumns: `repeat(${quarter + 1}, 1fr)`, gridTemplateRows: `repeat(${quarter + 1}, 1fr)` }">
          <div class="board-center" :style="{ gridRow: `2 / span ${quarter - 1}`, gridColumn: `2 / span ${quarter - 1}` }">
            <span class="center-label"><span class="status-dot"></span>{{ spectator ? '公共观战席' : '好友财富派对' }}</span>
            <h2>一步好运<br /><span>一城财富</span></h2>
            <City />
            <div class="turn-display"><span :style="{ background: current?.color }" class="avatar">{{ current?.name.slice(0, 1) }}</span><div><small>{{ room.status === 'finished' ? '本局财富赢家' : `第 ${room.turnNumber} 次行动` }}</small><strong>{{ current?.name }}{{ room.status === 'finished' ? '赢下了城市' : '的回合' }}</strong></div><div class="die" :class="{ fresh: room.dice !== null }" :key="`${room.turnNumber}-${room.dice}`" :aria-label="room.dice === null ? '等待掷骰' : `骰子 ${room.dice} 点`"><span v-for="dot in 9" :key="dot" :class="{ filled: faceDots[room.dice ?? 1]?.includes(dot) }"></span></div></div>
            <div class="turn-order"><span v-for="id in room.turnOrder" :key="id" :class="{ active: id === current?.id, eliminated: room.players.find(p => p.id === id)?.bankrupt }" :style="{ '--player-color': room.players.find(p => p.id === id)?.color }">{{ room.players.find(p => p.id === id)?.name }}</span></div>
          </div>
          <button v-for="tile in room.board" :key="tile.id" class="tile" :class="[tile.type, `district-${tile.district}`, { owned: tile.ownerId, mortgaged: tile.mortgaged }]" :style="{ gridRow: tileCoordinates(tile.id, room.board.length).row, gridColumn: tileCoordinates(tile.id, room.board.length).column, '--owner': ownerColor(tile) }" :aria-label="`${tile.id}号 ${tile.name}${tile.type === 'property' ? `，价格${tile.price}，租金${rentFor(tile)}` : ''}`" @click="emit('select', tile)">
            <span v-if="tile.ownerId" class="owner-strip"></span>
            <span v-if="tile.barrier" class="barrier-marker" title="路障">🚧</span>
            <span class="tile-icon" :class="{ built: tile.level > 0 }">{{ tile.type === 'property' && tile.level > 0 ? '▥' : icons[tile.type] }}</span>
            <strong>{{ tile.name }}</strong><small>{{ tile.type === 'property' ? (tile.mortgaged ? '已抵押' : `¥${tile.price.toLocaleString()}`) : ({ start: '+1,000', chance: '好运降临', fortune: '惊喜奖励', jail: '休息一轮' }[tile.type]) }}</small>
            <span v-if="tile.level" class="building-level">{{ '●'.repeat(tile.level) }}</span>
            <div v-if="occupants(tile.id).length" class="pawns"><span v-for="player in occupants(tile.id)" :key="player.id" :style="{ background: player.color }" :title="player.name">{{ player.name.slice(0, 1) }}</span></div>
            <span class="tile-number">{{ tile.id }}</span>
          </button>
        </div>
      </div>
    </div>
    <div class="map-legend"><span v-for="(name, i) in DISTRICTS" :key="name"><i :class="`district-${i}`"></i>{{ name }}</span></div>
  </div>
</template>
