<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { districtName, ITEMS, mapLength, MAP_SIZES, mortgageValue, redeemCost, rentFor, ruleMoney } from '../../shared/rules';
import type { Card, MapSize, Tile } from '../../shared/types';
import { forgetSession, readSession, useGameStore } from '../stores/game';
import { formatArchiveCode, selectArchiveCode } from '../archive';
import Logo from '../components/Logo.vue';
import City from '../components/City.vue';
import Board from '../components/Board.vue';
import Modal from '../components/Modal.vue';
import Assets from '../components/Assets.vue';
import CardAction from '../components/CardAction.vue';
import ContentCard from '../components/ContentCard.vue';
import ContentChoice from '../components/ContentChoice.vue';
import TradeEditor from '../components/TradeEditor.vue';
import Rules from '../components/Rules.vue';
import SaveArchive from '../components/SaveArchive.vue';
import RecoverySeats from '../components/RecoverySeats.vue';
import PublicLeaderboard from '../components/PublicLeaderboard.vue';
import RoomHistory from '../components/RoomHistory.vue';
import '../game-room.css';
import '../archive.css';

const props = withDefaults(defineProps<{ spectator?: boolean }>(), { spectator: false });
const route = useRoute();
const origin = window.location.origin;
const game = useGameStore();
const modal = ref<'rules' | 'assets' | 'card' | 'trade' | 'bankruptcy' | 'choice' | 'save' | null>(null);
const selectedCard = ref<Card | null>(null);
const selectedDefense = ref('');
const selectedTile = ref<Tile | null>(null);
type WorkbenchTab = 'assets' | 'cards' | 'trade' | 'logs' | 'history';
const workbenchTab = ref<WorkbenchTab>(props.spectator ? 'logs' : 'assets');
const workbench = ref<HTMLElement>();
const logSearch = ref('');
const roomMenu = ref<HTMLDetailsElement>();
let entryGeneration = 0;
const code = computed(() => String(route.params.code));
const owned = computed(() => game.room?.board.filter(tile => tile.ownerId === game.me?.id) ?? []);
const allReady = computed(() => !!game.room && game.room.players.length >= 2 && game.room.players.every(player => player.ready && player.online));
const previewPlayers = computed(() => Math.max(2, game.room?.players.length ?? 2));
const canPlay = computed(() => !props.spectator && game.canAct && game.canDecide && !game.room?.tradePending);
const choice = computed(() => game.privateData?.choice?.id === game.room?.pending?.id ? game.privateData?.choice : null);
const defenseCards = computed(() => game.privateData?.hand.filter(card => card.kind === 'halfPayment' || card.kind === 'immunity' && payment.value?.type === 'rent') ?? []);
const pendingTile = computed(() => game.room?.board[game.room.pending?.tileId ?? -1]);
const payment = computed(() => game.room?.pending?.type === 'rent' || game.room?.pending?.type === 'debt' ? game.room.pending : null);
const creditorName = computed(() => game.room?.players.find(player => player.id === payment.value?.creditorId)?.name ?? '银行');
const canTrade = computed(() => canPlay.value && game.room?.pending?.type !== 'choice' && (!payment.value || payment.value.defenseResolved) && (['pre_roll', 'manage'].includes(game.room?.phase ?? '') || !!payment.value?.creditorId));
const liquidationValue = computed(() => (game.me?.cash ?? 0) + owned.value.reduce((sum, tile) => sum + (tile.mortgaged ? 0 : mortgageValue(tile, game.room?.economy)), 0));
const canLiquidate = computed(() => canPlay.value && !!payment.value && liquidationValue.value < payment.value.amount && (!!payment.value.creditorId || owned.value.every(tile => tile.mortgaged)));
const elimination = computed(() => game.room?.players.filter(player => player.bankrupt).sort((a, b) => (a.eliminationOrder ?? 0) - (b.eliminationOrder ?? 0)) ?? []);
const isPlayerGame = computed(() => !props.spectator && game.room?.status === 'playing');
const currentTile = computed(() => game.room?.board[game.me?.position ?? game.current?.position ?? 0]);
const archiveCode = computed(() => game.privateData?.archiveCode ?? game.session?.archiveCode ?? '');
const visibleLogs = computed(() => game.room?.logs.filter(entry => entry.text.includes(logSearch.value.trim())) ?? []);
const latestLog = computed(() => game.room?.logs[0]);
const canRoll = computed(() => canPlay.value && !game.room?.recoveryPending && game.room?.status === 'playing' && game.room?.phase === 'pre_roll' && !game.room?.pending);
const isGameSession = computed(() => !!game.room && game.room.status !== 'lobby');
const handCount = computed(() => game.privateData?.hand.length ?? 0);
const liveSelectedTile = computed(() => selectedTile.value ? game.room?.board[selectedTile.value.id] : null);
const publicProgress = computed(() => {
  if (game.room?.recoveryPending) return '等待房主分配角色并继续对局。';
  if (game.room?.status === 'finished') return '本局已结束，等待房主再开一局。';
  if (game.room?.pausedReason) return game.room.pausedReason;
  if (game.room?.tradePending) return '双方正在商量交易。';
  if (game.room?.pending?.type === 'choice') return `${game.decisionPlayer?.name ?? '玩家'} 正在处理私人选择。`;
  const descriptions = { pre_roll: '准备移动', buy: '选择是否购买地产', upgrade: '选择是否升级建筑', rent: '选择路费支付方式', debt: '选择费用支付方式', manage: '管理资产或准备结束回合' };
  return `${game.decisionPlayer?.name ?? '玩家'} 正在${descriptions[game.room?.phase ?? 'pre_roll']}。`;
});

