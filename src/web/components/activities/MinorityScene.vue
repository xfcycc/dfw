<script setup lang="ts">
import { computed, ref } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import type { ActivityInput } from '../../activity-art';
import { useGameStore } from '../../stores/game';
const props = defineProps<{ activity: CornerActivity; enabled: boolean }>();
const emit = defineEmits<{ input: [value: ActivityInput] }>();
const game = useGameStore();
const selected = ref(0);
const chosen = computed(() => game.privateData?.activityId === props.activity.id && game.privateData.activityRound === props.activity.round ? game.privateData.activitySelection ?? selected.value : selected.value);
const stalls = [{ name: '火锅摊', icon: '🍲', line: '说好一起涮，谁先偷偷溜？' }, { name: '烧烤摊', icon: '🍢', line: '人少吃得香，别都跟过来！' }];
const revealed = computed(() => props.activity.stage === 'result');
</script>
<template>
  <div class="minority-scene">
    <p>免费参加 · 少数派中抽一人独得 <strong>¥{{ game.room?.activityRotation?.money.rpsPrize?.toLocaleString() }}</strong></p>
    <p class="activity-help">秘密选摊，人数少的一边争大奖；人数相同或全在同一摊时，从全员中抽一人。不重开、不加奖。</p>
    <div class="night-market"><button v-for="(stall, side) in stalls" :key="stall.name" class="activity-option night-stall" :class="{ selected: !revealed && chosen === side }" :disabled="!enabled" @click="selected = side"><span aria-hidden="true">{{ stall.icon }}</span><strong>{{ stall.name }}</strong><small>{{ stall.line }}</small><template v-if="revealed"><b>{{ activity.participants.filter(id => activity.revealed[id] === side).length }} 人</b><span v-for="id in activity.participants.filter(id => activity.revealed[id] === side)" :key="id" class="night-guest" :class="{ winner: activity.payouts[id] !== undefined }">{{ activity.payouts[id] !== undefined ? '✦ ' : '' }}{{ game.room?.players.find(player => player.id === id)?.name }}</span></template></button></div>
    <button v-if="activity.stage === 'select'" class="button primary" :disabled="!enabled" @click="emit('input', { selection: selected })">锁定{{ stalls[chosen]?.name }}</button>
    <div v-if="!revealed" class="horse-tickets"><span v-for="id in activity.participants" :key="id">{{ game.room?.players.find(player => player.id === id)?.name }} · {{ activity.submitted.includes(id) ? '已锁定摊位' : '还在犹豫' }}</span></div>
    <p v-if="activity.stage === 'minority_reveal'" class="activity-stage-caption">大家都选好了，看看谁偷偷换了摊位…</p>
  </div>
</template>
