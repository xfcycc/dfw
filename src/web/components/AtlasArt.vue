<script setup lang="ts">
import { useId } from 'vue';
import type { AtlasCrop } from '../effect-atlas-crops';
defineProps<{ url: string; crop: AtlasCrop }>();
const clipId = `atlas-${useId()}`;
</script>

<template>
  <svg class="atlas-artwork" :viewBox="crop.box.join(' ')" aria-hidden="true" focusable="false">
    <defs><clipPath :id="clipId" clipPathUnits="userSpaceOnUse"><path v-if="crop.path" :d="crop.path" /><rect v-else :x="crop.box[0]" :y="crop.box[1]" :width="crop.box[2]" :height="crop.box[3]" /></clipPath></defs>
    <image :href="url" :width="crop.width" :height="crop.height" :clip-path="`url(#${clipId})`" />
  </svg>
</template>

<style scoped>
.atlas-artwork { display:block; width:100%; height:100%; overflow:hidden; pointer-events:none; }
</style>
