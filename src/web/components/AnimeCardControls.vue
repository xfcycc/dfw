<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue';
import ThemedSelect from './ThemedSelect.vue';
import { useGameStore } from '../stores/game';
import { isKamui } from '../../shared/rules';
import type { Card, GameAction, Tile } from '../../shared/types';
const props = defineProps<{ card: Card }>();
const emit = defineEmits<{ change: [parameters: Omit<Extract<GameAction, { type: 'card' }>, 'type' | 'cardId'>, valid: boolean] }>();
const game = useGameStore();
const first = ref('');
const second = ref('');
const player = ref('');
const point = ref(3);
const direction = ref(1);
const swords = ref(['', '', '']);
const board = computed(() => game.room?.board ?? []);
const enemies = computed(() => board.value.filter(t => t.type === 'property' && !isKamui(t) && t.level > 0 && t.ownerId && t.ownerId !== game.me?.id && !game.room?.players.find(p => p.id === t.ownerId)?.bankrupt));
/** 输入格号，按真实矩形边长返回所属边。 */
function side(id: number): number { const h = Math.ceil(board.value.length / 4), v = Math.floor(board.value.length / 4); return id < h ? 0 : id < h + v ? 1 : id < 2 * h + v ? 2 : 3; }
const tiles = computed(() => {
  if (['starburst', 'corruption', 'sevenSwords'].includes(props.card.kind)) return enemies.value;
  if (props.card.kind === 'timeRestore') return board.value.filter(t => t.type === 'property' && !isKamui(t) && t.ownerId === game.me?.id && t.damageOwnerId === game.me?.id && (t.damageLevel ?? 0) > t.level && (game.me?.personalTurns ?? 0) <= (t.damageUntilTurn ?? 0));
  if (props.card.kind === 'voidTears') return board.value.filter(t => t.type === 'property' && !isKamui(t));
  if (props.card.kind === 'absoluteZero') return board.value.filter((t, index, all) => all.findIndex(v => side(v.id) === side(t.id)) === index);
  return board.value;
});
const players = computed(() => game.room?.players.filter(p => !p.bankrupt && p.id !== game.me?.id && (props.card.kind !== 'swordRain' || Math.min(Math.abs(p.position - (game.me?.position ?? 0)), board.value.length - Math.abs(p.position - (game.me?.position ?? 0))) <= 6)) ?? []);
/** 输入公开地产，返回既有选择控件可读的标签。 */
function label(tile: Tile): string { return props.card.kind === 'absoluteZero' ? `第 ${side(tile.id) + 1} 条边` : `${tile.id}号 ${tile.name} · ${game.room?.players.find(p => p.id === tile.ownerId)?.name ?? '无主'} · ${tile.level}级${tile.demolitionShield ? ` · 护盾${tile.demolitionShieldCharges || 1}层` : ''}`; }
const options = computed(() => [{ value: '', label: '请选择目标', disabled: true }, ...tiles.value.map(t => ({ value: String(t.id), label: label(t) }))]);
const playerOptions = computed(() => [{ value: '', label: '请选择玩家', disabled: true }, ...players.value.map(p => ({ value: p.id, label: p.name }))]);
const preview = computed(() => {
  if (props.card.kind === 'railgun') return Array.from({ length: 6 }, (_, i) => board.value[((game.me?.position ?? 0) + direction.value * (i + 1) + board.value.length) % board.value.length]?.name).filter(Boolean).join(' → ');
  if (props.card.kind === 'explosion' && first.value !== '') return Array.from({ length: 3 }, (_, i) => board.value[(Number(first.value) + i) % board.value.length]?.name).join(' → ');
  return '';
});
/** 输入本地表单，向原卡牌操作组件发送参数；服务端仍复核合法性。 */
watchEffect(() => {
  const p: Omit<Extract<GameAction, { type: 'card' }>, 'type' | 'cardId'> = {};
  let valid = true;
  if (['starburst', 'corruption', 'timeRestore', 'explosion', 'absoluteZero', 'voidTears'].includes(props.card.kind)) { valid = first.value !== '' && tiles.value.some(t => String(t.id) === first.value); if (valid) p.tileId = Number(first.value); }
  if (props.card.kind === 'voidTears') { p.secondTileId = Number(second.value); valid = valid && second.value !== '' && second.value !== first.value && tiles.value.some(t => String(t.id) === second.value); }
  if (['commandSpell', 'swordRain'].includes(props.card.kind)) { p.playerId = player.value; valid = players.value.some(v => v.id === player.value); }
  if (props.card.kind === 'commandSpell') p.point = point.value;
  if (props.card.kind === 'railgun') p.direction = direction.value;
  if (props.card.kind === 'sevenSwords') { p.tileIds = swords.value.filter(v => v !== '').map(Number); valid = p.tileIds.length > 0 && p.tileIds.every(id => enemies.value.some(t => t.id === id)); }
  if (props.card.kind === 'projection') valid = !!game.privateData?.projectionAvailable;
  if (props.card.kind === 'contract') valid = (game.me?.handLimit ?? 6) > 1;
  emit('change', p, valid);
});
</script>
<template>
  <div class="anime-card-controls">
    <label v-if="['starburst', 'corruption', 'timeRestore', 'explosion', 'absoluteZero', 'voidTears'].includes(card.kind)" class="field">{{ card.kind === 'absoluteZero' ? '冰封区域' : card.kind === 'explosion' ? '连续三格的起点' : card.kind === 'voidTears' ? '第一处空间端点' : '目标地产' }}<ThemedSelect v-model="first" label="选择目标地产" :options="options" /></label>
    <label v-if="card.kind === 'voidTears'" class="field">第二处空间端点<ThemedSelect v-model="second" label="选择第二端点" :options="options.filter(v => v.value !== first || v.value === '')" /></label>
    <label v-if="['commandSpell', 'swordRain'].includes(card.kind)" class="field">目标玩家<ThemedSelect v-model="player" label="选择目标玩家" :options="playerOptions" /></label>
    <label v-if="card.kind === 'commandSpell'" class="field">命令移动点数<ThemedSelect v-model="point" label="命令移动点数" :options="Array.from({ length: 6 }, (_, i) => ({ value: i + 1, label: `${i + 1} 点` }))" /></label>
    <label v-if="card.kind === 'railgun'" class="field">贯穿方向<ThemedSelect v-model="direction" label="贯穿方向" :options="[{ value: 1, label: '顺时针前方六格' }, { value: -1, label: '逆时针前方六格' }]" /></label>
    <template v-if="card.kind === 'sevenSwords'"><p class="notice">分配三道剑气，可重复选择地产集火，也可留空少用；护盾消耗一道剑气。</p><label v-for="(_, index) in swords" :key="index" class="field">第 {{ index + 1 }} 道剑气<ThemedSelect v-model="swords[index]" :label="`第${index + 1}道剑气`" :options="[{ value: '', label: '本剑气不使用' }, ...options.filter(v => v.value !== '')]" /></label></template>
    <p v-if="preview" class="notice">作用路径：{{ preview }}</p>
    <p v-if="card.kind === 'contract'" class="notice warning">确认获得实际现金 ¥3000，本局手牌上限永久减一，最低一张。</p>
    <p v-if="card.kind === 'projection' && !game.privateData?.projectionAvailable" class="notice">上一回合没有可投影的非UR主动道具，此卡保留。</p>
    <p v-if="card.kind === 'projection'" class="muted">确认后从上一回合实际使用过的非UR主动道具中选择；复制牌本回合可额外使用一次，回合结束消失。</p>
    <p v-if="card.kind === 'swordRain'" class="muted">剩余 {{ card.remainingUses || 2 }} 次；首次使用后下一个人回合重新可用。</p>
  </div>
</template>
