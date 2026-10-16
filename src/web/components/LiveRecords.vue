<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import type { HistoryEntry, HistoryPage, PublicLog, PublicRoomState } from '../../shared/types';
import { postJson } from '../http';
import PlayerText from './PlayerText.vue';
const props = defineProps<{ room: PublicRoomState; token?: string; compact?: boolean; journal?: boolean; chatOnly?: boolean; active?: boolean }>();
const stored = ref(new Map<number, HistoryEntry>());
const streamed = ref(new Map<string, PublicLog>());
const cursor = ref<number | null>(null);
const loading = ref(false);
const error = ref('');
const search = ref('');
const feed = ref<HTMLElement>();
const following = ref(true);
let loadingEarlier = false;
let feedHeight = 0;
let resizeObserver: ResizeObserver | undefined;
let generation = 0;
let timer: ReturnType<typeof setTimeout> | undefined;
let queued = false;
/** 合并存档和实时动态，时间相同时按历史序号及快照原始顺序展示，返回正序记录。 */
const rows = computed(() => {
  const history = [...stored.value.values()].filter(entry => entry.version <= props.room.version).sort((a, b) => a.sequence - b.sequence);
  const ids = new Set(history.map(entry => entry.logId).filter(Boolean));
  const legacy = new Set(history.filter(entry => !entry.logId).map(entry => `${entry.at}:${entry.text}`));
  const live = [...streamed.value.values()].filter(entry => !ids.has(entry.id) && !legacy.has(`${entry.at}:${entry.text}`));
  return [...history.map(entry => ({ id: `history-${entry.sequence}`, at: entry.at, text: entry.text, type: entry.type, mentions: entry.mentions })), ...live].sort((a, b) => a.at - b.at).filter(entry => (!props.chatOnly || entry.type === 'chat') && entry.text.includes(search.value.trim()));
});
/** 输入是否翻页，读取本局完整公开动态；头页更新与旧页合并，不清空已查记录。 */
async function load(more = false): Promise<void> {
  if (!props.token) return;
  if (loading.value) { if (!more) queued = true; return; }
  const current = generation; loading.value = true; error.value = '';
  const previousHeight = feed.value?.scrollHeight ?? 0;
  const previousTop = feed.value?.scrollTop ?? 0;
  loadingEarlier = more;
  try {
    const page = await postJson<HistoryPage>('rooms/logs', { token: props.token, ...(more && cursor.value != null ? { cursor: cursor.value } : {}) });
    if (current !== generation) return;
    for (const entry of page.entries) stored.value.set(entry.sequence, entry);
    if (more || stored.value.size <= page.entries.length) cursor.value = page.nextCursor;
    await nextTick();
    if (current !== generation) return;
    if (more && feed.value) feed.value.scrollTop = previousTop + feed.value.scrollHeight - previousHeight;
    else if (following.value) scrollLatest();
  } catch (cause) { if (current === generation) error.value = cause instanceof Error ? cause.message : '完整实时记录读取失败'; }
  finally { if (current === generation) { loading.value = false; loadingEarlier = false; if (queued) { queued = false; void load(); } } }
}
watch([() => props.room.roomId ?? props.room.code, () => props.room.gameId, () => props.token], () => {
  generation++; loading.value = false; loadingEarlier = false; following.value = true; queued = false; stored.value.clear(); streamed.value.clear(); cursor.value = null;
  for (const entry of [...props.room.logs].reverse()) streamed.value.set(entry.id, entry);
  void load();
}, { immediate: true });
watch(() => props.room.logs, logs => {
  for (const entry of [...logs].reverse()) streamed.value.set(entry.id, entry);
  clearTimeout(timer); timer = setTimeout(() => { void load(); }, 200);
});
/** 滚动到最新动态，只作用于记录区域，不改变页面或棋盘位置。 */
function scrollLatest(): void {
  if (!feed.value) return;
  feed.value.scrollTop = feed.value.scrollHeight; following.value = true;
}
/** 读取用户滚动位置，查看旧记录时暂停跟随新增动态。 */
function observeScroll(): void {
  if (!feed.value || loadingEarlier || feed.value.clientHeight !== feedHeight) return;
  following.value = feed.value.scrollHeight - feed.value.scrollTop - feed.value.clientHeight < 40;
}
/** 记录区域随布局改变尺寸时，维持原来的跟随状态，避免最新动态留在可视区外。 */
watch(feed, element => {
  resizeObserver?.disconnect();
  if (!element) return;
  feedHeight = element.clientHeight;
  resizeObserver = new ResizeObserver(() => {
    feedHeight = element.clientHeight;
    if (following.value && !loadingEarlier && props.active !== false) scrollLatest();
  });
  resizeObserver.observe(element);
});
watch([rows, () => props.active], async () => {
  const follow = following.value && !loadingEarlier;
  await nextTick();
  if (follow && !loadingEarlier && props.active !== false) scrollLatest();
});
onBeforeUnmount(() => { generation++; clearTimeout(timer); resizeObserver?.disconnect(); });
</script>
<template>
  <section class="live-records" aria-label="完整实时记录">
    <div v-if="!journal" class="section-heading"><h2>{{ compact ? `已加载 ${rows.length} 条 · 最新在下` : '📣 实时记录' }}</h2><button class="button secondary small" :disabled="loading" @click="load()">刷新</button></div>
    <p v-if="!compact" class="muted">本局公开动态、道具目标与效果、事件及结算结果，最新在下。已加载 {{ rows.length }} 条。</p>
    <label v-if="!journal" class="field">查找本局动态<input v-model="search" type="search" placeholder="玩家、道具、地块或事件" /></label>
    <p v-if="error" class="notice warning">{{ error }}。已同步动态仍可查阅。</p>
    <div ref="feed" class="live-record-feed" @scroll="observeScroll">
      <button v-if="cursor != null" class="button secondary full" :disabled="loading" @click="load(true)">加载更早的本局记录</button>
    <article v-for="entry in rows" :key="entry.id" class="live-record-row" :class="{ 'chat-record': entry.type === 'chat' }"><time>{{ new Date(entry.at).toLocaleTimeString('zh-CN') }}</time><small v-if="entry.type === 'chat'" class="chat-record-tag">聊天</small><p><PlayerText :text="entry.text" :players="entry.mentions?.length ? entry.mentions : room.players" /></p></article>
    <p v-if="!rows.length" class="muted">{{ loading ? '正在读取动态…' : chatOnly ? '还没有聊天消息。' : '没有匹配的记录。' }}</p>
    </div>
    <button v-if="!following" class="latest-record-button" @click="scrollLatest">回到最新动态 ↓</button>
  </section>
</template>
<style scoped>
.live-records { display: flex; flex-direction: column; min-height: 0; height: 100%; position: relative; }
.live-record-feed { flex: 1; min-height: 0; overflow-y: auto; overscroll-behavior: contain; scrollbar-width: thin; }
.latest-record-button { position: absolute; bottom: 10px; right: 12px; border: 1px solid #dce8db; border-radius: 18px; padding: 7px 12px; background: #fff; color: #58745e; box-shadow: 0 2px 8px #34563812; font-size: 11px; }
.live-record-row { padding: 10px 0; border-top: 1px solid #e6e7d8; }
.live-record-row time { color: #7f8c76; font-size: 10px; }
.live-record-row p { margin: 5px 0; font-size: 12px; line-height: 1.9; overflow-wrap: anywhere; }
</style>
