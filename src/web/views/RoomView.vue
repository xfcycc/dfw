<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { districtName, ITEMS, mapLength, MAP_SIZES, mortgageValue, redeemCost, rentFor, ruleMoney } from '../../shared/rules';
import type { Card, MapSize, Tile } from '../../shared/types';
import { forgetSession, readSession, useGameStore } from '../stores/game';
import { useTurnSound } from '../composables/useTurnSound';
import { playSound } from '../audio';
import AudioSettings from '../components/AudioSettings.vue';
import ActionIcon from '../components/ActionIcon.vue';
import AssetStatIcon from '../components/AssetStatIcon.vue';
import ChibiLettering from '../components/ChibiLettering.vue';
import ReadyIcon from '../components/ReadyIcon.vue';
import Logo from '../components/Logo.vue';
import Board from '../components/Board.vue';
import Modal from '../components/Modal.vue';
import Assets from '../components/Assets.vue';
import CardAction from '../components/CardAction.vue';
import CardTargetPicker from '../components/CardTargetPicker.vue';
import ContentCard from '../components/ContentCard.vue';
import CardReveal from '../components/CardReveal.vue';
import ContentChoice from '../components/ContentChoice.vue';
import TradeEditor from '../components/TradeEditor.vue';
import Rules from '../components/Rules.vue';
import SaveArchive from '../components/SaveArchive.vue';
import RecoverySeats from '../components/RecoverySeats.vue';
import PublicLeaderboard from '../components/PublicLeaderboard.vue';
import RoomHistory from '../components/RoomHistory.vue';
import LiveRecords from '../components/LiveRecords.vue';
import PlayerText from '../components/PlayerText.vue';
import CharacterPawn from '../components/CharacterPawn.vue';
import PropertyGrade from '../components/PropertyGrade.vue';
import PropertyTile from '../components/PropertyTile.vue';
import AvatarPicker from '../components/AvatarPicker.vue';
import '../game-room.css';
import '../archive.css';
import '../modern-room.css';
import '../lobby-room.css';

