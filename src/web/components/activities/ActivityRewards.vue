<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import { useGameStore } from '../../stores/game';
const props = defineProps<{ activity: CornerActivity }>();
const game = useGameStore();
const panel = ref<HTMLElement>();
const now = ref(Date.now());
const rewardDelay = `${-Math.max(0, now.value - props.activity.startedAt)}ms`;
const clock = setInterval(() => { now.value = Date.now(); }, 100);
const elapsed = computed(() => Math.max(0, now.value - props.activity.startedAt));
const playing = computed(() => elapsed.value < 3000);
const closingIn = computed(() => Math.max(0, Math.ceil(((props.activity.endsAt ?? now.value) - now.value) / 1000)));
// 二十盒场景可能超过窗口高度，结算出现时将奖励移入当前弹框视野。
onMounted(() => { if (panel.value?.getClientRects().length) panel.value.scrollIntoView({ block: 'nearest' }); });
onBeforeUnmount(() => { clearInterval(clock); });
</script>
<template>
  <div ref="panel" class="activity-result new-activity-rewards" :class="{ playing }" :style="{ '--reward-delay': rewardDelay }" role="status" aria-live="polite"><span v-if="playing && Object.keys(activity.payouts).length" class="reward-sparkles" aria-hidden="true">✦ ✧ ✦</span><strong>{{ activity.summary }}</strong><div v-for="(amount, id) in activity.payouts" :key="id" class="activity-payout"><span>{{ game.room?.players.find(player => player.id === id)?.name }}</span><b>+¥{{ amount.toLocaleString() }}</b></div><small>{{ playing ? '正在播放最终结算…' : `${closingIn} 秒后自动关闭` }}</small></div>
</template>
