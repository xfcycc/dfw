<script>
import { ref } from 'vue';
import ContentCard from '/components/ContentCard.vue';
import CardReveal from '/components/CardReveal.vue';
import ContentChoice from '/components/ContentChoice.vue';
import Modal from '/components/Modal.vue';
import { useGameStore } from '/stores/game.ts';
import samples from './samples.json';
export default {
  components: { ContentCard, CardReveal, ContentChoice, Modal },
  setup() {
    const game = useGameStore();
    const mode = ref('full');
    const rarity = ref('UR');
    const selected = ref(null);
    const hand = [...samples, { ...samples[2], id: 'extra-ssr' }, { ...samples[3], id: 'extra-ur' }];
    const choice = { id: 'frame-selection', type: 'hand_limit', cards: hand, tileIds: [], cardId: null, content: null, skipTurns: 0, jailed: false, discardCash: 400 };
    /** 输入展示模式或等级，仅切换本页的视觉状态，不创建会话或发送游戏命令。 */
    function show(nextMode = mode.value, nextRarity = rarity.value) {
      mode.value = nextMode;
      rarity.value = nextRarity;
      game.cardReveals = ['draw', 'play'].includes(nextMode) ? [{ key: `${Date.now()}-${Math.random()}`, mode: nextMode, actor: '苏暖暖', content: samples.find(card => card.rarity === nextRarity) }] : [];
    }
    return { mode, rarity, samples, hand, choice, selected, show };
  },
};
</script>
<template>
<main class="frame-preview-page">
    <header><h1>四档花纹卡框</h1><p>翡翠叶纹 · 紫晶月纹 · 鎏金星桂 · 玫瑰金曜冠</p><p>可切换查看完整卡面、手牌、选留和翻牌效果。此页只展示卡面，不连接对局。</p></header>
    <nav class="frame-preview-toolbar" aria-label="展示方式"><button v-for="item in [{id:'full',name:'完整卡面'},{id:'hand',name:'手牌与弹窗'},{id:'choice',name:'选留窗口'},{id:'draw',name:'抽卡动画'},{id:'play',name:'出牌动画'}]" :class="{active:mode===item.id}" @click="show(item.id)">{{item.name}}</button></nav>
    <section v-if="mode==='full'" class="frame-preview-grid"><ContentCard v-for="card in samples" :key="card.id" :content="card" /></section>
    <section v-if="mode==='hand'" class="modern-room game-session frame-preview-hand"><div class="dashboard-card-rail"><button v-for="card in hand" :key="card.id" class="dashboard-hand-card" @click="selected=card"><ContentCard :content="card" compact thumbnail /></button></div></section>
    <section v-if="mode==='choice'" class="modern-modal frame-preview-choice"><ContentChoice :choice="choice" /></section>
    <template v-if="mode==='draw'||mode==='play'"><nav class="frame-preview-toolbar" aria-label="卡牌等级"><button v-for="grade in ['R','SR','SSR','UR']" :class="{active:rarity===grade}" @click="show(mode,grade)">{{grade}}</button><button @click="show()">重播本档</button></nav><section class="frame-preview-reveal"><CardReveal /></section></template>
    <Modal v-if="selected" :title="selected.name" panel-class="modern-modal card-use-modal" wide @close="selected=null"><div class="card-use-layout"><div class="card-use-preview"><ContentCard :content="selected" /></div><div class="card-use-controls"><h3>{{selected.rarity}} · 卡面详情</h3><p>{{selected.effect}}</p><button class="button secondary" @click="selected=null">关闭预览</button></div></div></Modal>
  </main>
</template>
