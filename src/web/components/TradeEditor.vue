<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useGameStore } from '../stores/game';
const game = useGameStore();
const emit = defineEmits<{ done: [] }>();
const toId = ref('');
const giveCash = ref(0); const wantCash = ref(0);
const giveTiles = ref<number[]>([]); const wantTiles = ref<number[]>([]);
const now = ref(Date.now());
const timer = setInterval(() => { now.value = Date.now(); }, 500);
onBeforeUnmount(() => clearInterval(timer));
const trade = computed(() => game.privateData?.trade);
const partners = computed(() => game.room?.players.filter(player => !player.bankrupt && player.id !== game.me?.id) ?? []);
const partner = computed(() => game.room?.players.find(player => player.id === toId.value));
const own = computed(() => game.room?.board.filter(tile => tile.ownerId === game.me?.id) ?? []);
const theirs = computed(() => game.room?.board.filter(tile => tile.ownerId === toId.value) ?? []);
const seconds = computed(() => trade.value ? Math.max(0, Math.ceil((trade.value.remainingMs - (trade.value.runningSince === null ? 0 : now.value - trade.value.runningSince)) / 1000)) : 0);
watch(partners, values => { if (!values.some(value => value.id === toId.value)) toId.value = values[0]?.id ?? ''; }, { immediate: true });
watch(toId, () => { wantTiles.value = []; wantCash.value = 0; });
/** 输入玩家 ID，返回交易弹窗中的公开昵称。 */
function name(id: string): string { return game.room?.players.find(player => player.id === id)?.name ?? '玩家'; }
/** 输入地产索引列表，返回公开地产名称。 */
function tileNames(ids: number[]): string { return ids.map(id => game.room?.board[id]?.name).join('、') || '无地产'; }
/** 输入交易确认操作，发送后在成功时关闭。 */
async function respond(accept: boolean): Promise<void> { if (await game.act({ type: 'tradeRespond', accept })) emit('done'); }
/** 将当前报价发送给所选对手，报价仅对双方可见。 */
async function submit(): Promise<void> {
  await game.act({ type: 'tradeOffer', toId: toId.value, giveCash: Number(giveCash.value), wantCash: Number(wantCash.value), giveTiles: giveTiles.value, wantTiles: wantTiles.value });
}
</script>
<template>
  <template v-if="trade">
    <p class="muted">{{ name(trade.fromId) }} 与 {{ name(trade.toId) }} 的交易报价</p>
    <div class="trade-columns"><div class="trade-side"><h3>{{ name(trade.fromId) }} 提供</h3><strong class="trade-cash">¥{{ trade.giveCash.toLocaleString() }}</strong><p>{{ tileNames(trade.giveTiles) }}</p></div><div class="trade-side"><h3>{{ name(trade.toId) }} 提供</h3><strong class="trade-cash">¥{{ trade.wantCash.toLocaleString() }}</strong><p>{{ tileNames(trade.wantTiles) }}</p></div></div>
    <p class="notice">建筑与抵押状态随地产转移。{{ trade.runningSince === null ? '离线等待中，倒计时暂停。' : `${seconds} 秒后自动取消报价。` }}</p>
    <div v-if="trade.toId === game.me?.id" class="button-row"><button class="button secondary" :disabled="!game.canAct" @click="respond(false)">拒绝</button><button class="button primary" :disabled="!game.canAct" @click="respond(true)">接受并完成交易</button></div>
    <button v-else class="button secondary full" :disabled="!game.canAct" @click="game.act({ type: 'tradeCancel' }).then(ok => { if (ok) emit('done'); })">取消报价</button>
  </template>
  <form v-else @submit.prevent="submit">
    <label class="field">交易对象<select v-model="toId"><option v-for="player in partners" :key="player.id" :value="player.id">{{ player.name }}{{ player.online ? '' : '（离线）' }}</option></select></label>
    <div class="trade-columns">
      <section class="trade-side"><h3>我提供</h3><label class="field">现金 <small>持有 ¥{{ game.me?.cash }}</small><input v-model.number="giveCash" type="number" min="0" :max="game.me?.cash" step="1" /></label><label v-for="tile in own" :key="tile.id" class="checkbox-row"><input v-model="giveTiles" type="checkbox" :value="tile.id" /><span>{{ tile.name }}<small>{{ tile.level }} 级{{ tile.mortgaged ? ' · 已抵押' : '' }}</small></span></label><p v-if="!own.length" class="muted">暂无地产</p></section>
      <section class="trade-side"><h3>对方提供</h3><label class="field">现金 <small>持有 ¥{{ partner?.cash }}</small><input v-model.number="wantCash" type="number" min="0" :max="partner?.cash" step="1" /></label><label v-for="tile in theirs" :key="tile.id" class="checkbox-row"><input v-model="wantTiles" type="checkbox" :value="tile.id" /><span>{{ tile.name }}<small>{{ tile.level }} 级{{ tile.mortgaged ? ' · 已抵押' : '' }}</small></span></label><p v-if="!theirs.length" class="muted">暂无地产</p></section>
    </div>
    <p class="muted">对方接受后一次性完成交换。报价期间暂停其他操作，手牌不参与交易。</p>
    <button class="button primary full" :disabled="!game.canAct || !game.isMyTurn || !toId">发送交易报价</button>
  </form>
</template>
