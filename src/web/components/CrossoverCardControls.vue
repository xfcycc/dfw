<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue';
import type { Card, GameAction } from '../../shared/types';
import { isKamui } from '../../shared/rules';
import { useGameStore } from '../stores/game';
import ThemedSelect from './ThemedSelect.vue';
/** 输入两个棋盘索引和总格数，返回环形最短格距。 */
function nearbyDistance(first: number, second: number, size: number): number { const distance = Math.abs(first - second); return Math.min(distance, size - distance); }
const props = defineProps<{ card: Card }>();
const emit = defineEmits<{ change: [parameters: Omit<Extract<GameAction, { type: 'card' }>, 'type' | 'cardId'>, valid: boolean] }>();
const game = useGameStore();
const point = ref(1);
const target = ref('');
const first = ref('');
const second = ref('');
const board = computed(() => game.room?.board ?? []);
const enemyTiles = computed(() => board.value.filter(t => t.type === 'property' && !isKamui(t) && t.level > 0 && t.ownerId && t.ownerId !== game.me?.id && !game.room?.players.find(p => p.id === t.ownerId)?.bankrupt));
const players = computed(() => game.room?.players.filter(p => !p.bankrupt && p.id !== game.me?.id && (props.card.kind !== 'blueSilverBind' || nearbyDistance(p.position, game.me?.position ?? 0, board.value.length) <= 6)) ?? []);
const hasPlayer = computed(() => ['revealingSwords', 'kingsPressure', 'blueSilverBind'].includes(props.card.kind));
/** 输入地产，生成与现有选择框一致的公开标签。 */
function label(id: number): string { const t = board.value[id]; return `${id}号 ${t?.name ?? ''} · ${t?.level ?? 0}级`; }
const firstOptions = computed(() => [{ value: '', label: '请选择第一处地产', disabled: true }, ...enemyTiles.value.filter(t => enemyTiles.value.some(v => v.id !== t.id && nearbyDistance(v.id, t.id, board.value.length) === 1)).map(t => ({ value: String(t.id), label: label(t.id) }))]);
const secondOptions = computed(() => [{ value: '', label: '请选择相邻地产', disabled: true }, ...enemyTiles.value.filter(t => first.value !== '' && nearbyDistance(t.id, Number(first.value), board.value.length) === 1).map(t => ({ value: String(t.id), label: label(t.id) }))]);
/** 输入本地表单，发送现有主动命令参数；目标和次数仍由服务端确认。 */
watchEffect(() => {
  const parameters: Omit<Extract<GameAction, { type: 'card' }>, 'type' | 'cardId'> = {};
  let valid = true;
  if (props.card.kind === 'asunaFlash') parameters.point = point.value;
  if (hasPlayer.value) { parameters.playerId = target.value; valid = players.value.some(p => p.id === target.value); }
  if (props.card.kind === 'shadowBlade') { parameters.tileIds = [Number(first.value), Number(second.value)]; valid = first.value !== '' && second.value !== '' && secondOptions.value.some(v => v.value === second.value); }
  if (props.card.kind === 'fusion') valid = (game.privateData?.hand.length ?? 0) >= 3;
  if (props.card.kind === 'goldenCore') valid = !game.me?.cultivated;
  if (props.card.kind === 'nuwaMend') valid = board.value.some(t => t.ownerId === game.me?.id && !isKamui(t) && t.damageOwnerId === game.me?.id && (t.damageLevel ?? 0) > t.level && (t.damageUntilTurn ?? 0) >= (game.me?.personalTurns ?? 0));
  if ((game.me?.defenseSpecializationTurns ?? 0) > 0 && ['asunaFlash', 'revealingSwords', 'shadowBlade', 'blueSilverBind', 'kingsPressure'].includes(props.card.kind)) valid = false;
  if ((game.me?.blueBindTurns ?? 0) > 0 && props.card.kind === 'asunaFlash') valid = false;
  emit('change', parameters, valid);
});
</script>
<template>
  <div class="crossover-card-controls">
    <label v-if="card.kind === 'asunaFlash'" class="field">突进距离<ThemedSelect v-model="point" label="突进距离" :options="[1, 2, 3].map(value => ({ value, label: `向前突进 ${value} 格` }))" /></label>
    <label v-if="hasPlayer" class="field">目标玩家<ThemedSelect v-model="target" label="目标玩家" :options="[{ value: '', label: '请选择对手', disabled: true }, ...players.map(p => ({ value: p.id, label: p.name }))]" /></label>
    <template v-if="card.kind === 'shadowBlade'"><label class="field">第一处地产<ThemedSelect v-model="first" label="第一处地产" :options="firstOptions" /></label><label class="field">紧邻地产<ThemedSelect v-model="second" label="紧邻地产" :options="secondOptions" /></label></template>
    <p v-if="card.kind === 'fusion'" class="muted">接下来依次选择两张材料；第一张决定优先类别，查看固定结果后再确认消耗。</p>
    <p v-if="card.kind === 'goldenCore' || card.kind === 'namelessSword'" class="notice warning">确认后放弃本回合主动移动，仍可经营、交易和防御。</p>
    <p v-if="card.kind === 'goldenCore' && game.me?.cultivated" class="notice">本局已经成功结丹，不能重复结丹。</p>
    <p v-if="card.kind === 'nuwaMend'" class="muted">接下来从最近同一次攻击或灾害实际损坏的地产中选择，最多三处。</p>
  </div>
</template>
