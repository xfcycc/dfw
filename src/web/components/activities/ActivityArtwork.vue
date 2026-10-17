<script setup lang="ts">
import { computed, useId } from 'vue';
import { imageUrl } from '../../image-assets';
const artwork = {
  'casino-scene': { sheet: 'classic-scenes', bounds: [0, 0, 512, 512] },
  'horse-scene': { sheet: 'classic-scenes', bounds: [512, 0, 512, 512] },
  'roulette-scene': { sheet: 'classic-scenes', bounds: [1024, 0, 512, 512] },
  'packet-scene': { sheet: 'classic-scenes', bounds: [0, 512, 512, 512] },
  'rps-scene': { sheet: 'classic-scenes', bounds: [512, 512, 512, 512] },
  'casino-table': { sheet: 'classic-props', bounds: [18, 58, 405, 437] },
  cylinder: { sheet: 'classic-props', bounds: [427, 113, 363, 370] },
  'packet-closed': { sheet: 'classic-props', bounds: [803, 46, 335, 459] },
  'packet-open': { sheet: 'classic-props', bounds: [1176, 46, 336, 459] },
  'packet-lucky': { sheet: 'classic-props', bounds: [26, 519, 422, 461] },
  'rps-rock': { sheet: 'classic-props', bounds: [448, 552, 330, 414] },
  'rps-scissors': { sheet: 'classic-props', bounds: [788, 552, 330, 414] },
  'rps-paper': { sheet: 'classic-props', bounds: [1170, 552, 330, 414] },
  'bridge-scene': { sheet: 'variant-scenes', bounds: [0, 0, 768, 512] },
  'treasure-scene': { sheet: 'variant-scenes', bounds: [768, 0, 768, 512] },
  'market-scene': { sheet: 'variant-scenes', bounds: [0, 512, 768, 512] },
  glass: { sheet: 'variant-props', bounds: [4, 152, 376, 216] },
  'glass-safe': { sheet: 'variant-props', bounds: [388, 152, 376, 216] },
  'glass-broken': { sheet: 'variant-props', bounds: [772, 152, 376, 216] },
  'chest-closed': { sheet: 'variant-props', bounds: [1152, 0, 384, 416] },
  'chest-gold': { sheet: 'variant-props', bounds: [0, 544, 384, 416] },
  'chest-trap': { sheet: 'variant-props', bounds: [384, 544, 384, 416] },
  hotpot: { sheet: 'variant-props', bounds: [768, 449, 384, 548] },
  barbecue: { sheet: 'variant-props', bounds: [1152, 449, 384, 548] },
};
const props = defineProps<{ name: keyof typeof artwork; backdrop?: boolean }>();
const clipId = useId();
/** 输入场景或道具名称，返回图集地址及独立视窗，背景按容器覆盖、道具保持比例。 */
const art = computed(() => ({ ...artwork[props.name], source: imageUrl(`/images/activities/${artwork[props.name].sheet}.webp`) }));
</script>
<template>
  <svg class="activity-artwork" :class="{ backdrop }" :viewBox="art.bounds.join(' ')" :preserveAspectRatio="backdrop ? 'xMidYMid slice' : 'xMidYMid meet'" aria-hidden="true" focusable="false"><defs><clipPath :id="clipId"><rect :x="art.bounds[0]" :y="art.bounds[1]" :width="art.bounds[2]" :height="art.bounds[3]" /></clipPath></defs><image :href="art.source" width="1536" height="1024" :clip-path="`url(#${clipId})`" /></svg>
</template>
<style scoped>
.activity-artwork { display:block; width:100%; height:100%; overflow:hidden; pointer-events:none; }
.backdrop { position:absolute; z-index:-1; inset:0; }
</style>
