<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { ContentChoice, GameAction } from '../../shared/types';
import { useGameStore } from '../stores/game';
import ContentCard from './ContentCard.vue';
const props = defineProps<{ choice: ContentChoice }>();
const game = useGameStore();
const target = ref('');
const keep = ref<string[]>([]);
const releaseCard = ref('');
const releaseCards = computed(() => game.privateData?.hand.filter(c => c.kind === 'release') ?? []);
const enabled = computed(() => game.canAct && game.canDecide && !game.room?.tradePending);
const wager = computed(() => props.choice.content?.parameters?.wager ?? 0);
watch(() => props.choice.id, () => { target.value = ''; keep.value = []; releaseCard.value = ''; }, { immediate: true });
/** 输入是否确认，提交本次已保存选择的身份及用户选项，失败保留输入。 */
async function decide(accept: boolean): Promise<void> {
  const action: Extract<GameAction, { type: 'contentDecision' }> = { type: 'contentDecision', decisionId: props.choice.id, accept };
  if (props.choice.type === 'hand_limit') action.keepCardIds = keep.value;
  if (props.choice.type === 'free_upgrade' || props.choice.type === 'self_destroy') { if (target.value) action.tileId = Number(target.value); }
  if (props.choice.type === 'release' && accept) action.cardId = releaseCard.value;
  await game.act(action);
}
</script>
<template>
  <div class="content-choice">
    <ContentCard v-if="choice.content" :content="choice.content" />
    <template v-if="choice.type === 'hand_limit'">
      <p>从原有道具和本次全部补给中恰好保留六张，其他每张折现 ¥{{ choice.discardCash.toLocaleString() }}。</p>
      <p class="notice">已选择 {{ keep.length }} / 6 张，确认后折现 ¥{{ ((choice.cards.length - 6) * choice.discardCash).toLocaleString() }}。</p>
      <div class="choice-card-grid"><label v-for="card in choice.cards" :key="card.id" class="keep-card" :class="{ selected: keep.includes(card.id) }"><input v-model="keep" type="checkbox" :value="card.id" :disabled="!keep.includes(card.id) && keep.length >= 6" /><ContentCard :content="card" compact /></label></div>
      <button class="button primary full" :disabled="!enabled || keep.length !== 6" @click="decide(true)">确认保留六张并折现其余道具</button>
    </template>
    <template v-else-if="choice.type === 'free_upgrade' || choice.type === 'self_destroy'">
      <label class="field">{{ choice.type === 'free_upgrade' ? '选择免费升级的地产' : '选择摧毁全部建筑的自家地产' }}<select v-model="target"><option disabled value="">请选择地产</option><option v-for="id in choice.tileIds" :key="id" :value="String(id)">{{ game.room?.board[id]?.name }} · {{ game.room?.board[id]?.level }} 级</option></select></label>
      <p v-if="choice.type === 'free_upgrade'" class="muted">{{ choice.cardId ? '取消会保留这张卡和主动次数。' : '放弃本次升级不会获得替代现金。' }}</p>
      <div class="button-row"><button v-if="choice.type === 'free_upgrade'" class="button secondary" :disabled="!enabled" @click="decide(false)">{{ choice.cardId ? '取消并保留卡' : '放弃升级' }}</button><button class="button primary" :disabled="!enabled || !target" @click="decide(true)">{{ choice.type === 'free_upgrade' ? '确认免费升一级' : '确认摧毁全部建筑' }}</button></div>
    </template>
    <template v-else-if="choice.type === 'gamble'">
      <p class="muted">当前现金 ¥{{ game.me?.cash.toLocaleString() }}，此处只能用已有现金下注。</p>
      <div class="button-row"><button class="button secondary" :disabled="!enabled" @click="decide(false)">放弃赌博</button><button class="button primary" :disabled="!enabled || (game.me?.cash ?? 0) < wager" @click="decide(true)">下注 ¥{{ wager.toLocaleString() }}</button></div>
    </template>
    <template v-else-if="choice.type === 'release'">
      <p class="notice warning">即将{{ choice.jailed ? '被监禁' : '休息' }}，跳过后续 {{ choice.skipTurns }} 次行动。现在可使用解控卡。</p>
      <label class="field">选择解控卡<select v-model="releaseCard"><option disabled value="">请选择道具</option><option v-for="card in releaseCards" :key="card.id" :value="card.id">{{ card.name ?? '解除监禁／休息' }} · {{ card.rarity ?? 'R' }}</option></select></label>
      <ContentCard v-if="releaseCards.find(c => c.id === releaseCard)" :content="releaseCards.find(c => c.id === releaseCard)!" />
      <div class="button-row"><button class="button secondary" :disabled="!enabled" @click="decide(false)">接受本次限制</button><button class="button primary" :disabled="!enabled || !releaseCard" @click="decide(true)">使用解控卡</button></div>
    </template>
  </div>
</template>
