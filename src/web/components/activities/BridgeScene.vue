<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import type { ActivityInput } from '../../activity-art';
import { useGameStore } from '../../stores/game';
import CharacterPawn from '../CharacterPawn.vue';
import ActivityArtwork from './ActivityArtwork.vue';
const props = defineProps<{ activity: CornerActivity; enabled: boolean }>();
const emit = defineEmits<{ input: [value: ActivityInput] }>();
const game = useGameStore();
const money = computed(() => game.room?.activityRotation?.money ?? {});
const ownSelection = computed(() => game.privateData?.activityId === props.activity.id && game.privateData.activityRound === props.activity.round ? game.privateData.activitySelection : null);
const players = computed(() => game.room?.players.filter(player => props.activity.participants.includes(player.id)) ?? []);
const falls = ref<Record<string, { level: number; side: number }>>({});
const animate = ref(false);
watch(() => `${props.activity.stage}:${props.activity.level}:${props.activity.safePaths?.length}:${ownSelection.value}`, (_, previous) => {
  animate.value = previous !== undefined && game.connection === 'online' && !game.room?.recoveryPending;
  const a = props.activity;
  if (!['bridge_round', 'result'].includes(a.stage) || !a.safePaths?.length) return;
  for (const [id, side] of Object.entries(a.revealed)) {
    if (a.wagers[id] && side !== a.safePaths.at(-1)) falls.value[id] = { level: a.safePaths.length, side };
  }
}, { immediate: true });
watch(() => game.connection, () => { animate.value = false; });
/** 输入参与者编号，返回公开落点；本人可先看自己的选路，其他人等待揭晓。 */
function position(id: string): { level: number; side: number; fallen: boolean } {
  const a = props.activity, fall = falls.value[id];
  if (fall) return { ...fall, fallen: true };
  if (!a.wagers[id] || a.stage === 'select' || !a.contenders.includes(id)) return { level: 0, side: 0, fallen: false };
  if (id === game.me?.id && ['bridge_select', 'bridge_reveal'].includes(a.stage) && ownSelection.value != null) return { level: a.level ?? 1, side: ownSelection.value, fallen: false };
  const level = a.safePaths?.length ?? 0;
  return { level, side: a.safePaths?.at(-1) ?? 0, fallen: false };
}
/** 输入参与者编号，返回报名或过桥状态，不公开未揭晓的他人方向。 */
function status(id: string): string {
  const a = props.activity;
  if (a.stage === 'select') return a.submitted.includes(id) ? a.wagers[id] ? '已参加' : '围观' : '待决定';
  if (!a.wagers[id]) return '围观';
  if (!a.contenders.includes(id)) return '已掉落';
  if (a.settled) return '成功通关';
  if (a.stage === 'bridge_round') return '本关通过';
  return a.submitted.includes(id) ? '已锁定' : '等待下一跳';
}
</script>
<template>
  <div class="bridge-scene">
    <p>自愿支付 <strong>¥{{ money.horseStake?.toLocaleString() }}</strong> · 连过三关，每位通关者得 <strong>¥{{ money.horsePrize?.toLocaleString() }}</strong></p>
    <p class="activity-help">从左向右过桥，每列上下两块玻璃，分别对应左路与右路。点击本关玻璃秘密选路，全员锁定后揭晓。</p>
    <div class="glass-bridge" :class="{ animate }">
      <ActivityArtwork name="bridge-scene" backdrop />
      <div class="bridge-deck start"><strong>起点</strong><small>准备上桥</small></div>
      <div v-for="n in 3" :key="n" class="glass-column" :class="{ current: activity.level === n && !activity.settled }">
        <small class="glass-level">第 {{ n }} 关</small>
        <button v-for="side in 2" :key="side" class="glass-pane" :class="{ safe: activity.safePaths?.[n - 1] === side, latest: n === activity.safePaths?.length, broken: !!activity.safePaths?.[n - 1] && activity.safePaths[n - 1] !== side, chosen: activity.level === n && ['bridge_select', 'bridge_reveal'].includes(activity.stage) && ownSelection === side }" :disabled="!enabled || activity.stage !== 'bridge_select' || activity.level !== n" :aria-label="`第${n}关${side === 1 ? '左路' : '右路'}${activity.safePaths?.[n - 1] ? activity.safePaths[n - 1] === side ? '安全' : '已碎裂' : ''}`" @click="emit('input', { selection: side })">
          <ActivityArtwork v-if="activity.safePaths?.[n - 1] && activity.safePaths[n - 1] !== side" class="glass-remains" name="glass-broken" />
          <span class="glass-surface" aria-hidden="true"><ActivityArtwork :name="activity.safePaths?.[n - 1] === side ? 'glass-safe' : 'glass'" /></span><span class="glass-label">{{ activity.safePaths?.[n - 1] ? activity.safePaths[n - 1] === side ? '安全' : '碎裂' : side === 1 ? '左路' : '右路' }}</span>
          <i v-for="shard in 4" :key="shard" class="glass-shard" :style="{ '--shard': shard }" aria-hidden="true"></i>
        </button>
      </div>
      <div class="bridge-deck finish"><strong>终点</strong><small>三关大奖</small><span aria-hidden="true">⚑</span></div>
      <div class="bridge-pawns" aria-hidden="true">
        <div v-for="(player, index) in players" :key="player.id" class="bridge-traveler" :class="{ fallen: position(player.id).fallen, 'latest-fall': falls[player.id]?.level === activity.safePaths?.length, benched: activity.stage !== 'select' && (!activity.wagers[player.id] || !activity.contenders.includes(player.id) && !falls[player.id]) }" :style="{ left: `${position(player.id).level ? 2 + position(player.id).level * 24 : 7}%`, top: position(player.id).side === 1 ? '34%' : position(player.id).side === 2 ? '74%' : '55%', '--offset': `${(index % 3 - 1) * 16}px`, '--row': `${Math.floor(index / 3) * 15}px` }">
          <span :key="`${position(player.id).level}:${position(player.id).side}`" class="bridge-hop"><CharacterPawn :avatar-id="player.avatarId" :color="player.color" /></span>
        </div>
      </div>
    </div>
    <div class="bridge-roster"><span v-for="player in players" :key="player.id" :class="{ eliminated: activity.stage !== 'select' && !activity.contenders.includes(player.id) }"><CharacterPawn :avatar-id="player.avatarId" :color="player.color" /><strong>{{ player.name }}</strong><small>{{ status(player.id) }}</small></span></div>
    <div v-if="activity.stage === 'select'" class="button-row"><button class="button secondary" :disabled="!enabled" @click="emit('input', { selection: 0 })">围观助威</button><button class="button primary" :disabled="!enabled || (game.me?.cash ?? 0) < (money.horseStake ?? 0)" @click="emit('input', { selection: 1 })">{{ (game.me?.cash ?? 0) < (money.horseStake ?? 0) ? '现金不足' : `支付 ¥${money.horseStake?.toLocaleString()} 上桥` }}</button></div>
    <p v-else-if="activity.stage === 'bridge_select' && enabled" class="activity-stage-caption">第 {{ activity.level }} 关 · 点击亮起的一块玻璃，跳过去！</p>
    <p v-if="activity.stage === 'bridge_reveal'" class="activity-stage-caption">所有人已锁定，玻璃承重中…</p>
    <p v-if="activity.stage === 'bridge_round'" class="activity-stage-caption" role="status">{{ activity.summary }}</p>
    <p v-if="activity.stage === 'bridge_select' && ownSelection != null" class="activity-help">你已跳上{{ ownSelection === 1 ? '左路' : '右路' }}玻璃，等待其他玩家锁定。</p>
    <p v-if="activity.stage !== 'select' && game.me && !activity.contenders.includes(game.me.id)" class="activity-help">你在观赛席，为桥上的朋友助威吧。</p>
  </div>
</template>
