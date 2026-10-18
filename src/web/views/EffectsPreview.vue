<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import type { BoardEffect, BoardEffectTile, PublicRoomState, Tile } from '../../shared/types';
import Board from '../components/Board.vue';
import EffectSprite from '../components/EffectSprite.vue';
import CardReveal from '../components/CardReveal.vue';
import { useGameStore } from '../stores/game';
import { effectSpriteKind } from '../effect-sprites';
import { BOARD_EFFECTS, boardEffectDuration, boardEffectLabel } from '../board-effect-presentation';

const kinds = Object.keys(BOARD_EFFECTS) as BoardEffect['kind'][];
const search = ref('');
const visibleKinds = computed(() => kinds.filter(kind => `${BOARD_EFFECTS[kind].label} ${BOARD_EFFECTS[kind].description}`.includes(search.value.trim())));
const selected = ref<BoardEffect['kind']>('blackhole');
const playing = ref(false);
const completed = ref(false);
const epoch = ref(0);
const game = useGameStore();
const waitingForNotice = ref(false);
const records = ref<string[]>([]);
/** 无入参，返回独立演示局面，仅用于播放现有棋盘组件，不连接对局。 */
function initialRoom(): PublicRoomState {
  const names = ['阿洛', '小桃', '阿岚', '暖暖'];
  const colors = ['#739d81', '#d8978b', '#809fc1', '#c4a15e'];
  const board: Tile[] = Array.from({ length: 24 }, (_, id) => ({ id, type: id === 0 ? 'start' : 'property', name: id === 0 ? '起点' : id === 3 ? '梧桐公馆' : id === 9 ? '云朵商店' : id === 15 ? '晚风小筑' : `花园街 ${id}`, district: Math.floor(id / 6), price: 2000, ownerId: id === 0 ? null : `preview-${id % 4}`, level: id === 0 ? 0 : id === 9 ? 3 : 2, mortgaged: false, barrier: false, demolitionShield: false, treasure: false, concertStacks: 0, meteorCharges: 0, kamuiCasterId: null, kamuiTurns: 0, kamuiAppliedTurn: 0 }));
  const target = board[3]!;
  if (selected.value === 'blackhole') Object.assign(target, { barrier: true, demolitionShield: true, treasure: true, concertStacks: 2, meteorCharges: 1 });
  if (selected.value === 'kamuiRelease') Object.assign(target, { kamuiCasterId: 'preview-0', kamuiTurns: 1, treasure: true, concertStacks: 1 });
  if (selected.value === 'tornado') board[15]!.demolitionShield = true;
  if (selected.value === 'barrierHit') target.barrier = true;
  if (selected.value === 'shieldBlock') target.demolitionShield = true;
  if (selected.value === 'treasureClaim') target.treasure = true;
  if (selected.value === 'concertRent') target.concertStacks = 2;
  if (selected.value === 'talent') { target.ownerId = null; target.level = 0; }
  return { roomId: 'animation-preview', code: 'PREVIEW', version: 1, status: 'playing', hostId: 'preview-0', gameId: 'animation-preview',
    players: names.map((name, index) => ({ id: `preview-${index}`, name, color: colors[index]!, avatarId: ['explorer', 'cat', 'wizard', 'bunny'][index], cash: 12000, position: [1, 7, 13, 19][index]!, ready: true, online: true, bankrupt: false, skipTurns: 0, jailed: false, eliminationOrder: null })),
    board, turnOrder: names.map((_, index) => `preview-${index}`), turnIndex: 0, turnNumber: 1, phase: 'pre_roll', pending: null, tradePending: false, pausedReason: null, dice: null, motion: null, winnerId: null, logs: [] };
}
const room = ref(initialRoom());
let sequence = 0;
const duration = computed(() => (boardEffectDuration(selected.value) / 1000).toFixed(1));
/** 输入地产，返回动画协议使用的外观副本，不包含资产归属和余额。 */
function appearance(tile: Tile): BoardEffectTile {
  const { id, ownerId, level, barrier, demolitionShield, treasure, concertStacks, meteorCharges, kamuiCasterId, kamuiTurns, kamuiAppliedTurn } = tile;
  return { id, ownerId, level, barrier, demolitionShield, treasure, concertStacks, meteorCharges, kamuiCasterId, kamuiTurns, kamuiAppliedTurn };
}
/** 无入参，返回选中效果的固定演示事实，实际对局仍由服务端提供事实。 */
function sampleEffect(): BoardEffect {
  const kind = selected.value;
  const effect: BoardEffect = { kind, playerIds: ['preview-0'], moves: [], beforeTiles: [], afterTiles: [] };
  const positions = room.value.players.map(player => player.position);
  const destinations = kind === 'swap' ? [7, 1, 13, 19] : kind === 'swapAny' ? [1, 13, 7, 19] : kind === 'shuffle' ? [13, 19, 7, 1] : [1, 1, 1, 1];
  if (['swap', 'swapAny', 'shuffle', 'gravity'].includes(kind)) {
    effect.moves = room.value.players.flatMap((player, index) => positions[index] === destinations[index] ? [] : [{ playerId: player.id, from: player.position, to: destinations[index]! }]);
  } else if (['kamui', 'kamuiRelease', 'blackhole', 'meteor', 'tornado', 'barrier', 'barrierHit', 'demolish', 'shield', 'shieldBlock', 'upgrade', 'exchange', 'talent', 'treasure', 'treasureClaim', 'concert', 'concertRent'].includes(kind)) {
    const ids = kind === 'meteor' || kind === 'tornado' || kind === 'concert' ? [3, 9, 15] : [3];
    effect.beforeTiles = ids.map(id => appearance(room.value.board[id]!));
    effect.afterTiles = effect.beforeTiles.map(tile => {
      switch (kind) {
        case 'kamui': return { ...tile, kamuiCasterId: 'preview-0', kamuiTurns: 3, kamuiAppliedTurn: 1 };
        case 'kamuiRelease': return { ...tile, kamuiCasterId: null, kamuiTurns: 0 };
        case 'blackhole': return { ...tile, barrier: false, demolitionShield: false, treasure: false, concertStacks: 0, meteorCharges: 0 };
        case 'meteor': return { ...tile, meteorCharges: (tile.meteorCharges ?? 0) + 1 };
        case 'tornado': return { ...tile, level: tile.demolitionShield ? tile.level : tile.level - 1, demolitionShield: false };
        case 'barrier': return { ...tile, barrier: true };
        case 'barrierHit': return { ...tile, barrier: false };
        case 'demolish': return { ...tile, level: 0 };
        case 'shield': return { ...tile, demolitionShield: true };
        case 'shieldBlock': return { ...tile, demolitionShield: false };
        case 'upgrade': return { ...tile, level: 3 };
        case 'exchange': case 'talent': return { ...tile, ownerId: 'preview-0' };
        case 'treasure': return { ...tile, treasure: true };
        case 'treasureClaim': return { ...tile, treasure: false };
        case 'concert': return { ...tile, concertStacks: (tile.concertStacks ?? 0) + 1 };
        case 'concertRent': return { ...tile, concertStacks: 0 };
        default: return tile;
      }
    });
  } else if (['steal', 'seal', 'handDestruction', 'supply', 'morale'].includes(kind)) effect.playerIds = ['preview-0', 'preview-1'];
  return effect;
}
/** 输入可选效果，先用真实提示组件揭示事件，再交棋盘播放，完成前锁住继续按钮。 */
async function replay(kind = selected.value): Promise<void> {
  if (playing.value) return;
  selected.value = kind; completed.value = false; epoch.value++; room.value = initialRoom();
  await nextTick();
  playing.value = true; waitingForNotice.value = true;
  game.cardReveals = [{ key: `preview-notice-${++sequence}`, actor: '阿洛', mode: 'draw', content: { id: `preview-source-${sequence}`, pool: 'chance', name: boardEffectLabel(kind), effect: BOARD_EFFECTS[kind].description } }];
}
// 公开提示收起后才启动主动画；固定演示不发出对局命令。
watch(() => game.cardReveals.length, count => {
  if (!waitingForNotice.value || count) return;
  waitingForNotice.value = false;
  room.value = { ...room.value, version: 2, motion: { id: `preview-effect-${++sequence}`, playerId: 'preview-0', from: 1, path: [], teleport: true, boardEffect: sampleEffect() } };
});
/** 输入棋盘完成的片段标识，仅演出结束后应用演示结果并开放继续，不产生对局命令。 */
function finish(id: string): void {
  const effect = room.value.motion?.boardEffect;
  if (!playing.value || room.value.motion?.id !== id || !effect) return;
  room.value = { ...room.value, version: 3,
    players: room.value.players.map(player => ({ ...player, position: effect.moves.find(move => move.playerId === player.id)?.to ?? player.position, cash: player.cash + (effect.kind === 'tornado' ? effect.beforeTiles.filter(tile => !tile.demolitionShield && room.value.board[tile.id]?.ownerId === player.id).length * 100 : 0) })),
    board: room.value.board.map(tile => ({ ...tile, ...effect.afterTiles.find(after => after.id === tile.id) })) };
  playing.value = false; completed.value = true;
  records.value = [`${boardEffectLabel(effect.kind)} · 效果已完成，结果已更新`, ...records.value].slice(0, 6);
}
/** 无入参，切后台时结束当前演示并清理动画，返回页面后仍可重播，无返回值。 */
function finishHidden(): void {
  if (!document.hidden || !playing.value) return;
  if (waitingForNotice.value) { waitingForNotice.value = false; game.cardReveals = []; playing.value = false; return; }
  if (!room.value.motion) return;
  finish(room.value.motion.id); epoch.value++;
}
document.addEventListener('visibilitychange', finishHidden);
onBeforeUnmount(() => { document.removeEventListener('visibilitychange', finishHidden); game.cardReveals = []; });
</script>

