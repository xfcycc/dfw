<script setup lang="ts">
import ThemedSelect from './ThemedSelect.vue';
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
const limit = computed(() => props.choice.handLimit ?? game.me?.handLimit ?? 6);
const releaseCards = computed(() => game.privateData?.hand.filter(c => c.kind === 'release') ?? []);
const enabled = computed(() => game.canAct && game.canDecide && !game.room?.tradePending);
const wager = computed(() => props.choice.content?.parameters?.wager ?? 0);
const upgrading = computed(() => ['free_upgrade', 'full_upgrade'].includes(props.choice.type));
const choosingCards = computed(() => ['hand_limit', 'recycle', 'discover'].includes(props.choice.type));
watch(() => props.choice.id, () => { keep.value = []; releaseCard.value = ''; discovered.value = ''; }, { immediate: true });
/** 输入是否确认，提交已保存选择的身份及地产或解控卡，失败保留输入。 */
async function decide(accept: boolean): Promise<void> {
  const action: Extract<GameAction, { type: 'contentDecision' }> = { type: 'contentDecision', decisionId: props.choice.id, accept };
  if (props.choice.type === 'hand_limit') action.keepCardIds = keep.value;
  if (props.choice.type === 'recycle') action.discardCardIds = keep.value;
  if (props.choice.type === 'discover' && accept) action.cardId = discovered.value;
  if (['free_upgrade', 'full_upgrade', 'self_destroy', 'opponent_destroy'].includes(props.choice.type)) {
    if (accept && (props.tileId == null || !props.choice.tileIds.includes(props.tileId))) return;
    if (props.tileId != null) action.tileId = props.tileId;
  }
  if (props.choice.type === 'release' && accept) action.cardId = releaseCard.value;
  if (await game.act(action)) emit('done');
}
</script>
<template>
  <div class="content-choice" :class="{ 'card-selection': choosingCards }">
    <ContentCard v-if="choice.content && !compact && !choosingCards" :content="choice.content" />
    <template v-if="choice.type === 'hand_limit'">
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
    <template v-else-if="choice.type === 'release'">
      <p class="notice warning">即将{{ choice.jailed ? '被监禁' : '休息' }}，跳过后续 {{ choice.skipTurns }} 次行动。现在可使用解控卡。</p>
      <p v-if="choice.jailed" class="muted">接受监禁后暂停收租，服刑结束后恢复；其他小游戏正常参加。解控只解除休息和停租，不撤销已完成的入狱传送。</p>
      <label class="field">选择解控卡<ThemedSelect v-model="releaseCard" label="选择解控卡" :options="[{ value: '', label: '请选择道具', disabled: true }, ...releaseCards.map(card => ({ value: card.id, label: `${card.name ?? '解除监禁／休息'} · ${card.rarity ?? 'R'}` }))]" /></label>
      <ContentCard v-if="releaseCards.find(c => c.id === releaseCard)" :content="releaseCards.find(c => c.id === releaseCard)!" />
      <div class="button-row"><button class="button secondary" :disabled="!enabled" @click="decide(false)">接受本次限制</button><button class="button primary" :disabled="!enabled || !releaseCard" @click="decide(true)">使用解控卡</button></div>
    </template>
  </div>
</template>

<style scoped>
.card-selection .choice-actions { position: sticky; bottom: calc(-1 * var(--dialog-padding, 18px)); z-index: 2; margin-bottom: calc(-1 * var(--dialog-padding, 18px)); padding: 12px 0 18px; background: #fff9eb; border-top: 1px solid #e5d5b7; }
.card-selection .choice-actions > .button { flex: 1; min-width: 0; }
</style>
