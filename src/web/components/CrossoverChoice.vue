<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { ContentChoice, GameAction } from '../../shared/types';
import { useGameStore } from '../stores/game';
import ContentCard from './ContentCard.vue';
import ThemedSelect from './ThemedSelect.vue';
const props = defineProps<{ choice: ContentChoice }>();
const emit = defineEmits<{ done: [] }>();
const game = useGameStore();
const selected = ref<string[]>([]);
const first = ref('');
const second = ref('');
const card = ref('');
const player = ref('');
const tile = ref('');
const tiles = ref<number[]>([]);
const replacements = ref<Record<string, string>>({});
const enabled = computed(() => game.canAct && game.canDecide && !game.room?.tradePending);
const type = computed(() => props.choice.type);
const cardOptions = computed(() => [{ value: '', label: '请选择手牌', disabled: true }, ...props.choice.cards.map(c => ({ value: c.id, label: `${c.name ?? c.kind} · ${c.rarity ?? 'SR'}` }))]);
const tileOptions = computed(() => [{ value: '', label: '请选择地产', disabled: true }, ...props.choice.tileIds.map(id => ({ value: String(id), label: `${id}号 ${game.room?.board[id]?.name ?? ''}` }))]);
const valid = computed(() => {
  switch (type.value) {
    case 'cross_fusion_materials': return !!first.value && !!second.value && first.value !== second.value;
    case 'cross_pink_materials': return selected.value.length >= 1 && selected.value.length <= 3;
    case 'cross_pink_pick': return props.choice.materialIds?.every(id => !!replacements.value[id]);
    case 'cross_mend': return tiles.value.length >= 1 && tiles.value.length <= 3;
    case 'cross_charge': return tile.value !== '';
    case 'cross_purify': return tile.value !== '' && !!card.value;
    case 'cross_bind_release': case 'cross_duel_stake': return !!card.value;
    case 'cross_duel_target': return !!player.value;
    default: return true;
  }
});
watch(() => `${props.choice.id}:${props.choice.type}`, () => {
  selected.value = []; first.value = ''; second.value = ''; card.value = ''; player.value = ''; tiles.value = []; replacements.value = {};
  tile.value = props.choice.tileIds.length === 1 ? String(props.choice.tileIds[0]) : '';
}, { immediate: true });
/** 输入确认或放弃，提交当前持久化选择，不在客户端生成随机候选。 */
async function decide(accept: boolean): Promise<void> {
  if (!enabled.value || accept && !valid.value) return;
  const action: Extract<GameAction, { type: 'contentDecision' }> = { type: 'contentDecision', decisionId: props.choice.id, accept };
  if (accept) {
    if (type.value === 'cross_fusion_materials') action.discardCardIds = [first.value, second.value];
    if (type.value === 'cross_pink_materials') action.discardCardIds = selected.value;
    if (type.value === 'cross_pink_pick') action.cardSelections = replacements.value;
    if (type.value === 'cross_mend') action.tileIds = tiles.value;
    if (tile.value !== '') action.tileId = Number(tile.value);
    if (card.value) action.cardId = card.value;
    if (player.value) action.playerId = player.value;
  }
  if (await game.act(action)) {
    if (!game.privateData?.choice || !game.privateData.choice.type.startsWith('cross_')) emit('done');
  }
}
</script>
<template>
  <div class="content-choice crossover-choice">
    <p v-if="choice.prompt" class="notice">{{ choice.prompt }}</p>
    <template v-if="type === 'cross_fusion_materials'">
      <label class="field">第一张材料<ThemedSelect v-model="first" label="第一张材料" :options="cardOptions" /></label>
      <label class="field">第二张材料<ThemedSelect v-model="second" label="第二张材料" :options="cardOptions.filter(v => !v.value || v.value !== first)" /></label>
      <div class="choice-card-grid"><ContentCard v-for="c in choice.cards.filter(v => [first, second].includes(v.id))" :key="c.id" :content="c" compact thumbnail /></div>
    </template>
    <template v-else-if="type === 'cross_fusion_confirm'"><ContentCard v-for="c in choice.cards" :key="c.id" :content="c" /></template>
    <template v-else-if="type === 'cross_pink_materials'">
      <p class="muted">已选择 {{ selected.length }}／3 张，包含UR；手牌数量与稀有度保持不变。</p>
      <div class="choice-card-grid"><label v-for="c in choice.cards" :key="c.id" class="keep-card" :class="{ selected: selected.includes(c.id) }"><input v-model="selected" type="checkbox" :value="c.id" :disabled="!selected.includes(c.id) && selected.length >= 3" /><ContentCard :content="c" compact thumbnail /></label></div>
    </template>
    <template v-else-if="type === 'cross_pink_pick'">
      <div v-for="material in choice.materialIds" :key="material"><p>{{ choice.cards.find(c => c.id === material)?.name }} · 选择同档新效果</p><div class="choice-card-grid"><label v-for="c in choice.cardOptions?.[material] ?? []" :key="c.id" class="keep-card" :class="{ selected: replacements[material] === c.id }"><input v-model="replacements[material]" type="radio" :name="material" :value="c.id" /><ContentCard :content="c" compact thumbnail /></label></div></div>
    </template>
    <template v-else-if="type === 'cross_mend'">
      <label v-for="id in choice.tileIds" :key="id" class="field"><input v-model="tiles" type="checkbox" :value="id" :disabled="!tiles.includes(id) && tiles.length >= 3" />{{ id }}号 {{ game.room?.board[id]?.name }} · 当前{{ game.room?.board[id]?.level }}级</label>
    </template>
    <template v-else-if="type === 'cross_purify' || type === 'cross_charge'"><label class="field">目标地产<ThemedSelect v-model="tile" label="目标地产" :options="tileOptions" /></label><label v-if="type === 'cross_purify'" class="field">弃置材料<ThemedSelect v-model="card" label="弃置材料" :options="cardOptions" /></label></template>
    <label v-else-if="type === 'cross_duel_target'" class="field">邀请对手<ThemedSelect v-model="player" label="邀请对手" :options="[{ value: '', label: '请选择对手', disabled: true }, ...(choice.playerIds ?? []).map(id => ({ value: id, label: game.room?.players.find(p => p.id === id)?.name ?? id }))]" /></label>
    <template v-else-if="type === 'cross_duel_stake' || type === 'cross_bind_release'"><div class="choice-card-grid"><label v-for="c in choice.cards" :key="c.id" class="keep-card" :class="{ selected: card === c.id }"><input v-model="card" type="radio" :name="choice.id" :value="c.id" /><ContentCard :content="c" compact thumbnail /></label></div></template>
    <div class="button-row choice-actions">
      <button class="button secondary" :disabled="!enabled" @click="decide(false)">{{ type === 'cross_boost' ? '停止喷射并结算' : type === 'cross_purify' ? '暂不净化' : type.startsWith('cross_duel') ? '放弃赌局' : type === 'cross_charge' ? '放弃蓄力' : type === 'cross_bind_release' ? '接受缠绕' : type.startsWith('cross_fusion') || type === 'cross_mend' ? '取消并保留卡' : '放弃本次搭配' }}</button>
      <button class="button primary" :disabled="!enabled || !valid" @click="decide(true)">{{ type === 'cross_boost' ? '追加前进1格' : type === 'cross_purify' ? '弃牌净化' : type === 'cross_fusion_materials' ? '查看固定融合结果' : type === 'cross_pink_materials' ? '查看同档搭配' : type === 'cross_charge' ? '释放三道剑气' : '确认选择' }}</button>
    </div>
  </div>
</template>
