<script setup lang="ts">
import { computed, ref } from 'vue';
import type { ContentResult } from '../../shared/types';
import rules from '../../../backend/src/main/resources/game-rules/card-events.json';
import ContentCard from '../components/ContentCard.vue';
import { UR_CARD_SCENES } from '../ur-card-scenes';

// 预览直接读取当前卡池的 UR 皮肤，仅展示卡名和插画，不生成对局或效果数值。
const cards: ContentResult[] = rules.content.mechanisms.filter(mechanism => mechanism.enabled).flatMap(mechanism => mechanism.groups.flatMap(group => group.parameters.flatMap(parameter => parameter.skins.filter(skin => skin.rarity === 'UR').map(skin => ({ ...skin, rarity: 'UR' as const, cardType: '主动' as const, pool: 'card' as const })))));
const selectedId = ref('card_naruto_shinra_tensei');
const selected = computed(() => cards.find(card => card.id === selectedId.value)!);
const paused = ref(false);
</script>

<template>
  <main class="ur-preview" :class="{ paused }">
    <header><p>好友财富派对 · UR 收藏卡</p><h1>让稀有，流动起来。</h1><p>{{ selectedId === 'card_naruto_shinra_tensei' ? '人物悬浮与推掌 · 场景视差 · 斥力扩散' : UR_CARD_SCENES[selectedId]?.label ?? '呼吸插画 · 玫瑰金流光 · 上浮星尘' }}</p></header>
    <nav aria-label="选择 UR 卡面"><button v-for="card in cards" :key="card.id" :class="{ selected: selectedId === card.id }" @click="selectedId = card.id">{{ card.name }}</button></nav>
    <div class="preview-controls"><button @click="paused = !paused">{{ paused ? '继续动画' : '暂停动画' }}</button><a href="/">返回游戏 ↗</a></div>
    <section class="comparison" aria-label="静态与动态卡面对比">
      <div><h2>原版静态 <small>STATIC</small></h2><ContentCard :content="selected" :animated="false" preview /></div>
      <div><h2>动态卡面 <small>LIVE · UR</small></h2><ContentCard :content="selected" preview /></div>
    </section>
    <footer>正式卡面中的名称与效果文字保持稳定。系统开启“减少动态效果”时，自动显示静态卡面。</footer>
  </main>
</template>

<style scoped>
.ur-preview { min-height: 100vh; padding: 32px 20px; color: #78543d; background: radial-gradient(ellipse at 50% 25%, #ffe3c1, transparent 65%), #fbf2e6; }
header { text-align: center; } header p { margin: 8px 0; font-size: 13px; color: #a3795e; } h1 { margin: 8px 0 12px; font-size: clamp(26px, 4vw, 38px); }
nav { display: flex; justify-content: center; flex-wrap: wrap; gap: 8px; max-width: 900px; margin: 24px auto 14px; }
button { border: 1px solid #dcb99a; border-radius: 20px; padding: 8px 16px; color: #96634b; background: #fff9ef; font-size: 13px; } button.selected { color: #fff5e7; background: #b45b64; border-color: #b45b64; }
.preview-controls { display: flex; justify-content: center; align-items: center; gap: 20px; } .preview-controls a { color: #a3795e; font-size: 12px; }
.comparison { display: grid; grid-template-columns: repeat(2, minmax(0, 330px)); justify-content: center; gap: 40px; margin: 18px auto 0; }
.comparison > div { min-width: 0; } h2 { display: flex; justify-content: space-between; align-items: center; margin: 0 12px 8px; font-size: 14px; font-weight: 500; } h2 small { color: #b88a70; font-size: 10px; letter-spacing: 2px; }
.paused :deep(.ur-animated .card-illustration), .paused :deep(.ur-card-light::before), .paused :deep(.ur-card-light::after), .paused :deep(.ur-card-light i) { animation-play-state: paused; }
.paused :deep(.shinra-scene *), .paused :deep(.ur-scene *) { animation-play-state: paused; }
footer { margin: 24px auto 0; text-align: center; font-size: 12px; color: #a3795e; line-height: 1.8; }
@media (max-width: 600px) { .ur-preview { padding: 24px 12px; } .comparison { gap: 10px; } h2 { margin: 0 4px 8px; font-size: 12px; } h2 small { font-size: 8px; letter-spacing: 1px; } nav { gap: 6px; } nav button { padding: 7px 10px; font-size: 12px; } }
</style>
