<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { Card, GameAction } from '../../shared/types';
import { ITEMS } from '../../shared/rules';
import { useGameStore } from '../stores/game';
const props = defineProps<{ card: Card }>();
const emit = defineEmits<{ done: [] }>();
const game = useGameStore();
const point = ref(3);
const target = ref('');
const definition = computed(() => ITEMS[props.card.kind]);
const candidates = computed(() => {
  const room = game.room; const me = game.me;
  if (!room || !me) return [];
  if (props.card.kind === 'swap') return room.players.filter(player => !player.bankrupt && player.id !== me.id && !player.jailed).map(player => ({ value: player.id, label: player.name }));
  return room.board.filter(tile => {
    if (props.card.kind === 'demolish') return !!tile.ownerId && tile.ownerId !== me.id && tile.level > 0;
    const distance = (tile.id - me.position + room.board.length) % room.board.length;
    return tile.type === 'property' && !tile.barrier && distance >= 1 && distance <= 6;
  }).map(tile => ({ value: String(tile.id), label: `${tile.name} · ${tile.id}号` }));
});
const needsTarget = computed(() => ['barrier', 'demolish', 'swap'].includes(props.card.kind));
watch(candidates, values => { if (!values.some(value => value.value === target.value)) target.value = values[0]?.value ?? ''; }, { immediate: true });
/** 根据选中的私有道具与目标发送命令，成功后关闭弹窗。 */
async function submit(): Promise<void> {
  const action: Extract<GameAction, { type: 'card' }> = { type: 'card', cardId: props.card.id };
  if (props.card.kind === 'remote') action.point = point.value;
  if (props.card.kind === 'swap') action.playerId = target.value;
  if (props.card.kind === 'barrier' || props.card.kind === 'demolish') action.tileId = Number(target.value);
  if (await game.act(action)) emit('done');
}
</script>
<template>
  <div class="card-preview"><span>{{ definition.icon }}</span><p>{{ definition.description }}</p></div>
  <label v-if="card.kind === 'remote'" class="field">指定点数<select v-model.number="point"><option v-for="number in 6" :key="number" :value="number">{{ number }} 点</option></select></label>
  <label v-if="needsTarget" class="field">选择目标<select v-model="target"><option v-for="value in candidates" :key="value.value" :value="value.value">{{ value.label }}</option></select></label>
  <p v-if="needsTarget && !candidates.length" class="notice">当前没有符合条件的目标，可以保留到下次。</p>
  <p class="muted">每回合掷骰前最多主动使用一张道具。使用后道具消耗。</p>
  <button class="button primary full" :disabled="!game.canAct || !game.isMyTurn || game.room?.tradePending || (needsTarget && !target)" @click="submit">使用{{ definition.name }}</button>
</template>
