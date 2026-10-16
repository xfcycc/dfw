<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import { activityArt, type ActivityInput } from '../../activity-art';
import { useGameStore } from '../../stores/game';
const props = defineProps<{ activity: CornerActivity; enabled: boolean }>();
const emit = defineEmits<{ input: [value: ActivityInput] }>();
const game = useGameStore(); const selected = ref(1); const now = ref(Date.now());
const timer = setInterval(() => { now.value = Date.now(); }, 100);
onBeforeUnmount(() => { clearInterval(timer); });
const stake = computed(() => game.room?.activityRotation?.money.horseStake ?? 0);
const prize = computed(() => game.room?.activityRotation?.money.horsePrize ?? 0);
const names = ['莓莓', '蓝莓', '布丁', '薄荷', '葡萄', '蜜桃', '奶盖', '可可'];
let raceStartedAt = props.activity.startedAt;
let raceDuration = 8000;
const sprintStartedAt = ref<number | null>(null);
const sprintPositions = ref<number[]>([]);
const sprintDuration = 1000;
const sprinting = computed(() => sprintStartedAt.value !== null && now.value < sprintStartedAt.value + sprintDuration);
/** 输入马匹编号和当前时间，返回持续向前且不会提前冲线的展示进度。 */
function racingPosition(horse: number, time: number): number {
  const t = Math.max(0, (time - raceStartedAt) / raceDuration);
  const phase = horse * 2;
  const distance = t * 74 + 2 * (Math.sin(t * 12 + phase) - Math.sin(phase));
  return distance <= 74 ? distance : 74 + 12 * (1 - Math.exp(-(distance - 74) / 12));
}
// 揭晓时记录当前坐标，冠军和其他马都从原位向前冲刺；直接打开结果时展示最终位置。
watch(() => props.activity.stage, (stage, previous) => {
  const time = Date.now();
  now.value = time;
  if (stage === 'racing') {
    raceStartedAt = props.activity.startedAt;
    raceDuration = Math.max(1, (props.activity.endsAt ?? raceStartedAt + 8000) - raceStartedAt);
    sprintStartedAt.value = null;
  } else if (stage === 'result' && previous === 'racing') {
    sprintPositions.value = Array.from({ length: 8 }, (_, index) => racingPosition(index + 1, time));
    sprintStartedAt.value = time;
  }
}, { immediate: true });
/** 输入马匹编号，返回比赛或冲刺坐标，冠军仅使用服务端已揭晓的结果。 */
function position(horse: number): number {
  if (props.activity.stage === 'result') {
    if (!props.activity.result) return 0;
    const target = props.activity.result === horse ? 100 : 87 + horse * .8;
    if (sprintStartedAt.value === null) return target;
    const progress = Math.max(0, Math.min(1, (now.value - sprintStartedAt.value) / sprintDuration));
    const start = sprintPositions.value[horse - 1] ?? 0;
    return start + (target - start) * progress;
  }
  if (props.activity.stage !== 'racing') return 0;
  return racingPosition(horse, now.value);
}
</script>
<template>
  <div class="horse-scene">
    <p>八匹萌马机会均等 · 本金 ¥{{ stake.toLocaleString() }} · 押中总到账 ¥{{ prize.toLocaleString() }}（含本金）</p>
    <div class="horse-racetrack" :class="{ running: activity.stage === 'racing' || sprinting }">
      <div v-for="horse in 8" :key="horse" class="horse-lane" :class="{ champion: !sprinting && activity.result === horse }"><span class="horse-number">{{ horse }}</span><div class="horse-runner" :style="{ left: `calc(${position(horse)}% - ${position(horse) * .56}px)`, '--horse-delay': `${horse * -.08}s` }"><img :src="activityArt(`horse-${horse}`)" :alt="`${horse}号${names[horse - 1]}`" /></div><span class="horse-finish" aria-hidden="true"></span></div>
    </div>
    <div v-if="activity.stage === 'select'" class="activity-options horse-options"><button v-for="horse in 8" :key="horse" class="activity-option" :class="{ selected: selected === horse }" :disabled="!enabled" @click="selected = horse"><img :src="activityArt(`horse-${horse}`)" alt="" /><span>{{ horse }}号 · {{ names[horse - 1] }}</span></button></div>
    <div class="horse-tickets"><span v-for="id in activity.submitted" :key="id">{{ game.room?.players.find(p => p.id === id)?.name }}：{{ activity.bets[id] ? `${activity.bets[id]}号马` : '观赛' }}</span></div>
    <div v-if="activity.stage === 'select'" class="button-row"><button class="button secondary" :disabled="!enabled" @click="emit('input', { selection: 0 })">只看比赛</button><button class="button primary" :disabled="!enabled || (game.me?.cash ?? 0) < stake" @click="emit('input', { selection: selected })">下注 ¥{{ stake.toLocaleString() }}</button></div>
    <p v-else-if="activity.stage === 'racing' || sprinting" class="activity-stage-caption">{{ sprinting ? '最后冲刺！冲呀，小马！' : '比赛进行中！冲呀，小马！' }}</p>
  </div>
</template>
