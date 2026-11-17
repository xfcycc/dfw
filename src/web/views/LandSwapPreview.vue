<script setup lang="ts">
import { computed, provide, ref, shallowRef } from 'vue';
import type { ContentChoice, ContentResult, GameAction, LandSwapTarget, Tile } from '../../shared/types';
import { connectedRentPercent, propertyValue, rentFor } from '../../shared/rules';
import { mapTargetKey, type MapTargetRequest } from '../composables/useMapTarget';
import ContentCard from '../components/ContentCard.vue';
import LandSwapChoice from '../components/LandSwapChoice.vue';
import Modal from '../components/Modal.vue';

const actorId = 'you';
const card: ContentResult = {
  id: 'preview-your-name', skinId: 'chance_your_name', pool: 'chance', rarity: 'UR', name: '你的名字。',
  quote: '君の、名前は……？',
  description: '东京的石阶上，泷和三叶擦肩而过。走出几步，泷终于回过头叫住她。明明想不起对方的名字，两个人却都落下了眼泪。',
  effect: '自选自己与对手各1处地产，强制互换产权。差价不超过低价地产估值的10%，由拿到较贵地产的一方补足。',
};
const startingPlayers = [
  { id: actorId, name: '你', cash: 8000, color: '#487557' },
  { id: 'peach', name: '小桃', cash: 6400, color: '#ae655a' },
  { id: 'wind', name: '阿岚', cash: 3800, color: '#4b7196' },
];
/** 输入样例地产参数，返回与真实棋盘一致的地块结构。 */
function land(id: number, name: string, ownerId: string, price: number, level: number, extra: Partial<Tile> = {}): Tile {
  return { id, type: 'property', name, ownerId, price, level, district: Math.floor(id / 3), mortgaged: false, barrier: false, demolitionShield: false, ...extra };
}
const startingBoard = [
  land(0, '云朵商店', actorId, 3600, 1), land(1, '梧桐公馆', 'peach', 3600, 1, { concertStacks: 1, permanentRentPercent: 25 }),
  land(2, '松林花房', actorId, 1600, 0), land(3, '晚风小筑', actorId, 2500, 2),
  land(4, '海盐小镇', actorId, 3800, 1), land(5, '潮汐灯塔', 'wind', 3400, 1, { demolitionShield: true }),
  land(6, '青石老街', 'peach', 3500, 1, { mortgaged: true }), land(7, '琉璃剧院', 'peach', 5600, 3),
  land(8, '月影庭院', 'wind', 3600, 1, { kamuiCasterId: 'peach', kamuiTurns: 1 }),
];
const board = ref(startingBoard.map(t => ({ ...t })));
const players = ref(startingPlayers.map(p => ({ ...p })));
const step = ref(0);
const open = ref(false);
const result = ref('');
const mapRequest = shallowRef<MapTargetRequest | null>(null);
const mapOptions = computed(() => mapRequest.value?.options() ?? []);
provide(mapTargetKey, {
  open: request => { mapRequest.value = request; },
  close: request => { if (mapRequest.value === request) mapRequest.value = null; },
});
const targets = computed<LandSwapTarget[]>(() => board.value.filter(t => t.ownerId !== actorId).map(tile => {
  const value = propertyValue(tile);
  let reason = tile.mortgaged ? '地产已抵押' : tile.kamuiTurns ? '地产已封印' : '';
  const opponent = players.value.find(p => p.id === tile.ownerId)!;
  const pairs = reason ? [] : board.value.filter(own => own.ownerId === actorId && !own.mortgaged && !own.kamuiTurns).flatMap(own => {
    const ownValue = propertyValue(own), cashDue = value - ownValue;
    if (Math.abs(cashDue) > Math.min(value, ownValue) / 10 || cashDue > players.value[0].cash || -cashDue > opponent.cash) return [];
    const after = board.value.slice();
    const received = after[tile.id] = { ...tile, ownerId: actorId };
    const given = after[own.id] = { ...own, ownerId: opponent.id };
    return [{ ownTileId: own.id, ownValue, cashDue, receivedRent: rentFor(received, null, after), givenRent: rentFor(given, null, after),
      receivedConnected: connectedRentPercent(after, received), givenConnected: connectedRentPercent(after, given),
      previousReceivedConnected: connectedRentPercent(board.value, tile), previousGivenConnected: connectedRentPercent(board.value, own) }];
  }).sort((a, b) => Math.abs(a.cashDue) - Math.abs(b.cashDue));
  if (!reason && !pairs.length) reason = '没有价差和现金符合条件的自家地产';
  return { tileId: tile.id, value, rent: rentFor(tile, null, board.value), reason, pairs };
}));
const choice = computed<ContentChoice>(() => ({ id: `preview-${step.value}`, type: 'land_swap', landSwapTargets: targets.value, tileIds: targets.value.filter(t => t.pairs.length).map(t => t.tileId),
  cards: [], cardId: null, content: card, skipTurns: 0, jailed: false, discardCash: 0 }));
