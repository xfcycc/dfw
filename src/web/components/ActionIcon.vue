<script setup lang="ts">
import { imageUrl } from '../image-assets';
defineProps<{ action: 'trade' | 'roll' | 'endTurn' | 'send' }>();
const source = imageUrl('/images/actions/game-actions.png');
// 按图标范围展示同一张透明图集，保留比例，避免缩小后留白过多。
const viewBoxes = { trade: '70 120 750 330', roll: '940 0 430 512', endTurn: '200 520 560 504', send: '840 580 610 355' };
</script>
<template>
  <svg class="action-icon" :viewBox="viewBoxes[action]" aria-hidden="true" focusable="false">
    <image :href="source" width="1536" height="1024" />
  </svg>
</template>
<style scoped>
.action-icon { display: block; width: 34px; height: 30px; flex-shrink: 0; overflow: hidden; pointer-events: none; }
</style>
