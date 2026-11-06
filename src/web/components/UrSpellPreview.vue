<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import type { PublicRoomState, Tile } from '../../shared/types';
import Board from './Board.vue';
import UrSpellLayer from './UrSpellLayer.vue';
import RegularSpellLayer from './RegularSpellLayer.vue';
import CardSpellProp from './CardSpellProp.vue';
import { REGULAR_SPELL_SAMPLES, type RegularSpellSample } from '../regular-spell-preview';
import { prepareUrSpellAtlas, UR_SPELL_ATLAS, UR_SPELL_SAMPLES, type UrSpellSample } from '../ur-spell-preview';

const props = defineProps<{ regular?: boolean }>();
type PreviewSample = UrSpellSample | RegularSpellSample;
const samples = computed(() => props.regular ? REGULAR_SPELL_SAMPLES : UR_SPELL_SAMPLES);
const selected = ref<PreviewSample>(samples.value[0]!);
const state = ref<'idle' | 'loading' | 'playing' | 'done' | 'error'>('idle');
const showName = ref(true);
const result = ref('');
const playback = ref(0);
const systemReduced = ref(false);
const staticPreview = ref(false);
const reduced = computed(() => systemReduced.value || staticPreview.value);
const motionPreference = window.matchMedia('(prefers-reduced-motion: reduce)');
/** 无入参，创建固定展示局面；不连接后端，也不发送操作。 */
function initialRoom(): PublicRoomState {
  const board: Tile[] = Array.from({ length: 24 }, (_, id) => ({ id, type: id === 0 ? 'start' : 'property', name: id === 0 ? '起点' : id === 9 ? '云朵商店' : `花园街 ${id}`, district: Math.floor(id / 6), price: 2000, ownerId: id === 0 ? null : 'spell-target', level: id === 0 ? 0 : 3, mortgaged: false, barrier: false, demolitionShield: false, treasure: false, concertStacks: 0, meteorCharges: 0, kamuiCasterId: null, kamuiTurns: 0, kamuiAppliedTurn: 0 }));
  return { roomId: 'ur-spell-preview', code: 'PREVIEW', version: 1, status: 'playing', hostId: 'spell-actor', gameId: 'ur-spell-preview',
    players: [{ id: 'spell-actor', name: '阿洛', avatarId: 'explorer', color: '#739d81', position: 20 }, { id: 'spell-target', name: '小桃', avatarId: 'cat', color: '#d8978b', position: selected.value.theme === 'sap' ? 22 : selected.value.theme === 'vines' ? 1 : 9 }].map(player => ({ ...player, cash: 12000, ready: true, online: true, bankrupt: false, skipTurns: 0, jailed: false, eliminationOrder: null, handLimit: 6 })),
    board, turnOrder: ['spell-actor', 'spell-target'], turnIndex: 0, turnNumber: 1, phase: 'pre_roll', pending: null, tradePending: false, pausedReason: null, dice: null, motion: null, winnerId: null, logs: [] };
}
const room = ref(initialRoom());
const regularSample = computed(() => 'rarity' in selected.value ? selected.value : undefined);
const pathIds = computed(() => Array.from({ length: Math.abs(regularSample.value?.steps ?? 0) + 1 }, (_, i) => (20 + i * Math.sign(regularSample.value?.steps ?? 0) + 24) % 24));
const destination = computed(() => pathIds.value[pathIds.value.length - 1]!);
const participantTarget = computed(() => regularSample.value?.target === 'player' || ['sakura', 'reaper', 'slowBeam', 'snatch', 'thunder'].includes(selected.value.theme));
const selfTarget = computed(() => regularSample.value?.target === 'self');
const exchanging = computed(() => ['thunder', 'cloak', 'room'].includes(selected.value.theme));
const moving = computed(() => regularSample.value?.steps !== undefined);
const targetAppearance = computed(() => ['coupon', 'groupon'].includes(selected.value.theme) ? { name: '阿洛的账单' } : selfTarget.value ? { name: '阿洛' } : moving.value ? { name: `${destination.value} 号落点` } : participantTarget.value
  ? { name: '小桃', avatarId: 'cat', color: '#d8978b' }
  : { name: '云朵商店', level: 3 });