const props = withDefaults(defineProps<{ spectator?: boolean }>(), { spectator: false });
const route = useRoute();
const origin = window.location.origin;
const baseUrl = import.meta.env.BASE_URL;
const game = useGameStore();
const modal = ref<'tools' | 'rules' | 'bankruptcy' | 'save' | 'trade' | 'avatar' | 'card' | 'choice' | 'defense' | 'decision' | 'assets' | null>(null);
const selectedCard = ref<Card | null>(null);
const tradeEditorVersion = ref(0);
const submittingCard = ref(false);
const selectedTarget = ref('');
const selectedCardActive = ref(false);
const selectedCardTurn = ref('');
const mapView = ref<HTMLElement>();
const selectedDefense = ref('');
const selectedTile = ref<Tile | null>(null);
const recordMode = ref<'live' | 'history'>('live');
const chatMessage = ref('');
const canChat = computed(() => !props.spectator && !!game.me && game.connection === 'online' && !game.busy && !game.room?.recoveryPending);
/** 提交输入框的公开聊天，成功后清空正文并显示实时记录；失败保留输入。 */
async function sendChat(): Promise<void> {
  const message = chatMessage.value.trim();
  if (!canChat.value || !message) return;
  if (await game.act({ type: 'chat', message })) { chatMessage.value = ''; recordMode.value = 'live'; }
}
/** 输入回车事件，中文输入法选词时保留回车，Shift+Enter 换行。 */
function chatEnter(event: KeyboardEvent): void {
  if (event.isComposing || event.shiftKey) return;
  event.preventDefault(); void sendChat();
}
const boardAnimating = ref(false);
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
const canTrade = computed(() => !drafting.value && canPlay.value && game.room?.pending?.type !== 'choice' && (!payment.value || payment.value.defenseResolved) && (['pre_roll', 'manage'].includes(game.room?.phase ?? '') || !!payment.value?.creditorId));
const liquidationValue = computed(() => (game.me?.cash ?? 0) + owned.value.reduce((sum, tile) => sum + (tile.mortgaged ? 0 : mortgageValue(tile, game.room?.economy)), 0));
const canLiquidate = computed(() => canPlay.value && !!payment.value && liquidationValue.value < payment.value.amount && (!!payment.value.creditorId || owned.value.every(tile => tile.mortgaged)));
const elimination = computed(() => game.room?.players.filter(player => player.bankrupt).sort((a, b) => (a.eliminationOrder ?? 0) - (b.eliminationOrder ?? 0)) ?? []);
const archiveCode = computed(() => game.privateData?.archiveCode ?? game.session?.archiveCode ?? '');
const latestLog = computed(() => game.room?.logs[0]);
const canRoll = computed(() => !drafting.value && canPlay.value && !game.room?.recoveryPending && game.room?.status === 'playing' && game.room?.phase === 'pre_roll' && !game.room?.pending);
const isGameSession = computed(() => !!game.room && game.room.status !== 'lobby');
const isLobby = computed(() => game.room?.status === 'lobby');
const isSingleScreen = computed(() => isGameSession.value && !game.room?.recoveryPending);
const hasCentralDecision = computed(() => !game.settling && game.canDecide && !game.room?.tradePending && !!game.room?.pending);
const decisionTitle = computed(() => payment.value ? '选择支付方式' : choice.value ? choice.value.content?.name ?? '处理当前选择' : game.room?.pending?.type === 'choice' ? '处理当前选择' : game.room?.phase === 'buy' ? '购买地产' : '升级建筑');
const decisionKey = computed(() => hasCentralDecision.value && !game.cardReveals.length ? `${game.room?.roomId}:${game.room?.gameId}:${game.room?.pending?.id ?? game.room?.turnNumber}:${game.room?.pending?.type}:${game.room?.pending?.tileId}` : '');
const handCount = computed(() => game.privateData?.hand.length ?? 0);
const liveSelectedTile = computed(() => selectedTile.value ? game.room?.board[selectedTile.value.id] : null);
const selectedTileOwner = computed(() => game.room?.players.find(player => player.id === liveSelectedTile.value?.ownerId));
const turnIdentity = computed(() => `${game.room?.roomId ?? code.value}:${game.room?.gameId}:${game.room?.turnNumber}`);
const canPrepareCard = computed(() => !game.settling && !props.spectator && game.room?.status === 'playing' && game.isMyTurn && game.room.phase === 'pre_roll' && !game.room.pending && !game.room.tradePending && !game.privateData?.activeCardUsed);
const drafting = computed(() => !!selectedCard.value && selectedCardActive.value);
const mapChoice = computed(() => choice.value && ['free_upgrade', 'self_destroy'].includes(choice.value.type) ? choice.value : null);
const targetKind = computed(() => mapChoice.value ? 'tile' : drafting.value && ['barrier', 'demolish'].includes(selectedCard.value!.kind) ? 'tile' : drafting.value && ['swap', 'steal'].includes(selectedCard.value!.kind) ? 'player' : null);
const targeting = computed(() => targetKind.value !== null && (modal.value === 'card' || modal.value === 'choice'));
const eligibleTiles = computed(() => targetKind.value === 'tile' ? game.room?.board.filter(tile => !tileReason(tile)).map(tile => tile.id) ?? [] : []);
const eligiblePlayers = computed(() => targetKind.value === 'player' ? game.room?.players.filter(player => !player.bankrupt && player.id !== game.me?.id && (selectedCard.value?.kind !== 'swap' || !player.jailed)).map(player => player.id) ?? [] : []);
const targetTileId = computed(() => targetKind.value === 'tile' && selectedTarget.value !== '' ? Number(selectedTarget.value) : null);
const targetLabel = computed(() => targetKind.value === 'tile' && targetTileId.value != null ? `${game.room?.board[targetTileId.value]?.name} · ${targetTileId.value}号` : game.room?.players.find(player => player.id === selectedTarget.value)?.name ?? '');
const targetHint = computed(() => {
  if (mapChoice.value) return mapChoice.value.type === 'free_upgrade' ? '选择自己的未抵押、未满级且本回合未升级地产。' : '选择自己的有建筑地产，确认后摧毁全部建筑。';
  if (!targeting.value) return '';
  const rules = { barrier: '选择前方 1～6 格内任意无路障地块。', demolish: '选择对手有建筑的地产。', swap: '选择未服刑的其他玩家。', steal: '选择未破产的其他玩家。' };
  return rules[selectedCard.value!.kind as keyof typeof rules];
});
const dialogTargets = computed(() => {
  if (targetKind.value === 'player') return (game.room?.players ?? []).map(player => ({ id: player.id, name: player.name, detail: `现金 ¥${player.cash.toLocaleString()} · 手牌 ${player.cardCount ?? '—'}`, avatarId: player.avatarId, color: player.color, reason: eligiblePlayers.value.includes(player.id) ? '' : player.id === game.me?.id ? '不能选择自己' : player.bankrupt ? '该玩家已破产' : '该玩家正在服刑' })).sort((a, b) => Number(!!a.reason) - Number(!!b.reason));
  if (targetKind.value === 'tile') return (game.room?.board ?? []).map(tile => ({ id: String(tile.id), name: `${tile.name} · ${tile.id}号`, detail: tile.type === 'property' ? `${game.room?.players.find(p => p.id === tile.ownerId)?.name ?? '无主地产'} · ${tile.level}级${tile.mortgaged ? ' · 抵押' : ''}` : '特殊地块', reason: tileReason(tile) })).sort((a, b) => Number(!!a.reason) - Number(!!b.reason));
  return [];
});
const canEndTurn = computed(() => !drafting.value && canPlay.value && game.room?.phase === 'manage' && !game.room.pending);
const tradeReminder = computed(() => {
  const trade = game.privateData?.trade;
  if (!trade) return '';
  const other = game.room?.players.find(player => player.id === (trade.fromId === game.me?.id ? trade.toId : trade.fromId));
  return trade.toId === game.me?.id ? `收到 ${other?.name ?? '玩家'} 的报价` : `等待 ${other?.name ?? '玩家'} 回应`;
});
const showMyTurn = computed(() => !props.spectator && game.connection === 'online' && game.room?.status === 'playing' && !game.room.recoveryPending && !game.room.pausedReason && game.isMyTurn);
useTurnSound(() => showMyTurn.value ? turnIdentity.value : null);
let privateAudioBaseline = false;
watch(() => game.connection, state => { if (state !== 'online') privateAudioBaseline = true; }, { flush: 'sync' });
watch(() => game.privateData, (next, previous) => {
  if (!next || game.connection !== 'online' || props.spectator || game.room?.recoveryPending) return;
  if (privateAudioBaseline || !previous || next.playerId !== previous.playerId) { privateAudioBaseline = false; return; }
  if (next.activeCardUsed && !previous.activeCardUsed) playSound('magic');
  else if ((next.notice && next.notice.id !== previous.notice?.id) || next.hand.some(card => !previous.hand.some(before => before.id === card.id))) playSound('card');
});

