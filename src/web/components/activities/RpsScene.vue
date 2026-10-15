<script setup lang="ts">
import { ref } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import { activityArt, type ActivityInput } from '../../activity-art';
import { useGameStore } from '../../stores/game';
const props = defineProps<{ activity: CornerActivity; enabled: boolean }>();
const emit = defineEmits<{ input: [value: ActivityInput] }>();
const game = useGameStore(); const selected = ref(0); const names = ['石头', '剪刀', '布'];
</script>
<template>
  <div class="rps-scene" :class="{ revealing: activity.stage === 'revealing' }"><p>最后一名胜者独揽 <strong>¥{{ game.room?.activityRotation?.money.rpsPrize.toLocaleString() }}</strong> · 当前还剩 {{ activity.contenders.length }} 人</p><p class="activity-help">两种手势淘汰败方；一种或三种手势均为平局，继续出拳。</p>
    <div class="rps-players"><div v-for="id in activity.participants" :key="id" class="rps-player" :class="{ eliminated: !activity.contenders.includes(id) }"><strong>{{ game.room?.players.find(p => p.id === id)?.name }}</strong><div class="rps-hand"><img v-if="['round_result', 'result'].includes(activity.stage) && activity.revealed[id] !== undefined" :src="activityArt(`rps-${activity.revealed[id]}`)" :alt="names[activity.revealed[id]!]" /><span v-else>{{ !activity.contenders.includes(id) ? '出局' : activity.submitted.includes(id) ? '✓' : '?' }}</span></div><small>{{ !activity.contenders.includes(id) ? '围观助威' : activity.submitted.includes(id) ? '已锁定手势' : '准备出拳' }}</small></div></div>
    <template v-if="activity.stage === 'select'"><div class="activity-options rps-options"><button v-for="(name, n) in names" :key="name" class="activity-option" :class="{ selected: selected === n }" :disabled="!enabled" @click="selected = n"><img :src="activityArt(`rps-${n}`)" alt="" /><strong>{{ name }}</strong></button></div><button class="button primary" :disabled="!enabled" @click="emit('input', { selection: selected })">锁定{{ names[selected] }}</button></template><p v-if="activity.stage === 'revealing'" class="activity-stage-caption">石头、剪刀、布！</p><p v-if="activity.stage === 'round_result'" class="activity-stage-caption">{{ activity.summary }}</p>
  </div>
</template>
