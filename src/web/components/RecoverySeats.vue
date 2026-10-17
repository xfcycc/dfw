<script setup lang="ts">
import { ref } from 'vue';
import { archiveRequest } from '../archive';
import { useGameStore } from '../stores/game';
import type { SeatClaim } from '../../shared/types';
const game = useGameStore();
const selected = ref('');
const name = ref(localStorage.getItem('dfw:nickname') || '');
const loading = ref(false);
const error = ref('');
const claim = ref<SeatClaim | null>(null);
/** 无入参，以当前访客身份申请已保存角色，不根据昵称自动认领。 */
async function requestSeat(): Promise<void> {
  if (!game.session || !selected.value) return;
  loading.value = true; error.value = '';
  try { claim.value = await archiveRequest<SeatClaim>('rooms/claim', { token: game.session.token, playerId: selected.value, name: name.value }); }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '申请失败'; }
  finally { loading.value = false; }
}
/** 无入参，在恢复大厅解除暂停后沿用普通加入规则创建新玩家，不覆盖已有角色。 */
async function joinLobby(): Promise<void> {
  if (!game.room) return;
  loading.value = true; error.value = '';
  try { const session = await game.enter('join', game.room.code, name.value); game.connect(session); }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '加入失败'; }
  finally { loading.value = false; }
}
</script>
<template>
  <section v-if="game.room?.recovered && (!game.me || game.isHost && (game.room.recoveryPending || game.privateData?.seatClaims?.length))" class="panel recovery-panel">
    <div class="section-heading"><h2>{{ game.me ? '分配保存下来的角色' : '申请你的角色' }}</h2><span>恢复房间</span></div>
    <template v-if="!game.me">
      <p class="muted">角色的现金、地产、手牌与进度都已保留。选择角色，等待房主批准。</p>
      <form @submit.prevent="requestSeat"><label class="field">你的昵称 <span class="optional">选填</span><input v-model="name" placeholder="方便房主认出你" /></label><label class="field">保存角色<select v-model="selected" required><option value="" disabled>选择要使用的角色</option><option v-for="player in game.room.players.filter(player => !player.botDifficulty)" :key="player.id" :value="player.id">{{ player.name }}{{ player.online ? ' · 在线' : '' }}</option></select></label><p v-if="error" class="error-text" role="alert">{{ error }}</p><p v-if="claim" class="notice">申请已保存，等待房主批准。</p><button class="button primary" :disabled="loading || game.connection !== 'online'">{{ loading ? '正在申请…' : '申请角色' }}</button></form>
      <button v-if="game.room.status === 'lobby' && !game.room.recoveryPending && game.room.players.length < 6" class="button secondary" :disabled="loading || game.connection !== 'online'" @click="joinLobby">以新玩家身份加入大厅</button>
    </template>
    <template v-else-if="game.isHost">
      <p class="muted">邀请朋友进入房间后，在这里批准他们的角色申请。重新分配会撤销该角色的旧身份。</p>
      <div v-for="entry in game.privateData?.seatClaims" :key="entry.id" class="archive-list-row"><div><strong>{{ entry.name }}</strong><p class="muted">申请 {{ game.room.players.find(player => player.id === entry.playerId)?.name }}</p></div><button class="button secondary small" :disabled="game.busy || game.connection !== 'online'" @click="game.act({ type: 'assignSeat', claimId: entry.id })">分配角色</button></div>
      <p v-if="!game.privateData?.seatClaims?.length" class="muted">暂时没有待处理申请。</p>
      <button v-if="game.room.recoveryPending" class="button primary" :disabled="game.busy || game.connection !== 'online'" @click="game.act({ type: 'continueRecovery' })">继续对局 ↗</button>
    </template>
  </section>
</template>
