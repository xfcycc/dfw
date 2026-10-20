<script setup lang="ts">
import { imageUrl } from '../image-assets';
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { boardDimensions, DISTRICTS, districtName, hasPropertyEffects, rentFor, ruleMoney, tileCoordinates as standardTileCoordinates } from '../../shared/rules';
import type { ActivityKind, PublicPlayer, PublicRoomState, Tile, Trade } from '../../shared/types';
import City from './City.vue';
import CharacterPawn from './CharacterPawn.vue';
import DiceFace from './DiceFace.vue';
import DesktopDiceThrow from './DesktopDiceThrow.vue';
import BoardEffectLayer from './BoardEffectLayer.vue';
import PlayerText from './PlayerText.vue';
import PropertyGrade from './PropertyGrade.vue';
import PanelFrame from './PanelFrame.vue';
import { activityArt } from '../activity-art';
import { playSound } from '../audio';
import '../activities.css';
import { useGameEffects } from '../composables/useGameEffects';
const props = defineProps<{ room: PublicRoomState; classicLayout?: boolean; dashboardTarget?: string; statusTarget?: string; diceTarget?: string; desktopDice?: boolean; spectator?: boolean; spectatorFollow?: boolean; compact?: boolean; bounded?: boolean; decision?: boolean; stretch?: boolean; meId?: string; incomingTrade?: Trade | null; targeting?: boolean; targetHint?: string; eligibleTiles?: number[]; selectedTarget?: number | null; live?: boolean; presentationEpoch?: number }>();
const emit = defineEmits<{ select: [tile: Tile]; animating: [active: boolean]; settled: [id: string]; view: [mode: 'follow' | 'landscape'] }>();
const container = ref<HTMLElement>();
const viewport = ref<HTMLElement>();
const feedbackArea = ref<HTMLElement>();
const size = ref(700);
const availableWidth = ref(700);
const availableViewportHeight = ref(0);
const zoom = ref(1);
const fullMap = ref(false);
const referencedTile = ref<number | null>(null);
const following = computed(() => !!props.spectator && !!props.spectatorFollow);
const { money, tiles: tileEffects, rolling, celebrating, motion, boardEffect, finishBoardEffect, animatedPosition, moving, landing, step, diceFace, diceResult, diceRollMs, moveStepMs } = useGameEffects(() => props.room, () => !!props.live, id => { emit('settled', id); }, () => props.meId, () => props.presentationEpoch ?? 0, () => !!props.desktopDice);
let observer: ResizeObserver | undefined;
let lastWidth = 0;
let lastHeight = 0;
let lastLength = 0;
let lastViewportHeight = 0;
let followFrame = 0;
const districts = computed(() => DISTRICTS.map((fallback, side) => { const tile = props.room.board.find(tile => tile.district === side); return tile ? districtName(tile) : fallback; }));
const startIncome = computed(() => ruleMoney(props.room.economy, 'startIncome', 1000));
const desktopSingleScreen = computed(() => !!props.stretch && !props.classicLayout && !props.spectator && !props.dashboardTarget);
/** 输入地图格数和可用宽高，桌面按长方形排布闭环，返回行列数；不改变格号顺序。 */
const dimensions = computed(() => {
  const standard = boardDimensions(props.room.board.length);
  if (!desktopSingleScreen.value || !availableViewportHeight.value) return standard;
  const total = props.room.board.length / 2 + 2;
  // 低高度窗口让外圈地块更扁，给内圈卡牌和排行保留空间；仍保持至少约 50px 的格高。
  const compactRows = availableViewportHeight.value < 620 ? Math.floor(availableViewportHeight.value / 50) : 0;
  const rows = Math.max(7, Math.min(standard.rows, Math.max(compactRows, Math.ceil(total * availableViewportHeight.value / (availableWidth.value + availableViewportHeight.value)))));
  return { rows, columns: total - rows };
});
/** 输入格号和总格数，返回当前布局中的顺时针网格坐标，供地块、棋子和特效共用。 */
function tileCoordinates(index: number, length: number): { row: number; column: number } {
  if (!desktopSingleScreen.value) return standardTileCoordinates(index, length);
  const horizontal = dimensions.value.columns - 1; const vertical = dimensions.value.rows - 1;
  if (index <= horizontal) return { row: 1, column: index + 1 };
  if (index <= horizontal + vertical) return { row: index - horizontal + 1, column: horizontal + 1 };
  if (index <= horizontal * 2 + vertical) return { row: vertical + 1, column: horizontal * 2 + vertical - index + 1 };
  return { row: length - index + 1, column: 1 };
}
/** 输入本人收到的待处理报价，返回接收方将获得和失去的地产标记；观战与其他玩家不显示。 */
const tradeMarkers = computed(() => {
  const trade = props.incomingTrade;
  if (props.spectator || !props.room.tradePending || !trade || trade.toId !== props.meId) return [];
  return [...trade.giveTiles.map(id => ({ id, gaining: true, label: '将收到' })), ...trade.wantTiles.map(id => ({ id, gaining: false, label: '将失去' }))];
});
const height = computed(() => { if (desktopSingleScreen.value && availableViewportHeight.value) return Math.max(1, availableViewportHeight.value - 2); if (props.spectator && props.compact && !following.value && availableViewportHeight.value) return Math.max(1, availableViewportHeight.value - 10); const inset = props.compact ? 10 : 9; const gap = props.compact ? 4 : 3; return (size.value - inset * 2 + gap) / dimensions.value.columns * dimensions.value.rows + inset * 2 - gap; });
const current = computed(() => props.room.players.find(player => player.id === (props.room.status === 'finished' ? props.room.winnerId : props.room.turnOrder[props.room.turnIndex])));
const diceFullscreen = computed(() => !!motion.value && diceResult.value !== null && !moving.value && !landing.value);
const motionPlayer = computed(() => props.room.players.find(player => player.id === motion.value?.playerId));
const followedPlayer = computed(() => motionPlayer.value ?? current.value);
const visitedTiles = computed(() => new Set(motion.value?.path.slice(0, step.value) ?? []));
const movementLabel = computed(() => motion.value?.teleport ? `直接传送至 ${props.room.board[motion.value.path.at(-1) ?? motion.value.from]?.name ?? '目的地'}` : rolling.value ? '正在掷骰…' : landing.value ? `到达 ${props.room.board[animatedPosition.value?.position ?? 0]?.name ?? '目的地'}` : moving.value ? `正在移动 · ${step.value} / ${motion.value?.path.length ?? 0} 格` : diceResult.value !== null ? `掷出 ${diceResult.value} 点` : '准备移动…');
const effectTiles = computed(() => new Set(boardEffect.value?.boardEffect?.beforeTiles.map(tile => tile.id) ?? []));
const effectPlayers = computed(() => new Set(boardEffect.value?.boardEffect?.moves.map(move => move.playerId) ?? []));
const effectTileSize = computed(() => (size.value - (props.compact ? 20 : 18)) / dimensions.value.columns * zoom.value);
/** 输入地块编号，返回与棋子一致的缩放后中心坐标，供演出定位。 */
function effectPoint(id: number): { x: number; y: number } {
  const cell = tileCoordinates(id, props.room.board.length);
  const inset = props.compact ? 10 : 9; const gap = props.compact ? 4 : 3;
  return { x: (inset + (cell.column - .5) * (size.value - inset * 2 + gap) / dimensions.value.columns) * zoom.value,
    y: (inset + (cell.row - .5) * (height.value - inset * 2 + gap) / dimensions.value.rows) * zoom.value };
}
/** 输入玩家、格号及是否取换位终点，返回含同格错位的棋子坐标，仅供演出展示。 */
function effectPlayerPoint(playerId: string, position: number, after = false): { x: number; y: number } {
  const effect = boardEffect.value?.boardEffect;
  const together = props.room.players.filter(player => !player.bankrupt && (after ? effect?.moves.find(move => move.playerId === player.id)?.to ?? player.position : player.position) === position);
  const offset = (together.findIndex(player => player.id === playerId) - (together.length - 1) / 2) * 16;
  const point = effectPoint(position);
  return { x: point.x + offset, y: point.y };
}
// 效果沿用当前缩放与视野，不在播放前后强制切换全图或恢复地图位置。
const rotating = ref(false);
const displayedActivityKinds = ref<Record<number, ActivityKind>>({});
let rotationTimer: ReturnType<typeof setTimeout> | undefined;
let artSwapTimer: ReturnType<typeof setTimeout> | undefined;
let rotationBaseline = true;
const turnsUntilRotation = computed(() => props.room.activityRotation ? props.room.activityRotation.period - props.room.activityRotation.completedTurns % props.room.activityRotation.period : 0);
/** 输入房间，提取两个活动角当前图案用于翻转中点替换。 */
function activityKinds(room: PublicRoomState): Record<number, ActivityKind> { return Object.fromEntries(room.board.filter(t => t.activityKind).map(t => [t.id, t.activityKind! ])); }
watch(() => props.live, live => { if (!live) { rotationBaseline = true; rotating.value = false; clearTimeout(rotationTimer); clearTimeout(artSwapTimer); } });
/** 输入房间及本地重置标记，同步时取消旧轮转，其余按轮转版本播放动画，无返回值。 */
watch([() => props.room, () => props.presentationEpoch], ([next, epoch], [previous, previousEpoch]) => {
  const fresh = activityKinds(next);
  if (epoch !== previousEpoch) { rotating.value = false; clearTimeout(rotationTimer); clearTimeout(artSwapTimer); displayedActivityKinds.value = fresh; rotationBaseline = !props.live; return; }
  if (rotationBaseline || !previous || !props.live || next.recoveryPending || next.gameId !== previous.gameId || document.hidden) { displayedActivityKinds.value = fresh; rotationBaseline = !props.live; return; }
  if (next.activityRotation?.revision === previous.activityRotation?.revision) { if (!rotating.value) displayedActivityKinds.value = fresh; return; }
  rotating.value = true; playSound('rotation_switch'); clearTimeout(rotationTimer); clearTimeout(artSwapTimer);
  artSwapTimer = setTimeout(() => { displayedActivityKinds.value = fresh; }, 600);
  rotationTimer = setTimeout(() => { rotating.value = false; }, 1200);
}, { immediate: true });
watch(turnsUntilRotation, (next, previous) => { if (previous && next !== previous && next >= 1 && next <= 3 && props.live && !props.room.recoveryPending && !rotationBaseline) playSound('rotation_count'); });
onBeforeUnmount(() => { clearTimeout(rotationTimer); clearTimeout(artSwapTimer); });
watch([motion, () => rotating.value], ([value, rotation]) => { emit('animating', !!value || !!rotation); }, { flush: 'sync' });
// 操作面板置于未缩放的内圈，保持手机端文字和按钮可读。
const centerStyle = computed(() => {
  const inset = props.compact ? 10 : 9;
  const gap = props.compact ? 4 : 3;
  const cellWidth = (size.value - inset * 2 + gap) / dimensions.value.columns;
  const cellHeight = (height.value - inset * 2 + gap) / dimensions.value.rows;
  if (!props.compact) return { left: `${(inset + cellWidth) * zoom.value}px`, top: `${(inset + cellHeight) * zoom.value}px`, width: `${(dimensions.value.columns - 2) * cellWidth - gap}px`, height: `${(dimensions.value.rows - 2) * cellHeight - gap}px`, transform: `scale(${zoom.value})`, transformOrigin: 'top left' };
  const panelWidth = props.stretch && availableWidth.value < 760 ? Math.min(((dimensions.value.columns - 2) * cellWidth - gap) * zoom.value, availableWidth.value - 24) : ((dimensions.value.columns - 2) * cellWidth - gap) * zoom.value;
  return {
    left: `${(size.value * zoom.value - panelWidth) / 2}px`, top: `${(inset + cellHeight) * zoom.value}px`,
    width: `${panelWidth}px`,
    height: `${((dimensions.value.rows - 2) * cellHeight - gap) * zoom.value}px`,
  };
});
const icons = { activity: '✦', start: '➜', chance: '?', fortune: '✦', jail: 'Ⅱ', property: '⛺' };
/** 输入地块，返回其所有者的显示颜色。 */
function ownerColor(tile: Tile): string | undefined { return tileOwner(tile)?.color; }
/** 输入地块，按拥有者底色的相对亮度返回对比度更高的名牌文字颜色。 */
function ownerTextColor(tile: Tile): string {
  const color = ownerColor(tile) ?? '#8490a4';
  const [red, green, blue] = [1, 3, 5].map(offset => {
    const channel = parseInt(color.slice(offset, offset + 2), 16) / 255;
    return channel <= .04045 ? channel / 12.92 : ((channel + .055) / 1.055) ** 2.4;
  });
  const luminance = .2126 * red! + .7152 * green! + .0722 * blue!;
  return (luminance + .05) / .05 >= 1.05 / (luminance + .05) ? '#000' : '#fff';
}
/** 输入地块，返回其拥有者，未售出地块返回空。 */
function tileOwner(tile: Tile): PublicPlayer | undefined { return props.room.players.find(player => player.id === tile.ownerId); }
/** 输入地块索引，返回当前可见的棋子列表。 */
function occupants(id: number) {
  return props.room.players.filter(player => !player.bankrupt && (animatedPosition.value?.id === player.id ? animatedPosition.value.position : player.position) === id);
}
/** 输入公开玩家，返回不随地图文字缩小的小人位置；同格角色略错开显示。 */
function pawnStyle(player: PublicPlayer) {
  const position = animatedPosition.value?.id === player.id ? animatedPosition.value.position : player.position;
  const cell = tileCoordinates(position, props.room.board.length);
  const together = occupants(position); const offset = (together.findIndex(p => p.id === player.id) - (together.length - 1) / 2) * 16;
  const inset = props.compact ? 10 : 9; const gap = props.compact ? 4 : 3;
  return { left: `${(inset + (cell.column - 0.5) * (size.value - inset * 2 + gap) / dimensions.value.columns) * zoom.value + offset}px`, top: `${(inset + (cell.row - 0.5) * (height.value - inset * 2 + gap) / dimensions.value.rows) * zoom.value}px`, '--pawn-color': player.color };
}
/** 输入报价地产编号，返回内圈侧的箭头坐标、朝向及文字偏移，箭头向外指向对应地块。 */
function tradeMarkerStyle(id: number) {
  const cell = tileCoordinates(id, props.room.board.length);
  const inset = props.compact ? 10 : 9; const gap = props.compact ? 4 : 3;
  const cellWidth = (size.value - inset * 2 + gap) / dimensions.value.columns;
  const cellHeight = (height.value - inset * 2 + gap) / dimensions.value.rows;
  const dx = cell.row === 1 || cell.row === dimensions.value.rows ? 0 : cell.column === 1 ? -1 : 1;
  const dy = dx ? 0 : cell.row === 1 ? -1 : 1;
  const offset = (dx ? cellWidth : cellHeight) / 2 * zoom.value + 18;
  return {
    left: `${(inset + (cell.column - 0.5) * cellWidth) * zoom.value - dx * offset}px`,
    top: `${(inset + (cell.row - 0.5) * cellHeight) * zoom.value - dy * offset}px`,
    '--trade-arrow-angle': `${Math.atan2(dy, dx) * 180 / Math.PI - 90}deg`,
    '--trade-label-x': `${-dx * 44}px`, '--trade-label-y': `${-dy * 38}px`,
  };
}
/** 无入参，读取地图可用尺寸；桌面玩家以原字号铺满单屏，其他模式选点保留当前视野，无返回值。 */
function resizeBoard(): void {
  const width = container.value?.clientWidth ?? 700;
  const availableHeight = viewport.value?.clientHeight ?? 0;
  if (width <= 0) return;
  if (width === lastWidth && availableHeight === lastViewportHeight && window.innerHeight === lastHeight && props.room.board.length === lastLength) return;
  availableWidth.value = width;
  availableViewportHeight.value = availableHeight;
  lastWidth = width; lastHeight = window.innerHeight; lastLength = props.room.board.length;
  lastViewportHeight = availableHeight;
  if (desktopSingleScreen.value) size.value = Math.max(1, width - 2);
  else if (props.spectator && props.compact && !following.value) size.value = Math.max(1, width - 6);
  else if (props.stretch) size.value = Math.max(dimensions.value.columns * 112 + 16, width - 6);
  else size.value = props.compact ? Math.max(760, dimensions.value.columns * 64, Math.min(1280, width)) : Math.max(560, Math.min(900, width, window.innerHeight - 280));
  if (desktopSingleScreen.value) { zoom.value = 1; fullMap.value = true; return; }
  if (props.targeting) return;
  if (props.spectator && following.value) { fullMap.value = false; zoom.value = 1; }
  else if (props.spectator || fullMap.value || !props.stretch) fitBoard();
  else expandBoard();
}
/** 输入待定位玩家，默认使用当前移动者；恢复可读格距并调整局部视野，无返回值。 */
function expandBoard(player = followedPlayer.value): void {
  fullMap.value = false; zoom.value = 1;
  void nextTick(() => {
    cancelAnimationFrame(followFrame);
    if (props.dashboardTarget) locatePlayer(player, true);
    else if (viewport.value) viewport.value.scrollLeft = Math.max(0, (size.value - availableWidth.value) / 2);
  });
}
/** 输入待定位的玩家与是否平滑滚动，将本地地图视野移到其动画位置，无返回值。 */
function locatePlayer(player?: PublicPlayer, smooth = false): void {
  if (!player || !viewport.value) return;
  const position = animatedPosition.value?.id === player.id ? animatedPosition.value.position : player.position;
  const cell = tileCoordinates(position, props.room.board.length);
  const inset = props.compact ? 10 : 9; const gap = props.compact ? 4 : 3;
  viewport.value.scrollTo({ left: (inset + (cell.column - 0.5) * (size.value - inset * 2 + gap) / dimensions.value.columns) * zoom.value - viewport.value.clientWidth / 2, top: (inset + (cell.row - 0.5) * (height.value - inset * 2 + gap) / dimensions.value.rows) * zoom.value - viewport.value.clientHeight / 2, behavior: smooth ? 'smooth' : 'instant' });
}
/** 无入参，定位当前行动者；跟随时使用动画中的角色，避免提前跳到下一回合，无返回值。 */
function locateCurrent(): void { cancelAnimationFrame(followFrame); locatePlayer(followedPlayer.value, true); }
/** 输入地块编号及手机标志，强调对应地块；手机恢复可读缩放后将视窗移到地块，无返回值。 */
async function referenceTile(id: number, mobile: boolean): Promise<void> {
  if (!props.room.board.some(tile => tile.id === id)) return;
  cancelAnimationFrame(followFrame);
  referencedTile.value = id;
  if (mobile) { fullMap.value = false; zoom.value = 1; await nextTick(); }
  const view = viewport.value; const tile = view?.querySelector<HTMLElement>(`[data-cash-tile="${id}"]`);
  if (!view || !tile) return;
  const bounds = view.getBoundingClientRect(); const target = tile.getBoundingClientRect();
  const outside = target.left < bounds.left || target.right > bounds.right || target.top < bounds.top || target.bottom > bounds.bottom;
  if (mobile || outside) view.scrollTo({ left: view.scrollLeft + target.left + target.width / 2 - bounds.left - view.clientLeft - view.clientWidth / 2, top: view.scrollTop + target.top + target.height / 2 - bounds.top - view.clientTop - view.clientHeight / 2, behavior: 'smooth' });
}
/** 输入页面点击事件，点击引用与当前强调地块之外的位置时取消强调，无返回值。 */
function clearReference(event: MouseEvent): void {
  if (!(event.target instanceof Element) || event.target.closest('.chat-tile-reference') || event.target.closest(`[data-cash-tile="${referencedTile.value}"]`)) return;
  referencedTile.value = null;
}
defineExpose({ referenceTile });
/** 无入参，观战跟随与玩家局部地图按棋子实际位置平滑追踪，点选、全图及特效期间暂停，无返回值。 */
function followPawn(): void {
  cancelAnimationFrame(followFrame);
  if (boardEffect.value || props.targeting || fullMap.value || !(following.value || !props.spectator && props.dashboardTarget) || !viewport.value || referencedTile.value != null) return;
  const view = viewport.value;
  const pawn = Array.from(view.querySelectorAll<HTMLElement>('.map-character')).find(element => element.dataset.playerId === followedPlayer.value?.id);
  if (!pawn) return;
  let cameraLeft = view.scrollLeft; let cameraTop = view.scrollTop;
  let previousTime = performance.now();
  const until = previousTime + moveStepMs + 120;
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  /** 输入帧时间，按时间间隔渐近棋子中心，走格结束或切换行动者后继续收敛镜头，无返回值。 */
  const frame = (now: number): void => {
    const bounds = view.getBoundingClientRect();
    const position = pawn.getBoundingClientRect();
    const left = Math.max(0, Math.min(view.scrollWidth - view.clientWidth, view.scrollLeft + position.left + position.width / 2 - bounds.left - view.clientLeft - view.clientWidth / 2));
    const top = Math.max(0, Math.min(view.scrollHeight - view.clientHeight, view.scrollTop + position.top + position.height / 2 - bounds.top - view.clientTop - view.clientHeight / 2));
    const dx = left - cameraLeft; const dy = top - cameraTop;
    const blend = reducedMotion ? 1 : 1 - Math.exp(-(now - previousTime) / 80);
    previousTime = now;
    cameraLeft += dx * blend; cameraTop += dy * blend;
    view.scrollTo({ left: cameraLeft, top: cameraTop, behavior: 'instant' });
    if (now < until || Math.abs(dx) > 1 || Math.abs(dy) > 1) followFrame = requestAnimationFrame(frame);
  };
  followFrame = requestAnimationFrame(frame);
}
/** 无入参，恢复局部地图并定位本人附近，无返回值。 */
function locateMe(): void { expandBoard(props.room.players.find(player => player.id === props.meId)); }
/** 无入参，按地图视窗的宽高完整展示棋盘，无返回值。 */
function fitBoard(): void {
  fullMap.value = true;
  const detached = !!props.dashboardTarget || !!props.spectator && !!props.compact;
  const heightLimit = detached || props.bounded ? Math.max(1, (viewport.value?.clientHeight ?? height.value) - 10) / height.value : props.stretch ? Math.max(240, window.innerHeight - Math.max(0, container.value?.getBoundingClientRect().top ?? 0) - 24) / height.value : 1;
  zoom.value = Math.max(detached ? 0.01 : props.bounded ? 0.15 : 0.3, Math.min(1, heightLimit, ((container.value?.clientWidth ?? size.value) - 6) / size.value));
  void nextTick(() => viewport.value?.scrollTo({ left: 0, top: 0, behavior: 'instant' }));
}
// 输入布局、观战模式与地图规模变化，重新测量可用区域。
watch([() => props.classicLayout, () => props.dashboardTarget, () => props.stretch, () => props.spectator, () => props.room.board.length, following], async () => {
  fullMap.value = false; lastWidth = 0;
  await nextTick(); resizeBoard();
}, { flush: 'post' });
watch([following, () => props.spectator, () => props.dashboardTarget, () => props.targeting, fullMap, referencedTile, boardEffect, () => followedPlayer.value?.id, () => followedPlayer.value?.position, () => animatedPosition.value?.position, moving, zoom, size, height, availableWidth, availableViewportHeight], followPawn, { flush: 'post' });
watch([() => props.room.roomId, () => props.room.gameId], () => { referencedTile.value = null; });
onMounted(() => {
  observer = new ResizeObserver(resizeBoard);
  if (container.value) observer.observe(container.value);
  if (viewport.value) observer.observe(viewport.value);
  window.addEventListener('resize', resizeBoard);
  document.addEventListener('click', clearReference);
});
onBeforeUnmount(() => { cancelAnimationFrame(followFrame); observer?.disconnect(); window.removeEventListener('resize', resizeBoard); document.removeEventListener('click', clearReference); emit('animating', false); });
</script>
<template>
  <div ref="container" class="board-container" :class="{ 'game-board': compact, 'dense-board': compact && dimensions.columns >= 14, 'immersive-board': stretch, 'detached-board': !!dashboardTarget }" :style="{ '--map-zoom': zoom, '--town-background': `url(${imageUrl('/images/board/town-background.png')})`, '--tile-background': `url(${imageUrl('/images/board/tile-common.png')})`, '--property-levels': `url(${imageUrl('/images/board/property-levels.png')})`, '--turn-arrow': `url(${imageUrl('/images/board/turn-arrow.svg')})` }">
    <div v-if="!stretch || dashboardTarget || spectator" class="board-tools"><span class="eyebrow">{{ room.board.length }} 格城市 <span class="board-direction">· 顺时针前进 ↻</span></span><div class="board-controls"><button v-if="compact && !spectator" class="board-control" :aria-pressed="fullMap" @click="fitBoard">全图</button><button v-if="spectator && compact" class="board-control" :aria-pressed="following" @click="emit('view', 'follow')">跟随模式</button><button v-if="dashboardTarget && !spectator && meId" class="board-control" :aria-pressed="!fullMap" @click="locateMe">我的附近</button><button v-if="compact" class="board-control" @click="locateCurrent">定位棋子</button><label v-if="!spectator || !compact">缩放 <input v-model.number="zoom" aria-label="地图缩放" type="range" :min="dashboardTarget ? 0.01 : bounded ? 0.15 : 0.3" max="1.3" step="0.01" /></label></div></div>
    <p v-if="targetHint && !stretch" class="target-map-hint" role="status">{{ targetHint }}</p>
    <div ref="viewport" class="board-viewport">
      <div class="board-sizer" :style="{ width: `${size * zoom}px`, height: `${height * zoom}px` }">
        <div class="board" :style="{ width: `${size}px`, height: `${height}px`, transform: `scale(${zoom})`, gridTemplateColumns: `repeat(${dimensions.columns}, 1fr)`, gridTemplateRows: `repeat(${dimensions.rows}, 1fr)` }">
          <button v-for="tile in room.board" :key="tile.id" :data-cash-tile="tile.id" class="tile" :class="[tile.type, `district-${tile.district}`, effectTiles.has(tile.id) ? `effect-target-${boardEffect?.boardEffect?.kind}` : '', { 'kamui-frozen': !!tile.kamuiCasterId && !!tile.kamuiTurns, 'is-rotating': tile.type === 'activity' && rotating, owned: tile.ownerId, mortgaged: tile.mortgaged, [`level-${tile.level}`]: true, 'my-position': tile.id === (animatedPosition && animatedPosition.id === meId ? animatedPosition.position : room.players.find(p => p.id === meId)?.position), 'motion-trail': visitedTiles.has(tile.id), 'motion-landing': landing && animatedPosition?.position === tile.id, 'current-position': compact && (animatedPosition?.id === current?.id ? animatedPosition?.position : current?.position) === tile.id, 'chat-referenced': referencedTile === tile.id, 'target-eligible': targeting && eligibleTiles?.includes(tile.id), 'target-selected': targeting && selectedTarget === tile.id, 'target-unavailable': targeting && !eligibleTiles?.includes(tile.id) }]" :aria-pressed="targeting ? selectedTarget === tile.id : undefined" :style="{ gridRow: tileCoordinates(tile.id, room.board.length).row, gridColumn: tileCoordinates(tile.id, room.board.length).column, '--owner': ownerColor(tile) }" :aria-label="`${tile.id}号 ${tile.name}${tile.type === 'property' ? `，价格${tile.price}，租金${rentFor(tile, room.economy)}，${room.players.find(player => player.id === tile.ownerId)?.name ?? '待购入'}，${tile.level}级${tile.mortgaged ? '，已抵押' : ''}` : ''}${tile.barrier ? '，路障，首位经过者停下' : ''}`" @click="emit('select', tile)">
            <svg v-if="referencedTile === tile.id" class="chat-reference-glow" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true"><rect x="2" y="2" width="96" height="96" rx="7" pathLength="100" /></svg>
            <span v-if="compact" class="tile-ground" aria-hidden="true"></span>
            <span v-if="compact && tile.type === 'property'" class="property-tile-art" aria-hidden="true"></span>
            <span v-if="tile.ownerId" class="owner-strip"></span><span v-if="!stretch && tile.type === 'property' && tile.ownerId" class="tile-grade">{{ tile.level }}级</span>
            <span v-if="!compact && tile.type === 'property' && tile.level" class="building-decor" aria-hidden="true"><i v-for="building in tile.level" :key="building"></i><b v-if="tile.level === 3">★</b></span>
            <span v-if="tile.barrier && tile.type !== 'property'" class="barrier-marker" title="路障">🚧</span>
            <img v-if="tile.type !== 'property' && compact" class="special-tile-art"  :src="tile.type === 'activity' && tile.activityKind ? activityArt(`${displayedActivityKinds[tile.id] ?? tile.activityKind}-tile`) : imageUrl(`/images/tiles/${tile.type}.png`)" alt="" draggable="false" /><span v-if="!compact" class="tile-icon" :class="{ built: tile.level > 0 }" :key="`${tile.level}-${tile.ownerId}`">{{ tile.type === 'property' && tile.level > 0 ? '▥' : icons[tile.type] }}</span>
            <span v-for="effect in tileEffects.filter(effect => effect.tileId === tile.id)" :key="effect.key" class="tile-effect" :class="effect.kind" aria-hidden="true">{{ effect.kind === 'built' ? '✦' : effect.kind === 'bought' ? '⌂' : '✧' }}</span>
            <strong><span class="tile-name" :title="tile.name" :class="{ 'has-property-effects': tile.type === 'property' && hasPropertyEffects(tile, room.constructionEffects) }">{{ tile.name }}</span><PropertyGrade v-if="stretch && !desktopSingleScreen && tile.type === 'property'" :level="tile.level" /></strong><small>{{ tile.type === 'property' ? (tile.mortgaged ? '已抵押' : `¥${tile.price.toLocaleString()}`) : ({ start: `+${startIncome.toLocaleString()}`, chance: '好运降临', fortune: '惊喜奖励', jail: '休息一轮', activity: '游园活动' }[tile.type]) }}</small>
            <span v-if="stretch && tile.type === 'property'" class="tile-owner-label" :class="{ 'is-unowned': !tile.ownerId }" :style="{ '--owner-ink': tile.ownerId ? ownerTextColor(tile) : undefined }" :title="tile.ownerId ? `属于 ${tileOwner(tile)?.name ?? '玩家'}${tile.ownerId === meId && !spectator ? '（我）' : ''}${tile.mortgaged ? ' · 已抵押' : ''}` : '待购入'">
              <CharacterPawn v-if="tile.ownerId" :avatar-id="tileOwner(tile)?.avatarId" :color="ownerColor(tile)" />
              <PropertyGrade v-if="desktopSingleScreen && tile.ownerId" :level="tile.level" />
              <span class="tile-owner-name">{{ tile.ownerId ? tileOwner(tile)?.name ?? '玩家' : '待购入' }}</span>
              <b v-if="tile.ownerId && tile.ownerId === meId && !spectator" class="tile-owner-self">我</b>
            </span>
            <span v-else-if="compact && tile.type === 'property'" class="tile-ownership" :title="room.players.find(player => player.id === tile.ownerId)?.name ?? '待购入'"><i v-if="tile.ownerId" :style="{ background: ownerColor(tile) }"></i><PlayerText :text="room.players.find(player => player.id === tile.ownerId)?.name ?? '待购入'" :players="room.players" /><b v-if="tile.ownerId"> · {{ tile.level }}级</b></span>
            <span v-else-if="!stretch && tile.level" class="building-level">{{ '●'.repeat(tile.level) }}</span>
            <span v-if="tile.type === 'activity' && turnsUntilRotation <= 3" class="activity-tile-countdown">{{ turnsUntilRotation }}回合后轮换</span>
            <span class="tile-number">{{ tile.id }}</span>
          </button>
          <div v-for="(change, index) in money" :key="change.key" class="money-float" :class="{ expense: change.amount < 0 }" :style="{ gridRow: tileCoordinates(change.tileId, room.board.length).row, gridColumn: tileCoordinates(change.tileId, room.board.length).column, '--float-offset': `${index % 3 * 14}px` }" aria-hidden="true"><small><PlayerText :text="change.name" :players="room.players" /></small>{{ change.amount > 0 ? '+' : '−' }}¥{{ Math.abs(change.amount).toLocaleString() }}</div>
        </div>
        <div v-if="rotating" class="activity-rotation-glow" aria-hidden="true"></div>
        <div v-if="tradeMarkers.length" class="trade-map-layer">
          <div v-for="marker in tradeMarkers" :key="marker.id" class="trade-map-marker" :class="marker.gaining ? 'is-gaining' : 'is-losing'" :style="tradeMarkerStyle(marker.id)" role="img" :aria-label="`${marker.label}：${room.board[marker.id]?.name ?? ''} · ${marker.id}号地产`">
            <span class="trade-map-label" aria-hidden="true">{{ marker.label }}</span>
            <span class="trade-map-arrow" aria-hidden="true"></span>
          </div>
        </div>
        <div class="pawn-layer" aria-label="玩家位置"><div v-for="player in room.players.filter(p => !p.bankrupt)" :key="player.id" :data-player-id="player.id" class="map-character" :class="{ mine: player.id === meId, acting: player.id === current?.id, 'effect-pawn-hidden': effectPlayers.has(player.id), 'is-moving': moving && animatedPosition?.id === player.id, 'is-landing': landing && animatedPosition?.id === player.id }" :style="{ ...pawnStyle(player), '--step-duration': `${moveStepMs}ms` }" :title="`${player.name}${player.id === meId ? '（我）' : ''}`"><CharacterPawn variant="pawn" :avatar-id="player.avatarId" :color="player.color" :moving="moving && animatedPosition?.id === player.id" :landing="landing && animatedPosition?.id === player.id" /></div></div>
        <BoardEffectLayer v-if="boardEffect?.boardEffect" :key="boardEffect.id" :id="boardEffect.id" :actor-id="boardEffect.playerId" :effect="boardEffect.boardEffect" :players="room.players" :board="room.board" :point="effectPoint" :player-point="effectPlayerPoint" :zoom="zoom" :tile-size="effectTileSize" :board-width="size * zoom" :board-height="height * zoom" @done="finishBoardEffect" />
        <Teleport :to="dashboardTarget || 'body'" :disabled="!dashboardTarget" defer>
        <div class="board-center" :class="{ 'workbench-center': compact && $slots.controls, 'has-decision': decision }" :style="dashboardTarget ? undefined : centerStyle">
          <span class="center-label"><span class="status-dot"></span>{{ spectator ? '公共观战席' : '好友财富派对' }}</span>
          <h2>{{ compact ? '把城市，' : '一步好运' }}<br /><span>{{ compact ? '变成你的主场。' : '一城财富' }}</span></h2>
          <City />
          <div v-if="celebrating" class="village-confetti" aria-hidden="true"><i v-for="leaf in 18" :key="leaf" :style="{ '--leaf': leaf }">{{ leaf % 3 === 0 ? '✦' : '▰' }}</i></div>
          <Teleport :to="statusTarget || 'body'" :disabled="!statusTarget" defer>
          <div class="turn-display"><PanelFrame v-if="stretch" variant="notice" ornament="right" /><CharacterPawn v-if="current" :avatar-id="current.avatarId" :color="current.color" /><div class="turn-identity"><strong><PlayerText :text="current?.name ?? '玩家'" :players="room.players" />{{ stretch ? '' : room.status === 'finished' ? '赢下了城市' : '的回合' }}</strong><small>{{ room.status === 'finished' ? '本局财富赢家' : `第 ${room.turnNumber} 次行动` }}</small></div><slot name="action-status" /><DiceFace v-if="!diceTarget && (desktopDice && stretch || !diceFullscreen && (!stretch || room.dice !== null))" :class="{ 'turn-die-placeholder': desktopDice && stretch && (diceFullscreen || room.dice === null) }" :aria-hidden="desktopDice && stretch && (diceFullscreen || room.dice === null) ? true : undefined" :value="room.dice" /><slot name="summary" /></div>
          </Teleport>
          <div v-if="$slots.controls" class="board-center-controls"><slot name="controls" /></div>
          <div class="turn-order"><span v-for="id in room.turnOrder" :key="id" :class="{ active: id === current?.id, eliminated: room.players.find(p => p.id === id)?.bankrupt }" :style="{ '--player-color': room.players.find(p => p.id === id)?.color }"><PlayerText :text="room.players.find(p => p.id === id)?.name ?? '玩家'" :players="room.players" /></span></div>
          <slot name="center-overlay" />
        </div>
        </Teleport>
      </div>
    </div>
    <Teleport to="body">
      <DesktopDiceThrow v-if="desktopDice && diceFullscreen && motion && motionPlayer && diceResult !== null" :key="motion.id" :board="container" :player="motionPlayer" :mine="!spectator && motionPlayer.id === meId" :value="diceResult" :rolling="rolling" :duration="diceRollMs" />
    </Teleport>
    <div ref="feedbackArea" class="board-local-feedback"></div>
    <Teleport :to="diceTarget || feedbackArea || 'body'" defer>
      <Transition name="dice-overlay">
        <div v-if="!desktopDice && diceFullscreen && motion && motionPlayer" :key="motion.id" class="dice-overlay" :class="{ 'is-rolling': rolling }" role="status" aria-live="polite">
          <div class="dice-stage" aria-hidden="true"><DiceFace :value="rolling ? diceFace : diceResult" :rolling="rolling" :settled="!rolling" /></div>
          <div class="dice-overlay-caption"><strong><PlayerText :text="motionPlayer.name" :players="room.players" /></strong><span>{{ movementLabel }}</span></div>
        </div>
      </Transition>
      <Transition name="motion-feedback">
        <div v-if="motion && motionPlayer && !diceFullscreen && !motion.boardEffect" :key="motion.id" class="board-motion-feedback" :class="{ 'is-rolling': rolling, 'is-settled': !rolling }" :style="{ '--motion-color': motionPlayer.color }" role="status" aria-live="polite">
          <div v-if="diceResult !== null" class="dice-stage" aria-hidden="true"><DiceFace :value="rolling ? diceFace : diceResult" :rolling="rolling" :settled="!rolling" /></div>
          <CharacterPawn v-else :avatar-id="motionPlayer.avatarId" :color="motionPlayer.color" :moving="moving" />
          <div class="motion-caption"><strong><PlayerText :text="motionPlayer.name" :players="room.players" /></strong><span>{{ movementLabel }}</span><div v-if="!rolling && motion.path.length" class="motion-progress" aria-hidden="true"><i :style="{ width: `${step / motion.path.length * 100}%` }"></i></div></div>
        </div>
      </Transition>
    </Teleport>
    <div class="map-legend"><span v-for="(name, i) in districts" :key="i"><i :class="`district-${i}`"></i>{{ name }}</span></div>
    <div v-if="compact" class="board-state-legend"><span>彩色边框 · 地产主人</span><span>0级土地 · 1～3级建筑</span><span>🚧 路障 · 经过即停</span><span>▨ 条纹 · 已抵押</span></div>
  </div>
