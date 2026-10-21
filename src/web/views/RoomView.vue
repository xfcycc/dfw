<script setup lang="ts">
import ThemedSelect from '../components/ThemedSelect.vue';
import { imageUrl } from '../image-assets';
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { isKamui, hasPropertyEffects, baseRentFor, ITEMS, mapLength, MAP_SIZES, mortgageValue, propertyLiquidationValue, redeemCost, rentFor, ruleMoney, startCards } from '../../shared/rules';
import type { BotDifficulty, Card, ChatReference, MapSize, Tile } from '../../shared/types';
import { readSession, useGameStore } from '../stores/game';
import { useTurnSound } from '../composables/useTurnSound';
import { playSound } from '../audio';
import AudioSettings from '../components/AudioSettings.vue';
import ActionIcon from '../components/ActionIcon.vue';
import WorkbenchActionArt from '../components/WorkbenchActionArt.vue';
import MobileActionArt from '../components/MobileActionArt.vue';
import AssetStatIcon from '../components/AssetStatIcon.vue';
import CashBalance from '../components/CashBalance.vue';
import ChibiLettering from '../components/ChibiLettering.vue';
import ReadyIcon from '../components/ReadyIcon.vue';
import Logo from '../components/Logo.vue';
import Board from '../components/Board.vue';
import PanelFrame from '../components/PanelFrame.vue';
import FitPanel from '../components/FitPanel.vue';
import Modal from '../components/Modal.vue';
import MatchSettlement from '../components/MatchSettlement.vue';
import Assets from '../components/Assets.vue';
import CardAction from '../components/CardAction.vue';
import PropertyEffects from '../components/PropertyEffects.vue';
import { TILE_CARDS, PLAYER_CARDS } from '../card-targets';
import { RESPONSE_CARDS } from '../anime-cards';
import { cardEffect } from '../card-summary';
import CardTargetPicker from '../components/CardTargetPicker.vue';
import ContentCard from '../components/ContentCard.vue';
import HandSlots from '../components/HandSlots.vue';
import CardReveal from '../components/CardReveal.vue';
import ActivityDialog from '../components/activities/ActivityDialog.vue';
import { ACTIVITY_HINTS, activityArt } from '../activity-art';
import ContentChoice from '../components/ContentChoice.vue';
import TradeEditor from '../components/TradeEditor.vue';
import Rules from '../components/Rules.vue';
import SaveArchive from '../components/SaveArchive.vue';
import RecoverySeats from '../components/RecoverySeats.vue';
import PublicLeaderboard from '../components/PublicLeaderboard.vue';
import RoomHistory from '../components/RoomHistory.vue';
import ChatComposer from '../components/ChatComposer.vue';
import LiveRecords from '../components/LiveRecords.vue';
import PlayerText from '../components/PlayerText.vue';
import CharacterPawn from '../components/CharacterPawn.vue';
import PropertyGrade from '../components/PropertyGrade.vue';
import AvatarPicker from '../components/AvatarPicker.vue';
import '../game-room.css';
import '../archive.css';
import '../modern-room.css';
import '../lobby-room.css';
import '../responsive-room.css';
import '../desktop-room.css';

const props = withDefaults(defineProps<{ spectator?: boolean }>(), { spectator: false });
const route = useRoute();
const origin = window.location.origin;
const game = useGameStore();
const modal = ref<'tools' | 'rules' | 'bankruptcy' | 'save' | 'trade' | 'avatar' | 'rename' | 'card' | 'choice' | 'defense' | 'decision' | 'assets' | 'more' | 'workbench' | null>(null);
const nickname = ref('');
const botDifficulty = ref<BotDifficulty>('normal');
const botLevels = [{ value: 'easy', label: '简单' }, { value: 'normal', label: '普通' }, { value: 'hard', label: '困难' }] as const;
/** 输入机器人席位与难度，通过已有命令更新大厅难度，显示值沿用服务器状态。 */
async function changeBotDifficulty(playerId: string, level: BotDifficulty): Promise<void> {
  await game.act({ type: 'setBotDifficulty', playerId, botDifficulty: level });
}
const canRename = computed(() => !props.spectator && !!game.me && game.connection === 'online' && !game.busy && !game.settling && !game.room?.recoveryPending);
/** 无入参，使用本人当前昵称打开改名弹窗，无返回值。 */
function openRename(): void {
  if (!game.me || props.spectator) return;
  nickname.value = game.me.name; modal.value = 'rename';
}
/** 无入参，通过现有命令保存本人昵称；成功后记住下次入房昵称并关闭弹窗。 */
async function saveNickname(): Promise<void> {
  if (!canRename.value) return;
  const name = nickname.value.trim();
  if (await game.act({ type: 'setName', name })) {
    localStorage.setItem('dfw:nickname', name);
    if (modal.value === 'rename') modal.value = null;
    game.notify('昵称已更新');
  }
}
const selectedCard = ref<Card | null>(null);
/** 输入当前选牌，返回与完整卡面一致的统一效果及原卡故事说明。 */
const selectedCardCopy = computed(() => {
  const card = selectedCard.value;
  if (!card) return null;
  const fallback = ITEMS[card.kind].description;
  return { description: card.description ?? fallback, effect: cardEffect(card) };
});
const tradeEditorVersion = ref(0);
const tradeEditor = ref<InstanceType<typeof TradeEditor>>();
const submittingCard = ref(false);
const selectedTarget = ref('');
const selectedSecondTarget = ref('');
const mapPicking = ref(false);
const mapConfirmOpen = ref(false);
const selectedCardActive = ref(false);
const selectedCardTurn = ref('');
const mapView = ref<HTMLElement>();
const dashboardView = ref<HTMLElement>();
const classicLayout = ref(localStorage.getItem('dfw:room-layout') === 'classic');
/** 无入参，切换布局并记住本机偏好，不改变对局或当前行动，无返回值。 */
function toggleLayout(): void { classicLayout.value = !classicLayout.value; localStorage.setItem('dfw:room-layout', classicLayout.value ? 'classic' : 'single'); }
const classicMobileQuery = window.matchMedia('(max-width: 800px), (max-width: 1000px) and (max-height: 500px)');
const classicMobileViewport = ref(classicMobileQuery.matches);
/** 输入经典布局的屏幕匹配结果，更新其手机断点，无返回值。 */
function updateClassicViewport(event: MediaQueryListEvent): void { classicMobileViewport.value = event.matches; }
classicMobileQuery.addEventListener('change', updateClassicViewport);
onBeforeUnmount(() => classicMobileQuery.removeEventListener('change', updateClassicViewport));
const mobileQuery = window.matchMedia('(max-width: 1100px), (max-height: 579px)');
const singleMobileViewport = ref(mobileQuery.matches);
const mobileViewport = computed(() => classicLayout.value ? classicMobileViewport.value : singleMobileViewport.value);
const spectatorMode = ref<'follow' | 'landscape'>(mobileViewport.value ? 'follow' : 'landscape');
const spectatorFollowing = computed(() => props.spectator && spectatorMode.value === 'follow');
const detachedDashboard = computed(() => spectatorFollowing.value || !props.spectator && mobileViewport.value);
const workbenchTab = ref<'cards' | 'ranking'>('cards');
/** 无入参，打开本人本回合的七五折远程施工选择，失败保留资产窗口。 */
async function openRemoteConstruction(): Promise<void> { if (await game.act({ type: 'constructionBuild' })) modal.value = 'choice'; }
/** 输入手牌或排行区域，打开手机更多菜单对应的详情，无返回值。 */
function openMobileWorkbench(section: 'cards' | 'ranking'): void { workbenchTab.value = section; modal.value = 'workbench'; }
watch(detachedDashboard, mobile => { if (!mobile && (modal.value === 'workbench' || modal.value === 'more')) modal.value = null; });
/** 输入可用视口匹配结果，在窄屏或低高度时切换紧凑布局；浏览器缩放与横竖屏切换沿用同一份游戏面板，无返回值。 */
function updateMobileViewport(event: MediaQueryListEvent): void { singleMobileViewport.value = event.matches; }
mobileQuery.addEventListener('change', updateMobileViewport);
onBeforeUnmount(() => mobileQuery.removeEventListener('change', updateMobileViewport));
const selectedDefense = ref('');
const selectedTile = ref<Tile | null>(null);
const recordMode = ref<'live' | 'history'>('live');
const board = ref<InstanceType<typeof Board>>();
const canChat = computed(() => !props.spectator && !!game.me && game.connection === 'online' && !game.busy && !game.room?.recoveryPending);
/** 输入公开聊天正文与已选引用，成功切换实时记录，返回是否发送成功。 */
async function sendChat(message: string, references: ChatReference[]): Promise<boolean> {
  if (!canChat.value || !message.trim()) return false;
  const sent = await game.act({ type: 'chat', message, references });
  if (sent) recordMode.value = 'live';
  return sent;
}
/** 输入被引用的地块编号，电脑强调地块，手机将地图视野定位到该处，无返回值。 */
function locateChatTile(id: number): void { board.value?.referenceTile(id, mobileViewport.value); }
const boardAnimating = ref(false);
const awaitingJoin = ref(false);
const joining = ref(false);
const entryName = ref(localStorage.getItem('dfw:nickname') || '');
let entryGeneration = 0;
const code = computed(() => String(route.params.code));
/** 无入参，复制包含文案、房间号及入房链接的邀请内容，完成后无返回值。 */
async function inviteFriends(): Promise<void> {
  modal.value = null;
  await game.copy(`🎲 来和我一起玩《大富翁-奇迹大陆》！\n掷骰、买地、建房、收租，看看谁能成为大富翁！\n房间号：${code.value}\n点击链接加入：${origin}/room/${code.value}`);
}
const owned = computed(() => game.room?.board.filter(tile => tile.ownerId === game.me?.id) ?? []);
const allReady = computed(() => !!game.room && game.room.players.length >= 2 && game.room.players.every(player => player.ready && player.online));
const previewPlayers = computed(() => Math.max(2, game.room?.players.length ?? 2));
const canPlay = computed(() => !props.spectator && !boardAnimating.value && (!game.room?.activity || game.room.activity.stage === 'payment') && game.canAct && game.canDecide && !game.room?.tradePending);
const choice = computed(() => game.privateData?.choice?.id === game.room?.pending?.id ? game.privateData?.choice : null);
const defenseCards = computed(() => game.privateData?.hand.filter(card => !game.privateData?.disabledCardIds?.includes(card.id) && (card.kind === 'halfPayment' || card.kind === 'immunity' && payment.value?.type === 'rent')) ?? []);
const defenseOptions = computed(() => [{ value: '', label: '不使用道具' }, ...defenseCards.value.map(card => ({ value: card.id, label: card.name ?? ITEMS[card.kind].name }))]);
const pendingTile = computed(() => game.room?.board[game.room.pending?.tileId ?? -1]);
const payment = computed(() => game.room?.pending?.type === 'rent' || game.room?.pending?.type === 'debt' ? game.room.pending : null);
const soulRingAvailable = computed(() => payment.value?.type === 'rent' && payment.value.amount > 0 && !payment.value.defenseResolved && (game.privateData?.soulRingRemaining ?? 0) > 0);
const creditorName = computed(() => game.room?.players.find(player => player.id === payment.value?.creditorId)?.name ?? '银行');
const canTrade = computed(() => !drafting.value && canPlay.value && game.room?.pending?.type !== 'choice' && (!payment.value || payment.value.defenseResolved) && (['pre_roll', 'manage'].includes(game.room?.phase ?? '') || !!payment.value?.creditorId));
/** 根据现金及本人地产的最高银行变现能力，返回可偿债金额；与服务端清算校验一致。 */
const liquidationValue = computed(() => (game.me?.cash ?? 0) + owned.value.reduce((sum, tile) => sum + (isKamui(tile) ? 0 : propertyLiquidationValue(tile, game.room?.economy)), 0));
const canLiquidate = computed(() => canPlay.value && !!payment.value && liquidationValue.value < payment.value.amount && (!!payment.value.creditorId || owned.value.every(tile => tile.mortgaged || isKamui(tile))));
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
/** 根据桌面布局与手牌数量返回当前页，每页三张；移动端保留完整手牌。 */
const pagedHand = computed(() => !classicLayout.value && isSingleScreen.value && !props.spectator && !detachedDashboard.value);
const handPage = ref(0);
const handPages = computed(() => Math.max(2, Math.ceil(handCount.value / 3)));
const visibleHand = computed(() => pagedHand.value ? game.privateData?.hand.slice(handPage.value * 3, handPage.value * 3 + 3) : game.privateData?.hand);
/** 输入剩余页数，卡牌减少时将页码收回有效范围，无返回值。 */
watch(handPages, pages => { handPage.value = Math.min(handPage.value, pages - 1); });

