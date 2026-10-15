<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import type { PublicRoomState } from '../../shared/types';
import { playSound } from '../audio';
import AssetStatIcon from './AssetStatIcon.vue';

const props = defineProps<{ room: PublicRoomState; playerId: string; live: boolean }>();
const balance = computed(() => props.room.players.find(player => player.id === props.playerId)?.cash ?? 0);
const displayed = ref(balance.value);
const target = ref<HTMLElement>();
const flight = ref<{ key: number; amount: number; x: number; y: number; dx: number; dy: number } | null>(null);
const counting = ref(false);
const direction = ref(0);
const announcement = ref('');
const changes: { amount: number; to: number; tileId: number }[] = [];
const preference = window.matchMedia('(prefers-reduced-motion: reduce)');
let frame = 0;
let sequence = 0;
const FLIGHT_MS = 850;
const COUNT_MS = 650;

/** 无入参，取消飞行、计数和待播队列，直接同步最新现金，无返回值。 */
function reset(): void {
  cancelAnimationFrame(frame); frame = 0;
  changes.length = 0; flight.value = null; counting.value = false;
  displayed.value = balance.value; announcement.value = '';
}

/** 无入参，顺序播放下一笔收支，飞抵现金栏后计数并播放对应音效，无返回值。 */
function present(): void {
  const change = changes.shift();
  const destination = target.value?.getBoundingClientRect();
  if (!change || !destination) { reset(); return; }
  const board = target.value?.closest('.board-container');
  const source = board?.querySelector<HTMLElement>(`[data-cash-tile="${change.tileId}"]`)?.getBoundingClientRect();
  const endX = destination.left + destination.width / 2;
  const endY = destination.top + destination.height / 2;
  const visible = source && source.bottom > 0 && source.top < window.innerHeight && source.right > 0 && source.left < window.innerWidth;
  const x = visible ? Math.max(65, Math.min(window.innerWidth - 65, source.left + source.width / 2)) : Math.max(65, Math.min(window.innerWidth - 65, endX));
  const y = visible ? Math.max(40, Math.min(window.innerHeight - 40, source.top + source.height / 2)) : Math.min(window.innerHeight - 40, endY + 100);
  direction.value = change.amount > 0 ? 1 : -1;
  counting.value = false;
  flight.value = { key: ++sequence, amount: change.amount, x, y, dx: endX - x, dy: endY - y };
  const from = displayed.value;
  let started: number | undefined;
  /** 输入动画帧时间，飞行结束后平滑增减金额，结束时精确落到服务端余额，无返回值。 */
  function tick(now: number): void {
    started ??= now;
    const elapsed = now - started;
    if (elapsed >= FLIGHT_MS) {
      if (!counting.value) {
        flight.value = null; counting.value = true;
        playSound(change!.amount > 0 ? 'coin' : 'pay');
        announcement.value = `${change!.amount > 0 ? '收入' : '支出'} ¥${Math.abs(change!.amount).toLocaleString()}，现金 ¥${change!.to.toLocaleString()}`;
      }
      const progress = Math.min(1, (elapsed - FLIGHT_MS) / COUNT_MS);
      displayed.value = Math.round(from + (change!.to - from) * (1 - (1 - progress) ** 3));
      if (progress === 1) {
        displayed.value = change!.to; counting.value = false; frame = 0;
        if (changes.length) present();
        return;
      }
    }
    frame = requestAnimationFrame(tick);
  }
  frame = requestAnimationFrame(tick);
}

/** 输入公开快照和连接状态，首次同步只建立余额基线，其余真实收支按顺序展示。 */
watch([() => props.room, () => props.live, () => props.playerId], ([next, live, id], [previous, wasLive, previousId]) => {
  const player = next.players.find(value => value.id === id);
  const before = previous?.players.find(value => value.id === id);
  if (!live || !wasLive || id !== previousId || !before || next.roomId !== previous.roomId || next.gameId !== previous.gameId || next.recoveryPending || previous.recoveryPending || next.status === 'lobby' || document.hidden) { reset(); return; }
  if (next.version <= previous.version || !player || player.cash === before.cash) return;
  const amount = player.cash - before.cash;
  if (preference.matches) { reset(); playSound(amount > 0 ? 'coin' : 'pay'); return; }
  const rent = previous.pending?.type === 'rent' && previous.pending.creditorId === id;
  changes.push({ amount, to: player.cash, tileId: rent ? previous.pending!.tileId ?? player.position : player.position });
  if (!frame) present();
}, { flush: 'post' });