/** 输入当前房间路由，恢复本地身份或申请新的玩家/观战会话。 */
async function enter(): Promise<void> {
  const generation = ++entryGeneration;
  game.disconnect();
  try {
    const saved = readSession(code.value, props.spectator);
    const value = saved ?? await game.enter(props.spectator ? 'spectate' : 'join', code.value, localStorage.getItem('dfw:nickname') || '');
    if (generation === entryGeneration) game.connect(value);
  } catch (error) { if (generation === entryGeneration) game.fatalError = error instanceof Error ? error.message : '无法进入房间'; }
}
watch([code, () => props.spectator], enter, { immediate: true });
watch([() => game.privateData?.trade?.id, () => game.room?.recoveryPending], ([id, recovering]) => { if (id && !recovering) modal.value = 'trade'; else if (modal.value === 'trade') modal.value = null; });
watch(() => payment.value?.id, () => { selectedDefense.value = ''; if (modal.value === 'bankruptcy') modal.value = null; });
watch([() => choice.value?.id, () => game.room?.recoveryPending], ([id, recovering]) => { if (id && !props.spectator && !recovering) modal.value = 'choice'; else if (modal.value === 'choice') modal.value = null; });
watch([() => props.spectator, () => game.me?.bankrupt, () => game.room?.status], ([spectator, bankrupt, status]) => { if (spectator || bankrupt || status === 'finished') workbenchTab.value = 'logs'; });
onBeforeUnmount(() => { entryGeneration++; game.disconnect(); });

/** 输入工作台页签，切换后将页面滚动到可操作的面板。 */
async function openWorkbench(tab: WorkbenchTab): Promise<void> {
  workbenchTab.value = tab;
  await nextTick();
  workbench.value?.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'start' });
}

