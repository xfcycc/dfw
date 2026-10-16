<script setup lang="ts">
import { imageUrl } from '../image-assets';
defineProps<{ kind: 'cash' | 'worth' | 'cards' | 'property' }>();
const source = imageUrl('/images/actions/asset-stats.png');
const worthSource = imageUrl('/images/actions/net-worth.png');
const viewBoxes = { cash: '0 0 724 724', cards: '724 0 724 724', property: '1448 0 724 724' };
const hints = {
  cash: '现金：当前持有、可用于支付和交易的金额',
  worth: '净资产：现金加土地及建筑成本，抵押地产扣除赎回费用，单块地产剩余权益最低为零',
  cards: '手牌：当前持有的卡牌数量',
  property: '房产：当前拥有的地产数量，包含已抵押地产',
};
</script>
<template>
  <span class="asset-stat-icon" :title="hints[kind]" role="img" :aria-label="hints[kind]">
    <img v-if="kind === 'worth'" :src="worthSource" alt="" aria-hidden="true" decoding="async" draggable="false" />
    <svg v-else :viewBox="viewBoxes[kind]" aria-hidden="true" focusable="false">
      <image :href="source" width="2172" height="724" />
    </svg>
  </span>
</template>
<style scoped>
.asset-stat-icon { display: block; width: 30px; height: 30px; flex-shrink: 0; overflow: hidden; }
.asset-stat-icon > img, .asset-stat-icon > svg { display: block; width: 100%; height: 100%; object-fit: contain; pointer-events: none; }
</style>
