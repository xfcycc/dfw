import { createApp, h } from 'vue';
import ContentCard from '../../src/web/components/ContentCard.vue';
import '../../src/web/card-art.css';
import cards from './preview.json';

// 输入服务端静态卡面资料，渲染四张真实组件，不执行游戏操作。
createApp({ render: () => h('main', cards.map(content => h('section', [h('span', { class: 'event-pool' }, `${content.rarity} · ${{ card: '道具卡', chance: '机会事件', fortune: '财富事件' }[content.pool]}`), h(ContentCard, { content })]))) }).mount('#cards');
