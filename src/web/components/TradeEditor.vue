<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useGameStore } from '../stores/game';
import { redeemCost } from '../../shared/rules';
const game = useGameStore();
const emit = defineEmits<{ done: [] }>();
const toId = ref('');
const giveCash = ref(0); const wantCash = ref(0);
const debtCredit = ref(0);
const giveTiles = ref<number[]>([]); const wantTiles = ref<number[]>([]);
const trade = computed(() => game.privateData?.trade);
const payment = computed(() => game.room?.pending?.type === 'rent' || game.room?.pending?.type === 'debt' ? game.room.pending : null);
const toCreditor = computed(() => !!payment.value?.creditorId && toId.value === payment.value.creditorId);
const canOffer = computed(() => game.canAct && game.canDecide && !game.room?.tradePending && (['pre_roll', 'manage'].includes(game.room?.phase ?? '') || !!payment.value?.creditorId));
const partners = computed(() => game.room?.players.filter(player => !player.bankrupt && player.id !== game.me?.id) ?? []);
const partner = computed(() => game.room?.players.find(player => player.id === toId.value));
const own = computed(() => game.room?.board.filter(tile => tile.ownerId === game.me?.id) ?? []);
const theirs = computed(() => game.room?.board.filter(tile => tile.ownerId === toId.value) ?? []);
watch(partners, values => { if (!values.some(value => value.id === toId.value)) toId.value = values.find(value => value.id === payment.value?.creditorId)?.id ?? values[0]?.id ?? ''; }, { immediate: true });
watch(toId, () => { wantTiles.value = []; wantCash.value = 0; debtCredit.value = 0; });
/** 输入玩家 ID，返回交易弹窗中的公开昵称。 */
function name(id: string): string { return game.room?.players.find(player => player.id === id)?.name ?? '玩家'; }
/** 输入地产索引列表，返回公开地产名称。 */
function tileNames(ids: number[]): string { return ids.map(id => { const tile = game.room?.board[id]; return tile ? `${tile.name}（${tile.level} 级${tile.mortgaged ? `，赎回 ¥${redeemCost(tile)}` : ''}）` : ''; }).join('、') || '无地产'; }
/** 输入交易确认操作，发送后在成功时关闭。 */
async function respond(accept: boolean): Promise<void> { if (await game.act({ type: 'tradeRespond', accept })) emit('done'); }
/** 将当前报价发送给所选对手，报价仅对双方可见。 */
async function submit(): Promise<void> {
  await game.act({ type: 'tradeOffer', toId: toId.value, giveCash: Number(giveCash.value), wantCash: Number(wantCash.value), giveTiles: giveTiles.value, wantTiles: wantTiles.value, debtCredit: toCreditor.value ? Number(debtCredit.value) : 0 });
}
</script>
<template>
  <template v-if="trade">
    <p class="muted">{{ name(trade.fromId) }} 与 {{ name(trade.toId) }} 的交易报价</p>
    <div class="trade-columns"><div class="trade-side"><h3>{{ name(trade.fromId) }} 提供</h3><strong class="trade-cash">¥{{ trade.giveCash.toLocaleString() }}</strong><p>{{ tileNames(trade.giveTiles) }}</p></div><div class="trade-side"><h3>{{ name(trade.toId) }} 提供</h3><strong class="trade-cash">¥{{ trade.wantCash.toLocaleString() }}</strong><p>{{ tileNames(trade.wantTiles) }}</p><p v-if="trade.debtCredit">同意抵扣债务 <strong>¥{{ trade.debtCredit.toLocaleString() }}</strong>，此项无需支付现金。</p></div></div>
    <p v-if="trade.debtId" class="notice">接受后剩余待支付 ¥{{ Math.max(0, (payment?.amount ?? 0) - (trade.debtCredit ?? 0)).toLocaleString() }}。拒绝或取消后，继续处理原债务。</p>
    <p v-if="trade.debtId && trade.giveCash > 0" class="muted">报价中的现金为额外交易对价；欠款只减少双方约定的抵扣金额。</p>
    <p class="notice">建筑及固定赎回费用随地产转移。报价不限时，等待双方接受、拒绝或主动取消。{{ game.room?.pausedReason ? '相关玩家离线，重连后继续处理。' : '' }}</p>
    <div v-if="trade.toId === game.me?.id" class="button-row"><button class="button secondary" :disabled="!game.canAct" @click="respond(false)">拒绝</button><button class="button primary" :disabled="!game.canAct" @click="respond(true)">接受并完成交易</button></div>
    <button v-else class="button secondary full" :disabled="!game.canAct" @click="game.act({ type: 'tradeCancel' }).then(ok => { if (ok) emit('done'); })">取消报价</button>
  </template>
  <form v-else @submit.prevent="submit">
    <p v-if="payment?.creditorId" class="notice">向 {{ name(payment.creditorId) }} 尚需支付 ¥{{ payment.amount.toLocaleString() }}。可向债权人提出资产抵债，或向其他玩家出售资产筹款。</p>
    <label class="field">交易对象<select v-model="toId"><option v-for="player in partners" :key="player.id" :value="player.id">{{ player.name }}{{ player.online ? '' : '（离线）' }}</option></select></label>
    <div class="trade-columns">
      <section class="trade-side"><h3>我提供</h3><label class="field">现金 <small>持有 ¥{{ game.me?.cash }}</small><input v-model.number="giveCash" type="number" min="0" :max="game.me?.cash" step="1" /></label><label v-for="tile in own" :key="tile.id" class="checkbox-row"><input v-model="giveTiles" type="checkbox" :value="tile.id" /><span>{{ tile.name }}<small>{{ tile.level }} 级{{ tile.mortgaged ? ` · 赎回 ¥${redeemCost(tile)}` : '' }}</small></span></label><p v-if="!own.length" class="muted">暂无地产</p></section>
      <section class="trade-side"><h3>对方提供</h3><label class="field">现金 <small>持有 ¥{{ partner?.cash }}</small><input v-model.number="wantCash" type="number" min="0" :max="partner?.cash" step="1" /></label><label v-if="toCreditor" class="field">协商抵扣债务 <small>余额 ¥{{ payment?.amount }}，无需对方付现金</small><input v-model.number="debtCredit" type="number" min="0" :max="payment?.amount" step="1" /></label><label v-for="tile in theirs" :key="tile.id" class="checkbox-row"><input v-model="wantTiles" type="checkbox" :value="tile.id" /><span>{{ tile.name }}<small>{{ tile.level }} 级{{ tile.mortgaged ? ` · 赎回 ¥${redeemCost(tile)}` : '' }}</small></span></label><p v-if="!theirs.length" class="muted">暂无地产</p></section>
    </div>
    <p class="muted">对方接受后一次性完成交换。抵债报价须提供至少一块地产，金额由双方协商；部分抵债后继续处理余额。报价期间暂停其他操作，手牌不参与交易。</p>
    <p v-if="toCreditor" class="muted">现金栏是额外交易对价。欠款只按协商抵扣金额减少，余额另行确认付款；纯资产抵债可以把双方现金都设为零。</p>
    <button class="button primary full" :disabled="!canOffer || !toId || (toCreditor && debtCredit > 0 && !giveTiles.length)">发送交易报价</button>
  </form>
</template>
