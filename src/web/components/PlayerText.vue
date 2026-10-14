<script setup lang="ts">
import { computed } from 'vue';
import { useGameStore } from '../stores/game';
const props = defineProps<{ text: string; players?: Array<{ name: string; color: string }> }>();
const game = useGameStore();
const parts = computed(() => {
  const players = props.players ?? game.room?.players ?? [];
  const names = [...new Set(players.map(player => player.name).filter(Boolean))].sort((a, b) => b.length - a.length);
  if (!names.length) return [{ text: props.text, color: null }];
  const pattern = new RegExp(`(${names.map(name => name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|')})`, 'g');
  return props.text.split(pattern).filter(Boolean).map(text => ({ text, color: players.find(player => player.name === text)?.color ?? null }));
});
</script>
<template><span class="player-text"><template v-for="(part, index) in parts" :key="index"><span v-if="part.color" class="player-mention" :style="{ '--mention-color': part.color }">{{ part.text }}</span><template v-else>{{ part.text }}</template></template></span></template>
<style scoped>
.player-mention { display: inline; color: var(--mention-color); font-weight: 700; overflow-wrap: anywhere; }
</style>