/** 无入参，切后台或启用减少动态效果时停止未完成收支，不在返回时补播，无返回值。 */
function stopHidden(): void { if (document.hidden || preference.matches) reset(); }
document.addEventListener('visibilitychange', stopHidden);
preference.addEventListener('change', stopHidden);
onBeforeUnmount(() => { reset(); document.removeEventListener('visibilitychange', stopHidden); preference.removeEventListener('change', stopHidden); });
</script>

<template>
  <span ref="target" class="cash-balance" :class="{ 'cash-counting': counting, 'cash-expense': direction < 0 }" :aria-label="`现金 ¥${balance.toLocaleString()}`">
    <AssetStatIcon kind="cash" /><span class="cash-number" aria-hidden="true">¥{{ displayed.toLocaleString() }}</span>
    <span class="sr-only" role="status">{{ announcement }}</span>
  </span>
  <Teleport to="body">
    <div v-if="flight" :key="flight.key" class="cash-flight" :class="{ 'cash-expense': flight.amount < 0 }" :style="{ left: `${flight.x}px`, top: `${flight.y}px`, '--cash-dx': `${flight.dx}px`, '--cash-dy': `${flight.dy}px`, animationDuration: `${FLIGHT_MS}ms` }" aria-hidden="true">
      <AssetStatIcon kind="cash" /><strong>{{ flight.amount > 0 ? '+' : '−' }}¥{{ Math.abs(flight.amount).toLocaleString() }}</strong>
      <span>{{ flight.amount > 0 ? '收入' : '支出' }}</span>
    </div>
  </Teleport>
</template>

<style scoped>
.cash-balance { position: relative; display: inline-flex; align-items: center; gap: 6px; font-variant-numeric: tabular-nums; }
.cash-number { display: inline-block; transform-origin: center; }
.cash-counting { color: #4f7443; }
.cash-counting.cash-expense { color: #af7657; }
.cash-counting .cash-number { animation: cash-count .22s ease-in-out 3; }
.cash-counting::after { content: ''; position: absolute; inset: -9px; border: 2px solid #a9c485; border-radius: 24px; pointer-events: none; animation: cash-arrival .65s ease-out both; }
.cash-counting.cash-expense::after { border-color: #d8b297; }
.cash-flight { position: fixed; z-index: 180; display: flex; align-items: center; gap: 6px; width: max-content; padding: 9px 13px; border: 1px solid #cbdcb0; border-radius: 18px; background: #fffbea; box-shadow: 0 5px 20px #40583426; color: #4f7443; font-size: 18px; pointer-events: none; animation: cash-travel .85s cubic-bezier(.35, 0, .4, 1) both; }
.cash-flight > span { font-size: 11px; }
.cash-flight.cash-expense { color: #af7657; border-color: #dfc6ae; }
@keyframes cash-travel {
  0% { opacity: 0; transform: translate(-50%, -50%) scale(.65); }
  20%, 38% { opacity: 1; transform: translate(-50%, calc(-50% - 18px)) scale(1.08); }
  85% { opacity: 1; }
  100% { opacity: 0; transform: translate(calc(-50% + var(--cash-dx)), calc(-50% + var(--cash-dy))) scale(.4); }
}
@keyframes cash-count { 50% { transform: translateY(-3px) scale(1.06); } }
@keyframes cash-arrival { from { opacity: .9; transform: scale(.9); } to { opacity: 0; transform: scale(1.18); } }
@media (prefers-reduced-motion: reduce) { .cash-flight, .cash-counting::after { display: none; } .cash-counting .cash-number { animation: none; } }
</style>
