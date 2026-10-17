<script setup lang="ts">
import { computed } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import type { ActivityInput } from '../../activity-art';
import { useGameStore } from '../../stores/game';
import CharacterPawn from '../CharacterPawn.vue';
import ActivityArtwork from './ActivityArtwork.vue';
const props = defineProps<{ activity: CornerActivity; enabled: boolean }>();
const emit = defineEmits<{ input: [value: ActivityInput] }>();
const game = useGameStore();
const level = computed(() => props.activity.level ?? 0);
const stake = computed(() => game.room?.activityRotation?.money.treasureStake ?? 0);
const prize = computed(() => stake.value * 2 ** level.value);
const choosing = computed(() => ['select', 'treasure_choice'].includes(props.activity.stage));
const affordable = computed(() => level.value > 0 || (game.me?.cash ?? 0) >= stake.value);
const explorer = computed(() => game.room?.players.find(player => player.id === props.activity.triggerId));
const ownSelection = computed(() => game.privateData?.activityId === props.activity.id && game.privateData.activityRound === props.activity.round ? game.privateData.activitySelection : null);
const failed = computed(() => props.activity.settled && (props.activity.safePaths?.length ?? 0) > level.value);
const displayLevel = computed(() => Math.min(3, level.value + (level.value < 3 && !props.activity.settled ? 1 : 0)));
/** 输入左右箱编号，返回最终揭晓的金币或机关；正在挑战时不预先展示安全方向。 */
function chestState(side: number): string {
  if (failed.value || level.value === 3) return props.activity.safePaths?.at(-1) === side ? 'gold' : 'trap';
  return props.activity.stage === 'treasure_open' && ownSelection.value === side ? 'opening' : '';
}
</script>
<template>
  <div class="treasure-scene">
    <p>本金 <strong>¥{{ stake.toLocaleString() }}</strong> · 每层二选一，安全就翻倍，触发机关全部归零</p>
    <div class="treasure-vault" :class="{ failed, opening: activity.stage === 'treasure_open' }">
      <ActivityArtwork name="treasure-scene" backdrop />
      <div class="vault-route"><span v-for="n in 3" :key="n" :class="{ cleared: level >= n, current: displayLevel === n }"><b>{{ level >= n ? '✓' : n }}</b><strong>¥{{ (stake * 2 ** n).toLocaleString() }}</strong><small>{{ level >= n ? '已通过' : `第 ${n} 层` }}</small></span></div>
      <div class="vault-heading"><span>贪心地宫 · 第 {{ failed ? activity.safePaths?.length : displayLevel || 1 }} 层</span><strong>{{ failed ? '机关触发！' : activity.settled ? activity.payouts[activity.triggerId] ? '带着宝藏满载而归' : '本次探险已放弃' : activity.stage === 'treasure_open' ? '咔哒…宝箱正在打开' : '两只宝箱，只有一条安全路' }}</strong></div>
      <div class="vault-chests"><button v-for="side in 2" :key="side" class="vault-chest" :class="chestState(side)" :disabled="!enabled || !choosing || !affordable" :aria-label="`${side === 1 ? '左' : '右'}边宝箱${level === 0 ? `，支付${stake}开箱` : '，继续挑战'}`" @click="emit('input', { selection: side })"><span class="chest-art" aria-hidden="true"><i class="chest-light"></i><ActivityArtwork :key="chestState(side)" :name="chestState(side) === 'gold' ? 'chest-gold' : chestState(side) === 'trap' ? 'chest-trap' : 'chest-closed'" /></span><strong>{{ side === 1 ? '左边宝箱' : '右边宝箱' }}</strong><small>{{ chestState(side) === 'trap' ? '机关陷阱' : chestState(side) === 'gold' ? '安全宝藏' : level === 0 ? `支付 ¥${stake.toLocaleString()} 开箱` : '打开后挑战翻倍' }}</small></button></div>
      <div v-if="explorer" class="vault-explorer"><CharacterPawn :avatar-id="explorer.avatarId" :color="explorer.color" /><span>{{ explorer.name }}<small>{{ activity.stage === 'treasure_open' ? '正在试探宝箱…' : failed ? '这次空手而归' : activity.settled ? '探险结束' : '站在岔路口' }}</small></span></div>
      <div class="vault-bag"><ActivityArtwork class="vault-bag-icon" name="chest-closed" /><div><small>{{ activity.settled ? '本次实际到账' : '宝藏暂存 · 收手才到账' }}</small><strong>¥{{ (activity.settled ? activity.payouts[activity.triggerId] ?? 0 : level > 0 ? prize : 0).toLocaleString() }}</strong></div><span v-if="!activity.settled && level > 0">继续挑战<br />成功再翻倍</span></div>
    </div>
    <button v-if="choosing || activity.stage === 'treasure_open'" class="button secondary vault-exit" :disabled="!enabled || !choosing" @click="emit('input', { selection: 0 })">{{ level === 0 ? '离开地宫 · 放弃参加' : `走出口收手 · 带走 ¥${prize.toLocaleString()}` }}</button>
    <p v-if="!affordable && enabled" class="activity-help">现金不足，仅能使用已有现金参加。</p>
    <p v-if="choosing && level > 0" class="activity-help">{{ activity.summary }} · 再选宝箱会把背包中尚未到账的奖励一起押上。</p>
    <p v-if="activity.safePaths?.length" class="activity-help">探险足迹：{{ activity.safePaths.map((side, index) => `第${index + 1}层${side === 1 ? '左箱' : '右箱'}安全`).join(' → ') }}</p>
  </div>
</template>