const busy = computed(() => state.value === 'loading' || state.value === 'playing');

/** 输入样板，取消旧播放并恢复固定局面，无返回值。 */
function select(sample: PreviewSample): void {
  playback.value++; selected.value = sample; state.value = 'idle'; result.value = ''; room.value = initialRoom();
  if (sample.theme === 'repair') Object.assign(room.value.board[9]!, { level: 2, ownerId: 'spell-actor' });
  if (sample.theme === 'atField') room.value.board[9]!.ownerId = 'spell-actor';
}
/** 无入参，预解码素材后开始当前样板；过期加载不触发演出。 */
async function play(): Promise<void> {
  const current = ++playback.value;
  room.value = initialRoom(); result.value = ''; state.value = 'loading';
  if (selected.value.theme === 'repair') Object.assign(room.value.board[9]!, { level: 2, ownerId: 'spell-actor' });
  if (selected.value.theme === 'atField') room.value.board[9]!.ownerId = 'spell-actor';
  try {
    if (!props.regular) await prepareUrSpellAtlas();
    if (current !== playback.value) return;
    systemReduced.value = motionPreference.matches;
    state.value = 'playing';
  } catch (error) {
    if (current !== playback.value) return;
    console.warn('UR 招式预览素材加载失败', error);
    state.value = 'error'; result.value = '招式素材未能加载，请点击重新加载。';
  }
}
/** 无入参，结束当前演出并应用固定演示结果；不执行真实卡牌规则。 */
function finish(): void {
  if (state.value !== 'playing') return;
  state.value = 'done'; result.value = selected.value.result;
  if (selected.value.theme === 'kamui') Object.assign(room.value.board[9]!, { kamuiCasterId: 'spell-actor', kamuiTurns: 3, kamuiAppliedTurn: 1 });
  if (selected.value.theme === 'punch') room.value.board[9]!.level = 0;
  if (selected.value.theme === 'reaper') room.value.players.forEach(player => { player.handLimit = 5; });
  if (selected.value.theme === 'concrete') room.value.board[9]!.barrier = true;
  if (selected.value.theme === 'repair') room.value.board[9]!.level = 3;
  if (selected.value.theme === 'staff') room.value.board[9]!.level = 2;
  if (selected.value.theme === 'atField') room.value.board[9]!.demolitionShield = true;
  if (exchanging.value) { room.value.players[0]!.position = 9; room.value.players[1]!.position = 20; }
  if (moving.value) room.value.players[0]!.position = destination.value;
}
/** 无入参，跳过演出或取消未完成加载，无返回值。 */
function skip(): void {
  if (state.value === 'playing') finish();
  else { state.value = 'idle'; result.value = '已取消素材加载。'; }
  playback.value++;
}
/** 无入参，切后台时释放演出与待加载任务，无返回值。 */
function onVisibility(): void { if (document.hidden && busy.value) skip(); }
/** 无入参，同步减少动态效果设置；正在播放时直接完成并展示结果。 */
function onMotionPreference(): void { systemReduced.value = motionPreference.matches; }
// 系统设置或预览开关改为静态时，立即释放进行中的时间轴。
watch(reduced, value => { if (value && state.value === 'playing') finish(); });
/** 无入参，注册页面可见性和系统动态设置监听。 */
onMounted(() => { systemReduced.value = motionPreference.matches; document.addEventListener('visibilitychange', onVisibility); motionPreference.addEventListener('change', onMotionPreference); });
/** 无入参，取消待加载任务并移除监听，子组件负责清理时间轴。 */
onBeforeUnmount(() => { playback.value++; document.removeEventListener('visibilitychange', onVisibility); motionPreference.removeEventListener('change', onMotionPreference); });
</script>

