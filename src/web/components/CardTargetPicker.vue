<script setup lang="ts">
import CharacterPawn from './CharacterPawn.vue';
import PlayerText from './PlayerText.vue';
interface CardTargetOption { id: string; name: string; detail: string; avatarId?: string; color?: string; reason?: string }
defineProps<{ options: CardTargetOption[]; selected: string; playerTarget?: boolean; disabled?: boolean }>();
const emit = defineEmits<{ select: [id: string] }>();
</script>
<template>
  <div class="card-target-picker" :class="{ 'player-targets': playerTarget }" role="group" aria-label="选择卡牌目标">
    <button v-for="option in options" :key="option.id" type="button" class="target-option" :class="{ selected: selected === option.id, unavailable: !!option.reason }" :disabled="disabled || !!option.reason" :aria-pressed="selected === option.id" :aria-label="`选择目标 ${option.name}${option.reason ? `，${option.reason}` : ''}`" @click="emit('select', option.id)">
      <CharacterPawn v-if="playerTarget" :avatar-id="option.avatarId" :color="option.color" /><span v-else class="target-tile-symbol">⌂</span>
      <span class="target-option-copy"><strong><PlayerText :text="option.name" /></strong><small><PlayerText :text="option.reason || option.detail" /></small></span><span v-if="selected === option.id" class="target-check">✓</span>
    </button>
    <p v-if="!options.length" class="muted">本次没有可选目标。</p>
  </div>
</template>