const liveSelectedTile = computed(() => selectedTile.value ? game.room?.board[selectedTile.value.id] : null);
const selectedTileOwner = computed(() => game.room?.players.find(player => player.id === liveSelectedTile.value?.ownerId));
/** 根据详情地产及当前付款状态，返回本人是否可抵押或赎回；沿用资产页的归属、时机和现金限制。 */
const canManageSelectedProperty = computed(() => {
  const tile = liveSelectedTile.value;
  return canPlay.value && game.room?.pending?.type !== 'choice' && (!payment.value || payment.value.defenseResolved)
    && tile?.type === 'property' && !isKamui(tile) && tile.ownerId === game.me?.id
    && (!tile.mortgaged || !payment.value && (game.me?.cash ?? 0) >= redeemCost(tile));
});
const turnIdentity = computed(() => `${game.room?.roomId ?? code.value}:${game.room?.gameId}:${game.room?.turnNumber}`);
/** 根据普通主动次数和卍解剩余额度，返回当前是否可以准备使用手牌。 */
const canPrepareCard = computed(() => !game.room?.activity && !boardAnimating.value && !game.settling && !props.spectator && game.room?.status === 'playing' && game.isMyTurn && game.room.phase === 'pre_roll' && !game.room.pending && !game.room.tradePending && (!game.privateData?.activeCardUsed || game.privateData?.bankaiActive && (game.privateData.bankaiRemaining ?? 0) > 0));
const drafting = computed(() => !!selectedCard.value && selectedCardActive.value);
const mapChoice = computed(() => choice.value && ['free_upgrade', 'full_upgrade', 'self_destroy', 'opponent_destroy'].includes(choice.value.type) ? choice.value : null);
const targetKind = computed(() => mapChoice.value ? 'tile' : drafting.value && TILE_CARDS.includes(selectedCard.value!.kind) ? 'tile' : drafting.value && PLAYER_CARDS.includes(selectedCard.value!.kind) ? 'player' : null);
const targeting = computed(() => targetKind.value !== null && (modal.value === 'card' || modal.value === 'choice'));
const mapTargeting = computed(() => mapPicking.value && targeting.value && targetKind.value === 'tile' && !game.settling && !game.cardReveals.length);
const eligibleTiles = computed(() => targetKind.value === 'tile' ? game.room?.board.filter(tile => !tileReason(tile)).map(tile => tile.id) ?? [] : []);
/** 输入当前牌型及公开玩家，返回满足闷棍前后三格、暗影步前后六格、上限与服刑规则的目标。 */
const eligiblePlayers = computed(() => targetKind.value === 'player' ? game.room?.players.filter(player => {
  const kind = selectedCard.value?.kind;
  const distance = Math.abs(player.position - (game.me?.position ?? 0));
  return !player.bankrupt && (['blast', 'jail', 'swapAny'].includes(kind ?? '') || player.id !== game.me?.id)
    && (kind !== 'swap' || !player.jailed) && (kind !== 'sap' || Math.min(distance, (game.room?.board.length ?? 0) - distance) <= 3)
    && (kind !== 'shadowstep' || Math.min(distance, (game.room?.board.length ?? 0) - distance) <= 6)
    && (kind !== 'reaper' || (player.handLimit ?? 6) > 1 && (game.me?.handLimit ?? 6) > 1);
}).map(player => player.id) ?? [] : []);
const targetTileId = computed(() => targetKind.value === 'tile' && selectedTarget.value !== '' ? Number(selectedTarget.value) : null);
/** 输入当前目标及房间状态，返回玩家昵称或包含地块编号、当前归属的土地名称。 */
const targetLabel = computed(() => {
  if (targetKind.value !== 'tile') return game.room?.players.find(player => player.id === selectedTarget.value)?.name ?? '';
  const tile = targetTileId.value == null ? null : game.room?.board[targetTileId.value];
  if (!tile) return '';
  const owner = game.room?.players.find(player => player.id === tile.ownerId)?.name ?? (tile.type === 'property' ? '无主地产' : '公共地块');
  return `${tile.name} · ${tile.id}号 · 归属：${owner}${tile.type === 'property' ? ` · ${tile.level}级` : ''}`;
});
const targetHint = computed(() => {
  if (mapChoice.value) return ({ free_upgrade: '选择自己的未抵押、未满级且本回合未升级地产。', full_upgrade: '选择自己的未抵押、未满级且本回合未升级地产，确认后免费直升 3 级。', self_destroy: '选择自己的有建筑地产，确认后摧毁全部建筑。', opponent_destroy: '选择对手的一块有建筑地产，确认后摧毁全部建筑，土地产权保留。' } as Record<string, string>)[mapChoice.value.type];
  if (!targeting.value) return '';
  const rules: Partial<Record<Card['kind'], string>> = { shield: `选择没有护盾的自家地产，添加 ${selectedCard.value?.parameters?.charges ?? 1} 层护盾。`, exchange: '选择现金足以支付交换价的对手地产，最低为地价。', blackhole: '选择有附加效果的地产。', homeTeleport: '选择自家地产。', sap: '选择前后3格内的1名对手，含同格。', slow: '选择1名对手。', reaper: '选择手牌上限大于1的对手，双方上限永久各减1。', shadowstep: '选择前后6格内的另一名玩家，含同格。', swapAny: '选择2名不同玩家，可含自己。', barrier: '选择前后6格内无路障的地块，不含当前格。', demolish: '选择对手有建筑的地产。', fullDemolish: '选择对手有建筑的地产。', swap: '选择未服刑的对手。', steal: '选择1名对手。', blast: '选择1名玩家，可选自己。', jail: '选择1名玩家，可选自己。' };
  return rules[selectedCard.value!.kind] ?? cardEffect(selectedCard.value!);
});
/** 输入房间地块与玩家，返回合法地块及带禁用原因的玩家；地块携带价格和建筑插画。 */
const dialogTargets = computed(() => {
  if (targetKind.value === 'player') return (game.room?.players ?? []).map(player => ({ id: player.id, name: `${player.name}${player.id === game.me?.id ? '（我）' : ''}`, detail: ['swap', 'swapAny', 'shadowstep', 'sap'].includes(selectedCard.value?.kind ?? '') ? `位置：${game.room?.board[player.position]?.name ?? player.position} · ${player.position}号${player.jailed ? ' · 服刑中' : ''}` : `现金 ¥${player.cash.toLocaleString()} · 手牌 ${player.cardCount ?? '—'}`, avatarId: player.avatarId, color: player.color, reason: eligiblePlayers.value.includes(player.id) ? '' : player.bankrupt ? '该玩家已破产' : player.id === game.me?.id ? '不能选择自己' : selectedCard.value?.kind === 'sap' ? '目标不在前后3格内' : selectedCard.value?.kind === 'shadowstep' ? '目标不在前后6格内' : '不满足本牌的上限或服刑条件' })).sort((a, b) => Number(!!a.reason) - Number(!!b.reason));
  if (targetKind.value === 'tile') return (game.room?.board ?? []).filter(tile => eligibleTiles.value.includes(tile.id)).map(tile => ({ id: String(tile.id), name: `${tile.name}${hasPropertyEffects(tile, game.room?.constructionEffects) ? ' ✦' : ''} · ${tile.id}号`, detail: tile.type === 'property' ? `归属：${game.room?.players.find(p => p.id === tile.ownerId)?.name ?? '无主地产'} · ${tile.level}级${tile.mortgaged ? ' · 抵押' : ''}` : '公共地块', tile }));
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
  if (isKamui(tile)) return '神威空间中的地产无法操作或施加效果。';
  const kind = selectedCard.value?.kind;
  if (['shield', 'exchange', 'blackhole', 'homeTeleport'].includes(kind ?? '')) {
    if (tile.type !== 'property') return '请选择普通地产。';
    if (kind === 'shield') return tile.ownerId !== game.me?.id ? '请选择自家地产。' : tile.demolitionShield ? '该地产已有护盾。' : '';
    if (kind === 'homeTeleport') return tile.ownerId === game.me?.id ? '' : '请选择自家地产。';
    if (kind === 'exchange') return !tile.ownerId || tile.ownerId === game.me?.id ? '请选择对手地产。' : (game.me?.cash ?? 0) < Math.max(tile.price, baseRentFor(tile, game.room?.economy)) ? '现金不足以支付交换价（无修正租金，最低地价）。' : '';
    if (kind === 'blackhole') return hasPropertyEffects(tile, game.room?.constructionEffects) ? '' : '该地产没有附加效果。';
    return '';
  }
  if (['demolish', 'fullDemolish'].includes(selectedCard.value?.kind ?? '')) return tile.ownerId && tile.ownerId !== game.me?.id && !game.room?.players.find(p => p.id === tile.ownerId)?.bankrupt && tile.level > 0 ? '' : '拆房需要选择未破产对手有建筑的地产。';
  if (selectedCard.value?.kind === 'barrier') {
    const size = game.room?.board.length ?? 1;
    const forward = (tile.id - (game.me?.position ?? 0) + size) % size;
    const distance = Math.min(forward, size - forward);
    if (distance < 1 || distance > 6) return '路障只能放在附近 6 格内的地块（前后各 6 格，不含当前格）。';
    return tile.barrier ? '这个地块已有路障，请选择其他地块。' : '';
  }
  return '当前道具不需要选择地块。';
}
watch(() => game.room?.activity?.id, id => { if (id) { modal.value = null; selectedTile.value = null; } });
watch(() => choice.value?.id, id => {
  selectedTarget.value = '';
  mapPicking.value = false;
  if (id && (choice.value?.type.startsWith('construction_') || choice.value?.type.startsWith('cross_'))) { clearCard(); modal.value = 'choice'; return; }
  if (id) { const fromCard = modal.value === 'card' && ['bankai', 'freeUpgrade', 'fullUpgrade', 'recycle', 'discover', 'projection'].includes(selectedCard.value?.kind ?? ''); clearCard(); selectedTile.value = null; if (fromCard) modal.value = 'choice'; }
  else if (modal.value === 'choice') modal.value = null;
});
watch([turnIdentity, canPrepareCard, () => game.privateData?.hand], () => {
  if (selectedCard.value && (selectedCardTurn.value !== turnIdentity.value || !game.privateData?.hand.some(card => card.id === selectedCard.value?.id) || selectedCardActive.value && !canPrepareCard.value && !(selectedCard.value.projected && selectedCard.value.expiresAtTurn === game.room?.turnNumber && game.isMyTurn && game.room?.phase === 'pre_roll' && !game.room.pending) && !(selectedCard.value.kind === 'deathReturn' && canLiquidate.value && payment.value?.defenseResolved) && !submittingCard.value)) clearCard();
});
watch([eligibleTiles, eligiblePlayers], () => {
  if (selectedTarget.value && !(targetKind.value === 'tile' ? eligibleTiles.value.includes(Number(selectedTarget.value)) : eligiblePlayers.value.includes(selectedTarget.value))) {
    selectedTarget.value = ''; game.notify('原目标已不可用，请重新选择高亮目标。');
  }
  if (selectedSecondTarget.value && (!eligiblePlayers.value.includes(selectedSecondTarget.value) || selectedSecondTarget.value === selectedTarget.value)) selectedSecondTarget.value = '';
});
/** 输入窗口及目标类型，离开目标选择流程时恢复普通地图，无返回值。 */
watch([modal, targetKind], () => { if (!targeting.value || targetKind.value !== 'tile') mapPicking.value = false; });
const actionStatus = computed(() => {
  if (game.connection !== 'online') return '正在连接';
  if (game.room?.pausedReason) return '等待重连';
  if (game.room?.status === 'finished') return '本局结束';
  if (game.room?.tradePending) return '交易中';
  if (game.room?.activity && game.room.activity.stage !== 'payment') return '角落活动进行中';
  if (drafting.value) return '准备出牌';
  if (game.room?.pending?.type === 'choice') return game.canDecide ? '需要你确认' : '等待选择';
  if (game.room?.pending && game.canDecide) return '需要你确认';
  if (canRoll.value) return '轮到你了';
  const labels: Record<string, string> = { pre_roll: '准备掷骰', buy: '购买地产', upgrade: '升级建筑', rent: '支付路费', debt: '处理费用', manage: '管理资产' };
  return labels[game.room?.phase ?? 'pre_roll'] ?? '等待行动';
});

/** 输入当前房间路由，保留原身份并重新连接；新玩家等待主动加入，观战直接申请会话。 */
async function enter(): Promise<void> {
  const generation = ++entryGeneration;
  awaitingJoin.value = false; joining.value = false;
  clearCard(); selectedTile.value = null; recordMode.value = 'live';
  game.disconnect();
  try {
    const saved = readSession(code.value, props.spectator);
    if (!saved && !props.spectator) { awaitingJoin.value = true; return; }
    const value = saved ?? await game.enter('spectate', code.value);
    if (generation === entryGeneration) game.connect(value);
  } catch (error) { if (generation === entryGeneration) game.fatalError = error instanceof Error ? error.message : '无法进入房间'; }
}
/** 无入参，主动提交昵称并加入当前邀请房间；提交期间忽略重复操作，无返回值。 */
async function joinInvitation(): Promise<void> {
  if (joining.value || !awaitingJoin.value || props.spectator) return;
  const generation = ++entryGeneration;
  joining.value = true; game.fatalError = '';
  try {
    localStorage.setItem('dfw:nickname', entryName.value);
    const value = readSession(code.value) ?? await game.enter('join', code.value, entryName.value);
    if (generation === entryGeneration) { awaitingJoin.value = false; game.connect(value); }
  } catch (error) { if (generation === entryGeneration) game.fatalError = error instanceof Error ? error.message : '无法进入房间'; }
  finally { if (generation === entryGeneration) joining.value = false; }
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
/** 无入参，打开当前本人待办并返回列表选择，已同步的私人选择复用原弹窗，无返回值。 */
function openDecision(): void { if (!hasCentralDecision.value || game.cardReveals.length) return; void switchTargetMode(false); modal.value = choice.value ? 'choice' : payment.value && !payment.value.defenseResolved ? 'defense' : 'decision'; }
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
  (detachedDashboard.value ? dashboardView.value : mapView.value)?.querySelector('.dashboard-records')?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
}

/** 无入参，按详情中地产的最新归属打开交易并勾选该房产；不可交易时保留详情，无返回值。 */
function tradeSelectedProperty(): void {
  if (!canTrade.value || !liveSelectedTile.value || !tradeEditor.value?.selectProperty(liveSelectedTile.value.id)) return;
  selectedTile.value = null;
  modal.value = 'trade';
}

/** 无入参，按详情地产的最新抵押状态执行抵押或赎回；保留详情并由房间快照更新金额，无返回值。 */
async function manageSelectedProperty(): Promise<void> {
  const tile = liveSelectedTile.value;
  if (!tile || !canManageSelectedProperty.value) return;
  await game.act({ type: tile.mortgaged ? 'redeem' : 'mortgage', tileId: tile.id });
}

const fullscreenActive = ref(!!document.fullscreenElement);
/** 无入参，同步全屏状态；退出浏览器全屏时释放方向锁并保留当前观战布局，无返回值。 */
function syncFullscreen(): void {
  const exited = fullscreenActive.value && !document.fullscreenElement;
  fullscreenActive.value = !!document.fullscreenElement;
  if (exited && props.spectator) screen.orientation?.unlock?.();
}
document.addEventListener('fullscreenchange', syncFullscreen);
onBeforeUnmount(() => { document.removeEventListener('fullscreenchange', syncFullscreen); if (props.spectator) screen.orientation?.unlock?.(); });
/** 无入参，切换玩家页面的浏览器全屏；失败时提示兼容方式，无返回值。 */
async function fullscreen(): Promise<void> {
  try { if (document.fullscreenElement) await document.exitFullscreen(); else await document.documentElement.requestFullscreen(); }
  catch { game.notify('当前浏览器不支持全屏，请使用浏览器全屏或投屏功能。'); }
}
/** 输入观战模式，跟随时恢复上下布局，横屏时请求全屏和横向锁定；不支持时保留全图并提示，无返回值。 */
async function switchSpectatorView(mode: 'follow' | 'landscape'): Promise<void> {
  spectatorMode.value = mode;
  if (mode === 'follow') {
    screen.orientation?.unlock?.();
    if (document.fullscreenElement) await document.exitFullscreen().catch(() => game.notify('请使用浏览器退出全屏。'));
    return;
  }
  try { if (!document.fullscreenElement) await document.documentElement.requestFullscreen(); }
  catch { game.notify('已切换完整地图，当前浏览器不支持自动全屏，可横放手机观看。'); return; }
  const orientation = screen.orientation as ScreenOrientation & { lock?: (value: 'landscape') => Promise<void> };
  try { if (orientation?.lock) await orientation.lock('landscape'); else if (window.innerHeight > window.innerWidth) game.notify('请横放手机，查看完整地图。'); }
  catch { if (window.innerHeight > window.innerWidth) game.notify('当前设备不支持自动横屏，请横放手机观看。'); }
}
/** 输入本人手牌，在窗口中查看或准备使用；付款防御复用当前费用窗口。 */
function chooseCard(card: Card): void {
  if (game.settling) return;
  if (selectedCard.value?.id === card.id) { modal.value = 'card'; return; }
  if (payment.value && !payment.value.defenseResolved && defenseCards.value.some(value => value.id === card.id)) { selectedDefense.value = card.id; modal.value = 'defense'; return; }
  if (choice.value || game.privateData?.trade || game.busy) { game.notify('请先处理当前选择或报价。'); return; }
  selectedCard.value = card; selectedTarget.value = ''; selectedSecondTarget.value = ''; mapPicking.value = false; selectedTile.value = null;
  selectedCardActive.value = ((canPrepareCard.value || !!card.projected && card.expiresAtTurn === game.room?.turnNumber && game.isMyTurn && game.room?.phase === 'pre_roll' && !game.room?.pending) && !RESPONSE_CARDS.includes(card.kind)) || card.kind === 'deathReturn' && canLiquidate.value && !!payment.value?.defenseResolved;
  selectedCardTurn.value = turnIdentity.value; modal.value = 'card';
}
/** 无入参，取消本地选牌并关闭其窗口，不消费手牌或主动次数。 */
function clearCard(): void { submittingCard.value = false; selectedCard.value = null; selectedCardActive.value = false; selectedTarget.value = ''; selectedSecondTarget.value = ''; mapPicking.value = false; if (modal.value === 'card') modal.value = null; }
/** 输入成功使用的牌型，免费升级及风险道具接续服务端选择，其他道具结束选牌。 */
function cardUsed(kind: Card['kind']): void { clearCard(); if (choice.value?.type.startsWith('construction_') || choice.value?.type.startsWith('cross_')) { modal.value = 'choice'; return; } if (['bankai', 'recycle', 'discover', 'projection'].includes(kind) && choice.value) { modal.value = 'choice'; return; } if (['freeUpgrade', 'fullUpgrade'].includes(kind) && ['free_upgrade', 'full_upgrade'].includes(choice.value?.type ?? '') || kind === 'gamble' && choice.value?.type === 'gamble') modal.value = 'choice'; }
/** 输入目标标识和玩家槽位，按原规则更新选择；确认按钮才发送命令。 */
function selectDialogTarget(id: string, slot: 1 | 2 = 1): void {
  if (targetKind.value === 'tile') { const tile = game.room?.board[Number(id)]; if (tile) selectTile(tile); }
  else if (slot === 2 && selectedCard.value?.kind === 'swapAny') { if (game.canAct && eligiblePlayers.value.includes(id) && id !== selectedTarget.value) selectedSecondTarget.value = id; }
  else { selectPlayer(id); if (selectedSecondTarget.value === selectedTarget.value) selectedSecondTarget.value = ''; }
}
/** 输入是否地图点选，保留目标与地图视野，收起确认框并交还地图键盘焦点，无返回值。 */
async function switchTargetMode(picking: boolean): Promise<void> {
  mapConfirmOpen.value = false;
  mapPicking.value = picking;
  if (!picking) return;
  selectedTile.value = null;
  await nextTick();
  const id = targetTileId.value ?? eligibleTiles.value[0];
  if (id == null || !mapTargeting.value) return;
  mapView.value?.querySelector<HTMLElement>(`[data-cash-tile="${id}"]`)?.focus({ preventScroll: true });
}
/** 无入参，关闭地图确认框后继续点选，列表模式收起窗口，无返回值。 */
function closeTargetPicker(): void { if (mapPicking.value) void switchTargetMode(true); else modal.value = null; }
/** 输入地图地块，选择合法目标或打开普通地块详情，不提交游戏命令。 */
function selectTile(tile: Tile): void {
  if (!targeting.value) { selectedTile.value = tile; return; }
  if (targetKind.value === 'player') { game.notify('正在使用玩家道具，请点击排行榜的玩家姓名或头像。'); return; }
  const reason = tileReason(tile);
  if (reason) { game.notify(reason); return; }
  if (game.canAct) {
    selectedTarget.value = String(tile.id);
    if (mapTargeting.value) mapConfirmOpen.value = true;
  }
}
/** 输入排行榜玩家身份，选择合法道具目标；确认按钮才发送命令。 */
function selectPlayer(id: string): void {
  if (targetKind.value !== 'player') return;
  if (!eligiblePlayers.value.includes(id)) { game.notify(['blast', 'jail'].includes(selectedCard.value?.kind ?? '') ? '请选择未破产玩家，可以选择自己。' : selectedCard.value?.kind === 'swap' ? '请选择未破产、未服刑的其他玩家。' : '请选择未破产的其他玩家。'); return; }
  if (game.canAct) selectedTarget.value = id;
}
/** 输入地图规模，通过原命令通道保存房主设置，选中状态由服务端快照更新。 */
async function changeMapSize(mapSize: MapSize): Promise<void> { await game.act({ type: 'setMapSize', mapSize }); }
</script>
<template>
  <div class="room-page modern-room" :class="{ 'spectator-page': spectator, 'spectator-follow': spectatorFollowing, 'spectator-landscape': spectator && !spectatorFollowing, 'game-session': isGameSession, 'single-screen': isSingleScreen, 'lobby-room': isLobby, 'detached-dashboard': detachedDashboard, 'classic-layout': classicLayout, 'desktop-single-screen': !classicLayout && isSingleScreen && !spectator && !detachedDashboard }" :style="{ '--town-background': `url(${imageUrl('/images/board/town-background.png')})` }">
    <img v-if="isLobby" class="lobby-background" :src="imageUrl('/images/lobby-cottage.png')" alt="" fetchpriority="high" />
    <header class="site-header room-header immersive-header">
      <RouterLink to="/" aria-label="返回首页"><span v-if="isLobby" class="lobby-back">← 返回小镇</span><Logo v-else /></RouterLink>
      <button class="room-tag" aria-label="复制房间号" @click="game.copy(code)">房间 <strong>{{ code }}</strong></button>
      <div v-if="isGameSession" class="room-utilities game-utilities" role="group" aria-label="游戏工具栏">
        <span class="connection-label" :class="game.connection"><i></i>{{ game.connection === 'online' ? '实时同步' : game.connection === 'connecting' ? '连接中' : '已断开' }}</span>
        <button v-if="spectator && !spectatorFollowing" type="button" class="button" @click="switchSpectatorView('follow')">跟随模式</button>
        <button v-if="spectator" type="button" class="button spectator-fullscreen" :aria-pressed="fullscreenActive && !spectatorFollowing" @click="switchSpectatorView(fullscreenActive && !spectatorFollowing ? 'follow' : 'landscape')">{{ fullscreenActive && !spectatorFollowing ? '退出横屏' : '横屏模式' }}</button>
        <AudioSettings class="room-audio-control" />
        <button v-if="!spectator" type="button" class="button layout-toggle" :aria-label="classicLayout ? '切换为单屏布局' : '切换为经典布局'" :title="classicLayout ? '当前经典布局，点击切换单屏布局' : '当前单屏布局，点击切换经典布局'" :aria-pressed="classicLayout" @click="toggleLayout"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="4" width="18" height="16" rx="3"/><path :d="classicLayout ? 'M3 14h18M14 4v10' : 'M9 4v16M16 4v16M9 14h7'"/></svg><span>{{ classicLayout ? '经典' : '单屏' }}</span></button>
        <button class="button game-tools-trigger" aria-label="打开游戏工具" aria-haspopup="dialog" @click="modal = 'tools'">
          <svg viewBox="0 0 32 32" aria-hidden="true"><path d="m13 4 1-2h4l1 2 3 2 3-.2 2 3-1 3v4l1 3-2 3-3-.2-3 2-1 3h-4l-1-3-3-2-3 .2-2-3 1-3v-4l-1-3 2-3 3 .2Z" /><circle cx="16" cy="14.5" r="5" /></svg>
          工具
        </button>
      </div>
      <div v-else-if="isLobby" class="room-utilities" role="group" aria-label="房间工具栏">
        <AudioSettings class="room-audio-control" />
      </div>
    </header>
    <main v-if="awaitingJoin" class="loading-page">
      <div class="entry-icon">🎲</div><h2>加入好友房间</h2><p class="muted">朋友邀请你加入房间 {{ code }}，一起开启财富派对。</p>
      <form class="panel" :aria-busy="joining" @submit.prevent="joinInvitation">
        <label class="field">你的昵称 <span class="optional">选填</span><input v-model="entryName" placeholder="今天的财富新星" autocomplete="nickname" :disabled="joining" /></label>
        <p v-if="game.fatalError" class="error-text" role="alert">{{ game.fatalError }}</p>
        <div class="button-row"><RouterLink class="button secondary" to="/">返回首页</RouterLink><RouterLink class="button secondary" :to="`/watch/${code}`">进入观战</RouterLink><button type="submit" class="button primary" :disabled="joining">{{ joining ? '正在加入…' : '加入房间' }}</button></div>
      </form>
    </main>
    <main v-else-if="!game.room" class="loading-page"><div class="entry-icon">🎲</div><h2>{{ game.fatalError ? '暂时无法进入房间' : '正在打开你的城市…' }}</h2><p class="muted" role="status">{{ game.fatalError || '连接房间，恢复最新的棋盘和玩家信息。' }}</p><p v-if="game.fatalError && game.session?.playerId" class="muted">已保留你的玩家身份，重新连接会继续尝试恢复原座位。</p><div v-if="game.fatalError" class="button-row"><RouterLink class="button secondary" to="/">返回首页</RouterLink><RouterLink v-if="!spectator" class="button primary" :to="`/watch/${code}`">进入观战</RouterLink><button class="button ghost" @click="enter">重新连接</button></div></main>
    <main v-else>
      <div v-if="game.connection !== 'online'" class="notice warning connection-notice">{{ game.fatalError || '网络连接已断开，正在自动重连。当前显示最后同步的局面。' }}<button v-if="game.connection === 'offline' || game.connection === 'replaced'" class="button small secondary" @click="enter">在此页重新连接</button></div>
      <div v-if="game.room.pausedReason" class="notice warning connection-notice">Ⅱ <PlayerText :text="game.room.pausedReason" />。对局已保存，恢复后继续当前步骤。</div>
      <RecoverySeats v-if="!spectator" />
      <section v-if="game.room.researchCandidates?.length" class="panel" aria-label="天才俱乐部公共研究"><h3>天才俱乐部 · 公共研究</h3><p class="muted">每人秘密投入0～2张手牌，或放弃；触发者额外＋1贡献。最高者胜，并列随机，仅胜者支付投入牌并自选1张UR。</p><div class="choice-card-grid"><ContentCard v-for="candidate in game.room.researchCandidates" :key="candidate.id" :content="candidate" compact thumbnail /></div></section>
      <section v-if="isLobby" class="lobby-layout cottage-lobby" aria-label="房间大厅">
        <div class="lobby-panel cottage-panel">
          <div class="cottage-heading"><h1>好友房间 <small>{{ game.room.players.length }} / 6</small></h1><button class="button secondary small" @click="inviteFriends">邀请朋友</button></div>
          <div class="lobby-seats">
            <div v-for="player in game.room.players" :key="player.id" class="lobby-seat" :class="{ 'is-me': player.id === game.me?.id, offline: !player.online }">
              <CharacterPawn :avatar-id="player.avatarId" :color="player.color" />
              <div class="seat-identity"><strong :style="{ color: player.color }" :title="player.name">{{ player.name }}</strong><small>{{ player.botDifficulty ? `机器人 · ${botLevels.find(level => level.value === player.botDifficulty)?.label}` : player.id === game.room.hostId ? '房主' : player.id === game.me?.id ? '我' : '玩家' }}{{ player.id === game.room.hostId && player.id === game.me?.id ? ' · 我' : '' }}</small></div>
              <ThemedSelect v-if="game.isHost && player.botDifficulty" class="bot-level-select" :label="`${player.name}的难度`" :model-value="player.botDifficulty" :options="botLevels" :disabled="game.busy || !game.canAct" @update:model-value="changeBotDifficulty(player.id, $event)" />
              <ReadyIcon class="seat-ready" :ready="player.ready" :offline="!player.online" />
              <button v-if="game.isHost && (!player.online || player.botDifficulty) && player.id !== game.me?.id" class="seat-remove" :disabled="game.busy" :aria-label="`移出 ${player.name}`" @click="game.act({ type: 'removePlayer', playerId: player.id })">移出</button>
            </div>
            <div v-for="seat in 6 - game.room.players.length" :key="`empty-${seat}`" class="lobby-seat empty"><span class="empty-seat-mark" aria-hidden="true">＋</span><span>待加入</span></div>
          </div>
          <div class="cottage-map"><span>地图</span><div class="map-size-options" role="group" aria-label="地图大小"><button v-for="size in MAP_SIZES" :key="size.value" type="button" :class="{ selected: (game.room.mapSize ?? 'large') === size.value }" :aria-pressed="(game.room.mapSize ?? 'large') === size.value" :disabled="spectator || !game.isHost || !game.canAct" @click="changeMapSize(size.value)">{{ size.name }}<small>{{ mapLength(previewPlayers, size.value) }} 格</small></button></div></div>
          <div v-if="game.isHost && !spectator" class="lobby-bot-controls"><label class="field">机器人难度<ThemedSelect v-model="botDifficulty" label="机器人难度" :options="botLevels" :disabled="!game.canAct" /></label><button class="button secondary" :disabled="!game.canAct || game.room.players.length >= 6" @click="game.act({ type: 'addBot', botDifficulty })">＋ 添加机器人</button><p class="muted">与玩家遵守相同规则，自动准备；开局前可调整难度。</p></div>
          <section v-if="game.me && !spectator" class="lobby-avatar-picker"><div class="lobby-avatar-heading"><h2>选择头像</h2><button type="button" class="button secondary small lobby-rename" :disabled="!canRename" @click="openRename">改名</button></div><AvatarPicker /></section>
          <div v-if="!spectator" class="lobby-controls"><div class="cottage-buttons"><button class="button" :class="game.me?.ready ? 'secondary' : 'primary'" :disabled="!game.canAct" @click="game.act({ type: 'ready', value: !game.me?.ready })">{{ game.me?.ready ? '取消准备' : '准备好了' }}</button><button v-if="game.isHost" class="button primary" :disabled="!game.canAct || !allReady" @click="game.act({ type: 'start' })">开始游戏</button></div><p class="lobby-hint" role="status">{{ game.room.players.length < 2 ? '等待朋友加入' : allReady ? game.isHost ? '大家都准备好了' : '等待房主开局' : '等待所有玩家准备' }}</p></div>
          <p v-else class="lobby-hint">观战中 · 等待开局</p>
        </div>
      </section>
      <template v-else>
        <div class="game-layout immersive-layout">
          <div v-if="detachedDashboard" class="mobile-turn game-board immersive-board"><div id="mobile-turn-status" class="workbench-center"></div></div>
          <section class="map-panel"><div ref="mapView" class="board-view">
            <Board ref="board" :classic-layout="classicLayout" :desktop-dice="!mobileViewport" :status-target="detachedDashboard ? '#mobile-turn-status' : undefined" :dashboard-target="detachedDashboard ? '#room-dashboard' : undefined" :presentation-epoch="game.presentationEpoch" :live="game.connection === 'online' && !game.room.recoveryPending" :room="game.room" :spectator="spectator" :spectator-follow="spectatorFollowing" compact :bounded="false" :stretch="isSingleScreen" :me-id="game.me?.id" :incoming-trade="game.privateData?.trade" :decision="hasCentralDecision || drafting" :targeting="mapTargeting" :target-hint="targetHint" :eligible-tiles="eligibleTiles" :selected-target="targetTileId" @view="switchSpectatorView" @select="selectTile" @animating="boardAnimating = $event" @settled="game.finishMovement">
              <template #action-status>
                <ActivityDialog :paused="boardAnimating || game.settling || !!game.cardReveals.length" :spectator="spectator">
                <template v-if="game.me && !spectator && !game.settling && !boardAnimating">
                  <button v-if="drafting" type="button" class="turn-action-status turn-action-link" aria-haspopup="dialog" :aria-expanded="modal === 'card' && (!mapTargeting || mapConfirmOpen)" @click="switchTargetMode(false); modal = 'card'"><strong>继续使用：{{ selectedCard?.name ?? '道具' }}</strong><span aria-hidden="true">↗</span></button>
                  <button v-else-if="tradeReminder" type="button" class="turn-action-status turn-action-link needs-confirmation" aria-haspopup="dialog" :aria-expanded="modal === 'trade'" title="点击查看报价" @click="modal = 'trade'"><strong>当前行动：<PlayerText :text="tradeReminder" /></strong><span aria-hidden="true">↗</span></button>
                  <button v-else-if="hasCentralDecision" type="button" class="turn-action-status turn-action-link needs-confirmation" :disabled="!!game.cardReveals.length" aria-haspopup="dialog" :aria-expanded="modal === 'decision' || modal === 'choice' || modal === 'defense'" title="点击处理当前事项" @click="openDecision"><strong>当前行动：{{ actionStatus }}</strong><span aria-hidden="true">↗</span></button>
                  <div v-else class="turn-action-status" :class="{ 'is-waiting': game.connection !== 'online' || !!game.room.pausedReason || game.room.status === 'finished' || !game.isMyTurn && !game.canDecide }" role="status"><strong>当前行动：{{ actionStatus }}</strong></div>
                </template>
                </ActivityDialog>
              </template>
              <template #summary>
                <div v-if="showMyTurn" :key="turnIdentity" class="hub-turn-callout" role="status"><ChibiLettering kind="turn" /></div>
                <dl v-if="game.me && !spectator" class="turn-wallet" aria-label="我的资产概况"><div v-if="game.me.assetRank" class="turn-wallet-rank" :title="`净资产排名：第${game.me.assetRank}名`"><dt class="sr-only">我的排名</dt><dd><ChibiLettering kind="rank" :rank="game.me.assetRank" /></dd></div><div><dt class="sr-only">我的现金</dt><dd><CashBalance :presentation-epoch="game.presentationEpoch" :room="game.room" :player-id="game.me.id" :live="game.connection === 'online' && !game.room.recoveryPending" /></dd></div><div class="turn-wallet-worth"><dt class="sr-only">我的净资产</dt><dd><AssetStatIcon kind="worth" /><span>{{ game.me.netWorth === undefined ? '—' : `¥${game.me.netWorth.toLocaleString()}` }}</span></dd></div></dl>
              </template>
              <template #controls>
                <FitPanel :disabled="detachedDashboard || spectator || isSingleScreen && !classicLayout">
                <div class="dashboard-scroll">
                  <div v-if="game.room.status === 'finished' || game.me?.bankrupt" class="hub-finish">
                    <p v-if="game.room.status === 'finished'">淘汰顺序：<PlayerText :text="elimination.map(player => player.name).join(' → ') || '—'" /></p>
                    <MatchSettlement :key="`${game.room.gameId}:${spectator ? 'watch' : game.me?.id}`" :room="game.room" :me-id="spectator ? undefined : game.me?.id" :paused="boardAnimating || game.settling || !!game.cardReveals.length || !!game.room.activity || !!game.room.recoveryPending" :can-restart="game.isHost && !spectator" :busy="!game.canAct" @opened="modal = null; selectedTile = null" @restart="game.act({ type: 'newGame' })" />
                  </div>
                  <div class="dashboard-grid">
                    <section class="dashboard-records workspace-panel">
                      <div class="city-feed-panel">
                      <PanelFrame />
                      <div class="workspace-heading"><h2><span class="news-indicator" aria-hidden="true"></span>城市新鲜事</h2><div v-if="!spectator" class="record-switch" role="group" aria-label="记录范围"><button :aria-pressed="recordMode === 'live'" :class="{ active: recordMode === 'live' }" @click="recordMode = 'live'">实时</button><button :aria-pressed="recordMode === 'history'" :class="{ active: recordMode === 'history' }" @click="recordMode = 'history'">历史</button></div></div>
                      <Teleport to="#mobile-card-reveal" :disabled="!classicLayout || !detachedDashboard || spectator" defer><div class="mobile-city-reveal"><CardReveal :embedded="classicLayout" :paused="boardAnimating || game.settling && !game.settlementRevealing" /></div></Teleport>
                      <div class="records-scroll"><LiveRecords @tile="locateChatTile" compact journal :active="spectator || recordMode === 'live'" v-show="spectator || recordMode === 'live'" :key="`${game.room.roomId ?? code}-${game.room.gameId}`" :room="game.room" :token="game.session?.token" /><template v-if="recordMode === 'history' && !spectator"><RoomHistory @tile="locateChatTile" v-if="archiveCode && !spectator" compact :archive-code="archiveCode" :game-id="game.room.gameId ?? null" /><p v-else class="muted">历史回顾需要玩家的固定存档码；本局完整公开记录可在“实时”中查阅。</p></template></div>
                      </div>
                      <Teleport to="#desktop-hand-chat" :disabled="!pagedHand" defer>
                      <ChatComposer v-if="!spectator" :key="`${game.room.roomId ?? code}-${game.room.gameId}`" :room="game.room" :disabled="!canChat" :send="sendChat" :placeholder="game.connection !== 'online' ? '恢复连接后可发送消息' : '和大家聊聊，输入 @ 提及玩家或地块…'" />
                      </Teleport>
                    </section>
                    <aside class="dashboard-sidebar">
                      <Teleport :to="detachedDashboard && !spectator ? '#mobile-workbench-ranking' : 'body'" :disabled="!detachedDashboard || spectator" defer>
                      <aside class="dashboard-ranking workspace-panel"><PanelFrame variant="ranking" ornament="right" /><div class="workspace-heading"><div><h2>资产排行</h2></div><span class="workspace-count">{{ game.room.players.filter(p => !p.bankrupt).length }} 人</span></div><PublicLeaderboard compact :room="game.room" :me-id="game.me?.id" :can-remove="game.isHost && !spectator" :busy="game.busy" @select="selectTile" @remove="game.act({ type: 'removePlayer', playerId: $event })" /></aside>
                      </Teleport>
                      <div id="classic-workbench" v-show="classicLayout" class="classic-workbench-slot"></div>
                    </aside>
                  </div>
                  <Teleport :to="detachedDashboard ? '#mobile-workbench-cards' : 'body'" :disabled="!detachedDashboard" defer>
                  <section v-if="!spectator" class="dashboard-cards workspace-panel" aria-label="我的手牌"><PanelFrame variant="cards" /><div v-if="game.privateData?.activeCardUsed" class="workspace-heading"><span class="workspace-count">本回合已使用</span></div><template v-if="game.me && !spectator"><div class="dashboard-card-rail" :class="{ 'has-sealed-slots': (game.me.handLimit ?? 6) < 6 }"><button v-for="card in visibleHand" :key="card.id" class="dashboard-hand-card" :class="{ 'is-drafting': drafting && selectedCard?.id === card.id }" :aria-label="`${drafting && selectedCard?.id === card.id ? '继续使用' : '使用'}卡牌 ${card.name ?? ITEMS[card.kind].name}`" @click="chooseCard(card)"><ContentCard :content="card" compact thumbnail /><span v-if="drafting && selectedCard?.id === card.id" class="hand-card-in-use" aria-hidden="true">使用中</span></button><HandSlots :count="handCount" :limit="game.me.handLimit" :page="pagedHand ? handPage : undefined" /></div>
                      <div v-if="pagedHand" class="hand-pagination"><nav aria-label="手牌翻页"><button class="button secondary small" :disabled="handPage === 0" aria-label="上一页手牌" @click="handPage--">←</button><span aria-live="polite">{{ handPage + 1 }} / {{ handPages }}</span><button class="button secondary small" :disabled="handPage === handPages - 1" aria-label="下一页手牌" @click="handPage++">→</button></nav></div></template>
                      <p v-else class="workspace-caption">手牌仅本人可见</p><p v-if="detachedDashboard && !handCount" class="workspace-caption">暂无手牌</p></section>
                  </Teleport>

                  <div v-show="pagedHand" id="desktop-hand-chat" class="desktop-hand-chat"></div>

                  <Teleport to="#classic-workbench" :disabled="!classicLayout" defer>
                  <section v-if="game.me && !spectator && !detachedDashboard" class="dashboard-workbench workspace-panel" aria-label="工作台">
                    <PanelFrame variant="workbench" ornament="right" />
                    <div class="workbench-actions">
                      <button type="button" class="button workbench-action" :aria-label="`我的资产，${owned.length} 处地产`" :title="`我的资产，${owned.length} 处地产`" @click="modal = 'assets'"><WorkbenchActionArt action="assets" /></button>
                      <template v-if="!game.me.bankrupt && game.room.status === 'playing'">
                        <button type="button" class="button workbench-action" :disabled="!canTrade && !game.privateData?.trade" aria-label="交易" title="交易" @click="modal = 'trade'"><WorkbenchActionArt action="trade" /></button>
                        <button type="button" class="button workbench-action" data-dice-roll :disabled="!canRoll" :aria-label="game.me?.blueBindTurns ? '跳过移动，进入资产管理' : '掷骰'" :title="game.me?.blueBindTurns ? '蓝银缠绕：跳过移动，仍可管理资产' : '掷骰'" @click="game.act({ type: 'roll' })"><WorkbenchActionArt action="roll" /></button>
                        <button type="button" class="button workbench-action" :disabled="!canEndTurn" aria-label="结束行动" title="结束行动" @click="game.act({ type: 'endTurn' })"><WorkbenchActionArt action="endTurn" /></button>
                      </template>
                    </div>
                  </section>
                  </Teleport>
                </div>
                </FitPanel>
              </template>
            </Board>
          </div></section>
          <aside id="room-dashboard" ref="dashboardView" v-show="detachedDashboard" class="room-dashboard game-board immersive-board" :aria-label="spectator ? '城市新鲜事与资产排行' : '城市新鲜事与聊天'"></aside>
          <div v-if="detachedDashboard && !spectator" id="mobile-card-reveal" class="mobile-card-reveal"></div>
          <nav v-if="detachedDashboard && !spectator" class="mobile-workbench-bar" aria-label="手机游戏操作">
            <button type="button" class="mobile-image-button mobile-more-button" aria-label="更多" aria-haspopup="dialog" :aria-expanded="modal === 'more'" @click="modal = 'more'"><MobileActionArt action="more" /><span v-if="tradeReminder" class="mobile-action-badge">有报价</span></button>
            <button type="button" class="mobile-image-button mobile-roll-button" :disabled="!canRoll" aria-label="掷骰子" @click="game.act({ type: 'roll' })"><MobileActionArt action="roll" /></button>
            <button type="button" class="mobile-image-button mobile-end-button" :disabled="!canEndTurn" aria-label="结束行动" @click="game.act({ type: 'endTurn' })"><MobileActionArt action="endTurn" /></button>
          </nav>
        </div>
      </template>
    </main>
    <Modal v-if="detachedDashboard && !spectator && game.room" :open="modal === 'more'" panel-class="modern-modal mobile-more-modal" title="更多" @close="modal = null">
      <div class="mobile-more-grid">
        <button type="button" class="mobile-image-button" :disabled="!game.me" :aria-label="`我的资产，${owned.length} 处地产`" @click="modal = 'assets'"><MobileActionArt action="assets" /></button>
        <button type="button" class="mobile-image-button" aria-label="资产排行" @click="openMobileWorkbench('ranking')"><MobileActionArt action="ranking" /></button>
        <button type="button" class="mobile-image-button" :disabled="!game.me || game.me.bankrupt || game.room.status !== 'playing' || !canTrade && !game.privateData?.trade" aria-label="交易" @click="modal = 'trade'"><MobileActionArt action="trade" /><span v-if="tradeReminder" class="mobile-action-badge">有报价</span></button>
        <button type="button" class="mobile-image-button" :aria-label="`手牌 ${handCount}`" @click="openMobileWorkbench('cards')"><MobileActionArt action="cards" /><span class="mobile-action-badge">{{ handCount }}</span></button>
      </div>
    </Modal>
    <Modal v-if="game.room && !isLobby" :open="detachedDashboard && modal === 'workbench'" panel-class="modern-modal mobile-workbench-modal" :title="workbenchTab === 'cards' ? '我的手牌' : '资产排行'" @close="modal = null">
      <button type="button" class="button secondary small mobile-more-back" @click="modal = 'more'">← 返回更多</button>
      <div class="modern-room game-board immersive-board mobile-workbench-content"><div class="workbench-center">
        <div id="mobile-workbench-cards" v-show="workbenchTab === 'cards'" role="region" aria-label="我的手牌"></div>
        <div id="mobile-workbench-ranking" v-show="workbenchTab === 'ranking'" role="region" aria-label="资产排行"></div>
      </div></div>
    </Modal>
    <Modal panel-class="modern-modal decision-modal" v-if="modal === 'decision' && game.room && hasCentralDecision && !game.cardReveals.length && !game.settling" :title="decisionTitle"  @close="modal = null"><div class="action-details">
                        <template v-if="game.canDecide">
                          <template v-if="game.room.pending?.type === 'choice'">
                            <p v-if="game.isMyTurn && game.privateData?.bankaiActive && game.room?.phase === 'pre_roll'" class="notice">天锁斩月：剩余 {{ game.privateData.bankaiRemaining }} 次后续出牌，不能使用卍解或投影。</p>
                            <button v-if="game.isMyTurn && game.room?.phase === 'pre_roll' && !game.room?.pending && !game.room?.tradePending && game.privateData?.petalsAvailable" class="button secondary small" :disabled="!game.canAct" @click="game.act({ type: 'senbonzakuraStrike' })">释放千本樱 · 剩余{{ game.privateData.petalsRemaining }}片</button>
                            <button v-if="game.isMyTurn && !game.room?.pending && !game.room?.tradePending && game.room?.board.some(t => t.nzothOwnerId === game.me?.id && t.ownerId === game.me?.id)" class="button secondary small" :disabled="!game.canAct" @click="game.act({ type: 'purifyPollution' })">弃牌净化污染</button>
                            <template v-if="choice"><p class="muted">{{ choice.content?.name ?? '当前选择' }} · {{ choice.type === 'hand_limit' ? '整理手牌后继续行动' : '请打开窗口处理本次选择' }}</p><button class="button primary small" :disabled="!game.canAct" @click="openWorkbench('choice')">处理当前选择</button></template>
                            <p v-else class="muted">正在同步当前选择…</p>
                          </template>
                          <template v-else-if="game.room.phase === 'buy' || game.room.phase === 'upgrade'"><div class="landing-info"><span v-if="pendingTile" class="decision-property-art" :class="`level-${pendingTile?.level ?? 0}`" :style="{ backgroundImage: `url(${imageUrl('/images/board/property-levels.png')})` }" aria-hidden="true"></span><div><strong>{{ pendingTile?.name }}</strong><small>{{ game.room.phase === 'buy' ? '这块地还没有主人' : `你的地产 · 当前 ${pendingTile?.level} 级` }}</small></div></div><p class="decision-price">¥{{ game.room.pending?.amount.toLocaleString() }} <small>{{ game.room.phase === 'buy' ? '购入价格' : '升级费用' }}</small></p><dl v-if="pendingTile && game.me" class="decision-outcome"><div><dt>{{ game.room.phase === 'buy' ? '购买后剩余现金' : '升级后剩余现金' }}</dt><dd>{{ game.me.cash >= (game.room.pending?.amount ?? 0) ? `¥${(game.me.cash - (game.room.pending?.amount ?? 0)).toLocaleString()}` : '现金不足' }}</dd></div><div><dt>{{ game.room.phase === 'buy' ? '当前租金' : '升级后租金' }}</dt><dd>¥{{ rentFor(game.room.phase === 'buy' ? pendingTile : { ...pendingTile, level: pendingTile.level + 1 }, game.room.economy).toLocaleString() }}</dd></div></dl><div class="button-row"><button class="button secondary" :disabled="!canPlay" @click="game.act({ type: 'pass' })">暂时放弃</button><button class="button primary" :disabled="!canPlay || (game.me?.cash ?? 0) < (game.room.pending?.amount ?? 0)" @click="game.act({ type: game.room.phase === 'buy' ? 'buy' : 'upgrade' })">{{ game.room.phase === 'buy' ? '买下这块地' : '升级房子' }}</button></div></template>
                          <template v-else-if="payment">
                            <p class="muted">{{ payment.type === 'rent' ? `${pendingTile?.name} · 路费` : '本次强制费用' }} → <PlayerText :text="creditorName" />。</p>
                            <p class="decision-price">¥{{ payment.amount.toLocaleString() }} <small>剩余待支付</small></p>
                            <p class="muted">现金 ¥{{ game.me?.cash.toLocaleString() }} · 确认后才扣款。</p>
                            <template v-if="!payment.defenseResolved"><button class="button primary small" :disabled="!canPlay" @click="modal = 'defense'">选择防御与付款方式</button></template>
                            <div v-else class="button-row"><button class="button primary" :disabled="!canPlay || (game.me?.cash ?? 0) < payment.amount" @click="game.act({ type: 'payDebt' })">确认现金支付</button></div>
                            <div v-if="payment.defenseResolved" class="button-row"><button class="button secondary" :disabled="!canPlay" @click="openWorkbench('assets')">抵押／变现</button><button v-if="payment.creditorId" class="button secondary" :disabled="!canTrade" @click="openWorkbench('trade')">协商抵债／出售</button></div>
                            <p v-if="!payment.creditorId && (game.me?.cash ?? 0) < payment.amount" class="notice warning">系统费用需自行选择地产抵押或整套出售筹款。</p>
                            <template v-if="payment.defenseResolved && liquidationValue < payment.amount"><p class="muted">银行变现仍不足。{{ payment.creditorId ? '可继续与玩家协商成交，或主动结束协商并清算。' : '自行抵押或出售全部可抵押地产后仍不足，可确认清算。' }}</p><button class="button ghost small danger" :disabled="!canLiquidate" @click="modal = 'bankruptcy'">结束协商并申请清算</button></template>
                          </template>
                        </template>
                        </div></Modal>
    <Modal panel-class="modern-modal assets-modal" v-if="modal === 'assets' && game.me && !spectator" title="我的全部资产" wide @close="modal = null"><button v-if="game.privateData?.remoteBuildAvailable" class="button primary" :disabled="!game.canAct" @click="openRemoteConstruction">基建轮班：七五折远程施工</button><Assets /></Modal>
    <Modal :panel-class="mapTargeting ? 'modern-modal' : ['modern-modal', 'card-use-modal']" v-if="selectedCard" :open="modal === 'card' && (!mapTargeting || mapConfirmOpen)" :title="selectedCard.name ?? ITEMS[selectedCard.kind].name" :wide="!mapTargeting" @close="closeTargetPicker">
      <div v-show="!mapTargeting" class="card-use-layout" :class="{ 'has-targets': drafting && targetKind }">
        <div class="card-use-preview"><ContentCard :content="selectedCard" compact preview /></div>
        <div class="card-use-controls">
          <section v-if="selectedCardCopy" class="card-use-details">
            <div class="card-use-meta"><span>{{ selectedCard.rarity ?? 'R' }} · 道具卡</span><span v-if="selectedCard.cardType">{{ selectedCard.cardType }}</span></div>
            <p v-if="selectedCardCopy.description !== selectedCardCopy.effect" class="card-use-description">{{ selectedCardCopy.description }}</p>
            <p class="card-use-effect">{{ selectedCardCopy.effect }}</p>
            <blockquote v-if="selectedCard.quote" class="card-use-quote">“{{ selectedCard.quote }}”</blockquote>
          </section>
          <p v-if="!drafting" class="notice">{{ ['immunity', 'halfPayment', 'release'].includes(selectedCard.kind) ? '这张卡在付款防御或解除限制时使用。' : '当前不能主动使用，可以查看卡牌说明。' }}</p>
          <section v-if="drafting && targetKind" class="card-use-targets">
            <h3>选择{{ targetKind === 'player' ? '玩家' : '地块' }}</h3><p class="muted">{{ targetHint }}</p>
            <CardTargetPicker :key="selectedCard.id" :options="dialogTargets" :selected="selectedTarget" :second-selected="selectedSecondTarget" :player-target="targetKind === 'player'" :two-players="selectedCard.kind === 'swapAny'" :me-id="game.me?.id" :disabled="!game.canAct" @select="selectDialogTarget" @map="switchTargetMode(true)" />
          </section>
          <p v-if="game.connection !== 'online'" class="notice warning">连接中断，恢复连接后才能确认。</p>

        </div>
      </div>
      <template #footer>
        <button v-if="mapTargeting" type="button" class="button secondary small target-back-list" @click="switchTargetMode(true)">← 重新点选</button>
        <CardAction :key="selectedCard.id" :card="selectedCard" :target="selectedTarget" :second-target="selectedSecondTarget" :target-label="targetLabel" :has-candidates="!!(eligibleTiles.length || eligiblePlayers.length)" :readonly="!drafting" compact dialog @submitting="submittingCard = $event" @done="cardUsed" @cancel="clearCard" />
      </template>
    </Modal>
    <Modal :panel-class="['modern-modal', 'choice-use-modal']" v-if="choice" :open="modal === 'choice' && !game.settling && !game.cardReveals.length && (!mapTargeting || mapConfirmOpen)" :title="choice.type === 'opening_roll' ? '开局自选骰点' : choice.content?.name ?? '处理当前选择'" :wide="!mapTargeting" @close="closeTargetPicker">
      <p v-if="mapChoice && !mapTargeting" class="map-target-instructions">{{ targetHint }}</p>
      <CardTargetPicker v-if="mapChoice" v-show="!mapTargeting" :key="choice.id" :options="dialogTargets" :selected="selectedTarget" :me-id="game.me?.id" :disabled="!game.canAct" @select="selectDialogTarget" @map="switchTargetMode(true)" />
      <p v-if="game.connection !== 'online'" class="notice warning">连接中断，恢复后继续本次选择。</p>
      <ContentChoice v-if="!mapChoice" :key="choice.id" :choice="choice" :tile-id="targetTileId" :target-label="targetLabel" dialog @done="modal = null" />
      <template v-if="mapChoice" #footer>
        <button v-if="mapTargeting" type="button" class="button secondary small target-back-list" @click="switchTargetMode(true)">← 重新点选</button>
        <ContentChoice :key="choice.id" :choice="choice" :tile-id="targetTileId" :target-label="targetLabel" compact dialog @done="modal = null" />
      </template>
    </Modal>
    <Modal :panel-class="['modern-modal', 'defense-use-modal']" v-if="payment" :open="modal === 'defense' && !game.settling && !game.cardReveals.length" title="本次付款防御" @close="modal = null"><p class="defense-amount">待支付 ¥{{ payment.amount.toLocaleString() }} → <PlayerText :text="creditorName" /></p><template v-if="soulRingAvailable"><p class="notice">十万年魂环：剩余 {{ game.privateData?.soulRingRemaining }} 次免租，剩余 {{ game.privateData?.soulRingTurns }} 个本人回合。可以免除本笔最终租金，也可以保留次数。</p><button class="button primary full" :disabled="!canPlay" @click="game.act({ type: 'paymentDefense', ...(payment.id ? { decisionId: payment.id } : {}), useSoulRing: true })">使用十万年魂环，免除本笔租金</button></template><label class="field">选择防御卡<ThemedSelect v-model="selectedDefense" label="选择防御卡" :options="defenseOptions" /></label><ContentCard v-if="defenseCards.find(card => card.id === selectedDefense)" :content="defenseCards.find(card => card.id === selectedDefense)!" /><p class="muted">本笔至多使用一张防御卡，不占主动次数。</p><button class="button primary full" :disabled="!canPlay" @click="game.act({ type: 'paymentDefense', ...(payment.id ? { decisionId: payment.id } : {}), ...(selectedDefense ? { cardId: selectedDefense } : {}) })">{{ selectedDefense ? '确认使用防御卡' : '不使用道具，继续付款' }}</button></Modal>
    <Modal panel-class="modern-modal game-tools-modal" v-if="isGameSession && modal === 'tools'" title="小镇工具箱" @close="modal = null">
      <div class="game-tools-grid">
        <button v-if="game.me && !spectator" class="game-tool peach" @click="modal = 'avatar'"><span class="game-tool-icon" aria-hidden="true"><CharacterPawn :avatar-id="game.me.avatarId" :color="game.me.color" /></span><span>选择角色</span></button>
        <button v-if="game.me && !spectator" class="game-tool mint" :disabled="!canRename" @click="openRename"><span class="game-tool-icon" aria-hidden="true">✏️</span><span>修改昵称</span></button>
        <button class="game-tool mint" @click="modal = 'rules'"><span class="game-tool-icon" aria-hidden="true">📖</span><span>玩法说明</span></button>
        <button class="game-tool sky" @click="modal = null; spectator ? switchSpectatorView('landscape') : fullscreen()"><span class="game-tool-icon" aria-hidden="true">⛶</span><span>{{ spectator ? '横屏模式' : '全屏模式' }}</span></button>
        <button v-if="game.me && !spectator" class="game-tool lavender" @click="modal = 'save'"><span class="game-tool-icon" aria-hidden="true">💾</span><span>游戏存档</span></button>
        <button class="game-tool peach" @click="inviteFriends"><span class="game-tool-icon" aria-hidden="true">💌</span><span>邀请朋友</span></button>
        <button class="game-tool mint" @click="modal = null; game.copy(`${origin}/watch/${code}`)"><span class="game-tool-icon" aria-hidden="true">🔗</span><span>复制观战链接</span></button>
      </div>
    </Modal>
    <Modal panel-class="modern-modal" v-if="modal === 'rules'" title="大富翁玩法说明" wide @close="modal = null"><Rules :economy="game.room?.economy" :player-count="game.room?.players.length ?? 2" :spectator="spectator" :chance-events="game.room?.chanceEventDescriptions" :activity-money="game.room?.activityRotation?.money" /></Modal>
    <Modal panel-class="modern-modal" v-if="modal === 'save' && game.me && !spectator" title="标记当前存档" @close="modal = null"><SaveArchive /></Modal>




    <Modal panel-class="modern-modal" v-if="game.me && !spectator" :open="modal === 'trade'" title="交易报价与成交后结果" wide @close="modal = null; tradeEditorVersion++"><TradeEditor ref="tradeEditor" :key="`${game.room?.roomId}:${game.room?.gameId}:${game.me.id}:${tradeEditorVersion}`" :offer-disabled="drafting" @done="modal = null; if ($event) tradeEditorVersion++" /></Modal>
    <Modal panel-class="modern-modal" v-if="modal === 'avatar' && game.me && !spectator" title="选择角色形象" @close="modal = null"><AvatarPicker /></Modal>
    <Modal panel-class="modern-modal nickname-modal" v-if="modal === 'rename' && game.me && !spectator" title="修改昵称" :close-disabled="game.busy" @close="modal = null">
      <form class="nickname-form" @submit.prevent="saveNickname">
        <div class="nickname-player">
          <CharacterPawn :avatar-id="game.me.avatarId" :color="game.me.color" />
          <div class="nickname-identity"><small>当前昵称</small><strong>{{ game.me.name }}</strong></div>
        </div>
        <label class="field nickname-field" for="player-nickname">换个新名字</label>
        <input id="player-nickname" v-model="nickname" class="nickname-input" placeholder="输入你喜欢的昵称" autocomplete="nickname" aria-describedby="nickname-hint" :disabled="game.busy" />
        <p id="nickname-hint" class="nickname-hint">想用默认名字？留空后保存就好。</p>
        <div class="button-row nickname-actions">
          <button type="button" class="button secondary" :disabled="game.busy" @click="modal = null">取消</button>
          <button type="submit" class="button primary" :disabled="!canRename">{{ game.busy ? '正在保存…' : '保存昵称' }}</button>
        </div>
      </form>
    </Modal>
    <Modal panel-class="modern-modal" v-if="modal === 'bankruptcy' && payment && !spectator" title="结束偿债并清算" @close="modal = null"><p>待支付 ¥{{ payment.amount.toLocaleString() }}，现金加银行可变现资产 ¥{{ liquidationValue.toLocaleString() }}。</p><p class="notice warning">确认后全部地产归还银行，你退出本局竞争。<PlayerText :text="payment.creditorId ? `银行变现所得用于向 ${creditorName} 偿债。` : '剩余现金用于向银行偿债。'" /></p><div class="button-row"><button class="button secondary" @click="modal = null">继续筹款协商</button><button class="button danger" :disabled="!canLiquidate" @click="game.act({ type: 'declareBankruptcy' }).then(ok => { if (ok) modal = null; })">确认清算并退出竞争</button></div></Modal>
    <Modal panel-class="modern-modal tile-detail-modal" v-if="liveSelectedTile" :title="`${liveSelectedTile.name}${hasPropertyEffects(liveSelectedTile, game.room?.constructionEffects) ? ' ✦' : ''}`" @close="selectedTile = null">
      <div class="tile-detail tile-detail-card">
        <div class="tile-detail-hero" :class="`district-${liveSelectedTile.district}`">
          <div class="tile-detail-art" :class="[liveSelectedTile.type, `level-${liveSelectedTile.level}`, { mortgaged: liveSelectedTile.mortgaged }]" :style="{ '--detail-property-art': `url(${imageUrl('/images/board/property-levels.png')})` }" aria-hidden="true">
            <img v-if="liveSelectedTile.type !== 'property'" :src="liveSelectedTile.type === 'activity' && liveSelectedTile.activityKind ? activityArt(`${liveSelectedTile.activityKind}-tile`) : imageUrl(`/images/tiles/${liveSelectedTile.type}.png`)" alt="" />
          </div>
          <div class="tile-detail-intro">
            <p>{{ liveSelectedTile.name }}{{ hasPropertyEffects(liveSelectedTile, game.room?.constructionEffects) ? ' ✦' : '' }}</p>
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
            <div><dt>当前路费</dt><dd>{{ selectedTileOwner?.jailed && selectedTileOwner.skipTurns > 0 ? '¥0 · 房东服刑中' : `¥${rentFor(liveSelectedTile, game.room?.economy).toLocaleString()}` }}</dd></div>
          </dl>
          <dl class="tile-detail-facts">
            <div><dt>抵押状态</dt><dd :class="{ 'tile-detail-mortgaged': liveSelectedTile.mortgaged }">{{ liveSelectedTile.mortgaged ? `已抵押 · 赎回 ¥${redeemCost(liveSelectedTile).toLocaleString()}` : '未抵押' }}</dd></div>
          </dl>
          <div v-if="!spectator && game.me && selectedTileOwner" class="tile-detail-actions">
            <button v-if="liveSelectedTile.ownerId !== game.me.id" type="button" class="button primary" :disabled="!canTrade || selectedTileOwner.bankrupt || isKamui(liveSelectedTile)" @click="tradeSelectedProperty">交易此房产</button>
            <button v-else type="button" class="button primary" :disabled="!canManageSelectedProperty" @click="manageSelectedProperty">{{ liveSelectedTile.mortgaged ? `赎回此房产 −¥${redeemCost(liveSelectedTile).toLocaleString()}` : `抵押此房产 +¥${mortgageValue(liveSelectedTile, game.room?.economy).toLocaleString()}` }}</button>
          </div>
        </template>
        <p v-else-if="liveSelectedTile.type === 'activity' && liveSelectedTile.activityKind" class="tile-detail-description">{{ ACTIVITY_HINTS[liveSelectedTile.activityKind] }}。停在这里触发活动，经过不触发。</p>
        <p v-else-if="liveSelectedTile.type === 'start' || liveSelectedTile.type === 'jail'" class="tile-detail-description">{{ liveSelectedTile.type === 'start' ? `正向经过或最终停在起点，获得 ¥${ruleMoney(game.room?.economy, 'startIncome', 1000).toLocaleString()} 和 ${startCards(game.room?.players.length ?? 2)} 张道具补给；后退经过起点不领奖，后退停在起点可领奖一次。` : '停在此处，跳过下一次个人行动；服刑期间不收租，其他小游戏正常参与，可当场用解控卡解除限制。' }}</p>
        <PropertyEffects :tile="liveSelectedTile" :economy="game.room?.economy" />
      </div>
    </Modal>
    <footer v-if="!isLobby" class="room-footer">小小棋盘，大大野心。<span>好运和朋友，都在这里。</span></footer>
  </div>
</template>

<style scoped>
.tile-detail-actions { margin-top: 12px; }
.tile-detail-actions > .button { width: 100%; }
</style>
