<script setup lang="ts">
import { computed } from 'vue';
import { mortgageValue, redeemCost, rentFor, sellBuildingValue, landValue } from '../../shared/rules';
import { useGameStore } from '../stores/game';
import PropertyArt from './PropertyArt.vue';
import PropertyGrade from './PropertyGrade.vue';
const game = useGameStore();
const owned = computed(() => game.room?.board.filter(tile => tile.ownerId === game.me?.id) ?? []);
const allowed = computed(() => (!game.room?.activity || game.room.activity.stage === 'payment') && game.canAct && game.canDecide && !game.room?.tradePending && game.room?.pending?.type !== 'choice' && (!payment.value || game.room?.pending?.defenseResolved));
const payment = computed(() => game.room?.pending?.type === 'rent' || game.room?.pending?.type === 'debt');
</script>
<template>
  <p v-if="payment" class="asset-payment-note">变现后请确认支付；玩家债务可协商抵债。</p>
  <p v-if="game.me?.jailed && game.me.skipTurns > 0" class="notice warning">服刑期间全部地产暂停收租，出狱后恢复；其他小游戏正常参与。</p>
  <div v-if="!owned.length" class="asset-empty">暂无地产，停在无主地块时可购买。</div>
  <div class="asset-portfolio">
    <article v-for="tile in owned" :key="tile.id" class="portfolio-card">
      <div class="portfolio-heading"><PropertyArt :level="tile.level" :mortgaged="tile.mortgaged" /><div><strong>{{ tile.name }}</strong><PropertyGrade :level="tile.level" /><small>{{ tile.id }} 号地块</small></div><span class="portfolio-status" :class="{ mortgaged: tile.mortgaged }">{{ tile.mortgaged ? '已抵押' : '持有中' }}</span></div>
      <dl class="portfolio-metrics"><div><dt>地价</dt><dd>¥{{ tile.price.toLocaleString() }}</dd></div><div><dt>{{ tile.mortgaged ? '赎回费用' : '当前租金' }}</dt><dd>¥{{ (tile.mortgaged ? redeemCost(tile) : game.me?.jailed && game.me.skipTurns > 0 ? 0 : rentFor(tile, game.room?.economy)).toLocaleString() }}</dd></div></dl>
      <div class="portfolio-actions">
        <button v-if="tile.level > 0" class="button small secondary" :disabled="!allowed || tile.mortgaged" @click="game.act({ type: 'sellBuilding', tileId: tile.id })">卖一级房 +{{ sellBuildingValue(tile, game.room?.economy) }}</button>
        <button v-if="!tile.mortgaged" class="button small secondary" :disabled="!allowed" @click="game.act({ type: 'mortgage', tileId: tile.id })">抵押 +{{ mortgageValue(tile, game.room?.economy) }}</button>
        <button v-else class="button small secondary" :disabled="!allowed || payment || (game.me?.cash ?? 0) < redeemCost(tile)" @click="game.act({ type: 'redeem', tileId: tile.id })">赎回 −{{ redeemCost(tile) }}</button>
        <button class="button small ghost danger" :disabled="!allowed || tile.level > 0" @click="game.act({ type: 'sellLand', tileId: tile.id })">卖地 +{{ landValue(tile, game.room?.economy) }}</button>
      </div>
    </article>
  </div>
  <details class="asset-guidance"><summary>资产操作说明</summary><p class="muted">只有停在自己的地块时才能建房。土地与建筑可整体抵押，保留建筑并暂停收租；抵押期间不能单独卖房。费用结算期间不可赎回。</p></details>
</template>