/** 输入金额，返回展示站内现金的统一格式。 */
function money(value: number): string { return `¥${value.toLocaleString('zh-CN')}`; }
/** 无入参，恢复初始地块和余额，然后打开换地事件。 */
function replay(): void {
  board.value = startingBoard.map(t => ({ ...t }));
  players.value = startingPlayers.map(p => ({ ...p }));
  step.value++;
  mapRequest.value = null;
  result.value = '';
  open.value = true;
}
/** 输入示例棋盘地块，通过和正式弹框一致的地图点选入口选地。 */
function pick(tile: Tile): void {
  mapRequest.value?.select(tile.id);
  mapRequest.value = null;
}
/** 输入弹框发出的操作，只改本页示例产权和余额。 */
async function submit(action: Extract<GameAction, { type: 'contentDecision' }>): Promise<boolean> {
  if (!action.accept) { result.value = '已放弃换地，地产归属和现金均保持原样。'; return true; }
  const [targetId, ownId] = action.tileIds ?? [];
  const pair = targets.value.find(t => t.tileId === targetId)?.pairs.find(p => p.ownTileId === ownId);
  if (!pair) return false;
  const received = board.value[targetId], given = board.value[ownId];
  const opponent = players.value.find(p => p.id === received.ownerId)!;
  [received.ownerId, given.ownerId] = [actorId, opponent.id];
  players.value[0].cash -= pair.cashDue;
  opponent.cash += pair.cashDue;
  const cash = pair.cashDue > 0 ? `你向${opponent.name}支付 ${money(pair.cashDue)}` : pair.cashDue < 0 ? `${opponent.name}向你支付 ${money(-pair.cashDue)}` : '无需补差';
  result.value = `已交换：你获得「${received.name}」，${opponent.name}获得「${given.name}」。${cash}。`;
  return true;
}
</script>

<template>
  <main class="swap-demo">
    <header class="demo-heading">
      <div><span class="eyebrow">UR · 机会事件模拟</span><h1>你的名字。</h1><p>用样例地产体验强制换地。所有变更只在这个页面里生效。</p></div>
      <button type="button" class="button primary" @click="replay">{{ result || step ? '重置并再试一次' : '模拟抽到事件' }}</button>
    </header>
    <div class="demo-layout">
      <section class="board-panel" aria-label="模拟棋盘">
        <div class="wallets"><span v-for="p in players" :key="p.id" :style="{ '--owner': p.color }"><b>{{ p.name }}</b> {{ money(p.cash) }}</span></div>
        <p v-if="mapRequest" class="map-prompt"><strong>{{ mapRequest.label }} · {{ mapOptions.length }} 处可选</strong><button type="button" class="button secondary small" @click="mapRequest = null">返回弹框</button></p>
        <p v-else class="hint">建议先选小桃的「梧桐公馆」：可以试等价交换，也可以选「晚风小筑」，看补 ¥400 前后的变化。</p>
        <div class="land-grid">
          <button v-for="tile in board" :key="tile.id" type="button" class="demo-land"
            :class="{ available: mapOptions.some(o => o.id === tile.id), dimmed: mapRequest && !mapOptions.some(o => o.id === tile.id) }"
            :style="{ '--owner': players.find(p => p.id === tile.ownerId)?.color }" :disabled="!mapOptions.some(o => o.id === tile.id)" @click="pick(tile)">
            <small>{{ players.find(p => p.id === tile.ownerId)?.name }} · 建筑 {{ tile.level }} 级</small><strong>{{ tile.name }}</strong>
            <span>估值 {{ money(propertyValue(tile)) }}</span><span>当前租金 {{ money(rentFor(tile, null, board)) }}</span>
            <small v-if="tile.concertStacks || tile.permanentRentPercent">演唱会 · 永久涨租</small>
            <small v-else-if="tile.mortgaged || tile.kamuiTurns || tile.demolitionShield">{{ tile.mortgaged ? '已抵押' : tile.kamuiTurns ? '已封印' : '有护盾' }}</small>
          </button>
        </div>
        <p v-if="result" class="result" role="status">{{ result }}</p>
      </section>
      <aside class="demo-card"><ContentCard :content="card" /></aside>
    </div>
    <Modal :open="open && !mapRequest" title="你的名字。 · 选择交换地产" @close="open = false">
      <LandSwapChoice :key="choice.id" :choice="choice" :board="board" :players="players" :enabled="true" :submit="submit" @done="open = false" />
    </Modal>
  </main>
</template>

<style scoped>
.swap-demo { max-width: 1080px; margin: auto; padding: 36px 20px; color: #51402a; }
.demo-heading, .map-prompt { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.demo-heading { margin-bottom: 18px; flex-wrap: wrap; }
.demo-heading h1 { margin: 8px 0; font-size: clamp(32px, 6vw, 56px); line-height: 1.12; }
.demo-heading p, .hint { margin: 0; color: #70634f; line-height: 1.6; }
.eyebrow { color: #895f18; font-size: 13px; font-weight: 700; }
.demo-layout { display: grid; grid-template-columns: minmax(0, 2fr) minmax(220px, 1fr); gap: 20px; align-items: start; }
.board-panel { padding: 18px; border: 1px solid #dfcba9; border-radius: 18px; background: #fffaf0; }
.wallets { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
.wallets span { padding: 6px 10px; border: 1px solid #e9dbc4; border-radius: 999px; background: #fffef8; white-space: nowrap; }
.wallets b, .demo-land small { color: var(--owner); }
.land-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; margin-top: 12px; }
.demo-land { display: grid; align-content: start; justify-items: start; gap: 5px; min-width: 0; padding: 10px; border: 1px solid #e7d4b5; border-top: 4px solid var(--owner); border-radius: 10px; background: #fffef9; color: #51402a; font: inherit; text-align: left; }
.demo-land strong { line-height: 1.25; }
.demo-land span, .demo-land small { font-size: 12px; }
.demo-land.dimmed { opacity: .45; }
.demo-land.available { background: #eff6e4; outline: 2px solid #83a065; cursor: pointer; }
.demo-land.available:hover, .demo-land.available:focus-visible { background: #e0eecb; }
.result, .map-prompt { padding: 10px; border: 1px solid #aac093; border-radius: 10px; background: #edf5e3; color: #355839; }
.result { margin-bottom: 0; }
@media (max-width: 700px) { .demo-layout { grid-template-columns: 1fr; } .demo-card { justify-self: center; width: min(84%, 310px); } }
</style>
