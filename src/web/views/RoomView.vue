<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { DISTRICTS, ITEMS, rentFor } from '../../shared/rules';
import type { Card, Tile } from '../../shared/types';
import { readSession, useGameStore } from '../stores/game';
import Logo from '../components/Logo.vue';
import City from '../components/City.vue';
import Board from '../components/Board.vue';
import Modal from '../components/Modal.vue';
import Assets from '../components/Assets.vue';
import CardAction from '../components/CardAction.vue';
import TradeEditor from '../components/TradeEditor.vue';
import Rules from '../components/Rules.vue';

const props = withDefaults(defineProps<{ spectator?: boolean }>(), { spectator: false });
const route = useRoute();
const origin = window.location.origin;
const game = useGameStore();
const modal = ref<'rules' | 'assets' | 'card' | 'trade' | null>(null);
const selectedCard = ref<Card | null>(null);
const selectedTile = ref<Tile | null>(null);
let entryGeneration = 0;
const code = computed(() => String(route.params.code));
const owned = computed(() => game.room?.board.filter(tile => tile.ownerId === game.me?.id) ?? []);
const allReady = computed(() => !!game.room && game.room.players.length >= 2 && game.room.players.every(player => player.ready && player.online));
const canPlay = computed(() => !props.spectator && game.canAct && game.canDecide && !game.room?.tradePending);
const activeCardAllowed = computed(() => canPlay.value && game.room?.phase === 'pre_roll' && !game.privateData?.activeCardUsed);
const pendingTile = computed(() => game.room?.board[game.room.pending?.tileId ?? -1]);
const elimination = computed(() => game.room?.players.filter(player => player.bankrupt).sort((a, b) => (a.eliminationOrder ?? 0) - (b.eliminationOrder ?? 0)) ?? []);
const liveSelectedTile = computed(() => selectedTile.value ? game.room?.board[selectedTile.value.id] : null);
const publicProgress = computed(() => {
  if (game.room?.status === 'finished') return '本局已结束，等待房主再开一局。';
  if (game.room?.pausedReason) return game.room.pausedReason;
  if (game.room?.tradePending) return '双方正在商量交易。';
  const descriptions = { pre_roll: '准备移动', buy: '选择是否购买地产', upgrade: '选择是否升级建筑', rent: '处理本次路费', debt: '筹集现金支付费用', manage: '管理资产或准备结束回合' };
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
watch(() => game.privateData?.trade?.id, id => { if (id) modal.value = 'trade'; else if (modal.value === 'trade') modal.value = null; });
onBeforeUnmount(() => { entryGeneration++; game.disconnect(); });

/** 请求浏览器进入或退出全屏，失败时提示用户。 */
async function fullscreen(): Promise<void> {
  try { if (document.fullscreenElement) await document.exitFullscreen(); else await document.documentElement.requestFullscreen(); }
  catch { game.notify('当前浏览器不支持全屏，请使用浏览器全屏或投屏功能。'); }
}
/** 输入私有手牌，打开参数选择弹窗。 */
function chooseCard(card: Card): void { selectedCard.value = card; modal.value = 'card'; }
/** 用户明确重新加入时删除失效会话，返回重新认证流程。 */
async function resetSession(): Promise<void> {
  localStorage.removeItem(`dfw:${props.spectator ? 'viewer' : 'player'}:${code.value}`);
  await enter();
}
</script>
<template>
  <div class="room-page" :class="{ 'spectator-page': spectator }">
    <header class="site-header room-header">
      <RouterLink to="/" aria-label="返回首页"><Logo /></RouterLink>
      <div class="room-heading"><span class="room-tag" @click="game.copy(code)">房间 <strong>{{ code }}</strong><span>⧉</span></span><span v-if="spectator" class="viewer-tag">▣ 公共观战</span></div>
      <div class="header-actions"><span class="connection-label" :class="game.connection"><i></i>{{ game.connection === 'online' ? '实时同步' : game.connection === 'connecting' ? '连接中' : '连接已断开' }}</span><button class="button ghost small" @click="modal = 'rules'">玩法说明</button><button v-if="spectator" class="button secondary small" @click="fullscreen">⛶ 全屏</button><button v-else class="button secondary small" @click="game.copy(`${origin}/room/${code}`)">邀请好友 ↗</button></div>
    </header>
    <main v-if="!game.room" class="loading-page"><div class="entry-icon">🎲</div><h2>{{ game.fatalError ? '暂时无法进入房间' : '正在打开你的城市…' }}</h2><p class="muted" role="status">{{ game.fatalError || '连接房间，恢复最新的棋盘和玩家信息。' }}</p><div v-if="game.fatalError" class="button-row"><RouterLink class="button secondary" to="/">返回首页</RouterLink><RouterLink v-if="!spectator" class="button primary" :to="`/watch/${code}`">进入观战</RouterLink><button class="button ghost" @click="resetSession">重新加入</button></div></main>
    <main v-else>
      <div v-if="game.connection !== 'online'" class="notice warning connection-notice">{{ game.fatalError || '网络连接已断开，正在自动重连。当前显示最后同步的局面。' }}<button v-if="game.connection === 'replaced'" class="button small secondary" @click="enter">在此页重新连接</button></div>
      <div v-if="game.room.pausedReason" class="notice warning connection-notice">Ⅱ {{ game.room.pausedReason }}。对局已保存，恢复后继续当前步骤。</div>
      <section v-if="game.room.status === 'lobby'" class="lobby-layout">
        <div class="lobby-hero"><span class="pill">一场新旅程，即将开始</span><h1>让朋友们<br /><span>都坐下来。</span></h1><p class="muted">2～6 人准备好后，房主就能开启新的城市。</p><City /><div class="lobby-code"><small>分享房间号</small><strong>{{ code }}</strong><button class="icon-button" aria-label="复制房间号" @click="game.copy(code)">⧉</button></div><button class="button ghost" @click="game.copy(`${origin}/watch/${code}`)">复制投屏观战链接 ↗</button></div>
        <div class="panel lobby-panel"><div class="section-heading"><h2>玩家席位</h2><span>{{ game.room.players.length }} / 6</span></div><div class="lobby-seats"><div v-for="player in game.room.players" :key="player.id" class="lobby-seat"><span class="avatar" :style="{ background: player.color }">{{ player.name.slice(0, 1) }}</span><div><strong>{{ player.name }} <small v-if="player.id === game.me?.id">你</small></strong><small>{{ player.id === game.room.hostId ? '房主 · ' : '' }}{{ player.online ? '已连接' : '暂时离线' }}</small></div><span class="ready-pill" :class="{ ready: player.ready }">{{ player.ready ? '✓ 已准备' : '等待准备' }}</span><button v-if="game.isHost && !player.online && player.id !== game.me?.id" class="button ghost small danger" :disabled="game.busy" @click="game.act({ type: 'removePlayer', playerId: player.id })">移出</button></div><div v-for="seat in 6 - game.room.players.length" :key="`empty-${seat}`" class="lobby-seat empty"><span class="avatar">＋</span><span>等待朋友加入</span></div></div><div v-if="!spectator" class="lobby-controls"><button class="button full" :class="game.me?.ready ? 'secondary' : 'primary'" :disabled="!game.canAct" @click="game.act({ type: 'ready', value: !game.me?.ready })">{{ game.me?.ready ? '已准备 · 点击取消' : '我准备好了' }}</button><button v-if="game.isHost" class="button primary full" :disabled="!game.canAct || !allReady" @click="game.act({ type: 'start' })">开启财富旅程 ↗</button><p class="muted">{{ allReady ? '大家都已准备，可以开局了。' : '需要至少两人，所有玩家连接并准备后开局。' }}</p></div><p v-else class="notice">这是公共投屏页面。请让玩家在自己的手机或电脑上准备。</p></div>
      </section>
      <template v-else>
        <div class="game-title"><div><span class="eyebrow">THE CITY IS YOURS</span><h1>{{ game.room.status === 'finished' ? '这座城市，见证了赢家。' : '城市就位，好运出发。' }}</h1></div><button class="button ghost small" @click="game.copy(`${origin}/watch/${code}`)">▣ 观战链接</button></div>
        <div v-if="game.room.status === 'finished'" class="winner-banner"><span>🏆</span><div><small>本局财富赢家</small><h2>{{ game.room.players.find(player => player.id === game.room?.winnerId)?.name }}</h2><p>淘汰顺序：{{ elimination.map(player => player.name).join(' → ') }}</p></div><button v-if="game.isHost && !spectator" class="button primary" :disabled="!game.canAct" @click="game.act({ type: 'newGame' })">再来一局 ↗</button></div>
        <div class="game-layout">
          <section class="map-panel"><Board :room="game.room" :spectator="spectator" @select="selectedTile = $event" /></section>
          <aside class="game-sidebar">
            <section v-if="!spectator && !game.me?.bankrupt && game.room.status === 'playing'" class="panel action-panel"><div class="section-heading"><h2>{{ game.canDecide ? (game.room.phase === 'debt' ? '请处理偿债' : '轮到你了') : '下一步，好运' }}</h2><span class="turn-tag">{{ game.canDecide ? (game.room.phase === 'debt' ? '等待偿债' : '你的回合') : '等待行动' }}</span></div>
              <template v-if="game.room.tradePending"><p class="muted">双方正在商量交易，其他操作暂停。</p><button v-if="game.privateData?.trade" class="button secondary full" @click="modal = 'trade'">查看交易报价</button></template>
              <template v-else-if="game.canDecide">
                <template v-if="game.room.phase === 'pre_roll'"><p class="muted">可以先使用一张道具，再看看骰子把你带到哪里。</p><button class="button primary full roll-button" :disabled="!canPlay" @click="game.act({ type: 'roll' })">⚄ 掷骰子<span>向好运迈一步</span></button></template>
                <template v-else-if="game.room.phase === 'buy' || game.room.phase === 'upgrade'"><div class="landing-info"><span>⌂</span><div><strong>{{ pendingTile?.name }}</strong><small>{{ game.room.phase === 'buy' ? '这块地还没有主人' : `你的地产 · 当前 ${pendingTile?.level} 级` }}</small></div></div><p class="decision-price">¥{{ game.room.pending?.amount.toLocaleString() }} <small>{{ game.room.phase === 'buy' ? '购入价格' : '升级费用' }}</small></p><div class="button-row"><button class="button secondary" :disabled="!canPlay" @click="game.act({ type: 'pass' })">暂时放弃</button><button class="button primary" :disabled="!canPlay || (game.me?.cash ?? 0) < (game.room.pending?.amount ?? 0)" @click="game.act({ type: game.room.phase === 'buy' ? 'buy' : 'upgrade' })">{{ game.room.phase === 'buy' ? '买下这块地' : '升级房子' }}</button></div></template>
                <template v-else-if="game.room.phase === 'rent'"><p class="muted">停在 {{ pendingTile?.name }}，本次路费 ¥{{ game.room.pending?.amount }}。</p><div class="button-row"><button class="button secondary" :disabled="!canPlay" @click="game.act({ type: 'rentDecision', useCard: false })">支付路费</button><button class="button primary" :disabled="!canPlay" @click="game.act({ type: 'rentDecision', useCard: true })">🛡️ 使用免租</button></div></template>
                <template v-else-if="game.room.phase === 'debt'"><div class="notice warning">待支付 ¥{{ game.room.pending?.amount }}，当前现金 ¥{{ game.me?.cash }}。请变现资产筹集费用。</div><button class="button primary full" :disabled="!canPlay" @click="modal = 'assets'">管理资产并偿债</button></template>
                <template v-else><p class="muted">本次落点已处理，可以管理资产、交易或结束回合。</p><button class="button primary full" :disabled="!canPlay" @click="game.act({ type: 'endTurn' })">结束我的回合 →</button></template>
              </template>
              <div v-else class="waiting-turn"><span class="avatar" :style="{ background: game.decisionPlayer?.color }">{{ game.decisionPlayer?.name.slice(0, 1) }}</span><p>{{ game.decisionPlayer?.name }} 正在{{ game.room.phase === 'debt' ? '偿债' : '行动' }}<small>观察棋盘，筹划下一步。</small></p></div>
              <div class="quick-actions"><button class="button ghost small" @click="modal = 'assets'">⌂ 我的地产 {{ owned.length }}</button><button class="button ghost small" :disabled="!canPlay || !['pre_roll', 'manage'].includes(game.room.phase)" @click="modal = 'trade'">⇄ 发起交易</button></div>
            </section>
            <section class="panel players-panel"><div class="section-heading"><h2>城市里的朋友</h2><span>{{ game.room.players.filter(p => !p.bankrupt).length }} 人竞争中</span></div><div v-for="player in game.room.players" :key="player.id" class="player-row" :class="{ current: player.id === game.current?.id && game.room.status === 'playing', bankrupt: player.bankrupt }"><span class="avatar" :style="{ background: player.color }">{{ player.name.slice(0, 1) }}</span><div class="player-info"><strong>{{ player.name }}<small v-if="player.id === game.me?.id">你</small><small v-if="player.id === game.room.hostId">房主</small></strong><span>{{ player.bankrupt ? '已清算 · 观战中' : `${game.room.board.filter(tile => tile.ownerId === player.id).length} 块地产${player.skipTurns ? ' · 下次休息' : ''}` }}<small v-if="player.trapImmunityTurns"> · 陷阱免疫 {{ player.trapImmunityTurns }} 回合</small><i v-if="!player.online"> · 离线</i></span></div><strong class="player-cash">¥{{ player.cash.toLocaleString() }}</strong><button v-if="game.isHost && !spectator && !player.online && player.id !== game.me?.id && !player.bankrupt && game.room.status === 'playing'" class="remove-player" :disabled="game.busy" @click="game.act({ type: 'removePlayer', playerId: player.id })">移出并清算</button></div></section>
            <section v-if="!spectator && !game.me?.bankrupt && game.room.status === 'playing'" class="panel cards-panel"><div class="section-heading"><h2>我的道具</h2><span>仅你可见</span></div><p v-if="!game.privateData?.hand.length" class="muted">暂时没有道具，机会和财富格会带来新补给。</p><div class="hand-grid"><button v-for="card in game.privateData?.hand" :key="card.id" class="item-card" :title="ITEMS[card.kind].description" :disabled="!activeCardAllowed || card.kind === 'immunity'" @click="chooseCard(card)"><span>{{ ITEMS[card.kind].icon }}</span><strong>{{ ITEMS[card.kind].name }}</strong><small>{{ card.kind === 'immunity' ? '收费时使用' : '掷骰前使用' }}</small></button></div></section>
            <section v-if="spectator || game.me?.bankrupt" class="panel spectator-note"><span>▣</span><h3>{{ spectator ? '聚会的公共视角' : '你已退出竞争' }}</h3><p>{{ publicProgress }}</p><p>跟随棋盘实时变化，为朋友们的下一步加油。</p></section>
          </aside>
        </div>
        <section class="panel event-panel"><div class="section-heading"><h2>城市新鲜事</h2><span>实时公开记录</span></div><div class="event-list"><div v-for="entry in game.room.logs" :key="entry.id" class="event-row"><time>{{ new Date(entry.at).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) }}</time><p>{{ entry.text }}</p></div></div></section>
      </template>
    </main>
    <Modal v-if="modal === 'rules'" title="大富翁玩法说明" wide @close="modal = null"><Rules :spectator="spectator" :chance-events="game.room?.chanceEventDescriptions" /></Modal>
    <Modal v-if="modal === 'assets' && !spectator" title="我的地产与现金周转" wide @close="modal = null"><Assets /></Modal>
    <Modal v-if="modal === 'card' && selectedCard && !spectator" :title="ITEMS[selectedCard.kind].name" @close="modal = null"><CardAction :key="selectedCard.id" :card="selectedCard" @done="modal = null" /></Modal>
    <Modal v-if="modal === 'trade' && !spectator" title="朋友间的好生意" wide @close="modal = null"><TradeEditor @done="modal = null" /></Modal>
    <Modal v-if="liveSelectedTile" :title="liveSelectedTile.name" @close="selectedTile = null"><div class="tile-detail"><span :class="`district-${liveSelectedTile.district}`">{{ liveSelectedTile.type === 'property' ? '⌂' : '✦' }}</span><p>{{ DISTRICTS[liveSelectedTile.district] }} · {{ liveSelectedTile.id }} 号地块</p><template v-if="liveSelectedTile.type === 'property'"><dl><div><dt>地产价格</dt><dd>¥{{ liveSelectedTile.price.toLocaleString() }}</dd></div><div><dt>地产主人</dt><dd>{{ game.room?.players.find(player => player.id === liveSelectedTile?.ownerId)?.name || '待购入' }}</dd></div><div><dt>建筑等级</dt><dd>{{ liveSelectedTile.level }} / 3</dd></div><div><dt>当前路费</dt><dd>¥{{ rentFor(liveSelectedTile) }}</dd></div><div><dt>抵押状态</dt><dd>{{ liveSelectedTile.mortgaged ? '已抵押' : '未抵押' }}</dd></div><div><dt>路障</dt><dd>{{ liveSelectedTile.barrier ? '首位经过者将停下' : '无' }}</dd></div></dl></template><p v-else class="muted">{{ liveSelectedTile.type === 'start' ? '正向经过或到达，获得 ¥1,000。' : liveSelectedTile.type === 'jail' ? '停在此处，跳过下一次行动。' : '停在此处，服务端自动结算随机事件。' }}</p></div></Modal>
    <footer class="room-footer">小小棋盘，大大野心。<span>好运和朋友，都在这里。</span></footer>
  </div>
</template>
