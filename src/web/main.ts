import { createApp } from 'vue';
import { createPinia } from 'pinia';
import { createRouter, createWebHistory } from 'vue-router';
import App from './App.vue';
import Home from './views/Home.vue';
import RoomView from './views/RoomView.vue';
import Archives from './views/Archives.vue';
import './style.css';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Home },
    { path: '/archives', component: Archives },
    { path: '/room/:code', component: RoomView },
    { path: '/watch/:code', component: RoomView, props: { spectator: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
});
createApp(App).use(createPinia()).use(router).mount('#app');
