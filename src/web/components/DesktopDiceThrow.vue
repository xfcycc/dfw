<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import type { PublicPlayer } from '../../shared/types';
import DiceFace from './DiceFace.vue';
import HolidayDiceEffects from './HolidayDiceEffects.vue';
import { activeHoliday } from '../holiday-theme';
const props = defineProps<{ board?: HTMLElement; player: PublicPlayer; mine: boolean; value: number; rolling: boolean; duration: number }>();
const placement = ref<{ x: number; y: number; dx: number; dy: number }>();
const faces = [1, 6, 3, 4, 2, 5];
const rotations: Record<number, [number, number]> = { 1: [0, 0], 2: [-90, 0], 3: [0, -90], 4: [0, 90], 5: [90, 0], 6: [0, 180] };
/** 输入点数和实测起落点，返回飞行偏移、时长及最终朝向，不占用棋盘布局空间。 */
const throwStyle = computed(() => {
  const point = placement.value;
  const [x, y] = rotations[props.value] ?? rotations[1]!;
  return point ? { left: `${point.x}px`, top: `${point.y}px`, '--throw-x': `${point.dx}px`, '--throw-y': `${point.dy}px`, '--throw-duration': `${props.duration}ms`, '--face-x': `${x}deg`, '--face-y': `${y}deg` } : undefined;
});
/** 无入参，测量本人掷骰按钮或行动玩家地图头像与棋盘中央的视口坐标，无返回值。 */
function measureThrow(): void {
  const board = props.board?.querySelector<HTMLElement>('.board');
  if (!board) return;
  const bounds = board.getBoundingClientRect();
  const source = props.mine
    ? props.board?.closest('.room-page')?.querySelector<HTMLElement>('[data-dice-roll]')
    : Array.from(props.board?.querySelectorAll<HTMLElement>('.map-character[data-player-id]') ?? []).find(element => element.dataset.playerId === props.player.id);
  const origin = source?.getBoundingClientRect();
  const x = bounds.left + bounds.width / 2; const y = bounds.top + bounds.height / 2;
  placement.value = { x, y, dx: origin ? origin.left + origin.width / 2 - x : 0, dy: origin ? origin.top + origin.height / 2 - y : 0 };
}
onMounted(() => {
  measureThrow();
  window.addEventListener('resize', measureThrow);
  window.addEventListener('scroll', measureThrow, true);
});
onBeforeUnmount(() => {
  window.removeEventListener('resize', measureThrow);
  window.removeEventListener('scroll', measureThrow, true);
});
</script>
<template>
  <div v-if="placement" class="desktop-dice-throw" :class="{ 'is-rolling': rolling, 'holiday-dice-throw': !!activeHoliday }" :style="throwStyle" role="status" aria-live="polite">
    <span class="sr-only">{{ player.name }}{{ rolling ? '正在掷骰' : `掷出 ${value} 点` }}</span>
    <div class="throw-flight" aria-hidden="true">
      <div class="throw-shadow"></div>
      <HolidayDiceEffects :rolling="rolling" />
      <div class="throw-cube">
        <DiceFace v-for="(face, index) in faces" :key="face" class="throw-face" :class="`throw-face-${index}`" :value="index === 0 && !rotations[value] ? value : face" />
      </div>
    </div>
    <div v-if="!rolling" class="throw-result" aria-hidden="true"><strong>{{ value }} 点</strong><span>{{ player.name }}</span></div>
  </div>
