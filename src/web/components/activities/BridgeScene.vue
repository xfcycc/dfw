<script setup lang="ts">
import { computed } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import type { ActivityInput } from '../../activity-art';
import { useGameStore } from '../../stores/game';
const props = defineProps<{ activity: CornerActivity; enabled: boolean }>();
const emit = defineEmits<{ input: [value: ActivityInput] }>();
const game = useGameStore();
const money = computed(() => game.room?.activityRotation?.money ?? {});
const revealing = computed(() => ['bridge_round', 'result'].includes(props.activity.stage));
const ownSelection = computed(() => game.privateData?.activityId === props.activity.id && game.privateData.activityRound === props.activity.round ? game.privateData.activitySelection : null);
</script>
<template>
  <div class="bridge-scene">
    <p>自愿支付 <strong>¥{{ money.horseStake?.toLocaleString() }}</strong> · 连过三关，每人独得 <strong>¥{{ money.horsePrize?.toLocaleString() }}</strong></p>
    <p class="activity-help">每关左右各有一条安全桥。全员秘密选边后统一揭晓，可多人通关，奖金不分摊。</p>
    <div class="bridge-course"><div v-for="n in 3" :key="n" class="bridge-pair" :class="{ current: activity.level === n }"><small>第 {{ n }} 关</small><span v-for="side in 2" :key="side" :class="{ safe: activity.safePaths?.[n - 1] === side, broken: activity.safePaths?.[n - 1] && activity.safePaths[n - 1] !== side }">{{ activity.safePaths?.[n - 1] ? activity.safePaths[n - 1] === side ? '✓' : '×' : '?' }}</span></div></div>
    <div class="bridge-roster"><span v-for="id in activity.participants" :key="id" :class="{ eliminated: activity.stage !== 'select' && !activity.contenders.includes(id) }"><strong>{{ game.room?.players.find(player => player.id === id)?.name }}</strong><small>{{ activity.stage === 'select' ? activity.submitted.includes(id) ? activity.wagers[id] ? '已参加' : '已放弃' : '待决定' : !activity.wagers[id] ? '围观' : revealing && activity.revealed[id] ? `${activity.revealed[id] === 1 ? '左桥' : '右桥'} · ${activity.contenders.includes(id) ? '通过' : '掉落'}` : !activity.contenders.includes(id) ? '已淘汰' : activity.submitted.includes(id) ? '已锁定' : '正在选边' }}</small></span></div>
    <div v-if="activity.stage === 'select'" class="button-row"><button class="button secondary" :disabled="!enabled" @click="emit('input', { selection: 0 })">围观助威</button><button class="button primary" :disabled="!enabled || (game.me?.cash ?? 0) < (money.horseStake ?? 0)" @click="emit('input', { selection: 1 })">{{ (game.me?.cash ?? 0) < (money.horseStake ?? 0) ? '现金不足' : `支付 ¥${money.horseStake?.toLocaleString()} 上桥` }}</button></div>
    <div v-else-if="activity.stage === 'bridge_select'" class="activity-options bridge-choices"><button v-for="side in 2" :key="side" class="activity-option" :disabled="!enabled" @click="emit('input', { selection: side })"><span aria-hidden="true">{{ side === 1 ? '↖' : '↗' }}</span><strong>第 {{ activity.level }} 关 · {{ side === 1 ? '左桥' : '右桥' }}</strong><small>秘密锁定，揭晓前不公开</small></button></div>
    <p v-if="activity.stage === 'bridge_reveal'" class="activity-stage-caption">所有选择已锁定，安全桥即将揭晓…</p>
    <p v-if="activity.stage === 'bridge_round'" class="activity-stage-caption">{{ activity.summary }}</p>
    <p v-if="activity.stage === 'bridge_select' && ownSelection != null" class="activity-help">你已锁定{{ ownSelection === 1 ? '左桥' : '右桥' }}，等待其他玩家。</p>
    <p v-if="activity.stage === 'bridge_select' && game.me && !activity.contenders.includes(game.me.id)" class="activity-help">你在观赛席，为桥上的朋友助威吧。</p>
  </div>
</template>