<template>
  <section class="ur-preview" :aria-label="regular ? '非 UR 招式样板' : 'UR 招式样板'">
    <div class="spell-picker" :class="{ 'regular-picker': regular }"><button v-for="(sample, index) in samples" :key="sample.skinId" class="spell-choice" :class="{ selected: selected.skinId === sample.skinId }" :aria-pressed="selected.skinId === sample.skinId" @click="select(sample)"><span v-if="'rarity' in sample" class="choice-art"><CardSpellProp :kind="sample.theme" /></span><span v-else class="choice-art" :class="sample.theme" :style="{ backgroundImage: `url(${UR_SPELL_ATLAS})` }"></span><span><b>{{ showName ? sample.name : `样板 ${index + 1}` }}</b><small>{{ 'rarity' in sample ? sample.rarity : 'UR' }} · {{ (sample.duration / 1000).toFixed(1) }} 秒</small></span></button></div>
    <div class="spell-heading"><div><p class="spell-eyebrow">招式放映室 · {{ regular ? '18 张非 UR 样板 · 新增 12 张' : '四张独立样板' }}</p><h2>{{ showName ? selected.name : '隐藏卡名 · 辨识体验' }}</h2><p>{{ showName ? selected.description : '观察招式形态和动作顺序，看看能否认出它。' }}</p></div><label><input v-model="showName" type="checkbox" />显示卡名</label></div>
    <div class="spell-board" :class="{ 'is-playing': state === 'playing', 'swap-playing': state === 'playing' && exchanging && !reduced, 'move-playing': state === 'playing' && moving && !reduced }">
      <Board :room="room" compact bounded me-id="spell-actor">
        <template #effect-overlay="{ point, playerPoint, tileSize, width, height }">
          <RegularSpellLayer v-if="state === 'playing' && 'rarity' in selected" :key="playback" :sample="selected" :actor="playerPoint('spell-actor', 20)" :target="moving ? point(destination) : selfTarget || selected.theme === 'coupon' ? playerPoint('spell-actor', 20) : participantTarget ? playerPoint('spell-target', room.players[1]!.position) : point(9)" :path="pathIds.map(id => point(id))" :tile-size="tileSize" :width="width" :height="height" :reduced="reduced" :show-name="showName" @done="finish" />
          <UrSpellLayer v-else-if="state === 'playing' && !('rarity' in selected)" :key="playback" :sample="selected" :actor="playerPoint('spell-actor', 20)" :target="participantTarget ? playerPoint('spell-target', 9) : point(9)" :target-appearance="targetAppearance" :tile-size="tileSize" :width="width" :height="height" :reduced="reduced" :show-name="showName" @done="finish" />
        </template>
        <template #controls><div class="spell-notice" aria-live="polite"><span class="notice-star">✦</span><h3>{{ state === 'playing' ? '招式发动' : state === 'done' ? '演出完成' : '准备放映' }}</h3><p v-if="result">{{ result }}</p><p v-else>阿洛 → {{ targetAppearance.name }}<br />点击播放，观看招式作用于目标。</p><p v-if="state === 'done' && selected.theme === 'reaper'">阿洛 / 小桃 · 手牌上限 5</p></div></template>
      </Board>
    </div>
    <div class="spell-actions"><button class="button primary" :disabled="busy" @click="play">{{ state === 'loading' ? '素材加载中…' : state === 'error' ? '重新加载' : state === 'done' ? '再看一次' : '播放招式' }}</button><button v-if="busy" class="button secondary" @click="skip">{{ state === 'loading' ? '取消加载' : '跳过演出' }}</button><label><input v-model="staticPreview" type="checkbox" />静态演示</label><span v-if="reduced">已启用减少动态效果，将显示静态提示。</span><span v-else>{{ regular ? '专属道具 · 局部短演出 · 可随时跳过' : '短暂压暗 · 目标高亮 · 可随时跳过' }}</span></div>
    <p v-if="state === 'error'" class="spell-error" role="alert">{{ result }}</p>
    <details v-if="!regular" class="spell-assets"><summary>查看招式素材</summary><img :src="UR_SPELL_ATLAS" alt="空间旋涡、红色巨拳、刀刃与樱花、死神虚影四格透明素材" /><p>一张透明图集承载四组主体，配合光带、轨迹和冲击圈；仅用于本地样板预览。</p></details>
  </section>
</template>

