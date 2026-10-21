<script setup lang="ts">
import { computed } from 'vue';
import type { ContentResult } from '../../shared/types';
import { ITEMS } from '../../shared/rules';
import illustrationSources from '../card-illustrations.json';
import CardFrame from './CardFrame.vue';
import ShinraCardScene from './ShinraCardScene.vue';
import UrCardScene from './UrCardScene.vue';
import { ORIGINAL_UR_SKINS, STATIC_UR_SKINS, UR_CARD_SCENES } from '../ur-card-scenes';
import { cardEffect, cardSummary } from '../card-summary';
const props = withDefaults(defineProps<{ content: ContentResult; compact?: boolean; reveal?: boolean; thumbnail?: boolean; preview?: boolean; animated?: boolean }>(), { animated: true });
// 已生成分层的 UR 使用专属场景，静态对比和未知皮肤继续沿用原图。
/** 根据卡面身份、等级和展示开关返回是否播放 UR 卡面动画。 */
const urAnimated = computed(() => props.content.rarity === 'UR' && props.animated !== false && !STATIC_UR_SKINS.has(props.content.skinId ?? props.content.id));
/** 根据稳定卡面身份返回是否仅保留原图的呼吸与流光。 */
const originalUrArtwork = computed(() => ORIGINAL_UR_SKINS.has(props.content.skinId ?? props.content.id));
const shinraAnimated = computed(() => urAnimated.value && (props.content.skinId ?? props.content.id) === 'card_naruto_shinra_tensei');
/** 根据卡面身份返回已登记的分层场景；本批动漫 UR 保留原图与通用轻动态。 */
const urScene = computed(() => urAnimated.value && !originalUrArtwork.value ? UR_CARD_SCENES[props.content.skinId ?? props.content.id] : undefined);
const kind = computed(() => props.content.kind ?? null);
const fallback = computed(() => kind.value ? ITEMS[kind.value] : null);
const pool = computed(() => kind.value || props.content.pool === 'card' ? 'card' : props.content.pool === 'chance' ? 'chance' : 'fortune');
const poolName = computed(() => ({ card: '道具卡', chance: '机会卡', fortune: '财富卡' })[pool.value]);
// 旧手牌沿用原通用图集；UR 使用本档独立通用插画，正文由真实卡池填充。
const frames = {
  chance: { R: [22, 93, 487, 345], SR: [530, 93, 477, 345], SSR: [1028, 93, 487, 345] },
  fortune: { R: [32, 107, 485, 350], SR: [535, 107, 470, 350], SSR: [1020, 107, 485, 350] },
  card: { R: [31, 95, 483, 376], SR: [533, 95, 474, 376], SSR: [1020, 95, 483, 376] },
} as const;
const illustrations: Record<string, string> = illustrationSources;
const fallbackSources = {
  card: 'https://pic.caiguoyu.cn/typora/20261015104009181.webp',
  chance: 'https://pic.caiguoyu.cn/typora/20261015104009182.webp',
  fortune: 'https://pic.caiguoyu.cn/typora/20261015104009183.webp',
} as const;
/** 输入卡面身份和等级，返回匹配插画；未知 UR 使用同类 UR 通用图，其他旧牌沿用原图集。 */
const artwork = computed(() => {
  const source = illustrations[props.content.skinId ?? ''] ?? illustrations[props.content.id];
  if (source || props.content.rarity === 'UR') return { source: source ?? illustrations[`ur_${pool.value}`], viewBox: '0 0 512 512', width: 512, height: 512, sourceWidth: 512, sourceHeight: 512 };
  const frame = frames[pool.value][props.content.rarity ?? 'R'];
  return { source: fallbackSources[pool.value], viewBox: frame.join(' '), width: frame[2], height: frame[3], sourceWidth: 1536, sourceHeight: 1024 };
});
</script>
<template>
  <article :key="content.id" class="content-card illustrated-card" :class="[{ compact, thumbnail, 'card-reveal': reveal, 'ur-animated': urAnimated, 'scene-animated': shinraAnimated || urScene }, `rarity-${content.rarity ?? 'R'}`]" :aria-label="`${poolName} · ${content.rarity ?? 'R'} · ${content.name ?? fallback?.name ?? '事件'}`">
    <CardFrame :rarity="content.rarity" />
    <div class="card-surface">
      <div class="card-artwork">
        <svg class="card-illustration" :preserveAspectRatio="thumbnail ? 'xMidYMid slice' : 'xMidYMid meet'" :viewBox="artwork.viewBox" :width="artwork.width" :height="artwork.height" aria-hidden="true"><image :href="artwork.source" :width="artwork.sourceWidth" :height="artwork.sourceHeight" /></svg>
        <ShinraCardScene v-if="shinraAnimated" />
        <UrCardScene v-else-if="urScene" :key="urScene.skinId" :scene="urScene" />
        <span v-else-if="urAnimated" class="ur-card-light" aria-hidden="true"><i v-for="spark in originalUrArtwork ? 0 : 6" :key="spark" :style="{ '--spark': spark }"></i></span>
      </div>
      <div class="card-copy">
        <div class="content-card-heading"><strong>{{ content.name ?? fallback?.name ?? '事件' }}</strong></div>
        <p v-if="thumbnail" class="card-summary">{{ cardSummary(content) }}</p>
        <template v-else-if="!preview">
          <p class="card-description">{{ content.description ?? fallback?.description }}</p>
          <p class="content-effect">{{ cardEffect(content) }}</p>
          <blockquote v-if="content.quote">“{{ content.quote }}”</blockquote>
        </template>
      </div>
    </div>
  </article>
</template>

<style scoped>
.scene-animated .card-illustration { animation: none; }
.card-summary { margin: 0; color: #634e39; font-size: 12px; line-height: 1.45; overflow-wrap: anywhere; }
</style>
