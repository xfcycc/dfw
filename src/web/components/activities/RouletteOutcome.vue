<script setup lang="ts">
import { computed } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import { useGameStore } from '../../stores/game';
import CharacterPawn from '../CharacterPawn.vue';

const props = defineProps<{ activity: CornerActivity; animated?: boolean }>();
const game = useGameStore();
const duelists = computed(() => game.room?.players
  .filter(player => props.activity.participants.includes(player.id))
  .map(player => ({ ...player, lost: player.id === props.activity.lastShooterId })) ?? []);
</script>

<template>
  <div class="roulette-outcome" :class="{ 'roulette-outcome-float': animated }" role="status">
    <div v-for="player in duelists" :key="player.id" class="roulette-outcome-player" :class="{ lost: player.lost }">
      <div class="roulette-outcome-avatar">
        <CharacterPawn :avatar-id="player.avatarId" :color="player.color" />
        <span v-if="player.lost && animated" class="roulette-impact" aria-hidden="true">✦</span>
      </div>
      <strong>{{ player.name }}</strong>
      <small>{{ player.lost ? '落败' : '获胜' }}</small>
    </div>
  </div>
</template>
