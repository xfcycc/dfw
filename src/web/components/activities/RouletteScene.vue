<script setup lang="ts">
import { computed, ref } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import { activityArt, type ActivityInput } from '../../activity-art';
import { useGameStore } from '../../stores/game';
import CharacterPawn from '../CharacterPawn.vue';
const props = defineProps<{ activity: CornerActivity; enabled: boolean }>();
const emit = defineEmits<{ input: [value: ActivityInput] }>();
const game = useGameStore(); const target = ref('');
const opponents = computed(() => game.room?.players.filter(p => !p.bankrupt && p.id !== props.activity.triggerId) ?? []);
const duelists = computed(() => game.room?.players.filter(p => p.id === props.activity.triggerId || p.id === props.activity.opponentId) ?? []);
</script>
<template>
  <div class="roulette-scene">
    <div class="roulette-prop" :class="{ spinning: activity.stage === 'loading', firing: activity.stage === 'shot', hit: activity.stage === 'payment' || activity.stage === 'result' && activity.shots > 0 }"><img :src="activityArt('cylinder')" :style="{ transform: activity.stage === 'loading' ? undefined : `rotate(${activity.shots * 60}deg)` }" alt="六弹巢轮盘" /><span v-if="activity.stage === 'payment'" class="roulette-stars" aria-hidden="true">✦ ✧ ✦</span></div>
    <p>六个弹巢、一颗子弹 · 随机先手 · 期间不重新转动</p>
    <p class="activity-help">败者向胜者支付 ¥{{ game.room?.activityRotation?.money.rouletteDebt.toLocaleString() }}，按现有玩家费用处理，可使用适用防御及筹款。</p>
    <template v-if="activity.stage === 'select'"><label class="field">指定对手<select v-model="target" :disabled="!enabled"><option disabled value="">选择另一名未破产玩家</option><option v-for="p in opponents" :key="p.id" :value="p.id">{{ p.name }}</option></select></label><p class="activity-help">发起后对手必须应战。</p><div class="button-row"><button class="button secondary" :disabled="!enabled" @click="emit('input', { selection: 0 })">放弃发起</button><button class="button primary" :disabled="!enabled || !target" @click="emit('input', { selection: 1, playerId: target })">发起对决</button></div></template>
    <template v-else><div class="roulette-duelists"><div v-for="p in duelists" :key="p.id" :class="{ active: activity.currentPlayerId === p.id }"><CharacterPawn :avatar-id="p.avatarId" :color="p.color" /><strong>{{ p.name }}</strong><small>{{ activity.currentPlayerId === p.id ? '当前开枪者' : '等待对手' }}</small></div></div><div class="roulette-chambers" aria-label="开枪进度"><i v-for="n in 6" :key="n" :class="{ spent: n <= activity.shots }">{{ n }}</i></div><p v-if="activity.stage === 'duel'" class="activity-stage-caption">{{ game.room?.players.find(p => p.id === activity.currentPlayerId)?.name }}，轮到你了</p><button v-if="activity.stage === 'duel'" class="button primary" :disabled="!enabled" @click="emit('input', { selection: 1 })">开第 {{ activity.shots + 1 }} 枪</button><p v-if="activity.stage === 'loading'" class="activity-stage-caption">装填旋转一次，本场不再重新转动弹巢…</p><p v-if="activity.stage === 'shot'" class="activity-stage-caption">咔哒……</p></template>
  </div>
</template>
