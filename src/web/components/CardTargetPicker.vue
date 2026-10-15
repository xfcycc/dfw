<script setup lang="ts">
import CharacterPawn from './CharacterPawn.vue';
import PlayerText from './PlayerText.vue';
import PropertyArt from './PropertyArt.vue';
import { activityArt } from '../activity-art';
import type { Tile } from '../../shared/types';
interface CardTargetOption { id: string; name: string; detail: string; avatarId?: string; color?: string; reason?: string; tile?: Tile }
defineProps<{ options: CardTargetOption[]; selected: string; playerTarget?: boolean; disabled?: boolean }>();
const emit = defineEmits<{ select: [id: string] }>();
const baseUrl = import.meta.env.BASE_URL;
</script>
<template>
  <div class="card-target-picker" :class="{ 'player-targets': playerTarget }" role="group" aria-label="选择卡牌目标">
    <button v-for="option in options" :key="option.id" type="button" class="target-option" :class="{ selected: selected === option.id, unavailable: !!option.reason }" :disabled="disabled || !!option.reason" :aria-pressed="selected === option.id" :aria-label="`选择目标 ${option.name}，${option.detail}${option.tile?.type === 'property' ? `，地价 ¥${option.tile.price.toLocaleString()}` : ''}${option.reason ? `，${option.reason}` : ''}`" @click="emit('select', option.id)">
      <CharacterPawn v-if="playerTarget" :avatar-id="option.avatarId" :color="option.color" />
      <PropertyArt v-else-if="option.tile?.type === 'property'" :level="option.tile.level" :mortgaged="option.tile.mortgaged" />
      <img v-else-if="option.tile" class="target-tile-art" :src="option.tile.type === 'activity' && option.tile.activityKind ? activityArt(`${option.tile.activityKind}-tile`) : `${baseUrl}images/tiles/${option.tile.type}.png`" alt="" draggable="false" />
      <span v-else class="target-tile-symbol">⌂</span>
      <span class="target-option-copy"><strong><PlayerText :text="option.name" /></strong><small><PlayerText :text="option.detail" /></small><span v-if="option.tile?.type === 'property'" class="target-option-price">地价 ¥{{ option.tile.price.toLocaleString() }}</span><small v-if="option.reason" class="target-option-reason">{{ option.reason }}</small></span><span v-if="selected === option.id" class="target-check" aria-hidden="true">✓</span>
    </button>
    <p v-if="!options.length" class="muted">本次没有可选目标。</p>
  </div>
</template>
