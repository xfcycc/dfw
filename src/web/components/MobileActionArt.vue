<script setup lang="ts">
import { computed, useId } from 'vue';
import { imageUrl } from '../image-assets';
const props = defineProps<{ action: 'more' | 'roll' | 'endTurn' | 'assets' | 'ranking' | 'trade' | 'cards' }>();
const source = imageUrl('/images/actions/mobile-controls.webp');
const clipId = useId();
// 输入手机按钮用途，限定图集插画范围，避免按钮比例不同时露出相邻图片。
const viewBoxes = {
  more: [18, 140, 390, 330], roll: [406, 136, 410, 340], endTurn: [816, 148, 380, 324],
  assets: [10, 532, 394, 346], ranking: [404, 532, 386, 346], trade: [788, 568, 372, 310], cards: [1158, 560, 378, 320],
};
const bounds = computed(() => viewBoxes[props.action]);
</script>
<template>
  <svg class="mobile-action-art" :viewBox="bounds.join(' ')" aria-hidden="true" focusable="false">
    <defs><clipPath :id="clipId"><rect :x="bounds[0]" :y="bounds[1]" :width="bounds[2]" :height="bounds[3]" /></clipPath></defs>
    <image :href="source" width="1536" height="1024" :clip-path="`url(#${clipId})`" />
  </svg>
</template>
<style scoped>
.mobile-action-art { display: block; width: 100%; height: 100%; overflow: hidden; pointer-events: none; }
</style>
