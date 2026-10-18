<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import type { ActivityInput } from '../../activity-art';
import { useGameStore } from '../../stores/game';
import { playSound } from '../../audio';
import ActivityArtwork from './ActivityArtwork.vue';
import CharacterPawn from '../CharacterPawn.vue';
const props = defineProps<{ activity: CornerActivity; enabled: boolean }>();
const emit = defineEmits<{ input: [value: ActivityInput] }>();
const game = useGameStore();
/** 输入本次参与人数及本局金额快照，返回基础总奖池；旧快照缺失底金时按零计算。 */
const total = computed(() => (game.room?.activityRotation?.money.packetBase ?? 0) + props.activity.participants.length * (game.room?.activityRotation?.money.packetPerPlayer ?? 0));
const best = computed(() => Math.max(0, ...Object.values(props.activity.packets)));
const now = ref(Date.now());
const revealedAt = ref<Record<string, number>>({});
const timers: ReturnType<typeof setTimeout>[] = [];
const clock = setInterval(() => { now.value = Date.now(); }, 100);
const packets = computed(() => {
  const owners = Object.fromEntries(Object.entries(props.activity.packetSelections ?? {}).map(([id, slot]) => [slot, id]));
  return props.activity.participants.map((_, index) => ({ slot: index + 1, id: owners[index + 1] ?? '' }));
});
const playingResult = computed(() => props.activity.stage === 'result' && Object.values(revealedAt.value).some(time => now.value < time + 3000));
/** 输入领取者编号，返回本次开包动画已播放的毫秒数；已有结果不重复播放。 */
function elapsed(id: string): number {
  const startedAt = revealedAt.value[id];
  return startedAt === undefined ? Infinity : Math.max(0, now.value - startedAt);
}
/** 输入红包序号，提交一次领取；是否可领及是否已被抢走由服务端最终校验。 */
function claim(slot: number): void {
  if (props.enabled) emit('input', { selection: slot });
}
/** 无入参，清除本次开包与延迟音效，恢复静态到账结果。 */
function clearAnimation(): void { revealedAt.value = {}; timers.forEach(clearTimeout); timers.length = 0; }
// 只播放本次新增到账；恢复基线、刷新、后台及减少动态效果时直接显示结果。
watch(() => ({ ids: Object.keys(props.activity.payouts), epoch: game.presentationEpoch }), (current, previous) => {
  if (current.epoch !== previous.epoch) { clearAnimation(); return; }
  const fresh = current.ids.filter(id => !previous.ids.includes(id));
  if (!fresh.length || game.connection !== 'online' || game.room?.recoveryPending || document.hidden || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  now.value = Date.now();
  for (const id of fresh) revealedAt.value[id] = now.value;
  playSound('packet_open');
  timers.push(setTimeout(() => { if (game.connection === 'online') playSound('coin'); }, 650));
  if (fresh.some(id => (props.activity.bonuses[id] ?? 0) > 0)) {
    timers.push(setTimeout(() => { if (game.connection === 'online') playSound('packet_lucky'); }, 1200));
  }
});
watch(() => game.connection, state => { if (state !== 'online') clearAnimation(); });
const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');
/** 无入参，后台或减少动态效果时直接恢复已到账的静态款式与金额。 */
function suspendAnimation(): void { if (document.hidden || reduced.matches) clearAnimation(); }
document.addEventListener('visibilitychange', suspendAnimation);
reduced.addEventListener('change', suspendAnimation);
onBeforeUnmount(() => { clearInterval(clock); clearAnimation(); document.removeEventListener('visibilitychange', suspendAnimation); reduced.removeEventListener('change', suspendAnimation); });
</script>
<template>
  <div class="packet-scene classic-stage packet-stage">
    <ActivityArtwork name="packet-scene" backdrop />
    <div class="classic-stage-sign"><small>NUANNUAN · LUCKY ENVELOPES</small><strong>暖暖的好运红包铺</strong><span>一人一份，把好运拆出来</span></div>
    <p class="packet-pool">暖暖发出 <strong>¥{{ total.toLocaleString() }}</strong> 拼手气红包</p><p class="activity-help">任选一个未拆的红包，点击立即开包到账。人人有份，不扣现金，每人独立10%幸运翻倍。</p>
    <div class="packet-grid">
      <button v-for="packet in packets" :key="packet.slot" class="activity-option player-envelope packet-choice" :class="{ claimed: !!packet.id, opening: elapsed(packet.id) < 650, revealing: elapsed(packet.id) >= 650 && elapsed(packet.id) < 1200, lucky: (activity.bonuses[packet.id] ?? 0) > 0 && elapsed(packet.id) >= 1200, 'lucky-playing': (activity.bonuses[packet.id] ?? 0) > 0 && elapsed(packet.id) >= 1200 && elapsed(packet.id) < 3000, own: packet.id === game.me?.id }" :disabled="!enabled || !!packet.id" @click="claim(packet.slot)">
        <ActivityArtwork class="packet-illustration" :name="activity.payouts[packet.id] !== undefined && elapsed(packet.id) >= 650 ? activity.bonuses[packet.id] && elapsed(packet.id) >= 1200 ? 'packet-lucky' : 'packet-open' : 'packet-closed'" />
        <span v-if="elapsed(packet.id) >= 650 && elapsed(packet.id) < 3000" class="toy-prize-burst" aria-hidden="true"><i v-for="n in 7" :key="n" :style="{ '--particle': n }">{{ n % 2 ? '✦' : '●' }}</i></span>
        <CharacterPawn v-if="packet.id" :avatar-id="game.room?.players.find(p => p.id === packet.id)?.avatarId" :color="game.room?.players.find(p => p.id === packet.id)?.color" />
        <strong>{{ packet.id ? game.room?.players.find(p => p.id === packet.id)?.name : `${packet.slot} 号红包` }}{{ packet.id && packet.id === game.me?.id ? ' · 你' : '' }}</strong>
        <div v-if="activity.payouts[packet.id] !== undefined && elapsed(packet.id) >= 650" class="packet-reward" aria-live="polite">
          <b :key="elapsed(packet.id) < 1200 ? 'base' : 'final'">+¥{{ (elapsed(packet.id) < 1200 ? activity.packets[packet.id] : activity.payouts[packet.id])?.toLocaleString() }}</b><small>基础 ¥{{ activity.packets[packet.id]?.toLocaleString() }} · 已到账</small>
          <span v-if="activity.bonuses[packet.id] && elapsed(packet.id) >= 1200" class="lucky-badge">幸运大奖 · 翻倍 +¥{{ activity.bonuses[packet.id]?.toLocaleString() }}</span>
          <span v-if="activity.settled && activity.packets[packet.id] === best" class="best-badge">手气最佳</span>
          <small v-if="activity.cardRewards?.[packet.id]">＋{{ activity.cardRewards[packet.id] }} 张随机道具 · 已发放</small>
        </div>
        <small v-else>{{ packet.id ? (elapsed(packet.id) < 650 ? '红包开启中…' : '已选择') : enabled ? '点我拆开' : '等待领取' }}</small>
        <span v-if="(activity.bonuses[packet.id] ?? 0) > 0 && elapsed(packet.id) >= 1200 && elapsed(packet.id) < 3000" class="packet-lucky-celebration" aria-hidden="true">✦ 幸运翻倍 ✦</span>
      </button>
    </div>
    <div v-if="activity.stage === 'result'" class="activity-result packet-final" :class="{ 'packet-final-playing': playingResult }" role="status"><strong>{{ activity.summary }}</strong></div>
    <p v-else class="activity-stage-caption">{{ game.me && activity.submitted.includes(game.me.id) ? '你的红包已到账，等待其他玩家选择' : '挑一个喜欢的红包，拆开你的好运！' }} · {{ activity.submitted.length }} / {{ activity.participants.length }} 已领取</p>
  </div>
</template>
