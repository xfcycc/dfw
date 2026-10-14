<script setup lang="ts">
import { computed } from 'vue';
import type { ContentResult } from '../../shared/types';
import { ITEMS } from '../../shared/rules';
const props = defineProps<{ content: ContentResult; compact?: boolean; reveal?: boolean; thumbnail?: boolean }>();
const kind = computed(() => props.content.kind ?? null);
const fallback = computed(() => kind.value ? ITEMS[kind.value] : null);
const pool = computed(() => kind.value || props.content.pool === 'card' ? 'card' : props.content.pool === 'chance' ? 'chance' : 'fortune');
const poolName = computed(() => ({ card: '道具卡', chance: '机会卡', fortune: '财富卡' })[pool.value]);
// 每类图集保留确认稿的三个插画及右上角字标；视窗只展示插画，正文由真实卡池填充。
const frames = {
  chance: { R: [22, 93, 487, 345], SR: [530, 93, 477, 345], SSR: [1028, 93, 487, 345] },
  fortune: { R: [32, 107, 485, 350], SR: [535, 107, 470, 350], SSR: [1020, 107, 485, 350] },
  card: { R: [31, 95, 483, 376], SR: [533, 95, 474, 376], SSR: [1020, 95, 483, 376] },
} as const;
const artwork = computed(() => {
  const frame = frames[pool.value][props.content.rarity ?? 'R'];
  return { source: `/images/cards/${pool.value}.jpg`, viewBox: frame.join(' '), width: frame[2], height: frame[3] };
});
</script>
<template>
  <article :key="content.id" class="content-card illustrated-card" :class="[{ compact, thumbnail, 'card-reveal': reveal }, `rarity-${content.rarity ?? 'R'}`]" :aria-label="`${poolName} · ${content.rarity ?? 'R'} · ${content.name ?? fallback?.name ?? '事件'}`">
    <svg class="card-illustration" :viewBox="artwork.viewBox" :width="artwork.width" :height="artwork.height" aria-hidden="true"><image :href="artwork.source" width="1536" height="1024" /></svg>
    <div class="card-copy">
      <div class="content-card-heading"><strong>{{ content.name ?? fallback?.name ?? '事件' }}</strong></div>
      <p v-if="!thumbnail" class="card-description">{{ kind === 'barrier' ? fallback?.description : content.description ?? fallback?.description }}</p>
      <p class="content-effect">{{ kind === 'barrier' ? fallback?.description : content.effect ?? fallback?.description }}</p>
      <blockquote v-if="content.quote && !thumbnail">“{{ content.quote }}”</blockquote>
    </div>
  </article>
</template>
