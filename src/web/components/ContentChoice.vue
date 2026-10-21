<script setup lang="ts">
import ThemedSelect from './ThemedSelect.vue';
import ConstructionChoice from './ConstructionChoice.vue';
import CrossoverChoice from './CrossoverChoice.vue';
import { computed, ref, watch } from 'vue';
import type { ContentChoice, GameAction } from '../../shared/types';
import { useGameStore } from '../stores/game';
import ContentCard from './ContentCard.vue';
import PlayerText from './PlayerText.vue';
const props = defineProps<{ choice: ContentChoice; tileId?: number | null; targetLabel: string; compact?: boolean; dialog?: boolean }>();
const emit = defineEmits<{ done: [] }>();
const game = useGameStore();
const keep = ref<string[]>([]);
const releaseCard = ref('');
const discovered = ref('');
const vote = ref('');
const point = ref(3);
const route = ref('');
const petalTiles = ref<number[]>([]);
const petalBudget = ref(1);
const limit = computed(() => props.choice.handLimit ?? game.me?.handLimit ?? 6);
const releaseCards = computed(() => (props.choice.type === 'movement_release' ? props.choice.cards : game.privateData?.hand ?? []).filter(c => c.kind === 'release' && !game.privateData?.disabledCardIds?.includes(c.id)));
const enabled = computed(() => game.canAct && game.canDecide && !game.room?.tradePending);
const wager = computed(() => props.choice.content?.parameters?.wager ?? 0);
const upgrading = computed(() => ['free_upgrade', 'full_upgrade'].includes(props.choice.type));
const choosingCards = computed(() => ['neta_bid', 'neta_reward', 'neta_ignite', 'neta_research', 'hand_limit', 'recycle', 'discover', 'anime_projection', 'anime_rewind', 'anime_defense'].includes(props.choice.type));
watch(() => props.choice.id, () => { keep.value = []; releaseCard.value = ''; discovered.value = ''; vote.value = ''; route.value = ''; point.value = props.choice.type === 'opening_roll' ? game.room?.dice ?? 3 : 3; petalTiles.value = []; petalBudget.value = 1; }, { immediate: true });
/** 输入是否确认，提交已保存选择的身份及点数、地产或解控卡，失败保留输入。 */
async function decide(accept: boolean): Promise<void> {
  const action: Extract<GameAction, { type: 'contentDecision' }> = { type: 'contentDecision', decisionId: props.choice.id, accept };
  if (props.choice.type === 'opening_roll' && accept) action.point = point.value;
  if (props.choice.type === 'neta_bid' && accept && keep.value.length) action.discardCardIds = keep.value;
  if (props.choice.type === 'neta_reward' && accept) action.cardId = discovered.value;
  if (props.choice.type === 'neta_petals' && accept) action.tileIds = petalTiles.value;
  if (props.choice.type === 'neta_petal_defense' && accept) action.selection = petalBudget.value;
  if (props.choice.type === 'hand_limit') action.keepCardIds = keep.value;
  if (props.choice.type === 'recycle') action.discardCardIds = keep.value;
  if (props.choice.type === 'anime_vote') { if (accept && !vote.value) return; if (accept) action.playerId = vote.value; }
  if (['anime_projection', 'anime_rewind', 'anime_defense'].includes(props.choice.type) && accept) action.cardId = discovered.value;
  if (props.choice.type === 'discover' && accept) action.cardId = discovered.value;
  if (['neta_ignite', 'neta_research'].includes(props.choice.type) && accept) { action.cardId = discovered.value; action.point = point.value; }
  if (props.choice.type === 'neta_bankai') { if (!route.value) return; action.point = Math.abs(Number(route.value)); action.direction = Math.sign(Number(route.value)); }
  if (['free_upgrade', 'full_upgrade', 'self_destroy', 'opponent_destroy'].includes(props.choice.type)) {
    if (accept && (props.tileId == null || !props.choice.tileIds.includes(props.tileId))) return;
    if (props.tileId != null) action.tileId = props.tileId;
  }
  if (['release', 'movement_release'].includes(props.choice.type) && accept) action.cardId = releaseCard.value;
  if (await game.act(action)) emit('done');
}
</script>
<template>
  <CrossoverChoice v-if="choice.type.startsWith('cross_')" :choice="choice" @done="emit('done')" />
  <ConstructionChoice v-else-if="choice.type.startsWith('construction_')" :choice="choice" @done="emit('done')" />
  <div v-else class="content-choice" :class="{ 'card-selection': choosingCards }">
    <ContentCard v-if="choice.content && !compact && !choosingCards" :content="choice.content" />
    <p v-if="choice.prompt" class="notice">{{ choice.prompt }}</p>
    <template v-if="choice.type === 'opening_roll'">
      <label class="field">自选骰点<ThemedSelect v-model="point" label="选择开局骰点" :options="(choice.numbers ?? []).map(n => ({ value: n, label: `${n} 点` }))" /></label>
      <div class="button-row choice-actions"><button class="button secondary" :disabled="!enabled" @click="decide(false)">保留原骰点，暂不使用</button><button class="button primary" :disabled="!enabled" @click="decide(true)">使用唯一机会，选择 {{ point }} 点</button></div>
    </template>
    <template v-else-if="choice.type === 'neta_bid'">
      <p class="notice">已投入 {{ keep.length }}／2 张；可以零张参加。仅胜者弃牌，弃牌不折现。</p>
      <div class="choice-card-grid"><label v-for="card in choice.cards" :key="card.id" class="keep-card" :class="{ selected: keep.includes(card.id) }"><input v-model="keep" type="checkbox" :value="card.id" :disabled="!keep.includes(card.id) && keep.length >= 2" /><ContentCard :content="card" compact thumbnail /></label></div>
      <div class="button-row choice-actions"><button class="button secondary" :disabled="!enabled" @click="decide(false)">放弃竞标</button><button class="button primary" :disabled="!enabled" @click="decide(true)">锁定 {{ keep.length }} 张秘密投入</button></div>
    </template>
    <template v-else-if="choice.type === 'neta_reward'">
      <div class="choice-card-grid"><label v-for="card in choice.cards" :key="card.id" class="keep-card" :class="{ selected: discovered === card.id }"><input v-model="discovered" type="radio" :name="choice.id" :value="card.id" /><ContentCard :content="card" compact thumbnail /></label></div>
      <button class="button primary full" :disabled="!enabled || !discovered" @click="decide(true)">支付竞标材料并获得所选UR</button>
    </template>
    <template v-else-if="choice.type === 'neta_petals'">
      <div class="choice-card-grid"><label v-for="id in choice.tileIds" :key="id" class="keep-card" :class="{ selected: petalTiles.includes(id) }"><input v-model="petalTiles" type="checkbox" :value="id" :disabled="!petalTiles.includes(id) && petalTiles.length >= (choice.numbers?.[0] ?? 0)" /><PlayerText :text="`${id}号 ${game.room?.board[id]?.name} · ${game.room?.players.find(p => p.id === game.room?.board[id]?.ownerId)?.name} · ${game.room?.board[id]?.level}级`" /></label></div>
      <div class="button-row"><button class="button secondary" :disabled="!enabled" @click="decide(false)">取消并保留花瓣</button><button class="button primary" :disabled="!enabled || !petalTiles.length" @click="decide(true)">消耗 {{ petalTiles.length }} 片并释放</button></div>
    </template>
    <template v-else-if="choice.type === 'neta_petal_defense'">
      <label class="field">授权花瓣数量<ThemedSelect v-model="petalBudget" label="选择花瓣防御额度" :options="Array.from({ length: choice.numbers?.[0] ?? 0 }, (_, i) => ({ value: i + 1, label: `最多 ${i + 1} 片` }))" /></label>
      <div class="button-row"><button class="button secondary" :disabled="!enabled" @click="decide(false)">保留花瓣，接受破坏</button><button class="button primary" :disabled="!enabled" @click="decide(true)">确认花瓣防御</button></div>
    </template>
    <template v-else-if="choice.type === 'neta_bankai'">
      <p class="notice">固定骰点：{{ choice.numbers?.join('、') }}，选择后只移动和结算一次。</p>
      <label class="field">移动路线<ThemedSelect v-model="route" label="选择骰点与方向" :options="[{ value: '', label: '请选择路线', disabled: true }, ...[...new Set(choice.numbers ?? [])].flatMap(n => [{ value: String(n), label: `前进 ${n} 格` }, { value: String(-n), label: `后退 ${n} 格` }])]" /></label>
      <button class="button primary full" :disabled="!enabled || !route" @click="decide(true)">确认路线</button>
    </template>
    <template v-else-if="['neta_ignite', 'neta_research'].includes(choice.type)">
      <div class="choice-card-grid"><label v-for="card in choice.cards" :key="card.id" class="keep-card" :class="{ selected: discovered === card.id }"><input v-model="discovered" type="radio" :name="choice.id" :value="card.id" /><ContentCard :content="card" compact thumbnail /></label></div>
      <label v-if="choice.type === 'neta_ignite'" class="field">前进点数<ThemedSelect v-model="point" label="选择燃烧推进点数" :options="Array.from({ length: 6 }, (_, i) => ({ value: i + 1, label: `${i + 1} 格` }))" /></label>
      <div class="button-row choice-actions"><button class="button secondary" :disabled="!enabled" @click="decide(false)">放弃本次机会</button><button class="button primary" :disabled="!enabled || !discovered" @click="decide(true)">{{ choice.type === 'neta_ignite' ? '弃置所选牌并前进' : '确认秘密选择' }}</button></div>
    </template>
    <template v-else-if="['anime_projection', 'anime_rewind', 'anime_defense'].includes(choice.type)">
      <ContentCard v-if="choice.content && choice.type === 'anime_defense'" :content="choice.content" />
      <div class="choice-card-grid"><label v-for="card in choice.cards" :key="card.id" class="keep-card" :class="{ selected: discovered === card.id }"><input v-model="discovered" type="radio" :name="choice.id" :value="card.id" /><ContentCard :content="card" compact thumbnail /></label></div>
      <div class="button-row choice-actions"><button class="button secondary" :disabled="!enabled" @click="decide(false)">{{ choice.type === 'anime_projection' ? '取消并保留投影魔术' : choice.type === 'anime_rewind' ? '接受原骰点' : '不使用防御' }}</button><button class="button primary" :disabled="!enabled || !discovered" @click="decide(true)">{{ choice.type === 'anime_projection' ? '确认投影' : choice.type === 'anime_rewind' ? '使用时间溯行' : '使用所选防御' }}</button></div>
    </template>
    <template v-else-if="choice.type === 'anime_vote'">
      <label class="field">秘密投票<ThemedSelect v-model="vote" label="秘密投票" :options="[{ value: '', label: '请选择其他玩家', disabled: true }, ...(choice.playerIds ?? []).map(id => ({ value: id, label: game.room?.players.find(p => p.id === id)?.name ?? id }))]" /></label>
      <div class="button-row"><button class="button secondary" :disabled="!enabled" @click="decide(false)">弃权</button><button class="button primary" :disabled="!enabled || !vote" @click="decide(true)">确认秘密投票</button></div>
    </template>
    <template v-else-if="choice.type === 'hand_limit'">
      <p>从原有道具和本次全部补给中恰好保留 {{ limit }} 张，其他每张折现 ¥{{ choice.discardCash.toLocaleString() }}。</p>
      <p class="notice">已选择 {{ keep.length }} / {{ limit }} 张，确认后折现 ¥{{ ((choice.cards.length - limit) * choice.discardCash).toLocaleString() }}。</p>
      <div class="choice-card-grid"><label v-for="card in choice.cards" :key="card.id" class="keep-card" :class="{ selected: keep.includes(card.id) }"><input v-model="keep" type="checkbox" :value="card.id" :disabled="!keep.includes(card.id) && keep.length >= limit" /><ContentCard :content="card" compact thumbnail /></label></div>
      <div class="choice-actions"><button class="button primary full" :disabled="!enabled || keep.length !== limit" @click="decide(true)">确认保留 {{ limit }} 张并折现其余道具</button></div>
    </template>
    <template v-else-if="choice.type === 'recycle'">
      <p>选择至少一张其他旧牌，弃掉后等量重抽，弃牌不折现。</p>
      <div class="choice-card-grid"><label v-for="card in choice.cards" :key="card.id" class="keep-card" :class="{ selected: keep.includes(card.id) }"><input v-model="keep" type="checkbox" :value="card.id" /><ContentCard :content="card" compact thumbnail /></label></div>
      <div class="button-row choice-actions"><button class="button secondary" :disabled="!enabled" @click="decide(false)">取消并保留卡</button><button class="button primary" :disabled="!enabled || !keep.length" @click="decide(true)">确认更换 {{ keep.length }} 张</button></div>
    </template>
    <template v-else-if="choice.type === 'discover'">
      <p>从三个不同效果中选择一张，其他候选作废。取消后再次打开仍是本次候选。</p>
      <div class="choice-card-grid"><label v-for="card in choice.cards" :key="card.id" class="keep-card" :class="{ selected: discovered === card.id }"><input v-model="discovered" type="radio" :name="choice.id" :value="card.id" /><ContentCard :content="card" compact thumbnail /></label></div>
      <div class="button-row choice-actions"><button class="button secondary" :disabled="!enabled" @click="decide(false)">取消并保留卡</button><button class="button primary" :disabled="!enabled || !discovered" @click="decide(true)">确认获得所选道具</button></div>
    </template>
    <template v-else-if="['free_upgrade', 'full_upgrade', 'self_destroy', 'opponent_destroy'].includes(choice.type)">
      <p class="target-current"><PlayerText :text="tileId == null ? dialog ? '请选择地产，再确认' : '请点击地图高亮地产选择目标' : `已选目标：${targetLabel}`" /></p>
      <p v-if="upgrading" class="muted">{{ choice.cardId ? '取消会保留这张卡和主动次数。' : '放弃本次升级不会获得替代现金。' }}</p>
      <p v-if="choice.type === 'full_upgrade'" class="muted">所选地产将直接升至 3 级，计入本回合该地产的升级次数。</p>
      <div class="button-row"><button v-if="upgrading" class="button secondary small" :disabled="!enabled" @click="decide(false)">{{ choice.cardId ? '取消并保留卡' : '放弃升级' }}</button><button class="button primary small" :disabled="!enabled || tileId == null || !choice.tileIds.includes(tileId)" @click="decide(true)">{{ choice.type === 'full_upgrade' ? '确认免费直升满级' : choice.type === 'free_upgrade' ? '确认免费升一级' : '确认摧毁全部建筑' }}</button></div>
    </template>
    <template v-else-if="choice.type === 'gamble'">
      <p class="muted">当前现金 ¥{{ game.me?.cash.toLocaleString() }}，此处只能用已有现金下注。</p>
      <p v-if="choice.cardId" class="muted">确认下注才消耗卡牌和本回合主动次数；取消会保留卡牌。</p>
      <div class="button-row"><button class="button secondary" :disabled="!enabled" @click="decide(false)">{{ choice.cardId ? '取消并保留卡' : '放弃赌博' }}</button><button class="button primary" :disabled="!enabled || (game.me?.cash ?? 0) < wager" @click="decide(true)">下注 ¥{{ wager.toLocaleString() }}</button></div>
    </template>
    <template v-else-if="['release', 'movement_release'].includes(choice.type)">
      <p v-if="choice.type === 'release'" class="notice warning">即将{{ choice.jailed ? '被监禁' : '休息' }}，跳过后续 {{ choice.skipTurns }} 次行动。现在可使用解控卡。</p>
      <p v-if="choice.type === 'release' && choice.jailed" class="muted">接受监禁后暂停收租，服刑结束后恢复；其他小游戏正常参加。此处解除本次休息和停租，不撤销此前已完成的移动。</p>
      <label class="field">选择解控卡<ThemedSelect v-model="releaseCard" label="选择解控卡" :options="[{ value: '', label: '请选择道具', disabled: true }, ...releaseCards.map(card => ({ value: card.id, label: `${card.name ?? '解除监禁／休息'} · ${card.rarity ?? 'R'}` }))]" /></label>
      <ContentCard v-if="releaseCards.find(c => c.id === releaseCard)" :content="releaseCards.find(c => c.id === releaseCard)!" />
      <div class="button-row"><button class="button secondary" :disabled="!enabled" @click="decide(false)">{{ choice.type === 'movement_release' ? '接受本次位移' : '接受本次限制' }}</button><button class="button primary" :disabled="!enabled || !releaseCard" @click="decide(true)">{{ choice.type === 'movement_release' ? '使用解控卡抵消' : '使用解控卡' }}</button></div>
    </template>
  </div>
</template>

<style scoped>
.card-selection .choice-actions { position: sticky; bottom: calc(-1 * var(--dialog-padding, 18px)); z-index: 2; margin-bottom: calc(-1 * var(--dialog-padding, 18px)); padding: 12px 0 18px; background: #fff9eb; border-top: 1px solid #e5d5b7; }
.card-selection .choice-actions > .button { flex: 1; min-width: 0; }
</style>
