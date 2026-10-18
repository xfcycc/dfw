<script setup lang="ts">
import ThemedSelect from './ThemedSelect.vue';
import { computed, ref } from 'vue';
import { TILE_CARDS, PLAYER_CARDS } from '../card-targets';
import type { Card, GameAction } from '../../shared/types';
import ContentCard from './ContentCard.vue';
import PlayerText from './PlayerText.vue';
import { useGameStore } from '../stores/game';
const props = defineProps<{ card: Card; target?: string; secondTarget?: string; targetLabel?: string; hasCandidates?: boolean; compact?: boolean; readonly?: boolean; dialog?: boolean }>();
const emit = defineEmits<{ done: [kind: Card['kind']]; cancel: []; submitting: [value: boolean] }>();
const game = useGameStore();
const point = ref(3);
const secondPlayer = computed(() => game.room?.players.find(player => player.id === props.secondTarget && !player.bankrupt && player.id !== props.target));
const allowed = computed(() => !props.readonly && game.canAct && game.isMyTurn && game.room?.phase === 'pre_roll' && !game.room.pending && !game.privateData?.activeCardUsed && !game.room.tradePending && !['immunity', 'halfPayment', 'release'].includes(props.card.kind));
const needsTarget = computed(() => [...TILE_CARDS, ...PLAYER_CARDS].includes(props.card.kind));
/** 输入已选地块或玩家目标，提交私有卡牌命令；成功才结束选牌，全拆复用地产目标，爆破和入狱允许指定自己。 */
async function submit(): Promise<void> {
  if (!allowed.value || needsTarget.value && !props.target) return;
  const action: Extract<GameAction, { type: 'card' }> = { type: 'card', cardId: props.card.id };
  if (props.card.kind === 'remote') action.point = point.value;
  if (PLAYER_CARDS.includes(props.card.kind)) action.playerId = props.target;
  if (props.card.kind === 'swapAny') { if (!secondPlayer.value) return; action.secondPlayerId = secondPlayer.value.id; }
  if (TILE_CARDS.includes(props.card.kind)) action.tileId = Number(props.target);
  emit('submitting', true);
  try { if (await game.act(action)) emit('done', props.card.kind); }
  finally { emit('submitting', false); }
}
</script>
<template>
  <div class="card-action" :class="{ compact }">
    <ContentCard v-if="!compact" :content="card" />
    <label v-if="card.kind === 'remote' && !readonly" class="field">指定点数<ThemedSelect v-model="point" label="指定点数" :options="Array.from({ length: 6 }, (_, index) => ({ value: index + 1, label: `${index + 1} 点` }))" /></label>
    <p v-if="needsTarget && !readonly" class="target-current" role="status"><PlayerText :text="card.kind === 'swapAny' ? `第一位：${targetLabel || '未选择'} ↔ 第二位：${secondPlayer?.name ?? '未选择'}` : targetLabel ? `已选目标：${targetLabel}` : dialog ? '请选择目标，再确认使用' : PLAYER_CARDS.includes(card.kind) ? '请点击玩家头像选择目标' : '请点击地图高亮地块选择目标'" /></p>
    <p v-if="card.kind === 'swapAny' && target && secondTarget && game.room?.players.find(p => p.id === target)?.position === game.room?.players.find(p => p.id === secondTarget)?.position" class="notice">两名玩家当前同格，交换后位置不会变化。</p>
    <p v-if="needsTarget && !readonly && !hasCandidates" class="notice">当前没有符合条件的目标，可以保留到下次。</p>
    <p v-if="!compact" class="muted">每回合掷骰前最多主动使用一张道具。免费升级确认成功才消耗，抢夺空手对手会保留卡和次数。</p>
    <p v-if="['immunity', 'halfPayment', 'release'].includes(card.kind)" class="notice">此卡在对应的付款防御或限制施加窗口选择使用。</p>
    <div class="button-row">
      <button class="button secondary small" :disabled="game.busy" @click="emit('cancel')">{{ readonly ? '关闭说明' : '取消选牌' }}</button>
      <button v-if="!readonly && !['immunity', 'halfPayment', 'release'].includes(card.kind)" class="button primary small" :disabled="!allowed || (needsTarget && !target) || (card.kind === 'swapAny' && !secondPlayer)" @click="submit">{{ ['freeUpgrade', 'fullUpgrade'].includes(card.kind) ? '选择免费升级目标' : card.kind === 'gamble' ? '查看下注选择' : '确认使用' }}</button>
    </div>
    <p v-if="!allowed && !game.busy && !['immunity', 'halfPayment', 'release'].includes(card.kind)" class="muted">需在自己的掷骰前使用，且本回合尚未使用主动道具。</p>
  </div>
</template>