<template>
  <main class="effects-preview">
    <header><div><p class="eyebrow">好友财富派对 · 动画放映室</p><h1>看看，这张牌怎么生效。</h1><p>选择效果即可播放，也可以反复重看。</p></div><a href="/">返回游戏 ↗</a></header>
    <div class="preview-layout">
      <aside><label class="preview-search">效果清单 · {{ kinds.length }} 种<input v-model="search" aria-label="搜索效果" placeholder="搜索效果或说明" /></label><button v-for="kind in visibleKinds" :key="kind" :class="{ selected: selected === kind }" :disabled="playing" @click="replay(kind)"><span>{{ boardEffectLabel(kind) }}</span><small>{{ (boardEffectDuration(kind) / 1000).toFixed(1) }} 秒</small></button></aside>
      <section class="preview-stage"><div class="preview-intro"><div><h2>{{ boardEffectLabel(selected) }}</h2><p>{{ BOARD_EFFECTS[selected].description }}</p></div><span>{{ duration }} 秒</span></div>
        <Board :room="room" :live="true" :presentation-epoch="epoch" compact bounded me-id="preview-0" @settled="finish"><template #controls><section class="preview-news"><h3>城市新鲜事</h3><CardReveal /><p v-if="playing && !waitingForNotice" class="preview-news-hint">{{ boardEffectLabel(selected) }} · 正在生效位置播放</p><p v-for="(record, index) in records" :key="index">{{ record }}</p><p v-if="!records.length && !playing">这里保留提示和结果，主动画在目标处播放。</p></section></template></Board>
        <div class="preview-footer"><div role="status"><strong>{{ playing ? '正在播放，下一步已锁定' : completed ? '动画已结束，可以继续' : '点击播放，查看效果' }}</strong><p>{{ completed && selected === 'tornado' ? '演示补偿已在动画后展示。' : '播放期间不能提前进入下一步。' }}</p></div><button class="button secondary" :disabled="playing" @click="replay()">{{ completed ? '再看一次' : '播放演示' }}</button><button class="button primary" :disabled="!completed || playing" @click="completed = false">下一步</button></div>
        <details class="preview-material"><summary>查看原始逐帧素材</summary><div class="preview-art"><div class="preview-generated-art"><EffectSprite :key="selected" :kind="effectSpriteKind(selected)" :reverse="selected === 'kamuiRelease'" /></div><div class="preview-art-copy"><h3>逐帧动态素材</h3><p>16 帧循环 · 透明背景</p></div></div></details>
      </section>
    </div>
    <footer>使用游戏中的同一套棋盘与动画组件，局面和金额为固定演示数据，不连接真实房间。</footer>
  </main>
