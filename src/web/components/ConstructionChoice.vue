<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { ContentChoice, GameAction } from '../../shared/types';
import { useGameStore } from '../stores/game';
import { buildCost, isKamui, RULES } from '../../shared/rules';
import ThemedSelect from './ThemedSelect.vue';
import MapTileSelect from './MapTileSelect.vue';
import ContentCard from './ContentCard.vue';

const props = defineProps<{ choice: ContentChoice }>();
const emit = defineEmits<{ done: [] }>();
const game = useGameStore();
const first = ref<number | ''>('');
const second = ref<number | ''>('');
const materials = ref<string[]>([]);
const firstLevel = ref(0);
const bonus = ref<number | ''>('');
const kind = computed(() => props.choice.type.slice('construction_'.length));
const remote = computed(() => kind.value === 'remote');
const resonance = computed(() => kind.value === 'geoResonance');
const two = computed(() => ['growthLink', 'sereniteaPot', 'createBuild'].includes(kind.value));
const secondNeeded = computed(() => ['growthLink', 'sereniteaPot'].includes(kind.value) || kind.value === 'createBuild' && materials.value.length === 2);
const tiles = computed(() => props.choice.tileIds.map(id => game.room?.board.find(t => t.id === id)).filter(t => !!t));
/** 输入共鸣中心格号，按服务端同样的三格范围预览可升级地产。 */
function resonanceTargets(center: number) {
  const board = game.room?.board ?? [];
  return [-1, 0, 1].map(offset => board[(center + offset + board.length) % board.length])
    .filter(t => !!t && t.type === 'property' && t.ownerId === game.me?.id && !t.mortgaged && !isKamui(t) && !(t.zeroCasterId && (t.zeroUntilTurn ?? 0) > 0) && t.level < RULES.maxLevel && !game.room?.upgradedTiles?.includes(t.id));
}
const resonancePreview = computed(() => resonance.value && first.value !== '' ? resonanceTargets(first.value) : []);
const total = computed(() => (game.room?.board.find(t => t.id === first.value)?.level ?? 0) + (game.room?.board.find(t => t.id === second.value)?.level ?? 0) + 1);
const secondLevel = computed(() => total.value - firstLevel.value);
const levels = computed(() => Array.from({ length: 4 }, (_, value) => ({ value, label: `${value} 级`, disabled: total.value - value < 0 || total.value - value > 3 })));
const enabled = computed(() => game.canAct && game.canDecide && !game.room?.tradePending);
const valid = computed(() => kind.value === 'remoteConstruction' || first.value !== '' && (!secondNeeded.value || second.value !== '' && first.value !== second.value)
  && (kind.value !== 'createBuild' || materials.value.length >= 1 && materials.value.length <= 2)
  && (kind.value !== 'sereniteaPot' || secondLevel.value >= 0 && secondLevel.value <= 3 && (bonus.value === first.value ? firstLevel.value > 0 : bonus.value === second.value && secondLevel.value > 0)));

/** 输入当前合法地产，生成沿用已有选择组件的名称与本次实际建造费。 */
function options(excluded: number | ''): Array<{ value: number | ''; label: string; disabled?: boolean }> {
  return [{ value: '', label: resonance.value ? '请选择共鸣中心' : '请选择地产', disabled: true }, ...tiles.value.map(tile => ({ value: tile.id, disabled: tile.id === excluded,
    label: resonance.value ? `${tile.id}号 ${tile.name} · 可升级 ${resonanceTargets(tile.id).length} 处` : `${tile.id}号 ${tile.name} · ${tile.level}级${remote.value ? ` → ${tile.level + 1}级 · ¥${Math.floor(buildCost(tile, game.room?.economy) * 75 / 100).toLocaleString()}` : ''}` }))];
}
watch(() => props.choice.id, () => { first.value = ''; second.value = ''; materials.value = []; bonus.value = ''; }, { immediate: true });
watch([first, second], () => { firstLevel.value = Math.min(3, total.value); bonus.value = first.value; });

/** 输入确认或放弃，提交本次固定选择身份、建造目标和材料，不在客户端修改资产。 */
async function submit(accept: boolean): Promise<void> {
  if (accept && !valid.value) return;
  const action: Extract<GameAction, { type: 'contentDecision' }> = { type: 'contentDecision', decisionId: props.choice.id, accept };
  if (accept && kind.value !== 'remoteConstruction') {
    action.tileIds = [Number(first.value), ...(secondNeeded.value ? [Number(second.value)] : [])];
    action.tileId = kind.value === 'sereniteaPot' ? Number(bonus.value) : Number(first.value);
    if (kind.value === 'sereniteaPot') action.selection = firstLevel.value;
    if (kind.value === 'createBuild') action.discardCardIds = materials.value;
  }
  if (await game.act(action)) emit('done');
}
</script>

<template>
  <div class="content-choice">
    <ContentCard v-if="choice.content" :content="choice.content" />
    <p class="notice">{{ choice.prompt ?? choice.content?.effect }}</p>
    <template v-if="kind !== 'remoteConstruction' && (!remote || tiles.length)">
      <label v-if="resonance" class="field">共鸣中心<MapTileSelect v-model="first" label="共鸣中心" :options="options('')" :highlighted-tile-ids="resonancePreview.map(t => t.id)" /></label>
      <label v-else class="field">{{ remote ? '选择要升级的地产' : two ? '第一处地产' : '建设地产' }}<MapTileSelect v-model="first" :label="remote ? '选择要升级的地产' : '第一处建设地产'" :options="options(secondNeeded ? second : '')" /></label>
      <p v-if="resonancePreview.length" class="notice">将免费升级：{{ resonancePreview.map(t => `${t.id}号 ${t.name}（${t.level} → ${t.level + 1}级）`).join('、') }}</p>
      <template v-if="kind === 'createBuild'">
        <p>选择一至两张其他手牌作为材料，每张建设一处不同地产，材料不折现。</p>
        <div class="choice-card-grid"><label v-for="card in choice.cards" :key="card.id" class="keep-card" :class="{ selected: materials.includes(card.id) }"><input v-model="materials" type="checkbox" :value="card.id" :disabled="!materials.includes(card.id) && materials.length >= 2" /><ContentCard :content="card" compact thumbnail /></label></div>
      </template>
      <label v-if="secondNeeded" class="field">第二处地产<MapTileSelect v-model="second" label="第二处建设地产" :options="options(first)" /></label>
      <template v-if="kind === 'sereniteaPot' && first !== '' && second !== ''">
        <p>两处最终建筑合计 {{ total }} 级，包含免费新增的一级，每处最高三级。</p>
        <label class="field">第一处最终等级<ThemedSelect v-model="firstLevel" label="第一处最终建筑等级" :options="levels" /></label>
        <p>第二处最终等级：{{ secondLevel }} 级</p>
        <label class="field">新增一级的位置<MapTileSelect v-model="bonus" label="新增建筑所在地产" :options="[{ value: first, label: '第一处地产' }, { value: second, label: '第二处地产' }]" /></label>
      </template>
    </template>
    <div class="button-row"><button class="button secondary" :disabled="!enabled" @click="submit(false)">{{ remote ? '跳过本回合' : choice.cardId ? '取消并保留卡' : '放弃本次建设' }}</button><button v-if="!remote || tiles.length" class="button primary" :disabled="!enabled || !valid" @click="submit(true)">{{ kind === 'remoteConstruction' ? '开启基建轮班' : remote ? '支付并升级' : '确认建设' }}</button></div>
  </div>
</template>
