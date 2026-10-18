<script setup lang="ts">
import { ref, useId } from 'vue';
import sceneSources from '../ur-card-art.json';

const id = useId();
const artwork = sceneSources.card_naruto_shinra_tensei;
const loaded = ref(0);
const failed = ref(false);
const leftArm = 'M100 230H270L300 290 380 330 440 360 430 440 200 460 100 360Z';
const rightArm = 'M580 390 630 370 710 370 780 330 860 340 870 490 720 570 570 490Z';
const rocks = [
  { x: 20, y: 35, dx: '-65px', dy: '-45px', delay: '-.2s', size: 5 },
  { x: 77, y: 39, dx: '60px', dy: '-35px', delay: '-1.2s', size: 4 },
  { x: 32, y: 66, dx: '-45px', dy: '70px', delay: '-2.3s', size: 4 },
  { x: 66, y: 68, dx: '55px', dy: '75px', delay: '-3.2s', size: 6 },
  { x: 43, y: 18, dx: '-20px', dy: '-60px', delay: '-4.1s', size: 3 },
  { x: 83, y: 72, dx: '65px', dy: '35px', delay: '-4.8s', size: 3 },
];
/** 输入一次素材加载事件，累计两张分层图均可用后显示场景；无返回值。 */
function imageLoaded(): void { loaded.value++; }
/** 无入参，素材失败时保留底下的原始静态插画；无返回值。 */
function imageFailed(): void { failed.value = true; }
</script>

<template>
  <div class="shinra-scene" :class="{ ready: loaded === 2 && !failed }" aria-hidden="true">
    <img class="shinra-village" :src="artwork.background" alt="" @load="imageLoaded" @error="imageFailed" />
    <div class="shinra-pressure"><i></i><i></i></div>
    <div class="shinra-dust"></div>
    <svg class="shinra-pain" viewBox="0 0 1000 1000">
      <defs>
        <clipPath :id="`${id}-left`"><path :d="leftArm" /></clipPath>
        <clipPath :id="`${id}-right`"><path :d="rightArm" /></clipPath>
        <mask :id="`${id}-body`"><rect width="1000" height="1000" fill="white" /><path :d="leftArm" fill="black" /><path :d="rightArm" fill="black" /></mask>
      </defs>
      <image :href="artwork.foreground" width="1000" height="1000" :mask="`url(#${id}-body)`" @load="imageLoaded" @error="imageFailed" />
      <g class="shinra-arm-left"><image :href="artwork.foreground" width="1000" height="1000" :clip-path="`url(#${id}-left)`" /></g>
      <g class="shinra-arm-right"><image :href="artwork.foreground" width="1000" height="1000" :clip-path="`url(#${id}-right)`" /></g>
    </svg>
    <svg v-for="(rock, index) in rocks" :key="index" class="shinra-rubble" viewBox="0 0 40 50" :style="{ left: `${rock.x}%`, top: `${rock.y}%`, width: `${rock.size}%`, '--dx': rock.dx, '--dy': rock.dy, animationDelay: rock.delay }"><path d="M8 4 29 0 40 17 30 46 8 50 0 29Z" fill="#69503d" /><path d="M8 4 29 0 25 20 8 50 0 29Z" fill="#b49a74" /><path d="M25 20 40 17 30 46 8 50Z" fill="#44372d" /></svg>
  </div>
</template>

<style scoped>
.shinra-scene { position: absolute; inset: 0; overflow: hidden; opacity: 0; pointer-events: none; }
.shinra-scene.ready { opacity: 1; }
.shinra-scene:not(.ready) * { animation-play-state: paused; }
.shinra-village { display: block; width: 100%; height: 100%; object-fit: cover; transform-origin: 50% 60%; animation: shinra-village 6s ease-in-out infinite; }
.shinra-pain { position: absolute; left: 14%; top: 1%; width: 72%; height: 72%; overflow: visible; transform-origin: 50% 30%; animation: shinra-hover 6s ease-in-out infinite; }
.shinra-arm-left { transform-origin: 40% 39%; animation: shinra-left-arm 6s ease-in-out infinite; }
.shinra-arm-right { transform-origin: 61% 44%; animation: shinra-right-arm 6s ease-in-out infinite; }
.shinra-pressure { position: absolute; inset: 0; }
.shinra-pressure i { position: absolute; left: 15%; top: 36%; width: 70%; height: 38%; border: 3px solid #fff8e7; border-radius: 50%; box-shadow: 0 0 10px #ffefce90, inset 0 0 8px #fff2d980; opacity: 0; animation: shinra-pressure 6s ease-out infinite; }
.shinra-pressure i + i { animation-delay: .4s; }
.shinra-dust { position: absolute; inset: 0; background: radial-gradient(ellipse at 50% 62%, transparent 25%, #fff2ce65 60%, transparent 78%); mix-blend-mode: screen; opacity: 0; animation: shinra-dust 6s ease-out infinite; }
.shinra-rubble { position: absolute; height: auto; opacity: 0; animation: shinra-rubble 6s ease-in infinite; }
@keyframes shinra-hover { 0%, 100% { transform: translateY(0) rotate(-1deg); } 28% { transform: translateY(-2.5%) rotate(1deg) scale(1.015); } 42% { transform: translateY(-1%) rotate(0) scale(1.04); } 72% { transform: translateY(1.5%) rotate(-.5deg); } }
@keyframes shinra-left-arm { 0%, 100% { transform: rotate(1deg); } 28% { transform: rotate(5deg); } 42%, 60% { transform: rotate(-3deg); } }
@keyframes shinra-right-arm { 0%, 100% { transform: rotate(-1deg); } 28% { transform: rotate(-4deg); } 42%, 60% { transform: rotate(3deg); } }
@keyframes shinra-village { 0%, 100% { transform: scale(1.025); } 28% { transform: scale(1.005) translateY(.5%); } 36% { transform: scale(1.055) translate(-.4%, .3%); } 40% { transform: scale(1.05) translate(.4%, -.2%); } 46%, 68% { transform: scale(1.065) translateY(-.5%); } }
@keyframes shinra-pressure { 0%, 28% { opacity: 0; transform: scale(.15); } 34% { opacity: .85; } 66%, 100% { opacity: 0; transform: scale(2.5); } }
@keyframes shinra-dust { 0%, 28%, 80%, 100% { opacity: 0; transform: scale(.7); } 42% { opacity: .7; } 65% { opacity: 0; transform: scale(1.25); } }
@keyframes shinra-rubble { 0%, 25% { opacity: 0; transform: translate(0, 0) rotate(0) scale(.45); } 38%, 58% { opacity: .9; } 80%, 100% { opacity: 0; transform: translate(var(--dx), var(--dy)) rotate(140deg) scale(1.3); } }
@media (prefers-reduced-motion: reduce) { .shinra-scene { display: none; } }
</style>
