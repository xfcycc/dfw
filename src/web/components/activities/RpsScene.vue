<script setup lang="ts">
import { computed, ref } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import type { ActivityInput } from '../../activity-art';
import { useGameStore } from '../../stores/game';
import CharacterPawn from '../CharacterPawn.vue';
import ActivityArtwork from './ActivityArtwork.vue';
const props = defineProps<{ activity: CornerActivity; enabled: boolean }>();
const emit = defineEmits<{ input: [value: ActivityInput] }>();
const game = useGameStore(); const selected = ref(0); const names = ['石头', '剪刀', '布'];
const hands = ['rps-rock', 'rps-scissors', 'rps-paper'] as const;
/** 输入本人私有快照和本地选择，返回当前展示手势；其他玩家的选择只在公开揭晓后显示。 */
const chosen = computed(() => game.privateData?.activityId === props.activity.id && game.privateData.activityRound === props.activity.round ? game.privateData.activitySelection ?? selected.value : selected.value);
</script>
<template>
  <div class="rps-scene classic-stage rps-stage" :class="{ revealing: activity.stage === 'revealing' }">
    <ActivityArtwork name="rps-scene" backdrop />
    <div class="classic-stage-sign"><small>NUANNUAN · HANDS UP</small><strong>暖暖猜拳擂台</strong><span>秘密出拳，一起揭晓</span></div>
    <p>最后一名胜者独揽 <strong>¥{{ game.room?.activityRotation?.money.rpsPrize.toLocaleString() }}</strong> · 当前还剩 {{ activity.contenders.length }} 人</p><p class="activity-help">两种手势淘汰败方；一种或三种手势均为平局，继续出拳。</p>
    <div class="rps-players"><div v-for="id in activity.participants" :key="id" class="rps-player" :class="{ eliminated: !activity.contenders.includes(id), submitted: activity.submitted.includes(id), winner: activity.payouts[id] > 0 }"><span v-if="activity.payouts[id] > 0" class="rps-crown" aria-label="擂台冠军">♛</span><CharacterPawn :avatar-id="game.room?.players.find(p => p.id === id)?.avatarId" :color="game.room?.players.find(p => p.id === id)?.color" /><strong>{{ game.room?.players.find(p => p.id === id)?.name }}</strong><div class="rps-hand"><ActivityArtwork v-if="['round_result', 'result'].includes(activity.stage) && activity.revealed[id] !== undefined" :name="hands[activity.revealed[id]!]!" /><ActivityArtwork v-else-if="activity.stage === 'revealing' && activity.contenders.includes(id)" name="rps-rock" /><span v-else aria-hidden="true">{{ !activity.contenders.includes(id) ? '出局' : activity.submitted.includes(id) ? '✓' : '?' }}</span></div><small>{{ !activity.contenders.includes(id) ? '围观助威' : activity.submitted.includes(id) ? '已锁定手势' : '准备出拳' }}</small><span v-if="['round_result', 'result'].includes(activity.stage) && activity.revealed[id] !== undefined" class="rps-revealed-label">{{ names[activity.revealed[id]!] }}</span></div></div>
    <template v-if="activity.stage === 'select'"><div class="activity-options rps-options"><button v-for="(name, n) in names" :key="name" class="activity-option" :class="{ selected: chosen === n }" :aria-pressed="chosen === n" :disabled="!enabled" @click="selected = n"><ActivityArtwork :name="hands[n]!" /><strong>{{ name }}</strong><small>{{ chosen === n ? '✓ 你的选择' : '点选手势' }}</small></button></div><button class="button primary" :disabled="!enabled" @click="emit('input', { selection: chosen })">锁定{{ names[chosen] }}</button></template>
    <p v-if="activity.stage === 'revealing'" class="activity-stage-caption rps-count"><span>石头</span><span>剪刀</span><span>布！</span></p><p v-if="activity.stage === 'round_result'" class="activity-stage-caption">{{ activity.summary }}</p>
  </div>
</template>
