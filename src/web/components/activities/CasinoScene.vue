<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import type { ActivityInput } from '../../activity-art';
import { useGameStore } from '../../stores/game';
import DiceFace from '../DiceFace.vue';
const props = defineProps<{ activity: CornerActivity; enabled: boolean }>();
const emit = defineEmits<{ input: [value: ActivityInput] }>();
const game = useGameStore();
const mode = ref(1); const guess = ref(1); const face = ref(1);
const amounts = computed(() => game.room?.activityRotation?.money ?? {});
const stake = computed(() => amounts.value[mode.value === 1 ? 'casinoSmallStake' : 'casinoPointStake'] ?? 0);
const prize = computed(() => amounts.value[mode.value === 1 ? 'casinoSmallPrize' : 'casinoPointPrize'] ?? 0);
let timer: ReturnType<typeof setInterval> | undefined;
watch(() => props.activity.stage, stage => { clearInterval(timer); if (stage === 'rolling') timer = setInterval(() => { face.value = face.value % 6 + 1; }, 90); }, { immediate: true });
onBeforeUnmount(() => { clearInterval(timer); });
/** 输入模式，重置该模式的可选猜测，无返回值。 */
function chooseMode(value: number): void { mode.value = value; guess.value = 1; }
</script>
<template>
  <div class="casino-scene">
    <div class="activity-dice-table"><DiceFace :value="activity.stage === 'rolling' ? face : activity.result || null" :rolling="activity.stage === 'rolling'" :settled="activity.stage === 'result'" /></div>
    <template v-if="activity.stage === 'select'">
      <div class="activity-options mode-options"><button v-for="n in [1, 2]" :key="n" class="activity-option" :class="{ selected: mode === n }" :disabled="!enabled" @click="chooseMode(n)">{{ n === 1 ? '猜大小 · 二选一' : '猜点数 · 六选一' }}</button></div>
      <p>本金 <strong>¥{{ stake.toLocaleString() }}</strong> · 猜中总到账 <strong>¥{{ prize.toLocaleString() }}</strong>（含本金）</p>
      <div class="activity-options"><button v-for="n in mode === 1 ? 2 : 6" :key="n" class="activity-option" :class="{ selected: guess === n }" :disabled="!enabled" @click="guess = n">{{ mode === 1 ? n === 1 ? '小 · 1～3' : '大 · 4～6' : `${n} 点` }}</button></div>
      <p class="activity-help">只有落点玩家可下注；本次骰子不会移动棋子。</p>
      <div class="button-row"><button class="button secondary" :disabled="!enabled" @click="emit('input', { selection: 0 })">放弃</button><button class="button primary" :disabled="!enabled || (game.me?.cash ?? 0) < stake" @click="emit('input', { selection: mode, point: guess })">{{ (game.me?.cash ?? 0) < stake ? '现金不足' : `下注 ¥${stake.toLocaleString()}` }}</button></div>
    </template>
    <p v-else-if="activity.stage === 'rolling'" class="activity-stage-caption">暖暖正在掷骰，答案马上揭晓…</p>
  </div>
</template>
