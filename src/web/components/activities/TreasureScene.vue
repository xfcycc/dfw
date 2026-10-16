<script setup lang="ts">
import { computed } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import type { ActivityInput } from '../../activity-art';
import { useGameStore } from '../../stores/game';
const props = defineProps<{ activity: CornerActivity; enabled: boolean }>();
const emit = defineEmits<{ input: [value: ActivityInput] }>();
const game = useGameStore();
const level = computed(() => props.activity.level ?? 0);
const stake = computed(() => game.room?.activityRotation?.money.treasureStake ?? 0);
const prize = computed(() => stake.value * 2 ** level.value);
const choosing = computed(() => ['select', 'treasure_choice'].includes(props.activity.stage));
const affordable = computed(() => level.value > 0 || (game.me?.cash ?? 0) >= stake.value);
</script>
<template>
  <div class="treasure-scene">
    <p>本金 <strong>¥{{ stake.toLocaleString() }}</strong> · 每层二选一，安全就翻倍，触发机关全部归零</p>
    <div class="treasure-levels"><div v-for="n in 3" :key="n" :class="{ cleared: level >= n, current: level + 1 === n }"><span>{{ level >= n ? '✦' : '♜' }}</span><strong>第 {{ n }} 层</strong><b>¥{{ (stake * 2 ** n).toLocaleString() }}</b><small>{{ level >= n ? '安全通过' : level + 1 === n ? '下一层挑战' : '等待挑战' }}</small></div></div>
    <template v-if="choosing || activity.stage === 'treasure_open'">
      <p v-if="level > 0" class="treasure-earned">现在收手可得 <strong>¥{{ prize.toLocaleString() }}</strong>，继续成功可得 ¥{{ (prize * 2).toLocaleString() }}</p>
      <div class="activity-options treasure-boxes" :class="{ opening: activity.stage === 'treasure_open' }"><button v-for="side in 2" :key="side" class="activity-option" :disabled="!enabled || !affordable" @click="emit('input', { selection: side })"><span aria-hidden="true">🎁</span><strong>{{ side === 1 ? '左边宝箱' : '右边宝箱' }}</strong><small>{{ level === 0 ? `支付 ¥${stake.toLocaleString()} 开箱` : '继续挑战' }}</small></button></div>
      <button class="button secondary" :disabled="!enabled" @click="emit('input', { selection: 0 })">{{ level === 0 ? '放弃参加' : `见好就收 · 领取 ¥${prize.toLocaleString()}` }}</button>
      <p v-if="!affordable && enabled" class="activity-help">现金不足，仅能使用已有现金参加。</p>
    </template>
    <p v-if="activity.stage === 'treasure_open'" class="activity-stage-caption">宝箱正在打开，机关还是宝藏？</p>
    <p v-else-if="activity.stage === 'treasure_choice'" class="activity-help">{{ activity.summary }} · 前一层奖励尚未到账，收手后统一发放。</p>
    <p v-if="activity.safePaths?.length" class="activity-help">已揭晓安全宝箱：{{ activity.safePaths.map((side, index) => `第${index + 1}层${side === 1 ? '左' : '右'}`).join(' · ') }}</p>
  </div>
</template>
