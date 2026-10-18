<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import ContentCard from './ContentCard.vue';
import CardFrame from './CardFrame.vue';
import Modal from './Modal.vue';
import PlayerText from './PlayerText.vue';
import { cardSummary } from '../card-summary';
import { ITEMS } from '../../shared/rules';
import { useGameStore } from '../stores/game';
const props = defineProps<{ paused?: boolean }>();
const game = useGameStore();
const reveal = computed(() => game.cardReveals[0]);
const isItem = computed(() => !!reveal.value?.content.kind || reveal.value?.content.pool === 'card');
const publicNotice = computed(() => reveal.value?.mode === 'play' || !isItem.value && !reveal.value?.content.privateSupply);
const title = computed(() => reveal.value?.mode === 'play' ? '道具发动' : isItem.value ? '获得道具' : reveal.value?.content.pool === 'chance' ? '机会降临' : '财富来敲门');
const dismissLabel = computed(() => reveal.value?.mode === 'play' ? '关闭展示' : isItem.value ? '收起卡牌' : '关闭事件');
const viewport = ref<HTMLElement>();
const stage = ref<HTMLElement>();
const cardSize = ref({ width: 0, height: 0, scale: 1 });
const shownKey = ref('');
/** 输入揭卡标识和暂停状态，公开用卡或事件短暂提示后进入效果；私人新获道具仍由玩家收起，无返回值。 */
watch([() => reveal.value?.key, () => props.paused], ([key, paused], _, onCleanup) => {
  if (!key || paused) return;
  shownKey.value = key;
  if (!publicNotice.value && isItem.value && reveal.value?.mode === 'draw') return;
  const timer = setTimeout(() => {
    if (reveal.value?.key === key) game.dismissCardReveal();
  }, publicNotice.value ? 1600 : 6000);
  onCleanup(() => clearTimeout(timer));
}, { immediate: true, flush: 'post' });
// 输入展示区与卡面元素，整张卡牌连同文字按私人弹窗尺寸等比缩放。
watch([viewport, stage], ([area, card], _, onCleanup) => {
  if (!area || !card) return;
  const panel = area.closest<HTMLElement>('.card-reveal-panel');
  if (!panel) return;
  const modalVisible = Array.from(document.querySelectorAll<HTMLElement>('[role="dialog"]')).some(node => node.getClientRects().length > 0);
  if (!modalVisible) panel.focus({ preventScroll: true });
  /** 无入参，按容器尺寸等比缩放完整卡面并更新占位尺寸，无返回值。 */
  function fitCard(): void {
    if (!area || !card) return;
    if (!area.clientWidth || !area.clientHeight || !card.offsetWidth || !card.offsetHeight) return;
    const scale = Math.min(area.clientWidth / card.offsetWidth, area.clientHeight / card.offsetHeight);
    cardSize.value = { width: card.offsetWidth * scale, height: card.offsetHeight * scale, scale };
  }
  const observer = new ResizeObserver(fitCard);
  observer.observe(area);
  observer.observe(card);
  fitCard();
  onCleanup(() => observer.disconnect());
}, { flush: 'post' });
</script>
<template>
  <template v-if="reveal && (shownKey === reveal.key || !paused)">
  <section v-if="publicNotice && !paused" :key="reveal.key" class="city-alert public-card-notice" role="status" aria-live="polite"><div class="city-alert-message"><strong><PlayerText :text="reveal.actor" />{{ reveal.mode === 'play' ? ' 使用了 ' : ' 抽到了 ' }}{{ reveal.content.name ?? (reveal.content.kind ? ITEMS[reveal.content.kind].name : '事件') }}</strong><small>{{ cardSummary(reveal.content) }}</small></div><button class="icon-button" aria-label="收起提示" @click="game.dismissCardReveal">✕</button></section>
  <Modal v-else-if="!publicNotice" :key="reveal.key" :open="!paused" :title="`${reveal.actor} · ${title}`" hide-heading @close="game.dismissCardReveal">
  <section class="card-reveal-panel private-card-reveal" :class="[`reveal-${reveal.content.rarity ?? 'R'}`, { 'card-draw-reveal': reveal.mode === 'draw' }]" :inert="paused" :aria-hidden="paused" :aria-label="`${reveal.actor} · ${title}`" tabindex="-1">
    <div class="card-reveal-content">
    <button class="icon-button card-reveal-close" :aria-label="dismissLabel" @click="game.dismissCardReveal">✕</button>
    <div class="card-reveal-particles" aria-hidden="true"><i v-for="index in 18" :key="index" :style="{ '--x': `${(index * 37) % 100}%`, '--delay': `${(index % 7) * -.4}s` }"></i></div>
    <div class="card-reveal-heading"><h3>{{ title }}</h3><p>{{ reveal.actor }}{{ reveal.mode === 'play' ? ' 使用了这张道具' : ' 抽到了这张卡' }}</p></div>
    <div ref="viewport" class="card-reveal-viewport"><div class="card-reveal-fit" :style="{ width: `${cardSize.width}px`, height: `${cardSize.height}px` }"><div ref="stage" class="card-reveal-stage" :style="{ transform: `scale(${cardSize.scale})` }"><div v-if="reveal.mode === 'draw'" class="card-draw-back" aria-hidden="true"><CardFrame :rarity="reveal.content.rarity" /><div class="card-surface"><span class="card-back-label">{{ isItem ? '道具卡' : reveal.content.pool === 'chance' ? '机会卡' : '财富卡' }}</span></div></div><ContentCard :content="reveal.content" /></div></div></div>
    <div v-if="isItem" class="card-reveal-footer"><button class="button primary" @click="game.dismissCardReveal">{{ game.cardReveals.length > 1 ? `下一张 · 还有 ${game.cardReveals.length - 1} 张` : `${dismissLabel} →` }}</button></div>
    </div>
  </section>
  </Modal>
  </template>
</template>

<style scoped>
.public-card-notice { gap:8px; margin:6px 0; padding:8px 10px; border-width:1px; animation:none; }
.public-card-notice strong { font-size:12px; }
.public-card-notice .city-alert-message small { display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; font-size:10px; line-height:1.5; }
.public-card-notice .icon-button { flex-shrink:0; width:26px; height:26px; font-size:12px; }
.private-card-reveal { position:relative; inset:auto; width:100%; height:min(68dvh,620px); min-height:240px; border-radius:16px; }
</style>
