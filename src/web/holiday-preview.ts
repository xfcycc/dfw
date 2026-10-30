import { createApp, h } from 'vue';
import { createPinia } from 'pinia';
import { createRouter, createWebHistory, RouterView } from 'vue-router';
import HolidayGamePreview from './views/HolidayGamePreview.vue';
import './style.css';
import './game-room.css';
import './modern-room.css';
import './responsive-room.css';
import './desktop-room.css';
import './card-art.css';
import './audio-effects.css';
import './holiday-theme.css';

const router = createRouter({ history: createWebHistory(), routes: [{ path: '/holiday-preview.html', component: HolidayGamePreview }] });
createApp({ render: () => h(RouterView) }).use(createPinia()).use(router).mount('#app');
