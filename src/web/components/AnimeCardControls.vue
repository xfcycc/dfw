<script setup lang="ts">
import { computed, ref, watch, watchEffect } from 'vue';
import ThemedSelect from './ThemedSelect.vue';
import { useGameStore } from '../stores/game';
import { isKamui, rentFor } from '../../shared/rules';
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
const sealSide = ref('');
const seals = ref(['', '', '']);
const board = computed(() => game.room?.board ?? []);
const enemies = computed(() => board.value.filter(t => t.type === 'property' && !isKamui(t) && t.level > 0 && t.ownerId && t.ownerId !== game.me?.id && !game.room?.players.find(p => p.id === t.ownerId)?.bankrupt));
/** 输入格号，按真实矩形边长返回所属边。 */
function side(id: number): number { const h = Math.ceil(board.value.length / 4), v = Math.floor(board.value.length / 4); return id < h ? 0 : id < h + v ? 1 : id < 2 * h + v ? 2 : 3; }
const sealCandidates = computed(() => board.value.filter(t => t.type === 'property' && !isKamui(t)));
const sealSides = computed(() => [{ value: '', label: '请选择一条边', disabled: true }, ...Array.from({ length: 4 }, (_, index) => {
  const count = sealCandidates.value.filter(t => side(t.id) === index).length;
  return { value: String(index), label: `第 ${index + 1} 条边 · ${count} 处可选地产`, disabled: count === 0 };
})]);
const sealTiles = computed(() => sealCandidates.value.filter(t => sealSide.value !== '' && side(t.id) === Number(sealSide.value)));
const sealCount = computed(() => seals.value.filter(id => id !== '').length);
const sealingOwn = computed(() => sealTiles.value.some(t => t.ownerId === game.me?.id && seals.value.includes(String(t.id))));
/** 输入切换后的封印边，清空旧边所选地产，避免跨边提交；无返回值。 */
watch(sealSide, () => { seals.value = ['', '', '']; });
/** 输入封印槽位，返回同边且未被其他槽位选择的地产选项，可留空少选。 */
function sealOptions(index: number): { value: string; label: string }[] {
  return [{ value: '', label: '不选择此处' }, ...sealTiles.value.filter(t => !seals.value.some((id, slot) => slot !== index && id === String(t.id))).map(t => ({ value: String(t.id), label: label(t) }))];
}
const tiles = computed(() => {
  if (['thunderLord', 'fireBurial', 'igniteSea'].includes(props.card.kind)) return enemies.value;
  if (props.card.kind === 'keqingRush') return enemies.value.filter(t => { const distance = Math.abs(t.id - (game.me?.position ?? 0)); return Math.min(distance, board.value.length - distance) <= 6; });
  if (['starburst', 'corruption', 'sevenSwords'].includes(props.card.kind)) return enemies.value;
  if (props.card.kind === 'timeRestore') return board.value.filter(t => t.type === 'property' && !isKamui(t) && t.ownerId === game.me?.id && t.damageOwnerId === game.me?.id && (t.damageLevel ?? 0) > t.level && (game.me?.personalTurns ?? 0) <= (t.damageUntilTurn ?? 0));
  if (props.card.kind === 'voidTears') return board.value.filter(t => t.type === 'property' && !isKamui(t));
  if (props.card.kind === 'absoluteZero') return board.value.filter((t, index, all) => all.findIndex(v => side(v.id) === side(t.id)) === index);
  return board.value;
});
const sacrificeLands = computed(() => board.value.filter(t => t.type === 'property' && t.ownerId === game.me?.id && !t.mortgaged && !isKamui(t)));
const sacrifices = computed(() => board.value.filter(t => t.type === 'property' && t.ownerId === game.me?.id && t.level > 0 && !t.mortgaged && !isKamui(t)));
/** 输入当前突入目标，分别显示降级成功和被防御时的租金，仍以服务端结算为准。 */
const rushRent = computed(() => {
  if (props.card.kind !== 'keqingRush') return '';
  const tile = tiles.value.find(t => String(t.id) === first.value);
  if (!tile) return '';
  const jailed = game.room?.players.find(p => p.id === tile.ownerId)?.jailed;
  const multiplier = game.me?.nextRentDouble ? 2 : 1;
  const hit = jailed ? 0 : rentFor({ ...tile, level: Math.max(0, tile.level - 1) }, game.room?.economy) * multiplier;
  const blocked = jailed ? 0 : rentFor(tile, game.room?.economy) * multiplier;
  return `降级成功预计租金 ¥${hit.toLocaleString()}；降级被抵挡预计 ¥${blocked.toLocaleString()}。${tile.demolitionShield ? '当前护盾可抵挡本次降级。' : ''}以上未计本人付款防御；传送始终执行。`;
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
  if (['keqingRush', 'thunderLord', 'fireBurial', 'igniteSea', 'starburst', 'corruption', 'timeRestore', 'explosion', 'absoluteZero', 'voidTears'].includes(props.card.kind)) { valid = first.value !== '' && tiles.value.some(t => String(t.id) === first.value); if (valid) p.tileId = Number(first.value); }
  if (props.card.kind === 'fireBurial') { p.secondTileId = Number(second.value); valid = valid && second.value !== '' && sacrifices.value.some(t => String(t.id) === second.value); }
  if (props.card.kind === 'voidTears') { p.secondTileId = Number(second.value); valid = valid && second.value !== '' && second.value !== first.value && tiles.value.some(t => String(t.id) === second.value); }
  if (['commandSpell', 'swordRain'].includes(props.card.kind)) { p.playerId = player.value; valid = players.value.some(v => v.id === player.value); }
  if (props.card.kind === 'commandSpell') p.point = point.value;
  if (props.card.kind === 'railgun') p.direction = direction.value;
  if (props.card.kind === 'sevenSwords') { p.tileIds = swords.value.filter(v => v !== '').map(Number); valid = p.tileIds.length > 0 && p.tileIds.every(id => enemies.value.some(t => t.id === id)); }
  if (props.card.kind === 'kamui') {
    p.tileIds = seals.value.filter(id => id !== '').map(Number);
    valid = sealSide.value !== '' && p.tileIds.length >= 1 && p.tileIds.length <= 3 && new Set(p.tileIds).size === p.tileIds.length && p.tileIds.every(id => sealTiles.value.some(t => t.id === id));
  }
  if (props.card.kind === 'xiaowuSacrifice') { p.tileIds = [first.value, second.value].filter(id => id !== '').map(Number); valid = !(game.privateData?.soulRingRemaining ?? 0) && p.tileIds.length === 2 && first.value !== second.value && p.tileIds.every(id => sacrificeLands.value.some(t => t.id === id)); }
  if (props.card.kind === 'projection') valid = !!game.privateData?.projectionAvailable;
  if (props.card.kind === 'contract') valid = (game.me?.handLimit ?? 6) > 1;
  emit('change', p, valid);
});
</script>
<template>
  <div class="anime-card-controls">
    <label v-if="['keqingRush', 'thunderLord', 'fireBurial', 'igniteSea', 'starburst', 'corruption', 'timeRestore', 'explosion', 'absoluteZero', 'voidTears'].includes(card.kind)" class="field">{{ card.kind === 'absoluteZero' ? '冰封区域' : card.kind === 'explosion' ? '连续三格的起点' : card.kind === 'voidTears' ? '第一处空间端点' : '目标地产' }}<ThemedSelect v-model="first" label="选择目标地产" :options="options" /></label>
    <template v-if="card.kind === 'fireBurial'"><label class="field">献祭自家建筑<ThemedSelect v-model="second" label="选择献祭地产" :options="[{ value: '', label: '请选择自家有建筑地产', disabled: true }, ...sacrifices.map(t => ({ value: String(t.id), label: label(t) }))]" /></label><p class="notice warning">确认后先献祭一级建筑，无退款且四之弹不可恢复；攻击被防御也不返还。</p></template>
    <template v-if="card.kind === 'xiaowuSacrifice'">
      <label class="field">第一处献祭地产<ThemedSelect v-model="first" label="第一处献祭地产" :options="[{ value: '', label: '请选择自家未抵押地产', disabled: true }, ...sacrificeLands.filter(t => String(t.id) !== second).map(t => ({ value: String(t.id), label: label(t) }))]" /></label>
      <label class="field">第二处献祭地产<ThemedSelect v-model="second" label="第二处献祭地产" :options="[{ value: '', label: '请选择另一处自家地产', disabled: true }, ...sacrificeLands.filter(t => String(t.id) !== first).map(t => ({ value: String(t.id), label: label(t) }))]" /></label>
      <p class="notice warning">确认后失去两处土地及全部建筑，无退款、无补偿且不可修复。获得十万年魂环：含本回合共8个本人回合，自选免除最多3笔正租金；每笔看到金额后再决定。</p>
      <p v-if="game.privateData?.soulRingRemaining" class="notice">十万年魂环仍在生效，不能重复发动或刷新。</p>
    </template>
    <p v-if="rushRent" class="notice warning">{{ rushRent }}</p>
    <p v-if="card.kind === 'bankai'" class="notice">{{ (card.parameters?.version ?? 0) >= 2 ? '发动后，本回合最多再出2张其他主动牌，含UR；不能再用卍解或投影。替代移动后结束出牌，其他限制和费用照常。' : '确认发动后消耗卡与主动次数，并固定两枚骰点；随后必须选一枚前进或后退。' }}</p>
    <p v-if="card.kind === 'igniteSea' && first !== ''" class="notice warning">先移除所选地产全部护盾，再降低 {{ Math.min(3, 1 + (tiles.find(t => String(t.id) === first)?.demolitionShieldCharges || (tiles.find(t => String(t.id) === first)?.demolitionShield ? 1 : 0))) }} 级建筑（不低于0级）；对手可主动防御，其他附加效果保留。</p>
    <label v-if="card.kind === 'voidTears'" class="field">第二处空间端点<ThemedSelect v-model="second" label="选择第二端点" :options="options.filter(v => v.value !== first || v.value === '')" /></label>
    <label v-if="['commandSpell', 'swordRain'].includes(card.kind)" class="field">目标玩家<ThemedSelect v-model="player" label="选择目标玩家" :options="playerOptions" /></label>
    <label v-if="card.kind === 'commandSpell'" class="field">命令移动点数<ThemedSelect v-model="point" label="命令移动点数" :options="Array.from({ length: 6 }, (_, i) => ({ value: i + 1, label: `${i + 1} 点` }))" /></label>
    <label v-if="card.kind === 'railgun'" class="field">贯穿方向<ThemedSelect v-model="direction" label="贯穿方向" :options="[{ value: 1, label: '顺时针前方六格' }, { value: -1, label: '逆时针前方六格' }]" /></label>
    <template v-if="card.kind === 'sevenSwords'"><p class="notice">分配3道剑气，可重复选地集火，也可留空；每层护盾抵挡1道，未分配的剑气作废。</p><label v-for="(_, index) in swords" :key="index" class="field">第 {{ index + 1 }} 道剑气<ThemedSelect v-model="swords[index]" :label="`第${index + 1}道剑气`" :options="[{ value: '', label: '本剑气不使用' }, ...options.filter(v => v.value !== '')]" /></label></template>
    <template v-if="card.kind === 'kamui'">
      <p class="notice">同一条边手选 1～3 处地产，不必连续，可以混选自己、对手和无主地产；封印三个施术者后续个人回合。</p>
      <label class="field">封印范围<ThemedSelect v-model="sealSide" label="选择封印所在边" :options="sealSides" /></label>
      <template v-if="sealSide !== ''"><label v-for="(_, index) in seals" :key="index" class="field">第 {{ index + 1 }} 处地产<ThemedSelect v-model="seals[index]" :label="`第${index + 1}处封印地产`" :options="sealOptions(index)" /></label><p class="muted" role="status">已选 {{ sealCount }}／3 处，选择一处即可确认。</p></template>
      <p v-if="sealingOwn" class="notice warning">自家地产封印期间同样不能收租、升级、交易或变现；建筑与原有附加效果保留。</p>
    </template>
    <p v-if="preview" class="notice">作用路径：{{ preview }}</p>
    <p v-if="card.kind === 'contract'" class="notice warning">获得现金 ¥5000，本局手牌上限永久减1，最低1张。</p>
    <p v-if="card.kind === 'projection' && !game.privateData?.projectionAvailable" class="notice">上一回合没有可投影的主动道具，此卡保留。</p>
    <p v-if="card.kind === 'projection'" class="muted">从上一回合使用过的主动道具中选择，含UR，不含投影魔术；复制牌本回合可额外使用1次，回合结束消失。</p>
    <p v-if="card.kind === 'swordRain'" class="muted">剩余 {{ card.remainingUses || 2 }} 次；每个个人回合限用1次。</p>
  </div>
</template>
