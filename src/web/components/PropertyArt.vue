<script setup lang="ts">
import { computed } from 'vue';
import type { BuildingSeries } from '../../shared/types';
import { imageUrl } from '../image-assets';
const props = defineProps<{ level: number; series?: BuildingSeries | null; mortgaged?: boolean }>();
const variant = computed(() => props.level > 0 && props.series && props.series !== 'classic' ? props.series : null);
const original = imageUrl('/images/board/property-levels.png');
const source = computed(() => variant.value
  ? new URL(`../assets/properties/${variant.value}-${props.level}.webp`, import.meta.url).href
  : original);
</script>
<template>
  <span class="property-art" :class="[`level-${level}`, { mortgaged, variant }]" :style="{ backgroundImage: `url(${source})` }" aria-hidden="true"></span>
</template>
<style scoped>
.property-art { display: block; width: 80px; aspect-ratio: 1; flex-shrink: 0; background-position: 0 0; background-size: 200% 200%; background-repeat: no-repeat; }
.property-art.level-1 { background-position: 100% 0; }
.property-art.level-2 { background-position: 0 100%; }
.property-art.level-3 { background-position: 100% 100%; }
.property-art.variant { background-position: center; background-size: contain; }
.property-art.mortgaged { filter: grayscale(.8); opacity: .65; }
</style>
