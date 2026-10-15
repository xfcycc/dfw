<script setup lang="ts">
import type { ContentResult } from '../../shared/types';
import frameSources from '../card-frames.json';
const props = withDefaults(defineProps<{ rarity?: ContentResult['rarity'] }>(), { rarity: 'R' });
</script>
<template>
  <span class="card-ornament" aria-hidden="true">
    <svg class="card-frame-cap" viewBox="0 0 1254 376"><image :href="frameSources[props.rarity]" width="1254" height="1254" /></svg>
    <svg class="card-frame-side" viewBox="0 376 376 502" preserveAspectRatio="none"><image :href="frameSources[props.rarity]" width="1254" height="1254" /></svg>
    <span></span>
    <svg class="card-frame-side" viewBox="878 376 376 502" preserveAspectRatio="none"><image :href="frameSources[props.rarity]" width="1254" height="1254" /></svg>
    <svg class="card-frame-cap" viewBox="0 878 1254 376"><image :href="frameSources[props.rarity]" width="1254" height="1254" /></svg>
  </span>
</template>
<style scoped>
/* 同一张图包含等级字样和花纹；上下沿等比显示，只拉伸两侧，避免文字变形。 */
.card-ornament { position: absolute; inset: 0; z-index: 2; display: grid; grid-template-columns: 30% 40% 30%; grid-template-rows: auto minmax(0, 1fr) auto; pointer-events: none; }
.card-frame-cap { grid-column: 1 / -1; display: block; width: 100%; aspect-ratio: 1254 / 376; }
.card-frame-side { display: block; width: 100%; height: 100%; min-height: 0; }
</style>
