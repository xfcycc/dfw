<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue';
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
/** 输入马匹编号，返回纯展示的赛道进度，冠军仅在服务端揭晓后使用。 */
function position(horse: number): number {
  if (props.activity.stage === 'result') return props.activity.result === horse ? 92 : 65 + horse * 2;
  if (props.activity.stage !== 'racing') return 0;
  const t = Math.max(0, Math.min(.96, (now.value - props.activity.startedAt) / 8000));
  return Math.max(0, Math.min(87, t * 82 + Math.sin(t * 15 + horse * 2) * 7 * Math.sin(t * Math.PI)));
}
</script>
<template>
  <div class="horse-scene">
    <p>八匹萌马机会均等 · 本金 ¥{{ stake.toLocaleString() }} · 押中总到账 ¥{{ prize.toLocaleString() }}（含本金）</p>
    <div class="horse-racetrack" :class="{ running: activity.stage === 'racing' }">
      <div v-for="horse in 8" :key="horse" class="horse-lane" :class="{ champion: activity.result === horse }"><span class="horse-number">{{ horse }}</span><div class="horse-runner" :style="{ left: `${position(horse)}%`, '--horse-delay': `${horse * -.08}s` }"><img :src="activityArt(`horse-${horse}`)" :alt="`${horse}号${names[horse - 1]}`" /></div><span class="horse-finish" aria-hidden="true"></span></div>
    </div>
    <div v-if="activity.stage === 'select'" class="activity-options horse-options"><button v-for="horse in 8" :key="horse" class="activity-option" :class="{ selected: selected === horse }" :disabled="!enabled" @click="selected = horse"><img :src="activityArt(`horse-${horse}`)" alt="" /><span>{{ horse }}号 · {{ names[horse - 1] }}</span></button></div>
    <div class="horse-tickets"><span v-for="id in activity.submitted" :key="id">{{ game.room?.players.find(p => p.id === id)?.name }}：{{ activity.bets[id] ? `${activity.bets[id]}号马` : '观赛' }}</span></div>
    <div v-if="activity.stage === 'select'" class="button-row"><button class="button secondary" :disabled="!enabled" @click="emit('input', { selection: 0 })">只看比赛</button><button class="button primary" :disabled="!enabled || (game.me?.cash ?? 0) < stake" @click="emit('input', { selection: selected })">下注 ¥{{ stake.toLocaleString() }}</button></div>
    <p v-else-if="activity.stage === 'racing'" class="activity-stage-caption">比赛进行中！冲呀，小马！</p>
  </div>
</template>
