<script setup lang="ts">
import { useGameStore } from './stores/game';
import { useRoute } from 'vue-router';
import { useAudioEnvironment } from './audio';
import './audio-effects.css';
const game = useGameStore();
const route = useRoute();
useAudioEnvironment(() => route.path === '/' ? 'lobby' : /^\/(room|watch)\//.test(route.path) && game.room && game.connection === 'online' ? game.room.status === 'lobby' ? 'lobby' : 'game' : null);
</script>
<template>
  <RouterView />
  <Transition name="toast"><div v-if="game.message" class="toast" role="status">{{ game.message }}</div></Transition>
</template>
