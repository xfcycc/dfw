<script setup lang="ts">
import { useGameStore } from './stores/game';
const game = useGameStore();
</script>
<template>
  <RouterView />
  <Transition name="toast"><div v-if="game.message" class="toast" role="status">{{ game.message }}</div></Transition>
</template>