</template>
<style scoped>
.desktop-dice-throw { position: fixed; width: 0; height: 0; z-index: 150; pointer-events: none; perspective: 800px; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
.throw-flight { position: absolute; width: 64px; height: 64px; left: -32px; top: -32px; transform-style: preserve-3d; }
.is-rolling .throw-flight { animation: dice-throw-flight var(--throw-duration) linear both; }
.throw-shadow { position: absolute; left: -6px; right: -6px; bottom: -17px; height: 16px; background: #49351e40; border-radius: 50%; filter: blur(5px); }
.is-rolling .throw-shadow { animation: dice-throw-shadow var(--throw-duration) linear both; }
.throw-cube { width: 100%; height: 100%; position: relative; transform-style: preserve-3d; transform: rotateX(var(--face-x)) rotateY(var(--face-y)); }
.is-rolling .throw-cube { animation: dice-throw-tumble var(--throw-duration) linear both; }
.throw-cube :deep(.die) { position: absolute; inset: 0; width: 64px; height: 64px; margin: 0; padding: 10px; gap: 5px; border: 2px solid #d7bb8b; border-radius: 12px; background: linear-gradient(135deg, #fffef7, #f6e6c9); box-shadow: inset 0 0 0 2px #fffaf0; backface-visibility: hidden; }
.throw-cube :deep(.die span.filled) { background: #775133; box-shadow: inset 0 1px 2px #37271455; }
.throw-cube :deep(.die strong) { color: #775133; }
.throw-face-0 { transform: translateZ(32px); }
.throw-face-1 { transform: rotateY(180deg) translateZ(32px); }
.throw-face-2 { transform: rotateY(90deg) translateZ(32px); }
.throw-face-3 { transform: rotateY(-90deg) translateZ(32px); }
.throw-face-4 { transform: rotateX(90deg) translateZ(32px); }
.throw-face-5 { transform: rotateX(-90deg) translateZ(32px); }
.throw-result { position: absolute; top: 58px; left: 0; transform: translateX(-50%); display: flex; flex-direction: column; align-items: center; gap: 4px; width: max-content; max-width: 240px; padding: 8px 18px; border: 2px solid #d7bb8b; border-radius: 16px; background: #fff9ecf5; box-shadow: 0 5px 15px #49351e24; animation: dice-result-in .16s ease-out both; }
.throw-result strong { color: #80501e; font-size: 24px; line-height: 1.2; }
.throw-result span { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #80664b; font-size: 13px; }
.holiday-dice-throw .throw-shadow{background:var(--holiday-dice-glow);filter:blur(9px)}
.holiday-dice-throw .throw-result{background:var(--holiday-cream);border-color:var(--holiday-dice-edge);box-shadow:0 4px 20px color-mix(in srgb,var(--holiday-dice-glow) 30%,transparent)}
.holiday-dice-throw .throw-result strong{color:var(--holiday-ink)}
@keyframes dice-throw-flight {
  0% { transform: translate(var(--throw-x), var(--throw-y)) scale(.55); }
  35% { transform: translate(calc(var(--throw-x) * .3), calc(var(--throw-y) * .3 - 90px)) scale(1.12); }
  53% { transform: translate(calc(var(--throw-x) * .14), calc(var(--throw-y) * .14)) scale(1, .9); }
  65% { transform: translate(calc(var(--throw-x) * .08), calc(var(--throw-y) * .08 - 30px)) scale(1); }
  76% { transform: translate(calc(var(--throw-x) * .03), calc(var(--throw-y) * .03)) scale(1.04, .95); }
  85% { transform: translate(-10px, -10px) scale(1); }
  93% { transform: translate(3px, 0); }
  100% { transform: none; }
}
@keyframes dice-throw-tumble {
  0% { transform: rotateX(-25deg) rotateY(30deg) rotateZ(-20deg); }
  35% { transform: rotateX(300deg) rotateY(420deg) rotateZ(160deg); }
  65% { transform: rotateX(540deg) rotateY(780deg) rotateZ(280deg); }
  85% { transform: rotateX(calc(var(--face-x) + 690deg)) rotateY(calc(var(--face-y) + 1050deg)) rotateZ(350deg); }
  100% { transform: rotateX(calc(var(--face-x) + 720deg)) rotateY(calc(var(--face-y) + 1080deg)) rotateZ(360deg); }
}
@keyframes dice-throw-shadow { 0%, 35% { opacity: .2; transform: translateY(20px) scale(.5); } 53%, 76%, 100% { opacity: 1; transform: none; } 65%, 85% { opacity: .5; transform: translateY(10px) scale(.75); } }
@keyframes dice-result-in { from { opacity: 0; margin-top: 6px; } to { opacity: 1; margin-top: 0; } }
@media (prefers-reduced-motion: reduce) { .desktop-dice-throw { display: none; } }
</style>
