<script setup lang="ts">
defineProps<{ action: 'assets' | 'trade' | 'roll' | 'endTurn' }>();
const source = `${import.meta.env.BASE_URL}images/actions/workbench-buttons.png`;
// 输入按钮用途，按完整按钮边缘裁切图集，标题和插画均由图片呈现。
const viewBoxes = { assets: '16 40 748 464', trade: '772 40 748 464', roll: '16 520 748 464', endTurn: '772 520 748 464' };
</script>
<template>
  <svg class="workbench-action-art" :viewBox="viewBoxes[action]" aria-hidden="true" focusable="false">
    <image :href="source" width="1536" height="1024" />
  </svg>
</template>
<style scoped>
.workbench-action-art { display: block; width: 100%; height: 100%; overflow: hidden; pointer-events: none; }
</style>
