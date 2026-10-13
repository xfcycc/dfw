<script setup lang="ts">
import { computed } from 'vue';
import { mortgageValue, redeemCost, rentFor, sellBuildingValue, landValue } from '../../shared/rules';
import { useGameStore } from '../stores/game';
const game = useGameStore();
const owned = computed(() => game.room?.board.filter(tile => tile.ownerId === game.me?.id) ?? []);
const allowed = computed(() => game.canAct && game.canDecide && !game.room?.tradePending);
const payment = computed(() => game.room?.pending?.type === 'rent' || game.room?.pending?.type === 'debt');
</script>
<template>
  <p class="muted">只有停在自己的地块时才能建房。可以将土地和建筑整体抵押，保留建筑并暂停收租；抵押期间不能单独卖房。由你结算费用时也可管理资产，期间不可赎回。</p>
  <div v-if="payment" class="notice warning">资产变现后现金由你保留。关闭此窗，确认支付费用；向玩家付款也可协商资产抵债。</div>
  <div v-if="!owned.length" class="empty-state">⌂<h3>你的地产故事还未开始</h3><p>走到无主地块时，就有机会买下它。</p></div>
  <div v-for="tile in owned" :key="tile.id" class="asset-row">
    <div class="asset-info"><span :class="`district-${tile.district}`" class="asset-icon">⌂</span><div><strong>{{ tile.name }}</strong><small>地价 ¥{{ tile.price }} · {{ tile.level }} 级 · {{ tile.mortgaged ? `已抵押 · 赎回 ¥${redeemCost(tile)}` : `租金 ¥${rentFor(tile, game.room?.economy)}` }}</small></div></div>
    <div class="asset-actions">
      <button v-if="tile.level > 0" class="button small secondary" :disabled="!allowed || tile.mortgaged" @click="game.act({ type: 'sellBuilding', tileId: tile.id })">卖一级房 +{{ sellBuildingValue(tile, game.room?.economy) }}</button>
      <button v-if="!tile.mortgaged" class="button small secondary" :disabled="!allowed" @click="game.act({ type: 'mortgage', tileId: tile.id })">整体抵押 +{{ mortgageValue(tile, game.room?.economy) }}</button>
      <button v-else class="button small secondary" :disabled="!allowed || payment || (game.me?.cash ?? 0) < redeemCost(tile)" @click="game.act({ type: 'redeem', tileId: tile.id })">赎回 −{{ redeemCost(tile) }}</button>
      <button class="button small ghost danger" :disabled="!allowed || tile.level > 0" @click="game.act({ type: 'sellLand', tileId: tile.id })">卖地 +{{ landValue(tile, game.room?.economy) }}</button>
    </div>
  </div>
</template>
