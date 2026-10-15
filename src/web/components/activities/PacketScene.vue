<script setup lang="ts">
import { computed } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import { activityArt, type ActivityInput } from '../../activity-art';
import { useGameStore } from '../../stores/game';
const props = defineProps<{ activity: CornerActivity; enabled: boolean }>();
const emit = defineEmits<{ input: [value: ActivityInput] }>();
const game = useGameStore();
const total = computed(() => props.activity.participants.length * (game.room?.activityRotation?.money.packetPerPlayer ?? 0));
const best = computed(() => Math.max(0, ...Object.values(props.activity.packets)));
</script>
<template>
  <div class="packet-scene" :class="{ opening: activity.stage === 'opening', revealed: activity.settled }">
    <p class="packet-pool">暖暖发出 <strong>¥{{ total.toLocaleString() }}</strong> 拼手气红包</p><p class="activity-help">人人有份，不扣现金。基础份额1%～90%，每人独立10%幸运翻倍。</p>
    <div class="packet-grid"><div v-for="id in activity.participants" :key="id" class="player-envelope" :class="{ claimed: activity.submitted.includes(id), lucky: (activity.bonuses[id] ?? 0) > 0 }"><img :src="activityArt(activity.settled ? 'packet-open' : 'packet-closed')" alt="红包" /><strong>{{ game.room?.players.find(p => p.id === id)?.name }}</strong><template v-if="activity.settled"><b>¥{{ activity.payouts[id]?.toLocaleString() }}</b><small>基础 ¥{{ activity.packets[id]?.toLocaleString() }}</small><span v-if="activity.bonuses[id]" class="lucky-badge">幸运翻倍 +¥{{ activity.bonuses[id]?.toLocaleString() }}</span><span v-if="activity.packets[id] === best" class="best-badge">手气最佳</span></template><small v-else>{{ activity.submitted.includes(id) ? '已领取，等待揭晓' : '等待领取' }}</small></div></div>
    <button v-if="activity.stage === 'select'" class="button primary packet-claim" :disabled="!enabled" @click="emit('input', { selection: 1 })">领取我的红包</button><p v-if="activity.stage === 'opening'" class="activity-stage-caption">红包拆开，好运到来！</p>
  </div>
</template>
