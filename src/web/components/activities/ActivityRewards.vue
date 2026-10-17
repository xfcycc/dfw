<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import { useGameStore } from '../../stores/game';
import CharacterPawn from '../CharacterPawn.vue';
const props = defineProps<{ activity: CornerActivity; playing?: boolean }>();
const game = useGameStore();
const now = ref(Date.now());
const clock = setInterval(() => { now.value = Date.now(); }, 250);
const closingIn = computed(() => props.activity.endsAt == null ? null : Math.max(0, Math.ceil((props.activity.endsAt - now.value) / 1000)));
const winners = computed(() => Object.entries(props.activity.payouts).filter(([, amount]) => amount > 0).map(([id, amount]) => ({ id, amount, player: game.room?.players.find(player => player.id === id) })));
const motifs = { casino: ['⚄', '好运落定'], horse: ['♛', '冲线时刻'], roulette: ['✧', '对决落幕'], packet: ['✦', '好运揭晓'], rps: ['♛', '擂台加冕'], treasure: ['◆', '宝藏出库'], bridge: ['⚑', '通关纪念'], minority: ['✿', '夜市幸运签'], blindbox: ['✧', '今日珍藏'] };
const motif = computed(() => motifs[props.activity.kind]);
const heading = computed(() => winners.value.length ? motif.value[1] : props.activity.kind === 'roulette' ? '对决结束' : Object.keys(props.activity.wagers).length || props.activity.kind === 'bridge' ? '这一程，也值得纪念' : '本场活动结束');
onBeforeUnmount(() => { clearInterval(clock); });
</script>
<template>
  <div class="activity-result toy-rewards" :class="{ playing, 'has-prize': winners.length }" role="status" aria-live="polite">
    <div class="toy-reward-seal" aria-hidden="true">{{ motif[0] }}</div><small class="toy-reward-eyebrow">NUANNUAN · LITTLE LUCK</small><strong>{{ heading }}</strong><p v-if="activity.summary" class="toy-reward-summary">{{ activity.summary }}</p>
    <div v-if="winners.length" class="toy-winners"><div v-for="(winner, index) in winners" :key="winner.id" class="toy-winner" :class="{ own: winner.id === game.me?.id }" :style="{ '--winner-delay': `${index * 90}ms` }"><CharacterPawn :avatar-id="winner.player?.avatarId" :color="winner.player?.color" /><span><strong>{{ winner.player?.name }}{{ winner.id === game.me?.id ? ' · 你' : '' }}</strong><small>本场奖励已到账</small></span><b>+¥{{ winner.amount.toLocaleString() }}</b></div></div>
    <span v-else class="toy-reward-note">{{ activity.kind === 'roulette' ? '对决费用沿用本场结算记录' : '暖暖为你留一颗下次的幸运星' }}</span>
    <small v-if="closingIn !== null" class="toy-reward-countdown">{{ closingIn }} 秒后自动关闭</small>
  </div>
</template>
