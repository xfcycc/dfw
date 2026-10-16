<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import type { ActivityInput } from '../../activity-art';
import { useGameStore } from '../../stores/game';
import { playSound } from '../../audio';
const props = defineProps<{ activity: CornerActivity; enabled: boolean }>();
const emit = defineEmits<{ input: [value: ActivityInput] }>();
const game = useGameStore();
const money = computed(() => game.room?.activityRotation?.money ?? {});
const hiddenPrize = computed(() => (money.value.blindboxHiddenBase ?? 0) + props.activity.participants.length * (money.value.blindboxHiddenPerPlayer ?? 0));
const owners = computed(() => Object.fromEntries(Object.entries(props.activity.packetSelections).map(([id, slot]) => [slot, id])));
const openedAt = ref<Record<string, number>>({});
const now = ref(Date.now());
const clock = setInterval(() => { now.value = Date.now(); }, 100);
const timers: ReturnType<typeof setTimeout>[] = [];
/** 输入领取者编号，返回本次开盒动画已播放毫秒数；重连不重复播放已领取结果。 */
function elapsed(id: string | undefined): number { return id && openedAt.value[id] !== undefined ? Math.max(0, now.value - openedAt.value[id]!) : Infinity; }
/** 输入盲盒序号，返回已领取盒子的普通、隐藏或限定款名称，未领取不揭晓。 */
function rarity(slot: number): string { const id = owners.value[slot]; return id ? ['普通款', '隐藏款', '限定款'][props.activity.revealed[id] ?? 0] ?? '普通款' : '神秘盲盒'; }
watch(() => Object.keys(props.activity.payouts), (ids, previous) => {
  const fresh = ids.filter(id => !previous.includes(id));
  if (!fresh.length || game.connection !== 'online' || game.room?.recoveryPending) return;
  now.value = Date.now();
  for (const id of fresh) openedAt.value[id] = now.value;
  playSound('packet_open');
  timers.push(setTimeout(() => { if (game.connection === 'online') playSound('coin'); }, 650));
  if (fresh.some(id => props.activity.bonuses[id])) timers.push(setTimeout(() => { if (game.connection === 'online') playSound('packet_lucky'); }, 1200));
});
onBeforeUnmount(() => { clearInterval(clock); timers.forEach(clearTimeout); });
</script>
<template>
  <div class="blindbox-scene">
    <p>20 个盲盒 · 19 个普通款 <strong>¥{{ money.blindboxRegular?.toLocaleString() }}</strong> · 唯一隐藏款 <strong>¥{{ hiddenPrize.toLocaleString() }}</strong></p>
    <p class="activity-help">免费每人选一个，选中立即到账。只有隐藏款有10%概率变限定款，奖励翻倍；未领取的盒子不发奖。</p>
    <div class="blindbox-grid"><button v-for="slot in 20" :key="slot" class="activity-option blindbox-choice" :class="{ claimed: !!owners[slot], opening: elapsed(owners[slot]) < 650, hidden: owners[slot] && (activity.revealed[owners[slot]!] ?? 0) > 0 && elapsed(owners[slot]) >= 650, limited: owners[slot] && activity.revealed[owners[slot]!] === 2 && elapsed(owners[slot]) >= 1200, own: owners[slot] === game.me?.id }" :disabled="!enabled || !!owners[slot]" @click="emit('input', { selection: slot })"><span class="blindbox-icon" aria-hidden="true">{{ owners[slot] && elapsed(owners[slot]) >= 650 ? activity.revealed[owners[slot]!] === 0 ? '🌽' : '🏆' : '🎁' }}</span><strong>{{ slot }} 号 · {{ owners[slot] && elapsed(owners[slot]) >= 650 ? elapsed(owners[slot]) < 1200 && activity.revealed[owners[slot]!] === 2 ? '隐藏款' : rarity(slot) : '待开启' }}</strong><template v-if="owners[slot]"><small>{{ game.room?.players.find(player => player.id === owners[slot])?.name }}{{ owners[slot] === game.me?.id ? ' · 你' : '' }}</small><b v-if="elapsed(owners[slot]) >= 650">+¥{{ (elapsed(owners[slot]) < 1200 ? activity.packets[owners[slot]!] : activity.payouts[owners[slot]!])?.toLocaleString() }}</b><small v-else>开盒中…</small><span v-if="activity.bonuses[owners[slot]!] && elapsed(owners[slot]) >= 1200" class="lucky-badge">限定款 · 隐藏奖励翻倍</span></template><small v-else>{{ activity.settled ? '本场未领取' : '挑一个带回家' }}</small></button></div>
    <p class="activity-help">{{ activity.submitted.length }} / {{ activity.participants.length }} 人已领取 · {{ game.me && activity.submitted.includes(game.me.id) ? '你的奖励已到账' : '每人限领一个' }}</p>
  </div>
</template>
