import { createApp } from 'vue';
import { createPinia } from 'pinia';
import EffectsPreview from './views/EffectsPreview.vue';
import './style.css';
import './game-room.css';
import './audio-effects.css';

createApp(EffectsPreview).use(createPinia()).mount('#app');
