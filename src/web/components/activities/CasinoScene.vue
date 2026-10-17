<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import type { ActivityInput } from '../../activity-art';
import { useGameStore } from '../../stores/game';
import DiceFace from '../DiceFace.vue';
import ActivityArtwork from './ActivityArtwork.vue';
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
  <div class="casino-scene classic-stage casino-stage">
    <ActivityArtwork name="casino-scene" backdrop />
    <div class="classic-stage-sign"><small>NUANNUAN · LUCKY DICE</small><strong>暖暖的好运骰桌</strong><span>挑好答案，等骰子落定</span></div>
    <div class="activity-dice-table"><span v-if="activity.stage === 'result' && activity.result" class="dice-result-ring" aria-hidden="true"></span><ActivityArtwork class="casino-table-art" name="casino-table" /><div class="casino-live-die"><DiceFace :value="activity.stage === 'rolling' ? face : activity.result || null" :rolling="activity.stage === 'rolling'" :settled="activity.stage === 'result'" /></div></div>
    <div v-if="activity.stage === 'result' && activity.result" class="dice-result-label"><strong>{{ activity.result }} 点</strong><span>{{ activity.result <= 3 ? '小 · 1～3 点' : '大 · 4～6 点' }}</span></div>
    <div v-if="activity.stage === 'select'" class="casino-controls">
      <div class="activity-options mode-options"><button v-for="n in [1, 2]" :key="n" class="activity-option" :class="{ selected: mode === n }" :aria-pressed="mode === n" :disabled="!enabled" @click="chooseMode(n)">{{ n === 1 ? '猜大小 · 二选一' : '猜点数 · 六选一' }}</button></div>
      <p>本金 <strong>¥{{ stake.toLocaleString() }}</strong> · 猜中总到账 <strong>¥{{ prize.toLocaleString() }}</strong>（含本金）</p>
      <div class="activity-options casino-guess-options" :class="{ points: mode === 2 }"><button v-for="n in mode === 1 ? 2 : 6" :key="n" class="activity-option casino-guess" :class="{ selected: guess === n }" :aria-pressed="guess === n" :aria-label="mode === 1 ? n === 1 ? '猜小，1至3点' : '猜大，4至6点' : `猜${n}点`" :disabled="!enabled" @click="guess = n"><DiceFace v-if="mode === 2" :value="n" /><span v-else class="casino-size-label">{{ n === 1 ? '小' : '大' }}</span><strong>{{ mode === 1 ? n === 1 ? '1～3 点' : '4～6 点' : `${n} 点` }}</strong></button></div>
      <p class="activity-help">只有落点玩家可下注；本次骰子不会移动棋子。</p>
      <div class="button-row"><button class="button secondary" :disabled="!enabled" @click="emit('input', { selection: 0 })">放弃</button><button class="button primary" :disabled="!enabled || (game.me?.cash ?? 0) < stake" @click="emit('input', { selection: mode, point: guess })">{{ (game.me?.cash ?? 0) < stake ? '现金不足' : `下注 ¥${stake.toLocaleString()}` }}</button></div>
    </div>
    <p v-else-if="activity.stage === 'rolling'" class="activity-stage-caption">暖暖正在掷骰，答案马上揭晓…</p>
  </div>
</template>
