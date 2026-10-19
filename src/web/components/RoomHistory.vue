<script setup lang="ts">
import ThemedSelect from './ThemedSelect.vue';
import PropertyEffects from './PropertyEffects.vue';
import { onBeforeUnmount, ref, watch } from 'vue';
import type { ArchiveSnapshot, HistoryEntry, HistoryPage, Tile } from '../../shared/types';
import { archiveRequest, selectArchiveCode } from '../archive';
import Board from './Board.vue';
import PlayerText from './PlayerText.vue';
import ChatText from './ChatText.vue';
import CardRecordText from './CardRecordText.vue';
import Modal from './Modal.vue';
const props = defineProps<{ archiveCode: string; gameId: string | null; compact?: boolean }>();
const emit = defineEmits<{ tile: [id: number] }>();
const entries = ref<HistoryEntry[]>([]);
const nextCursor = ref<number | null>(null);
const allGames = ref(false);
const type = ref('');
const loading = ref(false);
const error = ref('');
const preview = ref<ArchiveSnapshot | null>(null);
const previewLoading = ref(false);
const selectedTile = ref<Tile | null>(null);
let generation = 0;
let previewGeneration = 0;
const eventTypes = [['chat', '玩家聊天'], ['card_used', '道具使用'], ['log', '城市动态'], ['event_drawn', '城市事件'], ['effect', '效果结算'], ['dice', '移动点数'], ['buy', '购买地产'], ['upgrade', '升级建筑'], ['tradeRespond', '交易回应'], ['turn_start', '行动开始'], ['save', '手动存档']];
/** 输入是否继续翻页，读取完整公开历史；筛选变化后忽略过时响应。 */
async function load(more = false): Promise<void> {
  const current = ++generation;
  loading.value = true; error.value = '';
  if (!more) { entries.value = []; nextCursor.value = null; }
  try {
    const page = await archiveRequest<HistoryPage>('archives/history', { archiveCode: props.archiveCode, gameId: allGames.value ? undefined : props.gameId ?? undefined, type: type.value || undefined, cursor: more ? nextCursor.value : undefined });
    if (current !== generation) return;
    entries.value = more ? [...entries.value, ...page.entries] : page.entries; nextCursor.value = page.nextCursor;
  } catch (cause) { if (current === generation) error.value = cause instanceof Error ? cause.message : '历史读取失败'; }
  finally { if (current === generation) loading.value = false; }
}
/** 输入历史保存点标识，读取当时局面供只读回顾，不修改正在游玩的棋盘。 */
async function inspect(snapshotId: string): Promise<void> {
  const current = ++previewGeneration;
  previewLoading.value = true; error.value = ''; selectedTile.value = null;
  try {
    const value = await archiveRequest<ArchiveSnapshot>('archives/snapshot', { archiveCode: props.archiveCode, snapshotId });
    if (current === previewGeneration) preview.value = value;
  } catch (cause) { if (current === previewGeneration) error.value = cause instanceof Error ? cause.message : '局面读取失败'; }
  finally { if (current === previewGeneration) previewLoading.value = false; }
}
watch([() => props.archiveCode, () => props.gameId, allGames, type], () => { ++previewGeneration; preview.value = null; previewLoading.value = false; void load(); }, { immediate: true });
onBeforeUnmount(() => { ++generation; ++previewGeneration; });
</script>
<template>
  <div class="room-history">
    <div class="history-toolbar"><label class="field">回顾范围<ThemedSelect v-model="allGames" label="回顾范围" :options="[{ value: false, label: '当前对局' }, { value: true, label: '房间全部对局' }]" /></label><label class="field">事件类型<ThemedSelect v-model="type" label="事件类型" :options="[{ value: '', label: '全部公开事件' }, ...eventTypes.map(([value, label]) => ({ value: value!, label: label! }))]" /></label><button class="button secondary small" :disabled="loading" @click="load()">刷新历史</button><RouterLink class="button ghost small" to="/archives" @click="selectArchiveCode(archiveCode)">存档与恢复 ↗</RouterLink></div>
    <p v-if="!compact" class="muted">回顾以往事件、道具使用与结算，按最新在前排列。当前局势请查看城市新鲜事及实时记录。点击“当时局面”查看历史棋盘，当前对局继续保留。</p>
    <p v-if="error" class="notice warning" role="alert">{{ error }}</p>
    <div class="history-entries"><article v-for="entry in entries" :key="entry.sequence" class="history-entry" :class="{ 'chat-record': entry.type === 'chat' }"><div><time>{{ new Date(entry.at).toLocaleString('zh-CN') }}</time><small v-if="entry.turnNumber">第 {{ entry.turnNumber }} 次行动<PlayerText v-if="entry.actorName" :text="` · ${entry.actorName}`" :players="[{ name: entry.actorName, color: '#416d55' }]" /></small><p><ChatText v-if="entry.type === 'chat'" :text="entry.text" :references="entry.references" :players="entry.mentions" :interactive="entry.gameId === gameId" @tile="emit('tile', $event)" /><CardRecordText v-else :actor-name="entry.actorName" :content="entry.content" :text="entry.text" :players="entry.mentions?.length ? entry.mentions : entry.actorName ? [{ name: entry.actorName, color: '#416d55' }] : undefined" /></p></div><button class="button ghost small" :disabled="previewLoading" @click="inspect(entry.snapshotId)">当时局面 ↗</button></article></div>
    <p v-if="loading" class="muted" role="status">正在读取历史…</p><p v-else-if="!entries.length && !error" class="muted">这个范围暂时没有记录。</p><button v-if="nextCursor !== null" class="button secondary" :disabled="loading" @click="load(true)">更早的事件 ↓</button>
    <Modal panel-class="modern-modal history-snapshot-modal" v-if="preview" title="历史局面回顾" wide @close="preview = null"><p class="notice">{{ new Date(preview.at).toLocaleString('zh-CN') }} · 第 {{ preview.turnNumber }} 次行动 · {{ preview.label || '自动保存点' }}</p><Board v-if="preview.state" :room="preview.state" spectator @select="selectedTile = $event" /><p v-else class="muted">这个保存点没有可展示的棋盘，请查看公开记录。</p><p v-if="selectedTile" class="notice"><PlayerText :players="preview.state?.players" :text="`${selectedTile.name} · ${selectedTile.type === 'property' ? `${preview.state?.players.find(player => player.id === selectedTile?.ownerId)?.name ?? '待购入'} · ${selectedTile.level}级${selectedTile.mortgaged ? ' · 已抵押' : ''}` : '特殊地块'}${selectedTile.barrier ? ' · 路障' : ''}`" /></p><PropertyEffects v-if="selectedTile" :tile="selectedTile" :economy="preview.state?.economy" /></Modal>
  </div>
</template>
