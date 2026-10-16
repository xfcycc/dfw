import { createApp, h } from 'vue';
import ContentCard from '../../src/web/components/ContentCard.vue';
import '../../src/web/card-art.css';
import cards from './preview.json';

// 输入服务端静态卡面资料，渲染真实组件，不创建房间或执行游戏操作。
createApp({ render: () => h('main', cards.map(content => h('section', [h('span', { class: 'event-pool' }, content.pool === 'chance' ? 'UR 机会事件' : 'UR 财富事件'), h(ContentCard, { content })]))) }).mount('#cards');
