<script setup lang="ts">
import ThemedSelect from '../components/ThemedSelect.vue';
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { ArchiveSnapshot, HistoryEntry, HistoryPage, RoomArchive, Session, Tile } from '../../shared/types';
import { archiveRequest, formatArchiveCode, normalizeArchiveCode } from '../archive';
import { commandId, recentRooms, rememberArchive, rememberSession, useGameStore } from '../stores/game';
import Logo from '../components/Logo.vue';
import Board from '../components/Board.vue';
import Modal from '../components/Modal.vue';
import '../archive.css';
withDefaults(defineProps<{ embedded?: boolean }>(), { embedded: false });
const emit = defineEmits<{ restoring: [value: boolean] }>();
const router = useRouter();
const game = useGameStore();
const inputCode = ref(localStorage.getItem('dfw:archive:last') || '');
const recent = ref(recentRooms());
const archive = ref<RoomArchive | null>(null);
const preview = ref<ArchiveSnapshot | null>(null);
const entries = ref<HistoryEntry[]>([]);
const nextCursor = ref<number | null>(null);
const loading = ref(false);
const previewLoading = ref(false);
const historyLoading = ref(false);
const bookmarksLoading = ref(false);
const error = ref('');
const gameId = ref('');
const type = ref('');
const turnNumber = ref('');
const restoring = ref(false);
const showRestore = ref(false);
const ownPlayerId = ref('');
const selectedTile = ref<Tile | null>(null);
let generation = 0;
let historyGeneration = 0;
let previewGeneration = 0;
const state = computed(() => preview.value?.state);
const types = [['save', '手动存档'], ['turn_start', '行动开始'], ['turn_end', '行动结束'], ['roll', '掷骰操作'], ['dice', '移动点数'], ['event_drawn', '城市事件'], ['effect', '效果结算'], ['buy', '购买地产'], ['upgrade', '升级建筑'], ['tradeOffer', '发起报价'], ['tradeRespond', '回应报价'], ['seat_assigned', '角色分配'], ['game_finished', '对局结束'], ['log', '公开动态']];
/** 输入时间戳，返回本地日期时间。 */
function time(at: number): string { return new Date(at).toLocaleString('zh-CN'); }
/** 输入存档码，加载索引与初始历史，并显示最新的保存局面。 */
async function load(code = inputCode.value): Promise<void> {
  const current = ++generation; ++previewGeneration; ++historyGeneration;
  loading.value = true; error.value = ''; selectedTile.value = null; archive.value = null; preview.value = null; entries.value = []; showRestore.value = false;
  previewLoading.value = false; historyLoading.value = false; bookmarksLoading.value = false;
  gameId.value = ''; type.value = ''; turnNumber.value = ''; inputCode.value = formatArchiveCode(code);
  try {
    const normalized = normalizeArchiveCode(code);
    const [value, history] = await Promise.all([archiveRequest<RoomArchive>('archives/query', { archiveCode: normalized }), archiveRequest<HistoryPage>('archives/history', { archiveCode: normalized })]);
    if (current !== generation) return;
    archive.value = value; preview.value = value.latest; entries.value = history.entries; nextCursor.value = history.nextCursor;
    rememberArchive({ roomId: value.roomId, code: value.code, archiveCode: value.archiveCode }); recent.value = recentRooms();
  } catch (cause) { if (current === generation) error.value = cause instanceof Error ? cause.message : '无法读取存档'; }
  finally { if (current === generation) loading.value = false; }
}
/** 输入保存点，读取当时的公开局面；过时响应不会覆盖新选择。 */
async function selectSnapshot(snapshotId?: string): Promise<void> {
  if (!archive.value) return;
  const current = ++previewGeneration; const roomGeneration = generation; previewLoading.value = true; selectedTile.value = null; showRestore.value = false; error.value = '';
  try {
    const value = await archiveRequest<ArchiveSnapshot>('archives/snapshot', { archiveCode: archive.value.archiveCode, snapshotId });
    if (current === previewGeneration && roomGeneration === generation) preview.value = value;
  } catch (cause) { if (current === previewGeneration && roomGeneration === generation) error.value = cause instanceof Error ? cause.message : '无法读取保存点'; }
  finally { if (current === previewGeneration) previewLoading.value = false; }
}
/** 输入是否翻页，按当前筛选读取历史；更换筛选时丢弃旧游标与旧响应。 */
async function history(more = false): Promise<void> {
  if (!archive.value) return;
  const current = ++historyGeneration; const roomGeneration = generation; historyLoading.value = true; error.value = '';
  try {
    const page = await archiveRequest<HistoryPage>('archives/history', { archiveCode: archive.value.archiveCode, gameId: gameId.value || undefined, type: type.value || undefined, turnNumber: turnNumber.value === '' ? undefined : Number(turnNumber.value), cursor: more ? nextCursor.value : undefined });
    if (current !== historyGeneration || roomGeneration !== generation) return;
    entries.value = more ? [...entries.value, ...page.entries] : page.entries; nextCursor.value = page.nextCursor;
  } catch (cause) { if (current === historyGeneration && roomGeneration === generation) error.value = cause instanceof Error ? cause.message : '历史读取失败'; }
  finally { if (current === historyGeneration) historyLoading.value = false; }
}
/** 无入参，读取下一页手动保存点，不改变当前选择的历史局面。 */
async function moreBookmarks(): Promise<void> {
  if (!archive.value?.nextCursor) return;
  const current = generation; bookmarksLoading.value = true;
  try {
    const page = await archiveRequest<RoomArchive>('archives/query', { archiveCode: archive.value.archiveCode, cursor: archive.value.nextCursor });
    if (current === generation && archive.value) { archive.value.bookmarks.push(...page.bookmarks); archive.value.nextCursor = page.nextCursor; }
  } catch (cause) { if (current === generation) error.value = cause instanceof Error ? cause.message : '保存点读取失败'; }
  finally { bookmarksLoading.value = false; }
}
/** 无入参，默认选择保存时房主角色，打开恢复角色表单。 */
function chooseRestore(): void { ownPlayerId.value = state.value?.hostId ?? ''; showRestore.value = true; }
/** 无入参，以稳定请求 ID 恢复当前保存点，重试不会创建多个房间。 */
async function restore(): Promise<void> {
  if (!archive.value || !preview.value || !ownPlayerId.value) return;
  restoring.value = true; emit('restoring', true); error.value = '';
  const choice = `${archive.value.archiveCode}:${preview.value.snapshotId}:${ownPlayerId.value}`;
  let requestId = commandId();
  try {
    const stored = JSON.parse(sessionStorage.getItem('dfw:restore-request') ?? 'null') as { choice: string; requestId: string } | null;
    if (stored?.choice === choice) requestId = stored.requestId;
    sessionStorage.setItem('dfw:restore-request', JSON.stringify({ choice, requestId }));
    const session = await archiveRequest<Session>('archives/restore', { archiveCode: archive.value.archiveCode, snapshotId: preview.value.snapshotId, requestId, playerId: ownPlayerId.value });
    rememberSession(session); await router.push(`/room/${session.code}`); sessionStorage.removeItem('dfw:restore-request');
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '恢复失败'; }
  finally { restoring.value = false; emit('restoring', false); }
}
onMounted(() => { game.disconnect(); if (inputCode.value) void load(); });
</script>
<template>
  <div class="archives-page" :class="{ 'archives-embedded': embedded }">
    <header v-if="!embedded" class="site-header"><RouterLink to="/" aria-label="返回首页"><Logo /></RouterLink><RouterLink class="button secondary small" to="/">返回首页</RouterLink></header>
    <main class="archives-main">
      <section class="panel archive-search"><span class="eyebrow">城市的每一步，都有记录</span><h1 v-if="!embedded">历史与恢复</h1><p class="muted">输入本房间固定存档码，查看完整记录，选择一个进度继续。</p><form class="archive-search-form" @submit.prevent="load()"><label class="field">固定存档码<input v-model="inputCode" placeholder="输入完整存档码，支持分组符号" autocomplete="off" required /></label><button class="button primary" :disabled="loading">{{ loading ? '正在读取…' : '查看存档' }}</button></form><p v-if="error" class="error-text" role="alert">{{ error }}</p>
        <div v-if="recent.length" class="archive-recent"><span class="muted">本浏览器的房间</span><button v-for="room in recent" :key="room.roomId" class="button ghost small" @click="load(room.archiveCode)">房间 {{ room.code }} · {{ formatArchiveCode(room.archiveCode).slice(0, 11) }}…</button></div>
      </section>
      <template v-if="archive">
        <section class="panel archive-heading"><div><h2>房间 {{ archive.code }} 的档案</h2><p class="archive-code-text">{{ formatArchiveCode(archive.archiveCode) }}</p></div><button class="button secondary small" @click="game.copy(formatArchiveCode(archive.archiveCode))">复制存档码</button><button v-if="archive.sourceArchiveCode" class="button ghost small" @click="load(archive.sourceArchiveCode)">查看来源历史 ↗</button><button class="button primary small" :disabled="previewLoading" @click="selectSnapshot()">查看最新进度</button></section>
        <div class="archive-layout">
          <section class="panel archive-timeline"><div class="section-heading"><h2>完整时间线</h2><span>按实际执行顺序保存</span></div>
            <form class="archive-filters" @submit.prevent="history()"><label class="field">对局<ThemedSelect v-model="gameId" label="对局" :options="[{ value: '', label: '全部对局' }, ...archive.games.map((item, index) => ({ value: item.gameId ?? 'lobby', label: item.gameId ? `对局 ${archive!.games.filter(g => g.gameId).length - archive!.games.slice(0, index).filter(g => g.gameId).length} · ${time(item.firstAt)}` : '大厅' }))]" /></label><label class="field">类型<ThemedSelect v-model="type" label="类型" :options="[{ value: '', label: '全部类型' }, ...types.map(([value, label]) => ({ value: value!, label: label! }))]" /></label><label class="field">行动编号<input v-model="turnNumber" type="number" min="0" placeholder="全部" /></label><button class="button secondary small" :disabled="historyLoading">筛选</button></form>
            <p v-if="!entries.length" class="muted">没有符合条件的记录。</p><button v-for="entry in entries" :key="entry.sequence" class="archive-event" :class="{ selected: entry.snapshotId === preview?.snapshotId }" @click="selectSnapshot(entry.snapshotId)"><time>{{ time(entry.at) }} · {{ entry.turnNumber == null ? '行动编号未保存' : `第 ${entry.turnNumber} 次行动` }}</time><strong>{{ entry.actorName ? `${entry.actorName} · ` : '' }}{{ entry.text }}</strong><span class="muted">{{ entry.type === 'legacy_log' ? '旧日志：查看迁移时的基线局面' : '查看完成后的局面 ↗' }}</span></button><button v-if="nextCursor" class="button secondary full" :disabled="historyLoading" @click="history(true)">{{ historyLoading ? '正在读取…' : '更早的记录' }}</button>
          </section>
          <div class="archive-preview-column">
            <section v-if="preview" class="panel archive-preview"><div class="section-heading"><h2>保存时的局面</h2><span>只读回顾</span></div><p class="muted">{{ time(preview.at) }} · 第 {{ preview.turnNumber }} 次行动 · 版本 {{ preview.version }}</p><p v-if="previewLoading" class="notice">正在读取保存点…</p><p v-if="preview.restoreError" class="notice warning">{{ preview.restoreError }}</p><p v-if="preview.label" class="notice">{{ preview.label }}</p><Board v-if="state?.board.length" :key="preview.snapshotId" :room="state" spectator @select="selectedTile = $event" /><p v-else class="muted">此保存点处于大厅，尚未生成棋盘。</p><div v-if="state" class="archive-assets"><div v-for="player in state.players" :key="player.id" class="archive-list-row"><strong>{{ player.name }}{{ player.id === state.hostId ? ' · 房主' : '' }}</strong><span>现金 ¥{{ player.cash.toLocaleString() }} · 地产 {{ state.board.filter(tile => tile.ownerId === player.id).length }} 块 · 净资产 ¥{{ player.netWorth?.toLocaleString() ?? '—' }}</span></div></div><p class="muted">手牌与私人报价不在公开历史中展示。恢复后由分配到该角色的玩家查看。</p><button class="button primary full" :disabled="!preview.restorable || previewLoading" @click="chooseRestore">从这个进度继续 ↗</button></section>
            <section class="panel"><div class="section-heading"><h2>手动保存点</h2><span>自动进度可从时间线选择</span></div><p v-if="!archive.bookmarks.length" class="muted">还没有手动标记。每次成功操作的进度已经自动保存。</p><button v-for="point in archive.bookmarks" :key="point.snapshotId" class="archive-event" @click="selectSnapshot(point.snapshotId)"><time>{{ time(point.at) }} · 第 {{ point.turnNumber }} 次行动</time><strong>{{ point.label || '手动存档' }}</strong></button><button v-if="archive.nextCursor" class="button secondary full" :disabled="bookmarksLoading" @click="moreBookmarks">更早的保存点</button></section>
          </div>
        </div>
      </template>
    </main>
    <Modal v-if="showRestore && state" title="选择你使用的保存角色" @close="!restoring && (showRestore = false)"><p class="muted">恢复会创建一个新房间。你成为新房主，原房间与历史保留，朋友加入后由你分配角色。</p><form @submit.prevent="restore"><label class="field">你的角色<ThemedSelect v-model="ownPlayerId" label="你的角色" :options="state.players.filter(player => !player.botDifficulty).map(player => ({ value: player.id, label: player.name + (player.id === state!.hostId ? ' · 保存时的房主' : '') }))" /></label><p v-if="error" class="error-text" role="alert">{{ error }}</p><button class="button primary full" :disabled="restoring">{{ restoring ? '正在恢复…' : '创建恢复房间' }}</button></form></Modal>
    <Modal v-if="selectedTile" :title="selectedTile.name" @close="selectedTile = null"><p>保存时的 {{ selectedTile.id }} 号地块</p><p v-if="selectedTile.type === 'property'">主人：{{ state?.players.find(player => player.id === selectedTile?.ownerId)?.name ?? '待购入' }} · 建筑 {{ selectedTile.level }} 级 · {{ selectedTile.mortgaged ? '已抵押' : '未抵押' }}</p><p v-if="selectedTile.type === 'property'">地产价格 ¥{{ selectedTile.price.toLocaleString() }}</p></Modal>
  </div>
</template>