</template>

<style scoped>
.effects-preview { min-height:100vh; padding:32px clamp(16px,4vw,64px); background:radial-gradient(ellipse at 0 0,#ffe6c5,transparent 55%),#faf3e7; color:#725442; }
header { display:flex; justify-content:space-between; align-items:center; gap:20px; max-width:1320px; margin:0 auto 25px; }h1 { margin:6px 0 9px; font-size:clamp(24px,3vw,38px); }header p { margin:0; color:#a18367; }header a { font-size:13px; white-space:nowrap; }
.preview-layout { display:grid; grid-template-columns:210px minmax(0,1fr); gap:22px; max-width:1320px; margin:auto; }aside { display:grid; align-content:start; gap:9px; max-height:80vh; overflow:auto; padding-right:4px; }aside button { display:flex; align-items:center; justify-content:space-between; gap:10px; padding:16px 13px; border:1px solid #e5d3b7; border-radius:14px; background:#fffaf1; color:#8b7056; text-align:left; }aside button.selected { border-color:#c99560; background:linear-gradient(120deg,#ffe4bd,#fff3db); color:#8b5739; box-shadow:0 4px 10px #9c6a3712; }aside button span { font-size:13px; font-weight:700; }aside button small { font-size:10px; white-space:nowrap; }
.preview-search { position:sticky; top:0; z-index:1; padding:0 0 10px; background:#faf3e7; font-size:11px; color:#a18367; }.preview-search input { display:block; width:100%; margin-top:7px; padding:10px; border:1px solid #e5d3b7; border-radius:10px; background:#fffaf1; color:#725442; }
.preview-stage { min-width:0; padding:20px; border:1px solid #e9d9bf; border-radius:22px; background:#fffaf2; box-shadow:0 10px 35px #936b3810; }.preview-intro { display:flex; justify-content:space-between; gap:15px; }.preview-intro h2 { margin:0 0 5px; font-size:19px; }.preview-intro p { margin:0 0 16px; font-size:12px; color:#a18367; }.preview-intro > span { font-size:12px; white-space:nowrap; }
.preview-art { display:flex; align-items:center; gap:22px; min-height:230px; padding:10px 20px; margin-bottom:18px; border:1px solid #d7c8bc; border-radius:18px; background:radial-gradient(ellipse at 22% 48%,#635067,#352d3b 56%,#302a33); overflow:hidden; }.preview-generated-art { position:relative; width:230px; height:230px; flex-shrink:0; --vfx-size:230px; }.preview-art-copy { color:#f6e7d0; }.preview-art-copy > small { color:#d2b990; font-size:9px; letter-spacing:3px; }.preview-art-copy h3 { margin:10px 0; font-size:21px; }.preview-art-copy p { color:#cfbcc0; font-size:12px; line-height:1.9; }.preview-art-copy > span { color:#dfc49c; font-size:10px; }
.preview-stage :deep(.board-viewport) { height:clamp(340px,55vh,620px); min-height:0; }.preview-stage :deep(.board-container) { padding:0; }.preview-stage :deep(.board-center) { pointer-events:none; }.preview-footer { display:flex; gap:10px; align-items:center; padding-top:18px; margin-top:5px; border-top:1px dashed #e7d9bf; }.preview-footer > div { flex:1; }.preview-footer strong { font-size:13px; }.preview-footer p { margin:4px 0 0; font-size:11px; color:#a18367; }footer { max-width:1320px; margin:18px auto 0; font-size:12px; color:#a99077; }
.preview-stage :deep(.board-center h2) { display:none; }.preview-stage :deep(.board-center) { display:flex; flex-direction:column; align-items:center; justify-content:center; gap:12px; }.preview-stage :deep(.turn-display) { margin:0; }.preview-stage :deep(.turn-order) { margin-top:8px; }
.preview-stage :deep(.turn-display) { gap:7px; padding:10px; max-width:94%; }.preview-stage :deep(.turn-identity) { min-width:85px; }.preview-stage :deep(.turn-display .die) { width:35px; height:35px; flex-shrink:0; }
.preview-stage :deep(.board-center) { pointer-events:auto; }
.preview-stage :deep(.board-center-controls) { min-height:0; width:100%; overflow:auto; }
.preview-news { width:100%; padding:10px; text-align:left; }
.preview-news h3 { margin:0 0 10px; font-size:15px; }.preview-news > p { padding:8px 0; margin:0; border-top:1px solid #e5d6b9; font-size:11px; line-height:1.6; }
.preview-news-hint { color:#9a693e; }.preview-material { margin-top:18px; }.preview-material summary { cursor:pointer; font-size:12px; color:#a18367; }.preview-material .preview-art { margin:10px 0 0; }
@media(max-width:760px) { .effects-preview { padding:20px 12px; }.preview-layout { grid-template-columns:1fr; gap:14px; }aside { grid-template-columns:repeat(3,minmax(0,1fr)); gap:6px; max-height:250px; }.preview-search { grid-column:1/-1; }aside button { display:grid; padding:10px 8px; gap:4px; }aside button span { font-size:11px; }.preview-stage { padding:12px; }.preview-footer { flex-wrap:wrap; }.preview-footer > div { flex-basis:100%; }.preview-footer .button { flex:1; } }
@media(max-width:480px) { .preview-art { gap:8px; padding:8px; }.preview-generated-art { width:160px; height:180px; --vfx-size:175px; }.preview-art-copy h3 { font-size:16px; }.preview-art-copy p { font-size:10px; }.preview-art-copy > small { letter-spacing:1px; } }
</style>