/** 输入当前地块，返回无法选为道具目标的原因；空字符串表示合法。 */
function tileReason(tile: Tile): string {
  if (mapChoice.value) return mapChoice.value.tileIds.includes(tile.id) ? '' : '该地块不在本次选择的合法目标中，请点击高亮地产。';
  if (selectedCard.value?.kind === 'demolish') return tile.ownerId && tile.ownerId !== game.me?.id && tile.level > 0 ? '' : '拆房需要选择对手有建筑的地产。';
  if (selectedCard.value?.kind === 'barrier') {
    const distance = (tile.id - (game.me?.position ?? 0) + (game.room?.board.length ?? 1)) % (game.room?.board.length ?? 1);
    if (distance < 1 || distance > 6) return '路障只能放在前方 1～6 格内的地块。';
    return tile.barrier ? '这个地块已有路障，请选择其他地块。' : '';
  }
  return '当前道具不需要选择地块。';
}
watch(() => choice.value?.id, id => {
  selectedTarget.value = '';
  if (id) { const fromCard = modal.value === 'card' && selectedCard.value?.kind === 'freeUpgrade'; clearCard(); selectedTile.value = null; if (fromCard) modal.value = 'choice'; }
  else if (modal.value === 'choice') modal.value = null;
});
watch([turnIdentity, canPrepareCard, () => game.privateData?.hand], () => {
  if (selectedCard.value && (selectedCardTurn.value !== turnIdentity.value || !game.privateData?.hand.some(card => card.id === selectedCard.value?.id) || selectedCardActive.value && !canPrepareCard.value && !submittingCard.value)) clearCard();
});
watch([eligibleTiles, eligiblePlayers], () => {
  if (selectedTarget.value && !(targetKind.value === 'tile' ? eligibleTiles.value.includes(Number(selectedTarget.value)) : eligiblePlayers.value.includes(selectedTarget.value))) {
    selectedTarget.value = ''; game.notify('原目标已不可用，请重新选择高亮目标。');
  }
});
const actionStatus = computed(() => {
  if (game.connection !== 'online') return '正在连接';
  if (game.settling) return game.settlementRevealing ? '查看事件卡，收起后继续移动' : '移动中，落地后结算';
  if (game.room?.pausedReason) return '等待重连';
  if (game.room?.status === 'finished') return '本局结束';
  if (game.room?.tradePending) return '交易中';
  if (drafting.value) return '准备出牌';
  if (game.room?.pending?.type === 'choice') return game.canDecide ? '需要你确认' : '等待选择';
  if (game.room?.pending && game.canDecide) return '需要你确认';
  if (canRoll.value) return '轮到你了';
  const labels: Record<string, string> = { pre_roll: '准备掷骰', buy: '购买地产', upgrade: '升级建筑', rent: '支付路费', debt: '处理费用', manage: '管理资产' };
  return labels[game.room?.phase ?? 'pre_roll'] ?? '等待行动';
});

/** 输入当前房间路由，恢复本地身份或申请新的玩家/观战会话。 */
async function enter(): Promise<void> {
  const generation = ++entryGeneration;
  clearCard(); selectedTile.value = null; recordMode.value = 'live'; chatMessage.value = '';
  game.disconnect();
  try {
    const saved = readSession(code.value, props.spectator);
    const value = saved ?? await game.enter(props.spectator ? 'spectate' : 'join', code.value, localStorage.getItem('dfw:nickname') || '');
    if (generation === entryGeneration) game.connect(value);
  } catch (error) { if (generation === entryGeneration) game.fatalError = error instanceof Error ? error.message : '无法进入房间'; }
}
watch([code, () => props.spectator], enter, { immediate: true });
watch(() => payment.value?.id, () => { selectedDefense.value = ''; if (modal.value === 'bankruptcy') modal.value = null; });
watch([() => payment.value?.id, () => payment.value?.defenseResolved], () => { if (modal.value === 'defense') { if (!payment.value) modal.value = null; else if (payment.value.defenseResolved) openDecision(); } });
/** 新的本人待办打开临时决策弹窗，同一事项的同步不重复打开，关闭后可从行动栏继续。 */
watch([decisionKey, () => choice.value?.id], ([key], [previousKey]) => {
  if (!key) { if (modal.value === 'decision') modal.value = null; return; }
  if (key !== previousKey) { selectedTile.value = null; openDecision(); }
  else if (modal.value === 'decision' && choice.value) modal.value = 'choice';
});
/** 打开当前本人待办，已同步的私人选择复用原选择弹窗。 */
function openDecision(): void { if (!hasCentralDecision.value || game.cardReveals.length) return; modal.value = choice.value ? 'choice' : payment.value && !payment.value.defenseResolved ? 'defense' : 'decision'; }
/** 输入本人收到的报价 ID，新报价打开交易弹窗，同一报价同步不重复打开；结束后收起窗口。 */
watch(() => !props.spectator && game.privateData?.trade?.toId === game.me?.id ? game.privateData?.trade?.id : null, (id, previousId) => {
  if (id && id !== previousId) { selectedTile.value = null; modal.value = 'trade'; }
  else if (!id && previousId && modal.value === 'trade') modal.value = null;
}, { immediate: true });
onBeforeUnmount(() => { entryGeneration++; game.disconnect(); });

/** 输入区域标识，打开对应弹窗或滚动到已展示的区域，不切换主体内容。 */
async function openWorkbench(section: 'assets' | 'trade' | 'choice' | 'history'): Promise<void> {
  if (section === 'trade' || section === 'choice' || section === 'assets') { modal.value = section; return; }
  if (section === 'history') recordMode.value = 'history';
  await nextTick();
  mapView.value?.querySelector('.dashboard-records')?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
}

