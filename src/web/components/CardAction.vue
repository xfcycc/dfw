<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { Card, GameAction } from '../../shared/types';
import ContentCard from './ContentCard.vue';
import { useGameStore } from '../stores/game';
const props = defineProps<{ card: Card }>();
const emit = defineEmits<{ done: [] }>();
const game = useGameStore();
const point = ref(3);
const target = ref('');

const candidates = computed(() => {
  const room = game.room; const me = game.me;
  if (!room || !me) return [];
  if (props.card.kind === 'swap' || props.card.kind === 'steal') return room.players.filter(player => !player.bankrupt && player.id !== me.id && (props.card.kind !== 'swap' || !player.jailed)).map(player => ({ value: player.id, label: player.name }));
  return room.board.filter(tile => {
    if (props.card.kind === 'demolish') return !!tile.ownerId && tile.ownerId !== me.id && tile.level > 0;
    const distance = (tile.id - me.position + room.board.length) % room.board.length;
    return tile.type === 'property' && !tile.barrier && distance >= 1 && distance <= 6;
  }).map(tile => ({ value: String(tile.id), label: `${tile.name} · ${tile.id}号` }));
});
const allowed = computed(() => game.canAct && game.isMyTurn && game.room?.phase === 'pre_roll' && !game.room.pending && !game.privateData?.activeCardUsed && !game.room.tradePending && !['immunity', 'halfPayment', 'release'].includes(props.card.kind));
const needsTarget = computed(() => ['barrier', 'demolish', 'swap', 'steal'].includes(props.card.kind));
watch(candidates, values => { if (!values.some(value => value.value === target.value)) target.value = ''; }, { immediate: true });
/** 根据选中的私有道具与目标发送命令，成功后关闭弹窗。 */
async function submit(): Promise<void> {
  const action: Extract<GameAction, { type: 'card' }> = { type: 'card', cardId: props.card.id };
  if (props.card.kind === 'remote') action.point = point.value;
  if (props.card.kind === 'swap' || props.card.kind === 'steal') action.playerId = target.value;
  if (props.card.kind === 'barrier' || props.card.kind === 'demolish') action.tileId = Number(target.value);
  if (await game.act(action)) emit('done');
}
</script>
<template>
  <ContentCard :content="card" />
  <label v-if="card.kind === 'remote'" class="field">指定点数<select v-model.number="point"><option v-for="number in 6" :key="number" :value="number">{{ number }} 点</option></select></label>
  <label v-if="needsTarget" class="field">选择目标<select v-model="target"><option disabled value="">请选择目标</option><option v-for="value in candidates" :key="value.value" :value="value.value">{{ value.label }}</option></select></label>
  <p v-if="needsTarget && !candidates.length" class="notice">当前没有符合条件的目标，可以保留到下次。</p>
  <p class="muted">每回合掷骰前最多主动使用一张道具。固定步数和主动位移会替代本次掷骰。免费升级确认成功才消耗，抢夺空手对手会保留卡和次数。</p>
  <p v-if="['immunity', 'halfPayment', 'release'].includes(card.kind)" class="notice">此卡在对应的付款防御或限制施加窗口选择使用。</p>
  <button v-else class="button primary full" :disabled="!allowed || (needsTarget && !target)" @click="submit">{{ card.kind === 'freeUpgrade' ? '选择免费升级目标' : '使用道具' }}</button>
</template>
