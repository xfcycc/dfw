<script setup lang="ts">
import { ref } from 'vue';
import { archiveRequest, formatArchiveCode } from '../archive';
import { useGameStore } from '../stores/game';
import type { ArchiveSnapshot } from '../../shared/types';
const game = useGameStore();
const label = ref('');
const loading = ref(false);
const error = ref('');
const saved = ref<ArchiveSnapshot | null>(null);
/** 无入参，标记当前版本为手动保存点，保留固定码且不暂停游戏。 */
async function save(): Promise<void> {
  if (!game.session || !game.room) return;
  loading.value = true; error.value = '';
  try {
    saved.value = await archiveRequest<ArchiveSnapshot>('archives/save', { token: game.session.token, expectedVersion: game.room.version, label: label.value || undefined });
    game.notify('当前进度已标记为存档');
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '存档失败'; }
  finally { loading.value = false; }
}
</script>
<template>
  <p class="muted">每次成功操作都会自动保存。这里可以给当前进度加一条备注，方便以后找到。</p>
  <form @submit.prevent="save">
    <label class="field">存档备注 <span class="optional">选填</span><input v-model="label" placeholder="例如：今晚先玩到这里" /></label>
    <p v-if="error" class="error-text" role="alert">{{ error }}</p>
    <p v-if="saved" class="notice">已保存第 {{ saved.turnNumber }} 次行动的进度。</p>
    <button class="button primary full" :disabled="loading || game.connection !== 'online'">{{ loading ? '正在存档…' : '标记当前进度' }}</button>
  </form>
  <div v-if="game.privateData?.archiveCode" class="archive-code-box"><span>本房间固定存档码</span><strong>{{ formatArchiveCode(game.privateData.archiveCode) }}</strong><button class="button secondary small" @click="game.copy(formatArchiveCode(game.privateData!.archiveCode!))">复制存档码</button><p class="muted">拿到这个码的人可以查看历史，并从保存点另建房间继续。</p></div>
</template>
