<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import ContentCard from './ContentCard.vue';
import CardFrame from './CardFrame.vue';
import Modal from './Modal.vue';
import type { CardReveal } from '../stores/game';
import { useGameStore } from '../stores/game';
import FullSpellProp from './FullSpellProp.vue';
import UniqueUrSpellLayer from './UniqueUrSpellLayer.vue';
import { FULL_CARD_SPELLS, prepareFullSpell } from '../full-card-spells';
const props = defineProps<{ paused?: boolean; embedded?: boolean; preview?: CardReveal }>();
const emit = defineEmits<{ close: [] }>();
const game = useGameStore();
const reveal = computed(() => props.preview ?? game.cardReveals[0]);
const isItem = computed(() => !!reveal.value?.content.kind || reveal.value?.content.pool === 'card');
const title = computed(() => reveal.value?.mode === 'play' ? '道具发动' : isItem.value ? '获得道具' : reveal.value?.content.pool === 'chance' ? '机会降临' : '财富来敲门');
const dismissLabel = computed(() => props.preview ? '关闭回看' : reveal.value?.mode === 'play' ? '关闭展示' : isItem.value ? '收起卡牌' : '关闭事件');
const viewport = ref<HTMLElement>();
const stage = ref<HTMLElement>();
const cardSize = ref({ width: 0, height: 0, scale: 1 });
const revealSize = ref({width:0,height:0});
const privateUr=computed(()=>drawSpell.value?.rarity==='UR'&&reveal.value?.content.pool!=='card'&&reveal.value?.content.privateSupply);
const shownKey = ref('');
const drawSpell = computed(() => !props.preview && reveal.value?.mode === 'draw' ? FULL_CARD_SPELLS.find(card=>card.skinId===reveal.value?.content.skinId) : undefined);
const drawReady = ref('');
// 输入本人的新揭卡身份，解码后仅在本地展示主体，不向公共棋盘发送私人事件身份。
watch(()=>reveal.value?.key,async key=>{drawReady.value='';if(!key||!drawSpell.value)return;try{await prepareFullSpell(drawSpell.value);if(reveal.value?.key===key)drawReady.value=key;}catch(error){console.warn('揭卡主体未加载，保留原卡面展示',error);}},{immediate:true});
/** 无入参，仅关闭回看或推进实时展示队列，不执行卡牌效果，无返回值。 */
function dismiss(): void {
  if (props.preview) emit('close');
  else game.dismissCardReveal();
}
/** 输入揭卡标识和暂停状态，按布局在新鲜事或全屏展示原揭卡动画；事件与已用道具六秒后关闭，新获道具由玩家收起，无返回值。 */
watch([() => reveal.value?.key, () => props.paused], ([key, paused], _, onCleanup) => {
  if (!key || paused) return;
  shownKey.value = key;
  if (props.preview || isItem.value && reveal.value?.mode === 'draw') return;
  const timer = setTimeout(() => {
    if (reveal.value?.key === key) game.dismissCardReveal();
  }, 6000);
  onCleanup(() => clearTimeout(timer));
}, { immediate: true, flush: 'post' });
// 输入展示区与卡面元素，卡面按舞台尺寸等比缩放；右侧提示与收起操作独立排布。
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
    revealSize.value={width:area.clientWidth,height:area.clientHeight};
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
  <component :is="embedded ? 'div' : Modal" v-if="reveal && (shownKey === reveal.key || !paused)" :open="!paused" :title="`${reveal.actor} · ${title}`" hide-heading panel-class="card-reveal-modal" @close="dismiss">
  <section :key="reveal.key" class="card-reveal-panel" :class="[`reveal-${reveal.content.rarity ?? 'R'}`, { 'card-reveal-split': !embedded, 'card-reveal-embedded': embedded, 'card-draw-reveal': !preview && reveal.mode === 'draw' }]" :style="{ visibility: paused ? 'hidden' : undefined }" :inert="paused" :aria-hidden="paused" :aria-label="`${reveal.actor} · ${title}`" tabindex="-1" @keydown.esc.stop="dismiss">
    <div class="card-reveal-content">
    <button class="icon-button card-reveal-close" :aria-label="dismissLabel" @click="dismiss">✕</button>
    <div class="card-reveal-particles" aria-hidden="true"><i v-for="index in 18" :key="index" :style="{ '--x': `${(index * 37) % 100}%`, '--delay': `${(index % 7) * -.4}s` }"></i></div>
    <div v-if="drawSpell && drawReady===reveal.key && !privateUr" class="draw-spell-subject"><FullSpellProp :card="drawSpell"/></div>

    <div ref="viewport" class="card-reveal-viewport"><div class="card-reveal-fit" :style="{ width: `${cardSize.width}px`, height: `${cardSize.height}px` }"><div ref="stage" class="card-reveal-stage" :style="{ transform: `scale(${cardSize.scale})` }"><div v-if="!preview && reveal.mode === 'draw'" class="card-draw-back" aria-hidden="true"><CardFrame :rarity="reveal.content.rarity" /><div class="card-surface"><span class="card-back-label">{{ isItem ? '道具卡' : reveal.content.pool === 'chance' ? '机会卡' : '财富卡' }}</span></div></div><ContentCard :content="reveal.content" /></div></div><UniqueUrSpellLayer v-if="privateUr && drawSpell && drawReady===reveal.key && !paused" :key="reveal.key" :card="drawSpell" :actor="{x:revealSize.width*.5,y:revealSize.height*.65}" :targets="[]" :width="revealSize.width" :height="revealSize.height" :tile-size="Math.min(revealSize.width,revealSize.height)/4" compact/></div>
    <aside class="card-reveal-caption">
    <div class="card-reveal-heading"><h3>{{ title }}</h3><p>{{ `${reveal.actor}${reveal.mode === 'play' ? ' 使用了这张道具' : ' 抽到了这张卡'}` }}</p></div>
    <div v-if="!embedded || isItem || preview" class="card-reveal-footer"><button class="button primary" @click="dismiss">{{ !preview && game.cardReveals.length > 1 ? `下一张 · 还有 ${game.cardReveals.length - 1} 张` : `${dismissLabel} →` }}</button></div>
    </aside>
    </div>
  </section>
  </component>
</template>
<style scoped>
.draw-spell-subject { position:absolute;z-index:3;left:6%;bottom:9%;width:clamp(48px,10vw,100px);height:clamp(48px,10vw,100px);pointer-events:none;animation:draw-spell-in .6s ease-out both; }
@keyframes draw-spell-in { from {opacity:0;transform:translateY(20px) scale(.5) rotate(-15deg)} to {opacity:1;transform:none} }
@media(prefers-reduced-motion:reduce){.draw-spell-subject{animation:none}}
</style>
