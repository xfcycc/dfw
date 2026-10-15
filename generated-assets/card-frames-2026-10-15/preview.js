import { createApp } from 'vue';
import { createPinia } from 'pinia';
import Preview from './Preview.vue';
import '/style.css';
import '/modern-room.css';
import '/card-art.css';
import './preview.css';

createApp(Preview).use(createPinia()).mount('#frame-preview');
