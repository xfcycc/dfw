<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import { useGameStore } from '../../stores/game';
import ActivityRewards from './ActivityRewards.vue';
const props = defineProps<{ activity: CornerActivity; inactive?: boolean; revealing?: boolean }>();
const emit = defineEmits<{ revealing: [active: boolean] }>();
const game = useGameStore();
const playing = ref(false);
const ready = ref(true);
const sequence = ref(0);
let finish: ReturnType<typeof setTimeout> | undefined;
let reveal: ReturnType<typeof setTimeout> | undefined;
const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');
const revealStages = ['rolling', 'racing', 'loading', 'shot', 'revealing', 'treasure_open', 'bridge_reveal', 'minority_reveal'];
const delay = computed(() => {
  const a = props.activity;
  if (['casino', 'horse'].includes(a.kind) && !a.result || ['bridge', 'treasure'].includes(a.kind) && !a.safePaths?.length) return 0;
  return ({ casino: 900, horse: 2500, rps: 1200, bridge: 1600, treasure: 1000, minority: 1550, roulette: 0, packet: 0, blindbox: 0 })[a.kind];
});
const rewarding = computed(() => Object.keys(props.activity.payouts).length > 0 || Object.values(props.activity.cardRewards ?? {}).some(count => count > 0));
const showRewards = computed(() => props.activity.stage === 'result' && props.activity.kind !== 'packet' && ready.value && !props.revealing);
/** 无入参，停止当前庆祝并展示静态结果；刷新、后台和重连不补播动画。 */
function stop(): void { clearTimeout(finish); clearTimeout(reveal); playing.value = false; ready.value = true; }
watch(ready, value => emit('revealing', !value));
watch(() => props.inactive, inactive => { if (inactive) stop(); });
watch(() => ({ id: props.activity.id, round: props.activity.round, stage: props.activity.stage, payouts: Object.keys(props.activity.payouts).join(','), epoch: game.presentationEpoch }), (current, previous) => {
  stop();
  if (props.inactive || current.id !== previous.id || current.epoch !== previous.epoch || game.connection !== 'online' || game.room?.recoveryPending || document.hidden || reduced.matches) return;
  if (current.stage === 'result' && props.activity.kind === 'roulette') return;
  if (!revealStages.includes(current.stage) && !['result', 'round_result', 'bridge_round', 'treasure_choice', 'duel'].includes(current.stage) && current.payouts === previous.payouts) return;
  playing.value = true; sequence.value++;
  if (current.stage === 'result' && delay.value) {
    ready.value = false;
    reveal = setTimeout(() => { ready.value = true; }, delay.value);
  }
  finish = setTimeout(() => { playing.value = false; }, Math.max(3200, delay.value + 2300));
});
watch(() => game.connection, state => { if (state !== 'online') stop(); });
/** 无入参，系统减少动态效果或页面进入后台时终止当前动效。 */
function suspend(): void { if (document.hidden || reduced.matches) stop(); }
document.addEventListener('visibilitychange', suspend);
reduced.addEventListener('change', suspend);
onBeforeUnmount(() => { stop(); emit('revealing', false); document.removeEventListener('visibilitychange', suspend); reduced.removeEventListener('change', suspend); });
</script>
<template>
  <div class="activity-presentation" :class="[`presentation-${activity.kind}`, `stage-${activity.stage}`, { 'moment-playing': playing, 'moment-win': rewarding, 'moment-reveal': revealStages.includes(activity.stage) }]">
    <slot />
    <ActivityRewards v-if="showRewards" :key="`${activity.id}:${activity.round}`" :activity="activity" :playing="playing" />
    <div v-if="playing && rewarding && activity.stage === 'result' && !['packet', 'blindbox'].includes(activity.kind)" :key="sequence" class="moment-confetti" aria-hidden="true"><i v-for="n in 14" :key="n" :style="{ '--particle': n, '--flight-delay': `${delay + n * 35}ms` }">{{ n % 3 ? '✦' : '●' }}</i></div>
  </div>
</template>
