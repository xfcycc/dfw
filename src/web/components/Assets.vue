<script setup lang="ts">
import { computed } from 'vue';
import { rentFor, RULES } from '../../shared/rules';
import { useGameStore } from '../stores/game';
const game = useGameStore();
const owned = computed(() => game.room?.board.filter(tile => tile.ownerId === game.me?.id) ?? []);
const allowed = computed(() => game.canAct && game.isMyTurn && !game.room?.tradePending && game.room?.phase !== 'rent');
</script>
<template>
  <p class="muted">只有停在自己的地块时才能建房。卖房、抵押和赎回可在自己的回合操作。</p>
  <div v-if="game.room?.phase === 'debt'" class="notice warning">先卖房、抵押或卖地筹集现金，足够后将自动支付债务。</div>
  <div v-if="!owned.length" class="empty-state">⌂<h3>你的地产故事还未开始</h3><p>走到无主地块时，就有机会买下它。</p></div>
  <div v-for="tile in owned" :key="tile.id" class="asset-row">
    <div class="asset-info"><span :class="`district-${tile.district}`" class="asset-icon">⌂</span><div><strong>{{ tile.name }}</strong><small>地价 ¥{{ tile.price }} · {{ tile.level }} 级 · {{ tile.mortgaged ? '已抵押' : `租金 ¥${rentFor(tile)}` }}</small></div></div>
    <div class="asset-actions">
      <button v-if="tile.level > 0" class="button small secondary" :disabled="!allowed" @click="game.act({ type: 'sellBuilding', tileId: tile.id })">卖一级房 +{{ tile.price * RULES.sellBuildingRate }}</button>
      <button v-if="!tile.mortgaged" class="button small secondary" :disabled="!allowed || tile.level > 0" @click="game.act({ type: 'mortgage', tileId: tile.id })">抵押 +{{ tile.price * RULES.mortgageRate }}</button>
      <button v-else class="button small secondary" :disabled="!allowed || game.room?.phase === 'debt' || (game.me?.cash ?? 0) < tile.price * RULES.redeemRate" @click="game.act({ type: 'redeem', tileId: tile.id })">赎回 −{{ tile.price * RULES.redeemRate }}</button>
      <button class="button small ghost danger" :disabled="!allowed || tile.level > 0" @click="game.act({ type: 'sellLand', tileId: tile.id })">卖地 +{{ tile.mortgaged ? 0 : tile.price * RULES.sellLandRate }}</button>
    </div>
  </div>
</template>