<style scoped>
.ur-preview { max-width:1320px; margin:0 auto; padding:24px; background:#fffaf2; border:1px solid #e9d9bf; border-radius:22px; box-shadow:0 10px 35px #936b3810; }
.move-playing :deep([data-player-id="spell-actor"]) { visibility:hidden; }
.regular-picker { max-height:270px; overflow-y:auto; padding:3px; }
.regular-picker { grid-template-columns:repeat(3,minmax(0,1fr))!important; }.swap-playing :deep(.pawn-layer) { visibility:hidden; }.spell-actions label { font-size:12px; }
.spell-picker { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:10px; }.spell-choice { display:flex; align-items:center; gap:10px; padding:10px; text-align:left; border:1px solid #e6d4bb; border-radius:14px; background:#fffaf1; color:#765b48; cursor:pointer; }.spell-choice.selected { background:#f5e6d5; border-color:#b9855d; box-shadow:0 2px 9px #9c6a371a; }.spell-choice b,.spell-choice small { display:block; }.spell-choice b { font-size:14px; }.spell-choice small { margin-top:6px; color:#a18367; font-size:11px; }.choice-art { flex:none; width:58px; height:58px; background-size:200% 200%; }.choice-art.kamui { background-position:0 0; }.choice-art.punch { background-position:100% 0; }.choice-art.sakura { background-position:0 100%; }.choice-art.reaper { background-position:100% 100%; }
.spell-heading { display:flex; justify-content:space-between; align-items:center; gap:16px; padding:20px 0 14px; }.spell-heading h2 { font-size:24px; margin:6px 0; }.spell-heading p { margin:0; font-size:13px; color:#a18367; }.spell-heading .spell-eyebrow { font-size:10px; letter-spacing:2px; }.spell-heading label { display:flex; align-items:center; gap:7px; font-size:12px; white-space:nowrap; }
.spell-board { max-width:720px; margin:0 auto; }.spell-board :deep(.board-container) { padding:0; }.spell-board :deep(.board-viewport) { height:auto; min-height:0; overflow:hidden; }.spell-board :deep(.board-center h2),.spell-board :deep(.turn-order) { display:none; }.spell-board :deep(.board-center) { justify-content:center; gap:8px; }.spell-board :deep(.board-center-controls) { min-height:0; overflow:auto; }.spell-board :deep(.turn-display) { padding:5px; gap:6px; }.spell-board :deep(.turn-identity) { min-width:0; }.spell-board :deep(.turn-display .die) { display:none; }
.spell-notice { text-align:center; padding:12px; color:#826851; }.notice-star { font-size:32px; color:#bf9062; }.spell-notice h3 { font-size:16px; margin:8px 0; }.spell-notice p { font-size:12px; line-height:1.8; margin:8px 0; }.is-playing :deep(.board-center) { visibility:hidden; }
.spell-actions { display:flex; gap:12px; align-items:center; flex-wrap:wrap; padding-top:18px; margin-top:12px; border-top:1px dashed #e5d4bc; }.spell-actions span { font-size:12px; color:#a18367; }.spell-error { color:#a83743; font-size:13px; }.spell-assets { margin-top:18px; font-size:12px; color:#a18367; }.spell-assets summary { cursor:pointer; }.spell-assets img { display:block; width:min(100%,500px); margin:12px auto; border-radius:12px; background:#302a40; }.spell-assets p { text-align:center; }
@media(max-width:760px) { .ur-preview { padding:12px; }.spell-picker { grid-template-columns:repeat(2,minmax(0,1fr)); gap:6px; }.choice-art { width:40px; height:40px; }.spell-choice { padding:8px; }.spell-choice b { font-size:12px; }.spell-heading h2 { font-size:20px; }.spell-heading p { font-size:11px; }.spell-notice { padding:0 3px; }.spell-notice h3 { font-size:12px; }.spell-notice p { font-size:10px; }.notice-star { display:none; }.spell-actions span { width:100%; font-size:11px; }.spell-board :deep(.turn-display) { display:none; } }
@media(max-width:760px) { .regular-picker { grid-template-columns:repeat(2,minmax(0,1fr))!important; } }
</style>