/** 请求浏览器进入或退出全屏，失败时提示用户。 */
async function fullscreen(): Promise<void> {
  try { if (document.fullscreenElement) await document.exitFullscreen(); else await document.documentElement.requestFullscreen(); }
  catch { game.notify('当前浏览器不支持全屏，请使用浏览器全屏或投屏功能。'); }
}
/** 输入本人手牌，在窗口中查看或准备使用；付款防御复用当前费用窗口。 */
function chooseCard(card: Card): void {
  if (game.settling) { game.notify('人物移动完成后再使用道具。'); return; }
  if (selectedCard.value?.id === card.id) { modal.value = 'card'; return; }
  if (payment.value && !payment.value.defenseResolved && defenseCards.value.some(value => value.id === card.id)) { selectedDefense.value = card.id; modal.value = 'defense'; return; }
  if (choice.value || game.privateData?.trade || game.busy) { game.notify('请先处理当前选择或报价。'); return; }
  selectedCard.value = card; selectedTarget.value = ''; selectedTile.value = null;
  selectedCardActive.value = canPrepareCard.value && !['immunity', 'halfPayment', 'release'].includes(card.kind);
  selectedCardTurn.value = turnIdentity.value; modal.value = 'card';
}
/** 无入参，取消本地选牌并关闭其窗口，不消费手牌或主动次数。 */
function clearCard(): void { submittingCard.value = false; selectedCard.value = null; selectedCardActive.value = false; selectedTarget.value = ''; if (modal.value === 'card') modal.value = null; }
/** 输入成功使用的牌型，免费升级接续服务端合法目标选择，其他道具结束选牌。 */
function cardUsed(kind: Card['kind']): void { clearCard(); if (kind === 'freeUpgrade' && choice.value?.type === 'free_upgrade') modal.value = 'choice'; }
/** 输入窗口内目标标识，按原规则更新选择；确认按钮才发送命令。 */
function selectDialogTarget(id: string): void { if (targetKind.value === 'tile') { const tile = game.room?.board[Number(id)]; if (tile) selectTile(tile); } else selectPlayer(id); }
/** 输入地图地块，选择合法目标或打开普通地块详情，不提交游戏命令。 */
function selectTile(tile: Tile): void {
  if (!targeting.value) { selectedTile.value = tile; return; }
  if (targetKind.value === 'player') { game.notify('正在使用玩家道具，请点击排行榜的玩家姓名或头像。'); return; }
  const reason = tileReason(tile);
  if (reason) { game.notify(reason); return; }
  if (game.canAct) selectedTarget.value = String(tile.id);
}
/** 输入排行榜玩家身份，选择合法玩家；不点击即出牌。 */
function selectPlayer(id: string): void {
  if (targetKind.value !== 'player') return;
  if (!eligiblePlayers.value.includes(id)) { game.notify(selectedCard.value?.kind === 'swap' ? '请选择未破产、未服刑的其他玩家。' : '请选择未破产的其他玩家。'); return; }
  if (game.canAct) selectedTarget.value = id;
}
/** 输入地图规模，通过原命令通道保存房主设置，选中状态由服务端快照更新。 */
async function changeMapSize(mapSize: MapSize): Promise<void> { await game.act({ type: 'setMapSize', mapSize }); }
/** 用户明确重新加入时删除失效会话，返回重新认证流程。 */
async function resetSession(): Promise<void> {
  forgetSession(code.value, props.spectator);
  if (!props.spectator) forgetSession(code.value, true);
  await enter();
}
</script>
<template>
  <div class="room-page modern-room" :class="{ 'spectator-page': spectator, 'game-session': isGameSession, 'single-screen': isSingleScreen, 'lobby-room': isLobby }" :style="{ '--town-background': `url(${baseUrl}images/board/town-background.png)` }">
    <img v-if="isLobby" class="lobby-background" :src="`${baseUrl}images/lobby-cottage.png`" alt="" fetchpriority="high" />
    <header class="site-header room-header immersive-header">
      <RouterLink to="/" aria-label="返回首页"><span v-if="isLobby" class="lobby-back">← 返回小镇</span><Logo v-else /></RouterLink>
      <button class="room-tag" aria-label="复制房间号" @click="game.copy(code)">房间 <strong>{{ code }}</strong></button>
      <div v-if="isGameSession" class="room-utilities game-utilities" role="group" aria-label="游戏工具栏">
        <span class="connection-label" :class="game.connection"><i></i>{{ game.connection === 'online' ? '实时同步' : game.connection === 'connecting' ? '连接中' : '已断开' }}</span>
        <AudioSettings class="room-audio-control" />
        <button class="button game-tools-trigger" aria-label="打开游戏工具" aria-haspopup="dialog" @click="modal = 'tools'">
          <svg viewBox="0 0 32 32" aria-hidden="true"><path d="m13 4 1-2h4l1 2 3 2 3-.2 2 3-1 3v4l1 3-2 3-3-.2-3 2-1 3h-4l-1-3-3-2-3 .2-2-3 1-3v-4l-1-3 2-3 3 .2Z" /><circle cx="16" cy="14.5" r="5" /></svg>
          工具
        </button>
      </div>
    </header>
    <main v-if="!game.room" class="loading-page"><div class="entry-icon">🎲</div><h2>{{ game.fatalError ? '暂时无法进入房间' : '正在打开你的城市…' }}</h2><p class="muted" role="status">{{ game.fatalError || '连接房间，恢复最新的棋盘和玩家信息。' }}</p><div v-if="game.fatalError" class="button-row"><RouterLink class="button secondary" to="/">返回首页</RouterLink><RouterLink v-if="!spectator" class="button primary" :to="`/watch/${code}`">进入观战</RouterLink><button class="button ghost" @click="resetSession">重新加入</button></div></main>
    <main v-else>
      <div v-if="game.connection !== 'online'" class="notice warning connection-notice">{{ game.fatalError || '网络连接已断开，正在自动重连。当前显示最后同步的局面。' }}<button v-if="game.connection === 'replaced'" class="button small secondary" @click="enter">在此页重新连接</button></div>
      <div v-if="game.room.pausedReason" class="notice warning connection-notice">Ⅱ <PlayerText :text="game.room.pausedReason" />。对局已保存，恢复后继续当前步骤。</div>
      <RecoverySeats v-if="!spectator" />
      <section v-if="isLobby" class="lobby-layout cottage-lobby" aria-label="房间大厅">
        <div class="lobby-panel cottage-panel">
          <div class="cottage-heading"><h1>好友房间 <small>{{ game.room.players.length }} / 6</small></h1><button class="button secondary small" @click="game.copy(`${origin}/room/${code}`)">邀请朋友</button></div>
          <div class="lobby-seats">
            <div v-for="player in game.room.players" :key="player.id" class="lobby-seat" :class="{ 'is-me': player.id === game.me?.id, offline: !player.online }">
              <CharacterPawn :avatar-id="player.avatarId" :color="player.color" />
              <div class="seat-identity"><strong :style="{ color: player.color }" :title="player.name">{{ player.name }}</strong><small>{{ player.id === game.room.hostId ? '房主' : player.id === game.me?.id ? '我' : '玩家' }}{{ player.id === game.room.hostId && player.id === game.me?.id ? ' · 我' : '' }}</small></div>
              <ReadyIcon class="seat-ready" :ready="player.ready" :offline="!player.online" />
              <button v-if="game.isHost && !player.online && player.id !== game.me?.id" class="seat-remove" :disabled="game.busy" :aria-label="`移出 ${player.name}`" @click="game.act({ type: 'removePlayer', playerId: player.id })">移出</button>
            </div>
            <div v-for="seat in 6 - game.room.players.length" :key="`empty-${seat}`" class="lobby-seat empty"><span class="empty-seat-mark" aria-hidden="true">＋</span><span>待加入</span></div>
          </div>
          <div class="cottage-map"><span>地图</span><div class="map-size-options" role="group" aria-label="地图大小"><button v-for="size in MAP_SIZES" :key="size.value" type="button" :class="{ selected: (game.room.mapSize ?? 'large') === size.value }" :aria-pressed="(game.room.mapSize ?? 'large') === size.value" :disabled="spectator || !game.isHost || !game.canAct" @click="changeMapSize(size.value)">{{ size.name }}<small>{{ mapLength(previewPlayers, size.value) }} 格</small></button></div></div>
          <section v-if="game.me && !spectator" class="lobby-avatar-picker"><h2>选择头像</h2><AvatarPicker /></section>
          <div v-if="!spectator" class="lobby-controls"><div class="cottage-buttons"><button class="button" :class="game.me?.ready ? 'secondary' : 'primary'" :disabled="!game.canAct" @click="game.act({ type: 'ready', value: !game.me?.ready })">{{ game.me?.ready ? '取消准备' : '准备好了' }}</button><button v-if="game.isHost" class="button primary" :disabled="!game.canAct || !allReady" @click="game.act({ type: 'start' })">开始游戏</button></div><p class="lobby-hint" role="status">{{ game.room.players.length < 2 ? '等待朋友加入' : allReady ? game.isHost ? '大家都准备好了' : '等待房主开局' : '等待所有玩家准备' }}</p></div>
          <p v-else class="lobby-hint">观战中 · 等待开局</p>
        </div>
      </section>
      <template v-else>
        <div class="game-layout immersive-layout">
          <section class="map-panel"><div ref="mapView" class="board-view">
            <Board :live="game.connection === 'online' && !game.room.recoveryPending" :room="game.room" :spectator="spectator" compact :bounded="false" :stretch="isSingleScreen" :me-id="game.me?.id" :decision="hasCentralDecision || drafting" :targeting="targetKind === 'tile'" :target-hint="targetHint" :eligible-tiles="eligibleTiles" :selected-target="targetTileId" @select="selectTile" @animating="boardAnimating = $event" @settled="game.finishMovement">
              <template #summary>
                <div v-if="showMyTurn" :key="turnIdentity" class="hub-turn-callout" role="status"><ChibiLettering kind="turn" /></div>
                <div class="hub-summary"><dl v-if="game.me" class="hub-wallet"><div><dt class="sr-only">现金</dt><dd><AssetStatIcon kind="cash" /><span>¥{{ game.me.cash.toLocaleString() }}</span></dd></div><div class="hub-net-worth"><dt class="sr-only">净资产</dt><dd><AssetStatIcon kind="worth" /><span>{{ game.me.netWorth === undefined ? '—' : `¥${game.me.netWorth.toLocaleString()}` }}</span></dd></div></dl><span v-if="game.me?.assetRank" class="hub-asset-rank"><ChibiLettering kind="rank" :rank="game.me.assetRank" /></span></div>
              </template>
              <template #controls>
                <div class="dashboard-scroll">
                  <div v-if="game.room.status === 'finished'" class="hub-finish"><p>淘汰顺序：<PlayerText :text="elimination.map(player => player.name).join(' → ') || '—'" /></p><button v-if="game.isHost && !spectator" class="button primary small" :disabled="!game.canAct" @click="game.act({ type: 'newGame' })">再来一局</button></div>
                  <div class="dashboard-grid">
                    <section class="dashboard-records workspace-panel">
                      <div class="city-feed-panel">
                      <div class="workspace-heading"><h2><span class="news-indicator" aria-hidden="true"></span>城市新鲜事</h2><div class="record-switch" role="group" aria-label="记录范围"><button :aria-pressed="recordMode === 'live'" :class="{ active: recordMode === 'live' }" @click="recordMode = 'live'">实时</button><button :aria-pressed="recordMode === 'history'" :class="{ active: recordMode === 'history' }" @click="recordMode = 'history'">历史</button></div></div>
                      <div class="records-scroll"><LiveRecords compact journal :active="recordMode === 'live'" v-show="recordMode === 'live'" :key="`${game.room.roomId ?? code}-${game.room.gameId}`" :room="game.room" :token="game.session?.token" /><template v-if="recordMode === 'history'"><RoomHistory v-if="archiveCode && !spectator" compact :archive-code="archiveCode" :game-id="game.room.gameId ?? null" /><p v-else class="muted">历史回顾需要玩家的固定存档码；本局完整公开记录可在“实时”中查阅。</p></template></div>
                      <CardReveal :paused="boardAnimating || game.settling && !game.settlementRevealing" />
                      </div>
                      <form class="room-chat" @submit.prevent="sendChat"><div class="chat-heading"><h3>聊天与快捷消息</h3><div class="dashboard-action-bar"><div class="action-bar-status"><span class="status-dot"></span><strong>当前行动：{{ actionStatus }}</strong><button type="button" v-if="drafting" class="button primary small" @click="modal = 'card'">继续使用 {{ selectedCard?.name ?? '道具' }}</button><button type="button" v-else-if="hasCentralDecision" :disabled="!!game.cardReveals.length" class="button primary small" @click="openDecision">处理当前事项</button></div><button v-if="tradeReminder" type="button" class="chat-todo" @click="modal = 'trade'"><PlayerText :text="tradeReminder" /><span>查看报价 →</span></button></div></div><div class="chat-composer">
                        <label class="sr-only" for="room-chat-message">房间聊天</label><textarea id="room-chat-message" v-model="chatMessage" maxlength="300" rows="1" :disabled="!canChat" :placeholder="spectator || !game.me ? '观战中，可以查看房间对话' : game.connection !== 'online' ? '恢复连接后可发送消息' : '和大家聊聊…'" @keydown.enter="chatEnter" />
                        <div class="chat-composer-actions"><div v-if="!spectator && game.me && !game.me.bankrupt && game.room.status === 'playing'" class="hub-actions"><button type="button" class="button secondary small game-action-button" :disabled="!canTrade && !game.privateData?.trade" @click="modal = 'trade'" aria-label="交易" title="交易"><ActionIcon action="trade" /><small>交易</small></button><button type="button" class="button primary small game-action-button" :disabled="!canRoll" @click="game.act({ type: 'roll' })" aria-label="掷骰" title="掷骰"><ActionIcon action="roll" /><small>掷骰</small></button><button type="button" class="button primary small game-action-button" :disabled="!canEndTurn" @click="game.act({ type: 'endTurn' })" aria-label="结束行动" title="结束行动"><ActionIcon action="endTurn" /><small>结束</small></button></div><button type="submit" class="button primary chat-send game-action-button" :disabled="!canChat || !chatMessage.trim()" aria-label="发送消息" title="发送消息"><ActionIcon action="send" /><small>发送</small></button></div>
                      </div></form>
                    </section>
                    <aside class="dashboard-sidebar">
                      <aside class="dashboard-ranking workspace-panel"><div class="workspace-heading"><div><h2>资产排行</h2></div><span class="workspace-count">{{ game.room.players.filter(p => !p.bankrupt).length }} 人</span></div><PublicLeaderboard compact :room="game.room" :me-id="game.me?.id" :can-remove="game.isHost && !spectator" :busy="game.busy" @select="selectTile" @remove="game.act({ type: 'removePlayer', playerId: $event })" /></aside>
                      <section v-if="game.me && !spectator" class="dashboard-assets workspace-panel"><div class="workspace-heading"><h2>我的资产</h2><span class="workspace-count">{{ owned.length }} 处地产</span></div><div class="asset-overview"><PropertyTile v-for="tile in owned" :key="tile.id" :tile="tile" show-price @select="selectTile" /><p v-if="!owned.length" class="muted">暂无地产</p></div><button class="asset-all" @click="modal = 'assets'">查看全部资产 <span aria-hidden="true">→</span></button></section>

                    </aside>
                  </div>
                  <section class="dashboard-cards workspace-panel"><div class="workspace-heading"><h2>我的手牌<span v-if="game.me && !spectator">{{ handCount }} / 6</span></h2><span v-if="game.privateData?.activeCardUsed" class="workspace-count">本回合已使用</span></div><template v-if="game.me && !spectator"><div class="dashboard-card-rail"><button v-for="card in game.privateData?.hand" :key="card.id" class="dashboard-hand-card" :aria-label="`使用卡牌 ${card.name ?? ITEMS[card.kind].name}`" @click="chooseCard(card)"><ContentCard :content="card" compact thumbnail /></button><span v-for="slot in Math.max(0, 6 - handCount)" :key="`empty-${slot}`" class="empty-hand-slot" aria-hidden="true"><span>＋</span>空位</span></div></template>
                      <p v-else class="workspace-caption">手牌仅本人可见</p></section>

                </div>
              </template>
            </Board>
          </div></section>
        </div>
      </template>
    </main>
    <Modal panel-class="modern-modal decision-modal" v-if="modal === 'decision' && game.room && hasCentralDecision && !game.cardReveals.length" :title="decisionTitle"  @close="modal = null"><div class="action-details">
                        <template v-if="game.canDecide">
                          <template v-if="game.room.pending?.type === 'choice'">
                            <template v-if="choice"><p class="muted">{{ choice.content?.name ?? '当前选择' }} · {{ choice.type === 'hand_limit' ? '整理手牌后继续行动' : '请打开窗口处理本次选择' }}</p><button class="button primary small" :disabled="!game.canAct" @click="openWorkbench('choice')">处理当前选择</button></template>
                            <p v-else class="muted">正在同步当前选择…</p>
                          </template>
                          <template v-else-if="game.room.phase === 'buy' || game.room.phase === 'upgrade'"><div class="landing-info"><span class="decision-property-art" :class="`level-${pendingTile?.level ?? 0}`" :style="{ backgroundImage: `url(${baseUrl}images/board/property-levels.png)` }" aria-hidden="true"></span><div><strong>{{ pendingTile?.name }}</strong><small>{{ game.room.phase === 'buy' ? '这块地还没有主人' : `你的地产 · 当前 ${pendingTile?.level} 级` }}</small></div></div><p class="decision-price">¥{{ game.room.pending?.amount.toLocaleString() }} <small>{{ game.room.phase === 'buy' ? '购入价格' : '升级费用' }}</small></p><dl v-if="pendingTile && game.me" class="decision-outcome"><div><dt>{{ game.room.phase === 'buy' ? '购买后剩余现金' : '升级后剩余现金' }}</dt><dd>{{ game.me.cash >= (game.room.pending?.amount ?? 0) ? `¥${(game.me.cash - (game.room.pending?.amount ?? 0)).toLocaleString()}` : '现金不足' }}</dd></div><div><dt>{{ game.room.phase === 'buy' ? '当前租金' : '升级后租金' }}</dt><dd>¥{{ rentFor(game.room.phase === 'buy' ? pendingTile : { ...pendingTile, level: pendingTile.level + 1 }, game.room.economy).toLocaleString() }}</dd></div></dl><div class="button-row"><button class="button secondary" :disabled="!canPlay" @click="game.act({ type: 'pass' })">暂时放弃</button><button class="button primary" :disabled="!canPlay || (game.me?.cash ?? 0) < (game.room.pending?.amount ?? 0)" @click="game.act({ type: game.room.phase === 'buy' ? 'buy' : 'upgrade' })">{{ game.room.phase === 'buy' ? '买下这块地' : '升级房子' }}</button></div></template>
                          <template v-else-if="payment">
                            <p class="muted">{{ payment.type === 'rent' ? `${pendingTile?.name} · 路费` : '本次强制费用' }} → <PlayerText :text="creditorName" />。</p>
                            <p class="decision-price">¥{{ payment.amount.toLocaleString() }} <small>剩余待支付</small></p>
                            <p class="muted">现金 ¥{{ game.me?.cash.toLocaleString() }} · 确认后才扣款。</p>
                            <template v-if="!payment.defenseResolved"><button class="button primary small" :disabled="!canPlay" @click="modal = 'defense'">选择防御与付款方式</button></template>
                            <div v-else class="button-row"><button class="button primary" :disabled="!canPlay || (game.me?.cash ?? 0) < payment.amount" @click="game.act({ type: 'payDebt' })">确认现金支付</button></div>
                            <div v-if="payment.defenseResolved" class="button-row"><button class="button secondary" :disabled="!canPlay" @click="openWorkbench('assets')">抵押／变现</button><button v-if="payment.creditorId" class="button secondary" :disabled="!canTrade" @click="openWorkbench('trade')">协商抵债／出售</button></div>
                            <p v-if="!payment.creditorId && (game.me?.cash ?? 0) < payment.amount" class="notice warning">系统费用需自行选择地产抵押筹款。</p>
                            <template v-if="payment.defenseResolved && liquidationValue < payment.amount"><p class="muted">银行变现仍不足。{{ payment.creditorId ? '可继续与玩家协商成交，或主动结束协商并清算。' : '抵押全部可用地产后仍不足，可确认清算。' }}</p><button class="button ghost small danger" :disabled="!canLiquidate" @click="modal = 'bankruptcy'">结束协商并申请清算</button></template>
                          </template>
                        </template>
                        </div></Modal>
    <Modal panel-class="modern-modal assets-modal" v-if="modal === 'assets' && game.me && !spectator" title="我的全部资产" wide @close="modal = null"><Assets /></Modal>
    <Modal :panel-class="['modern-modal', 'card-use-modal']" v-if="selectedCard" :open="modal === 'card'" :title="selectedCard.name ?? ITEMS[selectedCard.kind].name" wide @close="modal = null"><div class="card-use-layout"><div class="card-use-preview"><ContentCard :content="selectedCard" /></div><div class="card-use-controls"><p v-if="!drafting" class="notice">{{ ['immunity', 'halfPayment', 'release'].includes(selectedCard.kind) ? '这张卡在付款防御或解除限制时使用。' : '当前不能主动使用，可以查看卡牌说明。' }}</p><template v-if="drafting && targetKind"><h3>选择{{ targetKind === 'player' ? '玩家' : '地块' }}</h3><p class="muted">{{ targetHint }}</p><CardTargetPicker :options="dialogTargets" :selected="selectedTarget" :player-target="targetKind === 'player'" :disabled="!game.canAct" @select="selectDialogTarget" /></template><p v-if="game.connection !== 'online'" class="notice warning">连接中断，恢复连接后才能确认。</p><CardAction :key="selectedCard.id" :card="selectedCard" :target="selectedTarget" :target-label="targetLabel" :has-candidates="!!(eligibleTiles.length || eligiblePlayers.length)" :readonly="!drafting" compact dialog @submitting="submittingCard = $event" @done="cardUsed" @cancel="clearCard" /></div></div></Modal>
    <Modal :panel-class="['modern-modal', 'choice-use-modal']" v-if="choice" :open="modal === 'choice' && !game.settling && !game.cardReveals.length" :title="choice.content?.name ?? '处理当前选择'" wide @close="modal = null"><p v-if="mapChoice" class="muted">{{ targetHint }}</p><CardTargetPicker v-if="mapChoice" :options="dialogTargets" :selected="selectedTarget" :disabled="!game.canAct" @select="selectDialogTarget" /><p v-if="game.connection !== 'online'" class="notice warning">连接中断，恢复后继续本次选择。</p><ContentChoice :key="choice.id" :choice="choice" :tile-id="targetTileId" :compact="!!mapChoice" dialog @done="modal = null" /></Modal>
    <Modal :panel-class="['modern-modal', 'defense-use-modal']" v-if="payment" :open="modal === 'defense' && !game.settling && !game.cardReveals.length" title="本次付款防御" @close="modal = null"><p class="defense-amount">待支付 ¥{{ payment.amount.toLocaleString() }} → <PlayerText :text="creditorName" /></p><label class="field">选择防御卡<select v-model="selectedDefense"><option value="">不使用道具</option><option v-for="card in defenseCards" :key="card.id" :value="card.id">{{ card.name ?? ITEMS[card.kind].name }}</option></select></label><ContentCard v-if="defenseCards.find(card => card.id === selectedDefense)" :content="defenseCards.find(card => card.id === selectedDefense)!" /><p class="muted">本笔至多使用一张防御卡，不占主动次数。</p><button class="button primary full" :disabled="!canPlay" @click="game.act({ type: 'paymentDefense', ...(payment.id ? { decisionId: payment.id } : {}), ...(selectedDefense ? { cardId: selectedDefense } : {}) })">{{ selectedDefense ? '确认使用防御卡' : '不使用道具，继续付款' }}</button></Modal>
    <Modal panel-class="modern-modal game-tools-modal" v-if="isGameSession && modal === 'tools'" title="小镇工具箱" @close="modal = null">
      <div class="game-tools-grid">
        <button v-if="game.me && !spectator" class="game-tool peach" @click="modal = 'avatar'"><span class="game-tool-icon" aria-hidden="true"><CharacterPawn :avatar-id="game.me.avatarId" :color="game.me.color" /></span><span>选择角色</span></button>
        <button class="game-tool mint" @click="modal = 'rules'"><span class="game-tool-icon" aria-hidden="true">📖</span><span>玩法说明</span></button>
        <button class="game-tool sky" @click="modal = null; fullscreen()"><span class="game-tool-icon" aria-hidden="true">⛶</span><span>全屏模式</span></button>
        <button v-if="game.me && !spectator" class="game-tool lavender" @click="modal = 'save'"><span class="game-tool-icon" aria-hidden="true">💾</span><span>游戏存档</span></button>
        <button class="game-tool peach" @click="modal = null; game.copy(`${origin}/room/${code}`)"><span class="game-tool-icon" aria-hidden="true">💌</span><span>邀请朋友</span></button>
        <button class="game-tool mint" @click="modal = null; game.copy(`${origin}/watch/${code}`)"><span class="game-tool-icon" aria-hidden="true">🔗</span><span>复制观战链接</span></button>
      </div>
    </Modal>
    <Modal panel-class="modern-modal" v-if="modal === 'rules'" title="大富翁玩法说明" wide @close="modal = null"><Rules :economy="game.room?.economy" :spectator="spectator" :chance-events="game.room?.chanceEventDescriptions" /></Modal>
    <Modal panel-class="modern-modal" v-if="modal === 'save' && game.me && !spectator" title="标记当前存档" @close="modal = null"><SaveArchive /></Modal>




    <Modal panel-class="modern-modal" v-if="game.me && !spectator" :open="modal === 'trade'" title="交易报价与成交后结果" wide @close="modal = null"><TradeEditor :key="`${game.room?.roomId}:${game.room?.gameId}:${game.me.id}:${tradeEditorVersion}`" :offer-disabled="drafting" @done="modal = null; tradeEditorVersion++" /></Modal>
    <Modal panel-class="modern-modal" v-if="modal === 'avatar' && game.me && !spectator" title="选择角色形象" @close="modal = null"><AvatarPicker /></Modal>
    <Modal panel-class="modern-modal" v-if="modal === 'bankruptcy' && payment && !spectator" title="结束偿债并清算" @close="modal = null"><p>待支付 ¥{{ payment.amount.toLocaleString() }}，现金加银行可变现资产 ¥{{ liquidationValue.toLocaleString() }}。</p><p class="notice warning">确认后全部地产归还银行，你退出本局竞争。<PlayerText :text="payment.creditorId ? `银行变现所得用于向 ${creditorName} 偿债。` : '剩余现金用于向银行偿债。'" /></p><div class="button-row"><button class="button secondary" @click="modal = null">继续筹款协商</button><button class="button danger" :disabled="!canLiquidate" @click="game.act({ type: 'declareBankruptcy' }).then(ok => { if (ok) modal = null; })">确认清算并退出竞争</button></div></Modal>
    <Modal panel-class="modern-modal tile-detail-modal" v-if="liveSelectedTile" :title="liveSelectedTile.name" @close="selectedTile = null">
      <div class="tile-detail tile-detail-card">
        <div class="tile-detail-hero" :class="`district-${liveSelectedTile.district}`">
          <div class="tile-detail-art" :class="[liveSelectedTile.type, `level-${liveSelectedTile.level}`, { mortgaged: liveSelectedTile.mortgaged }]" :style="{ '--detail-property-art': `url(${baseUrl}images/board/property-levels.png)` }" aria-hidden="true">
            <img v-if="liveSelectedTile.type !== 'property'" :src="`${baseUrl}images/tiles/${liveSelectedTile.type}.png`" alt="" />
          </div>
          <div class="tile-detail-intro">
            <p>{{ districtName(liveSelectedTile) }}</p>
            <span class="tile-detail-location">{{ liveSelectedTile.id }} 号地块</span>
            <span v-if="liveSelectedTile.type === 'property'" class="tile-detail-status" :class="{ mortgaged: liveSelectedTile.mortgaged }">{{ liveSelectedTile.mortgaged ? '已抵押' : liveSelectedTile.ownerId ? '已购入' : '待购入' }}</span>
          </div>
        </div>
        <template v-if="liveSelectedTile.type === 'property'">
          <div class="tile-detail-title-deed" :style="{ '--deed-color': selectedTileOwner?.color ?? '#8490a4' }">
            <CharacterPawn v-if="selectedTileOwner" :avatar-id="selectedTileOwner.avatarId" :color="selectedTileOwner.color" />
            <div class="tile-detail-owner-info"><small>地产归属</small><strong v-if="selectedTileOwner">{{ !spectator && selectedTileOwner.id === game.me?.id ? '属于你' : '属于' }}<span v-if="spectator || selectedTileOwner.id !== game.me?.id"> {{ selectedTileOwner.name }}</span></strong><strong v-else>待购入</strong><span v-if="selectedTileOwner && !spectator && selectedTileOwner.id === game.me?.id">{{ selectedTileOwner.name }}</span></div>
            <PropertyGrade :level="liveSelectedTile.level" />
          </div>
          <dl class="tile-detail-metrics">
            <div><dt>地产价格</dt><dd>¥{{ liveSelectedTile.price.toLocaleString() }}</dd></div>
            <div><dt>当前路费</dt><dd>¥{{ rentFor(liveSelectedTile, game.room?.economy).toLocaleString() }}</dd></div>
          </dl>
          <dl class="tile-detail-facts">
            <div><dt>抵押状态</dt><dd :class="{ 'tile-detail-mortgaged': liveSelectedTile.mortgaged }">{{ liveSelectedTile.mortgaged ? `已抵押 · 赎回 ¥${redeemCost(liveSelectedTile).toLocaleString()}` : '未抵押' }}</dd></div>
          </dl>
        </template>
        <p v-else-if="liveSelectedTile.type === 'start' || liveSelectedTile.type === 'jail'" class="tile-detail-description">{{ liveSelectedTile.type === 'start' ? `正向经过或到达，获得 ¥${ruleMoney(game.room?.economy, 'startIncome', 1000).toLocaleString()}，并获得一张道具补给。` : '停在此处，跳过下一次行动。' }}</p>
        <p class="tile-detail-barrier" :class="{ active: liveSelectedTile.barrier }"><span aria-hidden="true">{{ liveSelectedTile.barrier ? '🚧' : '↝' }}</span><span><strong>{{ liveSelectedTile.barrier ? '设有路障' : '无路障' }}</strong><small v-if="liveSelectedTile.barrier">首位受影响的经过者将停下，随后正常结算地块</small></span></p>
      </div>
    </Modal>
    <footer v-if="!isLobby" class="room-footer">小小棋盘，大大野心。<span>好运和朋友，都在这里。</span></footer>
  </div>
</template>