</template>
<style scoped>
.tile .tile-name.has-property-effects { color:#b17a16; font-weight:900; }
.board-container { position:relative; }
.board-local-feedback { position:absolute; inset:0; pointer-events:none; z-index:13; }
.board-local-feedback .dice-overlay { position:absolute; gap:12px; background:#fff9ebb8; backdrop-filter:blur(2px); pointer-events:none; touch-action:auto; }
.board-local-feedback .dice-overlay .die { width:92px; height:92px; padding:16px; gap:7px; }
.board-local-feedback .board-motion-feedback { position:absolute; bottom:10px; max-width:calc(100% - 20px); gap:10px; padding:8px 12px; }
.effect-pawn-hidden { visibility:hidden; }
.tile.kamui-frozen .property-tile-art,.tile.kamui-frozen .tile-icon,.tile.kamui-frozen .building-decor { opacity:.25; filter:grayscale(.7); }
.tile.effect-target-kamui .property-tile-art,.tile.effect-target-kamui .tile-icon,.tile.effect-target-kamui .building-decor { transform-origin:center; animation:property-swallowed 1800ms ease-in-out both; }
.tile.effect-target-kamuiRelease .property-tile-art,.tile.effect-target-kamuiRelease .tile-icon,.tile.effect-target-kamuiRelease .building-decor { transform-origin:center; animation:property-restored 1800ms ease-in-out both; }
.tile.effect-target-blackhole { box-shadow:inset 0 0 26px #9e79b45c; }
.tile.effect-target-tornado .property-tile-art,.tile.effect-target-tornado .tile-icon,.tile.effect-target-tornado .building-decor { animation:property-storm 2400ms ease-in-out both; }
@keyframes property-swallowed { 0%,20% { scale:1; opacity:1; } 70%,100% { scale:.1; opacity:0; rotate:90deg; } }
@keyframes property-restored { 0%,25% { scale:.1; opacity:0; rotate:-90deg; } 85%,100% { scale:1; opacity:1; rotate:0deg; } }
@keyframes property-storm { 0%,25%,80%,100% { translate:0; } 35%,55%,75% { translate:-3px; } 45%,65% { translate:3px; } }

.trade-map-layer { position: absolute; inset: 0; z-index: 7; pointer-events: none; }
.trade-map-marker { position: absolute; filter: drop-shadow(0 2px 3px #49311d55); }
.trade-map-marker.is-gaining { color: #248547; }
.trade-map-marker.is-losing { color: #ce3e39; }
.trade-map-label { position: absolute; left: var(--trade-label-x); top: var(--trade-label-y); transform: translate(-50%, -50%); padding: 2px 7px; border: 2px solid currentColor; border-radius: 8px; background: #fffdf5; font-size: 12px; line-height: 1.4; font-weight: 800; white-space: nowrap; }
.trade-map-arrow { position: absolute; left: 0; top: 0; width: 48px; height: 46px; transform: translate(-50%, -50%) rotate(var(--trade-arrow-angle)); }
.trade-map-arrow::before { content: ''; position: absolute; inset: 0; background: var(--turn-arrow) center / contain no-repeat; animation: trade-arrow-float 1.8s ease-in-out infinite; }
.is-gaining .trade-map-arrow::before { filter: hue-rotate(85deg) saturate(1.4); }
.is-losing .trade-map-arrow::before { filter: hue-rotate(-45deg) saturate(1.4); }
@keyframes trade-arrow-float { 50% { transform: translateY(3px); } }
@media (prefers-reduced-motion: reduce) { .trade-map-arrow::before { animation: none; } }
</style>
