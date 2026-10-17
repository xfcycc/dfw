<script setup lang="ts">
import { computed, useId } from 'vue';
import { imageUrl } from '../../image-assets';
const props = withDefaults(defineProps<{ index: number; kind?: 'figure' | 'box' }>(), { kind: 'figure' });
const clipId = useId();
const source = computed(() => imageUrl(`/images/activities/nuannuan-${props.kind === 'box' ? 'boxes' : 'dolls'}.webp`));
/** 输入公仔或包装索引，返回对应图集的独立视窗，不显示相邻款式。 */
const bounds = computed(() => props.kind === 'box' ? [props.index * 768, 0, 768, 1024] : [props.index % 4 * 384, Math.floor(props.index / 4) * 512, 384, 512]);
</script>
<template>
  <svg class="blindbox-art" :viewBox="bounds.join(' ')" aria-hidden="true" focusable="false"><defs><clipPath :id="clipId"><rect :x="bounds[0]" :y="bounds[1]" :width="bounds[2]" :height="bounds[3]" /></clipPath></defs><image :href="source" width="1536" height="1024" :clip-path="`url(#${clipId})`" /></svg>
</template>
<style scoped>
.blindbox-art { display:block; width:100%; height:100%; overflow:hidden; pointer-events:none; }
</style>
