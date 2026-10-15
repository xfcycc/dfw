import { createApp, h } from 'vue';
import { createPinia } from 'pinia';
import { createMemoryHistory, createRouter, RouterView } from 'vue-router';
import RoomView from '/views/RoomView.vue';
import { useGameStore } from '/stores/game.ts';
import { generateMap } from '/@fs/Users/caiguoyu/projects/dfw/src/shared/rules.ts';
import samples from '../card-frames-2026-10-15/samples.json';
import '/style.css';
import '/card-art.css';

const pinia = createPinia();
const game = useGameStore(pinia);
// 人数和地图大小参数仅切换本页示例，便于查看最大地图的命名与排版。
const previewPlayerCount = Number(new URLSearchParams(location.search).get('players') || 4);
const previewMapSize = new URLSearchParams(location.search).get('size') || 'medium';
const players = [
  { id: 'preview-1', name: '苏暖暖', color: '#e982b2', avatarId: 'explorer', cash: 12800, netWorth: 18600, position: 0 },
  { id: 'preview-2', name: '小满', color: '#47ae88', avatarId: 'gardener', cash: 9600, netWorth: 15200, position: 8 },
  { id: 'preview-3', name: '阿橙', color: '#e6ad32', avatarId: 'chef', cash: 8400, netWorth: 12800, position: 17 },
  { id: 'preview-4', name: '大可', color: '#4287e5', avatarId: 'artist', cash: 6800, netWorth: 10600, position: 26 },
  { id: 'preview-5', name: '小禾', color: '#a078d3', avatarId: 'panda', cash: 7200, netWorth: 9800, position: 32 },
  { id: 'preview-6', name: '小桃', color: '#ef6c68', avatarId: 'mermaid', cash: 6400, netWorth: 9200, position: 36 },
].slice(0, previewPlayerCount).map((player, index) => ({ ...player, assetRank: index + 1, cardCount: 3, ready: true, online: true, bankrupt: false, skipTurns: 0, jailed: false, eliminationOrder: null }));
// 可通过预览地址的 cash 参数查看不同金额位数，仅影响本页示例数据。
const previewCash = new URLSearchParams(location.search).get('cash');
const previewDice = new URLSearchParams(location.search).get('dice');
if (previewCash !== null && Number.isFinite(Number(previewCash))) players[0].cash = Number(previewCash);
const board = generateMap(players.length, 'panel-frame-preview', undefined, undefined, previewMapSize);
for (const tile of board) {
  if (tile.type === 'property' && tile.id % 3 === 1) { tile.ownerId = players[tile.id % players.length].id; tile.level = tile.id % 3; }
}
const now = Date.now();
// 长动态示例用于检查滚动区域是否影响整组布局尺寸，仅作用于本页。
const previewLogCount = Number(new URLSearchParams(location.search).get('logs') || 4);
const previewLogs = Array.from({ length: previewLogCount }, (_, index) => ({
  id: `preview-log-${index}`, type: 'buy',
  text: `${players[index % players.length].name} 买下了灯塔岛，支付银行 ¥1,200，现金余额 ¥10,800。`,
  at: now - (previewLogCount - index) * 1000,
}));
game.session = { code: 'frame-preview', playerId: players[0].id, token: '' };
game.room = {
  code: 'frame-preview', version: 1, status: 'playing', mapSize: previewMapSize, hostId: players[0].id, gameId: 'panel-preview',
  players, board, turnOrder: players.map(player => player.id), turnIndex: 0, turnNumber: 8, phase: 'pre_roll', pending: null,
  tradePending: false, pausedReason: null, dice: previewDice === null ? null : Number(previewDice), motion: null, winnerId: null,
  logs: previewLogCount !== 4 ? previewLogs : [
    { id: 'preview-log-1', type: 'chat', text: '小满：这个边框上的小人好可爱！', at: now - 1000 },
    { id: 'preview-log-2', type: 'buy', text: '阿橙 购入了一处新地产，城市又热闹了一点。', at: now - 2000 },
    { id: 'preview-log-3', type: 'chat', text: '大可：下一轮轮到谁掷骰子？', at: now - 3000 },
    { id: 'preview-log-4', type: 'start', text: '苏暖暖 经过起点，获得补给。', at: now - 4000 },
  ],
};
game.privateData = { playerId: players[0].id, hand: samples, trade: null, activeCardUsed: false };
game.connection = 'online';
// 本页只给真实 RoomView 提供视觉示例，所有会话与游戏命令均停留在预览内。
game.disconnect = () => {};
game.connect = () => {};
game.enter = async () => game.session;
game.act = async () => false;
const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/room/:code', component: RoomView }] });
await router.push('/room/frame-preview');
await router.isReady();
const app = createApp({ render: () => h('div', [h('div', { style: 'padding:10px 24px;background:#fff3d8;color:#806344;font:13px sans-serif;border-bottom:1px solid #e5d5b4' }, 'Q 版边框样式预览 · 示例局面，使用实际游戏页面组件'), h(RouterView)]) });
app.use(pinia).use(router).mount('#panel-preview');