/** 请求浏览器进入或退出全屏，失败时提示用户。 */
async function fullscreen(): Promise<void> {
  try { if (document.fullscreenElement) await document.exitFullscreen(); else await document.documentElement.requestFullscreen(); }
  catch { game.notify('当前浏览器不支持全屏，请使用浏览器全屏或投屏功能。'); }
}
/** 输入私有手牌，打开参数选择弹窗。 */
function chooseCard(card: Card): void { selectedCard.value = card; modal.value = 'card'; }
/** 输入大小选择事件，以原命令通道保存房主设置，失败时恢复公开快照值。 */
async function changeMapSize(event: Event): Promise<void> {
  const select = event.target as HTMLSelectElement;
  if (!await game.act({ type: 'setMapSize', mapSize: select.value as MapSize })) select.value = game.room?.mapSize ?? 'large';
}
/** 用户明确重新加入时删除失效会话，返回重新认证流程。 */
async function resetSession(): Promise<void> {
  forgetSession(code.value, props.spectator);
  if (!props.spectator) forgetSession(code.value, true);
  await enter();
}
</script>
<template>
  <div class="room-page" :class="{ 'spectator-page': spectator, 'game-session': isGameSession }">
    <header class="site-header room-header">
      <RouterLink to="/" aria-label="返回首页"><Logo /></RouterLink>
      <div class="room-heading"><button class="room-tag" aria-label="复制房间号" @click="game.copy(code)">房间 <strong>{{ code }}</strong><span>⧉</span></button><span v-if="spectator" class="viewer-tag">▣ 公共观战</span></div>
      <div class="header-actions"><span class="connection-label" :class="game.connection"><i></i>{{ game.connection === 'online' ? '实时同步' : game.connection === 'connecting' ? '连接中' : '连接已断开' }}</span><button class="button ghost small" @click="modal = 'rules'">玩法说明</button><button v-if="spectator" class="button secondary small" @click="fullscreen">⛶ 全屏</button><button v-else class="button secondary small" @click="game.copy(`${origin}/room/${code}`)">邀请好友 ↗</button></div>
      <div v-if="game.me && !spectator" class="header-actions"><button class="button ghost small" @click="modal = 'save'">存档</button><RouterLink class="button ghost small" to="/archives" @click="selectArchiveCode(game.privateData?.archiveCode ?? game.session?.archiveCode)">历史回顾</RouterLink></div>
      <details v-if="isGameSession" ref="roomMenu" class="mobile-room-menu"><summary aria-label="房间菜单">⋯</summary><div @click="roomMenu && (roomMenu.open = false)"><button @click="modal = 'rules'">玩法说明</button><button @click="game.copy(`${origin}/room/${code}`)">邀请朋友 ↗</button><button @click="game.copy(`${origin}/watch/${code}`)">复制观战链接</button><button v-if="game.me" @click="modal = 'save'">存档</button><RouterLink v-if="game.me" to="/archives" @click="selectArchiveCode(game.privateData?.archiveCode ?? game.session?.archiveCode)">历史回顾</RouterLink><button v-if="game.privateData?.archiveCode" @click="game.copy(formatArchiveCode(game.privateData.archiveCode))">复制存档码</button></div></details>
    </header>
    <main v-if="!game.room" class="loading-page"><div class="entry-icon">🎲</div><h2>{{ game.fatalError ? '暂时无法进入房间' : '正在打开你的城市…' }}</h2><p class="muted" role="status">{{ game.fatalError || '连接房间，恢复最新的棋盘和玩家信息。' }}</p><div v-if="game.fatalError" class="button-row"><RouterLink class="button secondary" to="/">返回首页</RouterLink><RouterLink v-if="!spectator" class="button primary" :to="`/watch/${code}`">进入观战</RouterLink><button class="button ghost" @click="resetSession">重新加入</button></div></main>
    <main v-else>
      <div v-if="game.connection !== 'online'" class="notice warning connection-notice">{{ game.fatalError || '网络连接已断开，正在自动重连。当前显示最后同步的局面。' }}<button v-if="game.connection === 'replaced'" class="button small secondary" @click="enter">在此页重新连接</button></div>
      <div v-if="game.room.pausedReason" class="notice warning connection-notice">Ⅱ {{ game.room.pausedReason }}。对局已保存，恢复后继续当前步骤。</div>
      <RecoverySeats v-if="!spectator" />
      <section v-if="game.room.status === 'lobby'" class="lobby-layout">
        <div class="lobby-hero"><span class="pill">一场新旅程，即将开始</span><h1>让朋友们<br /><span>都坐下来。</span></h1><p class="muted">2～6 人准备好后，房主就能开启新的城市。</p><City /><div class="lobby-code"><small>分享房间号</small><strong>{{ code }}</strong><button class="icon-button" aria-label="复制房间号" @click="game.copy(code)">⧉</button></div><button class="button ghost" @click="game.copy(`${origin}/watch/${code}`)">复制投屏观战链接 ↗</button></div>
        <div class="panel lobby-panel"><div class="section-heading"><h2>玩家席位</h2><span>{{ game.room.players.length }} / 6</span></div><label class="field">地图大小<select :value="game.room.mapSize ?? 'large'" :disabled="spectator || !game.isHost || !game.canAct" @change="changeMapSize"><option v-for="size in MAP_SIZES" :key="size.value" :value="size.value">{{ size.name }} · {{ mapLength(previewPlayers, size.value) }} 格（16＋人数×{{ size.factor }}）</option></select></label><p class="muted">房主开局前选择，格数随人数变化。机会与财富各 {{ previewPlayers <= 3 ? 2 : 3 }} 格，数量不随地图大小变化。<span v-if="game.room.players.length < 2">当前按至少两人预览。</span></p><div class="lobby-seats"><div v-for="player in game.room.players" :key="player.id" class="lobby-seat"><span class="avatar" :style="{ background: player.color }">{{ player.name.slice(0, 1) }}</span><div><strong>{{ player.name }} <small v-if="player.id === game.me?.id">你</small></strong><small>{{ player.id === game.room.hostId ? '房主 · ' : '' }}{{ player.online ? '已连接' : '暂时离线' }}</small></div><span class="ready-pill" :class="{ ready: player.ready }">{{ player.ready ? '✓ 已准备' : '等待准备' }}</span><button v-if="game.isHost && !player.online && player.id !== game.me?.id" class="button ghost small danger" :disabled="game.busy" @click="game.act({ type: 'removePlayer', playerId: player.id })">移出</button></div><div v-for="seat in 6 - game.room.players.length" :key="`empty-${seat}`" class="lobby-seat empty"><span class="avatar">＋</span><span>等待朋友加入</span></div></div><div v-if="!spectator" class="lobby-controls"><button class="button full" :class="game.me?.ready ? 'secondary' : 'primary'" :disabled="!game.canAct" @click="game.act({ type: 'ready', value: !game.me?.ready })">{{ game.me?.ready ? '已准备 · 点击取消' : '我准备好了' }}</button><button v-if="game.isHost" class="button primary full" :disabled="!game.canAct || !allReady" @click="game.act({ type: 'start' })">开启财富旅程 ↗</button><p class="muted">{{ allReady ? '大家都已准备，可以开局了。' : '需要至少两人，所有玩家连接并准备后开局。' }}</p></div><p v-else class="notice">这是公共投屏页面。请让玩家在自己的手机或电脑上准备。</p></div>
      </section>
      <template v-else>
        <div v-if="isPlayerGame" class="session-overview">
          <div class="session-title"><span class="eyebrow">CITY CLUB · 好友财富派对</span><h1>下一步，轮到好运。</h1><p>第 {{ game.room.turnNumber }} 次行动 <span>·</span> {{ game.current?.name }} 的回合</p></div>
          <div v-if="game.me" class="session-wallet"><div><span>我的现金</span><strong>¥{{ game.me.cash.toLocaleString() }}</strong></div><div><span>净资产 <small v-if="game.me.assetRank">第 {{ game.me.assetRank }} 名</small></span><strong>¥{{ game.me.netWorth?.toLocaleString() ?? '—' }}</strong></div><button class="button secondary small" @click="openWorkbench('assets')">⌂ 我的地产 <b>{{ owned.length }}</b></button></div>
        </div>
        <div v-else class="game-title"><div><span class="eyebrow">THE CITY IS YOURS</span><h1>{{ game.room.status === 'finished' ? '这座城市，见证了赢家。' : '城市就位，好运出发。' }}</h1></div><button class="button ghost small" @click="game.copy(`${origin}/watch/${code}`)">▣ 观战链接</button></div>
        <div v-if="game.room.status === 'finished'" class="winner-banner"><span>🏆</span><div><small>本局财富赢家</small><h2>{{ game.room.players.find(player => player.id === game.room?.winnerId)?.name }}</h2><p>淘汰顺序：{{ elimination.map(player => player.name).join(' → ') }}</p></div><button v-if="game.isHost && !spectator" class="button primary" :disabled="!game.canAct" @click="game.act({ type: 'newGame' })">再来一局 ↗</button></div>
        <section class="live-broadcast" aria-label="实时事件播报">
          <span class="broadcast-label"><i class="status-dot"></i>实时播报</span>
          <div class="broadcast-message" aria-live="polite" aria-atomic="true"><time v-if="latestLog">{{ new Date(latestLog.at).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) }}</time><p>{{ latestLog?.text ?? publicProgress }}</p></div>
          <button class="button ghost small" @click="openWorkbench('logs')">查看记录 ↗</button>
        </section>
        <div class="game-layout">
          <section class="map-panel">
            <div class="board-view">
              <Board :room="game.room" :spectator="spectator" compact @select="selectedTile = $event">
                <template #controls>
                  <button v-if="!spectator && !game.me?.bankrupt && game.room.status === 'playing'" class="button primary center-roll-button" :disabled="!canRoll" @click="game.act({ type: 'roll' })">{{ game.busy ? '正在同步…' : canRoll ? '🎲 掷骰子' : game.isMyTurn ? '等待当前结算' : '等待朋友行动' }}</button>
                  <p class="center-progress">{{ publicProgress }}</p>
                  <button v-if="!spectator && !game.me?.bankrupt && game.room.status === 'playing'" class="board-workbench-link" @click="openWorkbench('cards')">掷骰前，去工作台看看手牌 ↓</button>
                </template>
              </Board>
              <button v-if="currentTile" class="position-brief" @click="selectedTile = currentTile"><span class="position-icon">⌖</span><div><small>{{ game.me ? '我的位置' : '当前行动位置' }}</small><strong>{{ currentTile.name }} <span>{{ currentTile.id }} 号地块</span></strong></div><span class="position-meta">{{ currentTile.type === 'property' ? `当前租金 ¥${rentFor(currentTile, game.room.economy).toLocaleString()}` : '查看地块说明' }} <b>↗</b></span></button>
            </div>
            <section v-if="!spectator && !game.me?.bankrupt && game.room.status === 'playing'" class="panel action-panel"><div class="section-heading"><h2>{{ game.canDecide ? (payment ? '选择支付方式' : game.isMyTurn ? '轮到你了' : '由你处理') : '下一步，好运' }}</h2><span class="turn-tag">{{ game.canDecide ? (payment ? '等待结算' : game.isMyTurn ? '你的回合' : '你的选择') : '等待行动' }}</span></div>
              <div class="action-details">
              <template v-if="game.room.tradePending"><p class="muted">双方正在商量交易，其他操作暂停。</p><button v-if="game.privateData?.trade" class="button secondary full" @click="modal = 'trade'">查看交易报价</button></template>
              <template v-else-if="game.canDecide">
                <template v-if="game.room.pending?.type === 'choice'"><p class="muted">请先处理当前选择，确认后继续原结算。</p><button class="button primary full" :disabled="!choice || !canPlay" @click="modal = 'choice'">打开当前选择</button></template>
                <template v-else-if="game.room.phase === 'pre_roll'"><p class="muted">可以先使用一张道具，再看看骰子把你带到哪里。</p><button class="button secondary small" @click="openWorkbench('cards')">查看手牌，筹划下一步 ↓</button></template>
                <template v-else-if="game.room.phase === 'buy' || game.room.phase === 'upgrade'"><div class="landing-info"><span>⌂</span><div><strong>{{ pendingTile?.name }}</strong><small>{{ game.room.phase === 'buy' ? '这块地还没有主人' : `你的地产 · 当前 ${pendingTile?.level} 级` }}</small></div></div><p class="decision-price">¥{{ game.room.pending?.amount.toLocaleString() }} <small>{{ game.room.phase === 'buy' ? '购入价格' : '升级费用' }}</small></p><div class="button-row"><button class="button secondary" :disabled="!canPlay" @click="game.act({ type: 'pass' })">暂时放弃</button><button class="button primary" :disabled="!canPlay || (game.me?.cash ?? 0) < (game.room.pending?.amount ?? 0)" @click="game.act({ type: game.room.phase === 'buy' ? 'buy' : 'upgrade' })">{{ game.room.phase === 'buy' ? '买下这块地' : '升级房子' }}</button></div></template>
                <template v-else-if="payment">
                  <p class="muted">{{ payment.type === 'rent' ? `停在 ${pendingTile?.name}，本次路费` : '本次强制费用' }}付给 {{ creditorName }}。</p>
                  <p class="decision-price">¥{{ payment.amount.toLocaleString() }} <small>剩余待支付</small></p>
                  <p class="muted">当前现金 ¥{{ game.me?.cash.toLocaleString() }}。可先管理资产保留现金流，确认后才扣款。</p>
                  <template v-if="!payment.defenseResolved">
                    <p class="notice">先选择本笔费用的一种防御，或放弃使用。确认后再管理资产或协商抵债。</p>
                    <label v-if="defenseCards.length" class="field">选择防御道具<select v-model="selectedDefense"><option value="">不使用道具</option><option v-for="card in defenseCards" :key="card.id" :value="card.id">{{ card.name ?? ITEMS[card.kind].name }} · {{ card.rarity ?? 'R' }}</option></select></label>
                    <ContentCard v-if="defenseCards.find(card => card.id === selectedDefense)" :content="defenseCards.find(card => card.id === selectedDefense)!" />
                    <button class="button primary full" :disabled="!canPlay" @click="game.act({ type: 'paymentDefense', ...(payment.id ? { decisionId: payment.id } : {}), ...(selectedDefense ? { cardId: selectedDefense } : {}) })">{{ selectedDefense ? '确认使用本笔防御' : '不使用道具，继续支付选择' }}</button>
                  </template>
                  <div v-else class="button-row"><button class="button primary" :disabled="!canPlay || (game.me?.cash ?? 0) < payment.amount" @click="game.act({ type: 'payDebt' })">确认现金支付</button></div>
                  <div v-if="payment.defenseResolved" class="button-row"><button class="button secondary" :disabled="!canPlay" @click="modal = 'assets'">选择资产抵押或变现</button><button v-if="payment.creditorId" class="button secondary" :disabled="!canTrade" @click="modal = 'trade'">协商抵债或出售资产</button></div>
                  <p v-if="!payment.creditorId && (game.me?.cash ?? 0) < payment.amount" class="notice warning">系统费用需自行选择地产抵押筹款。</p>
                  <template v-if="payment.defenseResolved && liquidationValue < payment.amount"><p class="muted">银行变现仍不足。{{ payment.creditorId ? '可继续与玩家协商成交，或主动结束协商并清算。' : '抵押全部可用地产后仍不足，可确认清算。' }}</p><button class="button ghost small danger" :disabled="!canLiquidate" @click="modal = 'bankruptcy'">结束协商并申请清算</button></template>
                </template>
                <template v-else><p class="muted">本次落点已处理，可以管理资产、交易或结束回合。</p><button class="button primary full" :disabled="!canPlay" @click="game.act({ type: 'endTurn' })">结束我的回合 →</button></template>
              </template>
              <div v-else class="waiting-turn"><span class="avatar" :style="{ background: game.decisionPlayer?.color }">{{ game.decisionPlayer?.name.slice(0, 1) }}</span><p>{{ game.decisionPlayer?.name }} 正在{{ game.room.phase === 'debt' ? '偿债' : '行动' }}<small>观察棋盘，筹划下一步。</small></p></div>
              </div>
              <div class="quick-actions"><button class="button ghost small" title="我的地产" aria-label="我的地产" @click="openWorkbench('assets')">⌂ 我的地产 {{ owned.length }}</button><button class="button ghost small" title="发起交易" aria-label="发起交易" :disabled="!canTrade" @click="openWorkbench('trade')">⇄ 发起交易</button></div>
            </section>
          </section>
          <aside class="game-sidebar">
            <PublicLeaderboard :room="game.room" :me-id="game.me?.id" :can-remove="game.isHost && !spectator" :busy="game.busy" @select="selectedTile = $event" @remove="game.act({ type: 'removePlayer', playerId: $event })" />
            <section class="panel city-feed"><div class="section-heading"><h2>📣 城市新鲜事</h2><button class="button ghost small" @click="openWorkbench('history')">回顾 ↗</button></div><p class="muted">{{ publicProgress }}</p><div class="event-list"><div v-for="entry in game.room.logs.slice(0, 4)" :key="entry.id" class="event-row"><time>{{ new Date(entry.at).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) }}</time><p>{{ entry.text }}</p></div></div><p v-if="!game.room.logs.length" class="muted">城市动态将在这里实时更新。</p></section>
            <section v-if="game.room.event" class="panel public-content-panel"><details :key="game.room.event.id" open><summary>{{ game.room.players.find(player => player.id === game.room?.eventPlayerId)?.name }} 的城市事件 ⌄</summary><ContentCard :content="game.room.event" compact /><p v-if="game.room.event.targetId" class="notice">本次目标：{{ game.room.players.find(player => player.id === game.room?.event?.targetId)?.name }}（事件开始时锁定）</p></details></section>
            <section v-if="spectator || game.me?.bankrupt" class="panel spectator-note"><span>▣</span><h3>{{ spectator ? '聚会的公共视角' : '你已退出竞争' }}</h3><p>棋盘、资产榜和事件实时更新，陪朋友们继续这场财富派对。</p></section>
          </aside>
        </div>
        <section ref="workbench" class="workbench" aria-label="游玩工作台">
          <div class="workbench-heading"><div><span class="eyebrow">YOUR NEXT MOVE</span><h2>🧰 工作台</h2></div><p>资产在手，好戏继续。<span v-if="spectator"> · 公共观战</span></p><div v-if="game.me && !spectator" class="button-row"><button class="button secondary small" @click="modal = 'save'">标记存档</button><RouterLink class="button ghost small" to="/archives" @click="selectArchiveCode(archiveCode)">存档与恢复 ↗</RouterLink></div></div>
          <nav class="workbench-nav" aria-label="工作台内容">
            <button v-if="!spectator && !game.me?.bankrupt && game.room.status === 'playing'" :class="{ active: workbenchTab === 'assets' }" :aria-pressed="workbenchTab === 'assets'" @click="workbenchTab = 'assets'">⌂ 资产 <small>{{ owned.length }}</small></button>
            <button v-if="!spectator && !game.me?.bankrupt && game.room.status === 'playing'" :class="{ active: workbenchTab === 'cards' }" :aria-pressed="workbenchTab === 'cards'" @click="workbenchTab = 'cards'">▣ 卡牌 <small>{{ handCount }}</small></button>
            <button v-if="!spectator && !game.me?.bankrupt && game.room.status === 'playing'" :class="{ active: workbenchTab === 'trade' }" :aria-pressed="workbenchTab === 'trade'" @click="workbenchTab = 'trade'">⇄ 交易 <small v-if="game.room.tradePending">进行中</small></button>
            <button :class="{ active: workbenchTab === 'logs' }" :aria-pressed="workbenchTab === 'logs'" @click="workbenchTab = 'logs'">≡ 实时记录</button>
            <button :class="{ active: workbenchTab === 'history' }" :aria-pressed="workbenchTab === 'history'" @click="workbenchTab = 'history'">↶ 历史回顾</button>
          </nav>
          <div class="workbench-content">
            <section v-if="workbenchTab === 'assets' && !spectator && !game.me?.bankrupt && game.room.status === 'playing'" class="panel workbench-assets"><div class="section-heading"><h2>我的资产</h2><span>现金 ¥{{ game.me?.cash.toLocaleString() }} · 地产 {{ owned.length }} 处</span></div><Assets /></section>
            <section v-if="workbenchTab === 'cards' && !spectator && !game.me?.bankrupt && game.room.status === 'playing'" class="panel cards-panel"><div class="section-heading"><h2>我的手牌 <small>{{ handCount }} / 6</small></h2><span>仅你可见</span></div><p class="hand-hint"><span :class="{ used: game.privateData?.activeCardUsed }">{{ game.privateData?.activeCardUsed ? '本回合主动道具已使用' : '掷骰前最多使用一张主动道具' }}</span> 点击卡牌查看效果与使用时机</p><p v-if="!game.privateData?.hand.length" class="muted">暂时没有道具，机会和财富格会带来新补给。</p><div class="hand-grid"><button v-for="card in game.privateData?.hand" :key="card.id" class="item-card content-hand-card" :title="card.description ?? ITEMS[card.kind].description" @click="chooseCard(card)"><ContentCard :content="card" compact /></button></div></section>
            <section v-if="workbenchTab === 'cards' && !spectator && game.privateData?.noticeText" class="panel private-content-panel"><div class="section-heading"><h2>私人结果</h2><span>仅你可见</span></div><p>{{ game.privateData.noticeText }}</p><ContentCard v-if="game.privateData.event" :content="game.privateData.event" /><ContentCard v-if="game.privateData.notice" :content="game.privateData.notice" /></section>
            <section v-if="workbenchTab === 'trade' && !spectator && !game.me?.bankrupt && game.room.status === 'playing'" class="panel workbench-trade"><div class="section-heading"><h2>朋友间的好生意</h2><span>报价仅双方可见</span></div><p v-if="!canTrade && !game.privateData?.trade" class="notice">{{ game.room.tradePending ? '其他玩家正在交易，等待他们完成后继续。' : '轮到你移动前、落点处理完毕或处理玩家债务时，可以发起交易。' }}</p><TradeEditor @done="workbenchTab = 'assets'" /></section>
            <section v-if="workbenchTab === 'logs'" class="panel event-panel"><div class="section-heading"><h2>实时公开记录</h2><span>最近 {{ game.room.logs.length }} 条 · 最新在前</span></div><label class="field log-search">查找记录<input v-model="logSearch" type="search" placeholder="输入玩家或地块名称" /></label><div class="event-list"><div v-for="entry in visibleLogs" :key="entry.id" class="event-row"><time>{{ new Date(entry.at).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) }}</time><p>{{ entry.text }}</p></div></div><p v-if="!visibleLogs.length" class="muted">{{ logSearch ? '没有找到匹配的动态。' : '暂时没有公开动态。' }}</p><button class="button ghost small" @click="workbenchTab = 'history'">查看更早的完整历史 ↗</button></section>
            <section v-if="workbenchTab === 'history'" class="panel"><div class="section-heading"><h2>历史事件回顾</h2><span>事件与当时的棋盘</span></div><RoomHistory v-if="archiveCode && !spectator" :archive-code="archiveCode" :game-id="game.room.gameId" /><div v-else><p class="muted">完整历史需要本房间的固定存档码。可以向参与玩家获取，在历史页输入后回顾。</p><RouterLink class="button secondary" to="/archives">打开历史与恢复 ↗</RouterLink></div></section>
          </div>
        </section>
      </template>
    </main>
    <Modal v-if="modal === 'rules'" title="大富翁玩法说明" wide @close="modal = null"><Rules :economy="game.room?.economy" :spectator="spectator" :chance-events="game.room?.chanceEventDescriptions" /></Modal>
    <Modal v-if="modal === 'save' && game.me && !spectator" title="标记当前存档" @close="modal = null"><SaveArchive /></Modal>
    <Modal v-if="modal === 'assets' && !spectator" title="我的地产与现金周转" wide @close="modal = null"><Assets /></Modal>
    <Modal v-if="modal === 'card' && selectedCard && !spectator" :title="selectedCard.name ?? ITEMS[selectedCard.kind].name" @close="modal = null"><CardAction :key="selectedCard.id" :card="selectedCard" @done="modal = choice ? 'choice' : null" /></Modal>
    <Modal v-if="modal === 'choice' && choice && !spectator" title="处理当前选择" :wide="choice.type === 'hand_limit'" @close="modal = null"><ContentChoice :key="choice.id" :choice="choice" /></Modal>
    <Modal v-if="modal === 'trade' && !spectator" title="朋友间的好生意" wide @close="modal = null"><TradeEditor @done="modal = null" /></Modal>
    <Modal v-if="modal === 'bankruptcy' && payment && !spectator" title="结束偿债并清算" @close="modal = null"><p>待支付 ¥{{ payment.amount.toLocaleString() }}，现金加银行可变现资产 ¥{{ liquidationValue.toLocaleString() }}。</p><p class="notice warning">确认后全部地产归还银行，你退出本局竞争。{{ payment.creditorId ? `银行变现所得用于向 ${creditorName} 偿债。` : '剩余现金用于向银行偿债。' }}</p><div class="button-row"><button class="button secondary" @click="modal = null">继续筹款协商</button><button class="button danger" :disabled="!canLiquidate" @click="game.act({ type: 'declareBankruptcy' }).then(ok => { if (ok) modal = null; })">确认清算并退出竞争</button></div></Modal>
    <Modal v-if="liveSelectedTile" :title="liveSelectedTile.name" @close="selectedTile = null"><div class="tile-detail"><span :class="`district-${liveSelectedTile.district}`">{{ liveSelectedTile.type === 'property' ? '⌂' : '✦' }}</span><p>{{ districtName(liveSelectedTile) }} · {{ liveSelectedTile.id }} 号地块</p><template v-if="liveSelectedTile.type === 'property'"><dl><div><dt>地产价格</dt><dd>¥{{ liveSelectedTile.price.toLocaleString() }}</dd></div><div><dt>地产主人</dt><dd>{{ game.room?.players.find(player => player.id === liveSelectedTile?.ownerId)?.name || '待购入' }}</dd></div><div><dt>建筑等级</dt><dd>{{ liveSelectedTile.level }} / 3</dd></div><div><dt>当前路费</dt><dd>¥{{ rentFor(liveSelectedTile, game.room?.economy) }}</dd></div><div><dt>抵押状态</dt><dd>{{ liveSelectedTile.mortgaged ? `已抵押 · 赎回 ¥${redeemCost(liveSelectedTile)}` : '未抵押' }}</dd></div><div><dt>路障</dt><dd>{{ liveSelectedTile.barrier ? '首位经过者将停下' : '无' }}</dd></div></dl></template><p v-else class="muted">{{ liveSelectedTile.type === 'start' ? `正向经过或到达，获得 ¥${ruleMoney(game.room?.economy, 'startIncome', 1000).toLocaleString()}。` : liveSelectedTile.type === 'jail' ? '停在此处，跳过下一次行动。' : '停在此处，服务端抽取随机事件，费用由玩家选择支付方式。' }}</p></div></Modal>
    <footer class="room-footer">小小棋盘，大大野心。<span>好运和朋友，都在这里。</span></footer>
  </div>
</template>
