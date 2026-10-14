<script setup lang="ts">
defineProps<{ kind: 'cash' | 'worth' | 'cards' | 'property' }>();
const source = `${import.meta.env.BASE_URL}images/actions/asset-stats.png`;
const worthSource = `${import.meta.env.BASE_URL}images/actions/net-worth.png`;
const viewBoxes = { cash: '0 0 724 724', cards: '724 0 724 724', property: '1448 0 724 724' };
</script>
<template>
  <img v-if="kind === 'worth'" class="asset-stat-icon" :src="worthSource" alt="" aria-hidden="true" decoding="async" draggable="false" />
  <svg v-else class="asset-stat-icon" :viewBox="viewBoxes[kind]" aria-hidden="true" focusable="false">
    <image :href="source" width="2172" height="724" />
  </svg>
</template>
<style scoped>
.asset-stat-icon { display: block; width: 30px; height: 30px; flex-shrink: 0; overflow: hidden; object-fit: contain; pointer-events: none; }
</style>
