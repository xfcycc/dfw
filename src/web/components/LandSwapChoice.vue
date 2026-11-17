<script setup lang="ts">
import { computed, ref } from 'vue';
import type { ContentChoice, GameAction, PublicPlayer, Tile } from '../../shared/types';
import ThemedSelect from './ThemedSelect.vue';
import MapTargetButton from './MapTargetButton.vue';

const props = defineProps<{
  choice: ContentChoice;
  board: readonly Tile[];
  players: readonly Pick<PublicPlayer, 'id' | 'name'>[];
  enabled: boolean;
  submit: (action: Extract<GameAction, { type: 'contentDecision' }>) => Promise<boolean>;
}>();
const emit = defineEmits<{ done: [] }>();
const opponentId = ref('');
const targetId = ref('');
const ownId = ref('');
const targets = computed(() => props.choice.landSwapTargets ?? []);
const board = computed(() => props.board);
const opponents = computed(() => props.players.filter(p => targets.value.some(t => board.value[t.tileId]?.ownerId === p.id)));
const target = computed(() => targets.value.find(t => String(t.tileId) === targetId.value));
const pair = computed(() => target.value?.pairs.find(p => String(p.ownTileId) === ownId.value));
const targetTile = computed(() => target.value ? board.value[target.value.tileId] : undefined);
const ownTile = computed(() => pair.value ? board.value[pair.value.ownTileId] : undefined);
const opponentName = computed(() => opponents.value.find(p => p.id === targetTile.value?.ownerId)?.name ?? '对手');
const opponentOptions = computed(() => [{ value: '', label: '全部对手' }, ...opponents.value.map(p => ({ value: p.id, label: p.name }))]);
const targetOptions = computed(() => targets.value.filter(t => !opponentId.value || board.value[t.tileId]?.ownerId === opponentId.value)
  .map(t => {
    const tile = board.value[t.tileId];
    const owner = opponents.value.find(p => p.id === tile?.ownerId)?.name ?? '对手';
    return { value: String(t.tileId), label: `${owner} · ${tile?.name ?? t.tileId} · ${tile?.level ?? 0}级 · ${money(t.value)}${t.reason ? ` · ${t.reason}` : ''}`, disabled: !!t.reason };
  }).sort((a, b) => Number(a.disabled) - Number(b.disabled)));
const ownOptions = computed(() => (target.value?.pairs ?? []).map(p => {
  const tile = board.value[p.ownTileId];
  return { value: String(p.ownTileId), label: `${tile?.name ?? p.ownTileId} · ${tile?.level ?? 0}级 · ${money(p.ownValue)} · ${difference(p.cashDue)}` };
}));

/** 输入金额，返回弹框统一的现金显示。 */
function money(amount: number): string { return `¥${amount.toLocaleString('zh-CN')}`; }
/** 输入抽牌人的应付差价，返回补差方向。 */
function difference(due: number): string { return due > 0 ? `你补 ${money(due)}` : due < 0 ? `对方补 ${money(-due)}` : '无需补差'; }
/** 输入连地加成，返回玩家可读的持有组合。 */
function connection(percent: number): string { return percent === 35 ? '三连及以上' : percent === 20 ? '两连' : '无连地'; }
/** 输入对手筛选项，只保留当前筛选下仍然选中的目标。 */
function selectOpponent(id: string): void {
  opponentId.value = id;
  if (!targetOptions.value.some(option => option.value === targetId.value && !option.disabled)) selectTarget('');
}
/** 输入新的对手地块，切换目标时清空旧配对。 */
function selectTarget(id: string): void {
  if (targetId.value === id) return;
  targetId.value = id;
  ownId.value = '';
}
/** 输入确认或放弃；确认时按对手地、自家地的顺序提交给服务端复核。 */
async function decide(accept: boolean): Promise<void> {
  const selected = target.value, offer = pair.value;
  if (accept && (!selected || !offer)) return;
  const tileIds = accept && selected && offer ? [selected.tileId, offer.ownTileId] : undefined;
  if (await props.submit({ type: 'contentDecision', decisionId: props.choice.id, accept, tileIds })) emit('done');
}
</script>

<template>
  <div class="content-choice land-swap-choice">
    <p class="notice">先选对手的地，再选你拿哪块换。确认后直接交换，对手无需同意。</p>
    <label v-if="opponents.length > 1" class="field">按对手筛选<ThemedSelect :model-value="opponentId" label="按对手筛选" :options="opponentOptions" @update:model-value="selectOpponent" /></label>
    <label class="field">1. 选对手的地产
      <ThemedSelect :model-value="targetId" label="选对手的地产" :options="[{ value: '', label: '选择对手地产', disabled: true }, ...targetOptions]" :disabled="!enabled" @update:model-value="selectTarget" />
    </label>
    <MapTargetButton label="选对手的地产" :options="targetOptions" :selected="target ? [target.tileId] : []" :disabled="!enabled" @select="selectTarget(String($event))" />
    <p v-if="target" class="muted">{{ targetTile?.name }}当前租金 {{ money(target.rent) }}</p>
    <template v-if="target">
      <label class="field">2. 选自己的交换地产
        <ThemedSelect v-model="ownId" label="选自己的交换地产" :options="[{ value: '', label: '选择自家地产', disabled: true }, ...ownOptions]" :disabled="!enabled" />
      </label>
      <MapTargetButton label="选自己的交换地产" :options="ownOptions" :selected="pair ? [pair.ownTileId] : []" :disabled="!enabled" @select="ownId = String($event)" />
    </template>
    <div class="swap-footer">
      <div v-if="pair && target" class="notice swap-preview" aria-live="polite">
        <div><strong>你获得 {{ targetTile?.name }}</strong><span>估值 {{ money(target.value) }} · 换后租金 {{ money(pair.receivedRent) }}</span><span>连地：{{ connection(pair.previousReceivedConnected) }} → {{ connection(pair.receivedConnected) }}</span></div>
        <div><strong>{{ opponentName }}获得 {{ ownTile?.name }}</strong><span>估值 {{ money(pair.ownValue) }} · 换后租金 {{ money(pair.givenRent) }}</span><span>连地：{{ connection(pair.previousGivenConnected) }} → {{ connection(pair.givenConnected) }}</span></div>
        <strong>{{ difference(pair.cashDue) }}</strong>
      </div>
      <div class="button-row choice-actions"><button class="button secondary" :disabled="!enabled" @click="decide(false)">放弃换地</button><button class="button primary" :disabled="!enabled || !pair" @click="decide(true)">确认交换</button></div>
    </div>
  </div>
</template>

<style scoped>
.land-swap-choice .field { margin-top: 4px; }
.swap-footer { position: sticky; bottom: calc(-1 * var(--dialog-padding, 18px)); padding: 8px 0 12px; background: #fff9eb; }
.swap-preview, .swap-preview > div { display: grid; gap: 4px; }
.swap-preview { grid-template-columns: repeat(2, minmax(0, 1fr)); margin: 0 0 8px; }
.swap-preview > strong { grid-column: 1 / -1; }
</style>
