<script setup lang="ts">
import ThemedSelect from './ThemedSelect.vue';
import { isKamui } from '../../shared/rules';
import { computed, ref, watch } from 'vue';
import { useGameStore } from '../stores/game';
import PlayerText from './PlayerText.vue';
import CharacterPawn from './CharacterPawn.vue';
import { redeemCost } from '../../shared/rules';
const props = defineProps<{ offerDisabled?: boolean }>();
const game = useGameStore();
const emit = defineEmits<{ done: [completed: boolean] }>();
const toId = ref('');
const giveCash = ref(0); const wantCash = ref(0);
const debtCredit = ref(0);
const giveTiles = ref<number[]>([]); const wantTiles = ref<number[]>([]);
let cancelledTradeId = '';
const trade = computed(() => game.privateData?.trade);
const payment = computed(() => game.room?.pending?.type === 'rent' || game.room?.pending?.type === 'debt' ? game.room.pending : null);
const toCreditor = computed(() => !!payment.value?.creditorId && toId.value === payment.value.creditorId);
const canOffer = computed(() => (!game.room?.activity || game.room.activity.stage === 'payment') && !props.offerDisabled && game.canAct && game.canDecide && !game.room?.tradePending && game.room?.pending?.type !== 'choice' && (!payment.value || payment.value.defenseResolved) && (['pre_roll', 'manage'].includes(game.room?.phase ?? '') || !!payment.value?.creditorId));
const partners = computed(() => game.room?.players.filter(player => !player.bankrupt && player.id !== game.me?.id) ?? []);
const partner = computed(() => game.room?.players.find(player => player.id === toId.value));
const own = computed(() => game.room?.board.filter(tile => tile.ownerId === game.me?.id && !isKamui(tile)) ?? []);
const theirs = computed(() => game.room?.board.filter(tile => tile.ownerId === toId.value && !isKamui(tile)) ?? []);
const offer = computed(() => trade.value ?? { fromId: game.me?.id ?? '', toId: toId.value, giveCash: giveCash.value, wantCash: wantCash.value, giveTiles: giveTiles.value, wantTiles: wantTiles.value, debtId: payment.value?.id, debtCredit: toCreditor.value ? debtCredit.value : 0 });
const from = computed(() => game.room?.players.find(player => player.id === offer.value.fromId));
const to = computed(() => game.room?.players.find(player => player.id === offer.value.toId));
const counterparty = computed(() => from.value?.id === game.me?.id ? to.value : from.value);
const associatedDebt = computed(() => !!payment.value?.creditorId && (!trade.value || !!trade.value.debtId && trade.value.debtId === payment.value.id));
const previewError = computed(() => {
  const value = offer.value;
  if (!from.value || !to.value || from.value.id === to.value.id || from.value.bankrupt || to.value.bankrupt) return '请选择有效交易对象。';
  if (![value.giveCash, value.wantCash, value.debtCredit ?? 0].every(amount => typeof amount === 'number' && Number.isSafeInteger(amount) && amount >= 0)) return '请输入有效的非负整数金额，再查看成交后结果。';
  if (value.giveCash > from.value.cash || value.wantCash > to.value.cash) return '报价现金超过对应玩家持有现金，请调整后查看结果。';
  if (value.giveTiles.some(id => game.room?.board[id]?.ownerId !== from.value?.id) || value.wantTiles.some(id => game.room?.board[id]?.ownerId !== to.value?.id)) return '报价地产的归属已变化，请重新选择。';
  if ([...value.giveTiles, ...value.wantTiles].some(id => game.room?.board[id] && isKamui(game.room.board[id]!))) return '神威空间中的地产不能交易。';
  if (new Set([...value.giveTiles, ...value.wantTiles]).size !== value.giveTiles.length + value.wantTiles.length) return '交易地产不能重复。';
  if ((value.debtCredit ?? 0) > 0 && (!associatedDebt.value || value.toId !== payment.value?.creditorId || (value.debtCredit ?? 0) > (payment.value?.amount ?? 0))) return '协商结清须向当前债权人报价，抵扣金额不能超过当前欠款。';
  if (!value.giveCash && !value.wantCash && !value.giveTiles.length && !value.wantTiles.length && !(value.debtCredit ?? 0)) return '选择现金、地产或填写协商抵扣金额后，这里将展示成交后的结果。';
  return '';
});
const outcomes = computed(() => {
  if (previewError.value || !from.value || !to.value) return [];
  const value = offer.value;
  return [
    { player: from.value, received: value.wantCash, paid: value.giveCash, gained: value.wantTiles, lost: value.giveTiles },
    { player: to.value, received: value.giveCash, paid: value.wantCash, gained: value.giveTiles, lost: value.wantTiles },
  ].sort((a, b) => Number(b.player.id === game.me?.id) - Number(a.player.id === game.me?.id));
});
watch(partners, values => { if (!values.some(value => value.id === toId.value)) toId.value = values.find(value => value.id === payment.value?.creditorId)?.id ?? values[0]?.id ?? ''; }, { immediate: true });
// 切换对象时同步清空对方报价，保证详情入口随后勾选的地产不会被异步清掉。
watch(toId, () => { wantTiles.value = []; wantCash.value = 0; debtCredit.value = 0; }, { flush: 'sync' });
// 输入前后交易状态，仅本人报价成交后清空草稿；拒绝、取消和断线均保留原内容。
watch(trade, (current, previous) => {
  if (current || !previous || !game.privateData || previous.fromId !== game.me?.id || game.privateData.tradeRejection || previous.id === cancelledTradeId) return;
  giveCash.value = 0; wantCash.value = 0; debtCredit.value = 0;
  giveTiles.value = []; wantTiles.value = [];
});
/** 输入详情地产编号，按当前归属选择交易对象并勾选地产；返回是否成功，保留同一对象的其他草稿内容。 */
function selectProperty(tileId: number): boolean {
  if (!canOffer.value || trade.value) return false;
  const tile = game.room?.board[tileId];
  if (tile?.type !== 'property' || !tile.ownerId) return false;
  if (tile.ownerId === game.me?.id && !isKamui(tile)) {
    if (!giveTiles.value.includes(tileId)) giveTiles.value.push(tileId);
  } else {
    if (!partners.value.some(player => player.id === tile.ownerId)) return false;
    toId.value = tile.ownerId;
    if (!wantTiles.value.includes(tileId)) wantTiles.value.push(tileId);
  }
  return true;
}
defineExpose({ selectProperty });
/** 输入当前报价、增加金额及持有现金，返回累加后不超过持有现金的报价；空输入按零处理。 */
function increaseCash(current: number, amount: number, limit = 0): number {
  return Math.min(Math.max(0, Number(current) || 0) + amount, Math.max(0, limit));
}
/** 输入玩家 ID，返回交易中的公开昵称及本人标记。 */
function name(id: string): string { return `${game.room?.players.find(player => player.id === id)?.name ?? '玩家'}${id === game.me?.id ? '（我）' : ''}`; }
/** 输入地产索引列表，返回地块编号、地价、建筑及固定赎回费用。 */
function tileNames(ids: number[]): string { return ids.map(id => { const tile = game.room?.board[id]; return tile ? `${tile.name} · ${tile.id}号（地价 ¥${tile.price.toLocaleString()}，${tile.level}级${tile.mortgaged ? `，已抵押，赎回 ¥${redeemCost(tile).toLocaleString()}` : ''}）` : ''; }).join('、') || '无'; }
/** 输入是否接受报价，操作成功后通知父面板是否成交；拒绝时保留原草稿。 */
async function respond(accept: boolean): Promise<void> { if (accept && previewError.value) return; if (await game.act({ type: 'tradeRespond', accept })) emit('done', accept); }
/** 无入参，取消本人报价并保留草稿；取消失败时恢复该报价的成交监听，无返回值。 */
async function cancel(): Promise<void> {
  if (!trade.value) return;
  cancelledTradeId = trade.value.id;
  if (await game.act({ type: 'tradeCancel' })) emit('done', false);
  else cancelledTradeId = '';
}
/** 无入参，将有效报价发送给所选对手，双方及报价明细同步公开记录。 */
async function submit(): Promise<void> {
  if (!canOffer.value || previewError.value || trade.value) return;
  await game.act({ type: 'tradeOffer', toId: toId.value, giveCash: giveCash.value, wantCash: wantCash.value, giveTiles: giveTiles.value, wantTiles: wantTiles.value, debtCredit: toCreditor.value ? debtCredit.value : 0 });
}
</script>
<template>
  <div class="trade-identity" aria-label="交易双方">
    <div><CharacterPawn :avatar-id="game.me?.avatarId" :color="game.me?.color" /><strong><small>我</small><PlayerText :text="game.me?.name ?? '玩家'" /></strong></div>
    <span aria-hidden="true">⇄</span>
    <div><CharacterPawn v-if="counterparty" :avatar-id="counterparty.avatarId" :color="counterparty.color" /><span v-else class="avatar">?</span><strong><small>交易对象{{ payment?.creditorId && counterparty?.id === payment.creditorId ? ' · 债权人' : '' }}</small><PlayerText :text="counterparty?.name ?? '请选择玩家'" /></strong></div>
  </div>
  <form @submit.prevent="submit">
    <template v-if="trade">
      <p class="notice"><PlayerText :text="`${name(trade.fromId)} 发起报价，${name(trade.toId)} 确认后成交。`" />建筑及固定赎回费用随地产转移。{{ game.room?.pausedReason ? '相关玩家离线，重连后继续处理。' : '' }}</p>
    </template>
    <template v-else>
      <p v-if="game.privateData?.tradeRejection" class="notice warning" role="status"><PlayerText :text="game.privateData.tradeRejection" /></p>
      <p v-if="payment?.creditorId" class="notice"><PlayerText :text="`${name(payment.playerId)} 向 ${name(payment.creditorId)} 尚需支付 ¥${payment.amount.toLocaleString()}。`" />可向债权人提出协商结清，提供少量现金、地产或均不提供，债权人接受后才抵扣欠款；也可向其他玩家出售资产筹款。</p>
      <label class="field">交易对象<ThemedSelect v-model="toId" label="交易对象" :options="partners.map(player => ({ value: player.id, label: player.name + (player.id === payment?.creditorId ? '（债权人）' : '') + (player.online ? '' : '（离线）') }))" /></label>
      <div class="trade-columns">
        <section class="trade-side"><h3><PlayerText :text="`${game.me?.name}（我）提供`" /></h3><div class="field"><label>现金 <small>持有 ¥{{ game.me?.cash }}</small><input v-model.number="giveCash" type="number" min="0" :max="game.me?.cash" step="1" /></label><div class="button-row trade-cash-actions"><button v-for="amount in [1000, 100]" :key="amount" type="button" class="button secondary small" :disabled="giveCash >= (game.me?.cash ?? 0)" @click="giveCash = increaseCash(giveCash, amount, game.me?.cash)">+{{ amount }}</button></div></div><label v-for="tile in own" :key="tile.id" class="checkbox-row"><input v-model="giveTiles" type="checkbox" :value="tile.id" /><span>{{ tile.name }} · {{ tile.id }}号<small>地价 ¥{{ tile.price.toLocaleString() }} · {{ tile.level }}级{{ tile.mortgaged ? ` · 已抵押，赎回 ¥${redeemCost(tile)}` : '' }}</small></span></label><p v-if="!own.length" class="muted">暂无地产</p></section>
        <section class="trade-side"><h3><PlayerText :text="`${partner?.name ?? '对方'} 提供`" /></h3><div class="field"><label>现金 <small>持有 ¥{{ partner?.cash }}</small><input v-model.number="wantCash" type="number" min="0" :max="partner?.cash" step="1" /></label><div class="button-row trade-cash-actions"><button v-for="amount in [1000, 100]" :key="amount" type="button" class="button secondary small" :disabled="wantCash >= (partner?.cash ?? 0)" @click="wantCash = increaseCash(wantCash, amount, partner?.cash)">+{{ amount }}</button></div></div><label v-if="toCreditor" class="field">协商抵扣债务 <small>余额 ¥{{ payment?.amount }}，无需对方付现金</small><input v-model.number="debtCredit" type="number" min="0" :max="payment?.amount" step="1" /></label><label v-for="tile in theirs" :key="tile.id" class="checkbox-row"><input v-model="wantTiles" type="checkbox" :value="tile.id" /><span>{{ tile.name }} · {{ tile.id }}号<small>地价 ¥{{ tile.price.toLocaleString() }} · {{ tile.level }}级{{ tile.mortgaged ? ` · 已抵押，赎回 ¥${redeemCost(tile)}` : '' }}</small></span></label><p v-if="!theirs.length" class="muted">暂无地产</p></section>
      </div>
    </template>
    <section class="trade-preview" aria-label="成交后结果预览" aria-live="polite">
      <h3>成交后 · 双方确认才生效</h3>
      <p v-if="previewError" class="notice warning">{{ previewError }}</p>
      <template v-else>
        <div class="trade-columns"><section v-for="outcome in outcomes" :key="outcome.player.id" class="trade-side" :class="{ 'trade-me': outcome.player.id === game.me?.id }">
          <h3><PlayerText :text="name(outcome.player.id)" /></h3>
          <dl class="trade-outcome"><div><dt>获得地产</dt><dd>{{ tileNames(outcome.gained) }}</dd></div><div><dt>失去地产</dt><dd>{{ tileNames(outcome.lost) }}</dd></div><div><dt>收到现金</dt><dd>¥{{ outcome.received.toLocaleString() }}</dd></div><div><dt>支付现金</dt><dd>¥{{ outcome.paid.toLocaleString() }}</dd></div><div><dt>现金净变化</dt><dd>{{ outcome.received - outcome.paid >= 0 ? '+' : '−' }}¥{{ Math.abs(outcome.received - outcome.paid).toLocaleString() }}</dd></div><div><dt>最终现金</dt><dd><strong>¥{{ (outcome.player.cash + outcome.received - outcome.paid).toLocaleString() }}</strong></dd></div></dl>
        </section></div>
        <p v-if="associatedDebt && payment" class="notice warning"><PlayerText :text="`${name(payment.playerId)} 向 ${name(payment.creditorId!)}`" /> 的原欠款 ¥{{ payment.amount.toLocaleString() }}，协商抵扣 ¥{{ (offer.debtCredit ?? 0).toLocaleString() }}，成交后剩余欠款 <strong>¥{{ (payment.amount - (offer.debtCredit ?? 0)).toLocaleString() }}</strong>。{{ offer.toId !== payment.creditorId ? '本次向第三人交易只筹款，原欠款仍需另行确认支付。' : '债权人接受后才生效。现金按上方报价实际转移，不再额外减少欠款；现金和地产均可为零。' }}{{ payment.amount === (offer.debtCredit ?? 0) ? '接受后本笔欠款结清，无需另行付款。' : '剩余欠款需另行确认支付。' }}</p>
      </template>
    </section>
    <template v-if="trade">
      <div v-if="trade.toId === game.me?.id" class="button-row trade-actions"><button type="button" class="button secondary" :disabled="!game.canAct" @click="respond(false)"><PlayerText :text="`拒绝 ${counterparty?.name ?? '玩家'} 的报价`" /></button><button type="button" class="button primary" :disabled="!game.canAct || !!previewError" @click="respond(true)"><PlayerText :text="`接受与 ${counterparty?.name ?? '玩家'} 的交易`" /></button></div>
      <div v-else class="button-row trade-actions"><button type="button" class="button secondary" :disabled="!game.canAct" @click="cancel"><PlayerText :text="`取消向 ${counterparty?.name ?? '玩家'} 的报价`" /></button></div>
    </template>
    <template v-else>
      <p class="muted">对方接受后一次性完成交换。报价内容、同意、拒绝及取消均进入公开记录，观众可查看。向债权人协商结清可只提供少量现金，也可不提供现金和地产；填写约定抵扣金额，经债权人接受后抵扣，部分抵扣后继续处理余额。银行费用不适用。报价期间暂停其他操作，手牌不参与交易。</p>
      <div class="button-row trade-actions"><button class="button primary" :disabled="!canOffer || !!previewError"><PlayerText :text="`向 ${partner?.name ?? '对方'} 发送交易报价`" /></button></div>
    </template>
  </form>
</template>
<style scoped>
.trade-identity .character-pawn { width: 30px; height: 30px; border-radius: 9px; }
.trade-cash-actions { margin-top: 8px; }
.trade-actions { justify-content: flex-end; flex-wrap: wrap; }
.trade-actions > .button { flex: 0 1 auto; width: auto; max-width: 100%; }
</style>
