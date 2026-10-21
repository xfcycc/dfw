<script setup lang="ts">
import { ref, useId } from 'vue';
import type { UrCardScene } from '../ur-card-scenes';
import sceneSources from '../ur-card-art.json';
import RedLotusCardScene from './RedLotusCardScene.vue';

const props = defineProps<{ scene: UrCardScene }>();
const id = useId();
const sources: Record<string, { background: string; foreground: string }> = sceneSources;
const { background, foreground } = sources[props.scene.skinId];
const loaded = ref(0);
const failed = ref(false);
/** 输入一次主素材加载事件，两张素材均就绪后覆盖原图；无返回值。 */
function imageLoaded(): void { loaded.value++; }
/** 无入参，素材失败时隐藏分层场景，保留底下的原始卡面；无返回值。 */
function imageFailed(): void { failed.value = true; }
</script>

<template>
  <RedLotusCardScene v-if="scene.theme === 'lotus'" />
  <div v-else class="ur-scene" :class="[scene.theme, { ready: loaded === 2 && !failed }]" aria-hidden="true">
    <img class="scene-background" :src="background" alt="" @load="imageLoaded" @error="imageFailed" />
    <img v-if="['wood', 'kamui', 'reaper'].includes(scene.theme)" class="scene-depth" :src="background" alt="" />
    <div class="scene-light"></div>
    <div class="actor-placement" :style="{ transform: scene.placement }">
      <svg class="scene-actor" viewBox="0 0 1000 1000">
        <defs v-if="scene.secondaryPath">
          <mask :id="`${id}-body`"><rect width="1000" height="1000" fill="white" /><path :d="scene.secondaryPath" fill="black" /></mask>
        </defs>
        <image :href="foreground" width="1000" height="1000" :mask="scene.secondaryPath ? `url(#${id}-body)` : undefined" @load="imageLoaded" @error="imageFailed" />
      </svg>
    </div>
    <div v-if="scene.secondaryPath" class="actor-placement secondary-placement" :style="{ transform: scene.secondaryPlacement ?? scene.placement }">
      <svg class="scene-secondary" viewBox="0 0 1000 1000">
        <defs><clipPath :id="`${id}-secondary`"><path :d="scene.secondaryPath" /></clipPath></defs>
        <image :href="foreground" width="1000" height="1000" :clip-path="`url(#${id}-secondary)`" />
      </svg>
    </div>
    <svg v-if="scene.theme === 'alchemy'" class="scene-energy" viewBox="0 0 1000 1000">
      <g class="alchemy-circle"><ellipse cx="520" cy="740" rx="340" ry="95" /><ellipse cx="520" cy="740" rx="280" ry="72" /><path d="M190 740 685 658 685 822ZM850 740 355 658 355 822Z" /></g>
      <path class="energy-bolt" d="M100 200 160 305 120 335 240 480 200 515 375 690M905 230 840 350 890 390 800 520 835 570 630 710" />
    </svg>
    <svg v-else-if="scene.theme === 'reaper'" class="scene-energy" viewBox="0 0 1000 1000"><g class="seal-ribbons"><path d="M-80 335C160 350 200 670 480 590S760 620 1080 810M-80 780C190 520 380 830 640 730S850 450 1080 530" /><path d="M-80 335C160 350 200 670 480 590S760 620 1080 810M-80 780C190 520 380 830 640 730S850 450 1080 530" /></g></svg>
    <svg v-else-if="scene.theme === 'coin'" class="scene-energy" viewBox="0 0 1000 1000"><path class="coin-thunder" d="M790 30 740 150 800 170 705 310 760 340 690 470" /></svg>
    <svg v-else-if="scene.theme === 'punch'" class="scene-energy" viewBox="0 0 1000 1000"><g class="punch-wave"><circle cx="780" cy="270" r="125" /><circle cx="780" cy="270" r="210" /><path d="M780 270 1000 50M780 270 1000 270M780 270 980 520M780 270 550 20" /></g></svg>
    <svg v-else-if="scene.theme === 'wish'" class="scene-energy" viewBox="0 0 1000 1000"><ellipse class="wish-aura" cx="500" cy="795" rx="320" ry="90" /></svg>
    <svg v-else-if="scene.theme === 'teapot'" class="scene-energy teapot-energy" viewBox="0 0 1000 1000">
      <ellipse class="teapot-aura" cx="640" cy="735" rx="280" ry="80" />
      <path class="teapot-route" d="M545 710C925 660 1010 490 835 435S500 390 635 240" />
      <path class="teapot-route teapot-return" d="M635 240C430 325 650 410 835 435S1005 680 545 710" />
      <g class="teapot-build"><path d="M692 235 775 192 858 235 846 246 704 246ZM713 252V317H838V252M740 252V317M810 252V317M702 322H850" /></g>
    </svg>
    <span v-for="spark in 6" :key="spark" class="scene-mote" :style="{ '--spark': spark }"></span>
  </div>
</template>

<style scoped>
.ur-scene { --scene-accent: #ffe5a3; position: absolute; inset: 0; overflow: hidden; pointer-events: none; opacity: 0; }
.ur-scene.ready { opacity: 1; }
.ur-scene:not(.ready) * { animation-play-state: paused; }
.scene-background, .scene-depth { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.scene-background { animation: scene-pan 8s ease-in-out infinite; }
.actor-placement { position: absolute; inset: 0; transform-origin: 0 0; }
.scene-actor, .scene-secondary { display: block; width: 100%; height: 100%; overflow: visible; }
.scene-actor { transform-origin: 50% 78%; animation: actor-breathe 6s ease-in-out infinite; }
.scene-light { position: absolute; inset: 0; mix-blend-mode: screen; opacity: .3; animation: scene-light 6s ease-in-out infinite; }
.scene-energy { position: absolute; inset: 0; width: 100%; height: 100%; fill: none; stroke: var(--scene-accent); stroke-width: 3; pointer-events: none; }
.scene-mote { position: absolute; left: calc(8% + var(--spark) * 13%); top: 84%; width: 4px; height: 4px; border-radius: 50%; background: var(--scene-accent); box-shadow: 0 0 7px var(--scene-accent); opacity: 0; animation: scene-mote 5s linear infinite; animation-delay: calc(var(--spark) * -.8s); }
.coin { --scene-accent: #ffe29b; }
.coin .scene-light { background: linear-gradient(105deg, #ffd34880, transparent 48%, #8e49ff85); animation: coin-light 6s ease-in-out infinite; }
.coin .scene-actor { transform-origin: 48% 85%; animation: coin-actor 6s ease-in-out infinite; }
.coin .scene-secondary { transform-origin: 52% 28%; animation: coin-flip 6s ease-in-out infinite; }
.coin-thunder { stroke: #e7c8ff; stroke-width: 5; opacity: 0; animation: coin-thunder 6s ease-in-out infinite; }
.ticket .scene-actor { transform-origin: 33% 84%; animation: ticket-nervous 3.5s ease-in-out infinite; }
.ticket .scene-secondary { transform-origin: 72% 55%; animation: ticket-speech 3s ease-in-out infinite; }
.ticket .scene-light { background: radial-gradient(ellipse at 75% 20%, #fff0a0a0, transparent 62%); }
.wood { --scene-accent: #ffe8ac; }
.wood .scene-depth { mask-image: radial-gradient(ellipse 40% 32% at 65% 15%, #000 45%, transparent 100%); transform-origin: 50% 65%; animation: wood-grow 7s ease-in-out infinite; }
.wood .scene-actor { transform-origin: 55% 69%; animation: wood-seal 7s ease-in-out infinite; }
.wood .scene-light { background: radial-gradient(ellipse at 55% 55%, #ffe78e90, transparent 60%); }
.wood .scene-mote { width: 5px; height: 9px; border-radius: 1px; background: #b98443; box-shadow: none; animation-name: wood-chip; }
.alchemy { --scene-accent: #b8f5ff; }
.alchemy .scene-actor { transform-origin: 52% 83%; animation: alchemy-press 6s ease-in-out infinite; }
.alchemy .scene-secondary { transform-origin: 85% 60%; animation: sword-float 6s ease-in-out infinite; }
.alchemy .scene-light { background: radial-gradient(ellipse at 52% 72%, #61c8ff85, transparent 65%); }
.alchemy .scene-mote { width: 7px; height: 10px; border-radius: 0; background: linear-gradient(120deg, #a8b6bd 40%, #56616c 41% 70%, #303848 71%); clip-path: polygon(25% 0, 82% 10%, 100% 58%, 55% 100%, 0 72%); box-shadow: none; animation-name: wood-chip; }
.alchemy-circle { transform-origin: 52% 74%; animation: alchemy-circle 6s ease-in-out infinite; }
.energy-bolt { stroke-width: 4; stroke-dasharray: 100 1000; animation: alchemy-bolt 3s linear infinite; }
.kamui { --scene-accent: #c397ff; }
.kamui .scene-depth { mask-image: radial-gradient(ellipse 40% 36% at 82% 34%, #000 45%, transparent 100%); transform-origin: 82% 40%; animation: kamui-vortex 9s ease-in-out infinite; }
.kamui .scene-actor { transform-origin: 34% 84%; animation: kamui-reach 7s ease-in-out infinite; }
.kamui .scene-light { background: radial-gradient(ellipse at 82% 40%, #a63dff60, transparent 57%); }
.kamui .scene-mote { top: calc(8% + var(--spark) * 12%); left: 90%; animation: kamui-mote 5s linear infinite; animation-delay: calc(var(--spark) * -.8s); }
.reaper { --scene-accent: #b7ecff; }
.reaper .scene-depth { mask-image: linear-gradient(#000 0 25%, transparent 43%); animation: reaper-spirit 7s ease-in-out infinite; }
.reaper .scene-actor { transform-origin: 40% 85%; animation: reaper-cast 7s ease-in-out infinite; }
.reaper .scene-light { background: radial-gradient(ellipse at 48% 25%, #93b8ff85, transparent 75%); }
.seal-ribbons { opacity: .7; filter: drop-shadow(0 0 3px #5ebeff); }
.seal-ribbons path { stroke-width: 8; opacity: .3; }
.seal-ribbons path + path { stroke-width: 3; opacity: 1; stroke-dasharray: 16 28; animation: seal-flow 7s linear infinite; }
@keyframes scene-pan { 0%, 100% { transform: scale(1.025) translate(-.3%, .2%); } 50% { transform: scale(1.045) translate(.3%, -.3%); } }
@keyframes actor-breathe { 0%, 100% { transform: none; } 50% { transform: translateY(-1%) rotate(.7deg); } }
@keyframes scene-light { 0%, 100% { opacity: .15; } 50% { opacity: .55; } }
@keyframes scene-mote { 0% { opacity: 0; transform: translate(0, 0) scale(.4); } 25%, 65% { opacity: .8; } 100% { opacity: 0; transform: translate(12px, -160px) scale(1.1); } }
@keyframes coin-light { 0%, 100% { opacity: .2; transform: translateX(-4%); } 50% { opacity: .6; transform: translateX(4%); } }
@keyframes coin-actor { 0%, 100% { transform: rotate(-.6deg); } 50% { transform: translateY(-.8%) rotate(1deg); } }
@keyframes coin-flip { 0%, 100% { transform: translateY(0) rotateY(0) rotate(-4deg); } 50% { transform: translateY(-2%) rotateY(180deg) rotate(7deg); } }
@keyframes coin-thunder { 0%, 40%, 70%, 100% { opacity: 0; } 48%, 60% { opacity: .75; } 54% { opacity: .15; } }
@keyframes ticket-nervous { 0%, 100% { transform: rotate(-1deg); } 45% { transform: translate(-.6%, .2%) rotate(1deg); } 60% { transform: translate(.3%, 0) rotate(-.4deg); } }
@keyframes ticket-speech { 0%, 100% { transform: rotate(-1.5deg); } 50% { transform: translateY(-.6%) rotate(1.5deg); } }
@keyframes wood-grow { 0%, 100% { transform: translateY(1.5%) scaleY(.985); } 55%, 70% { transform: translateY(-2.5%) scaleY(1.025); } }
@keyframes wood-seal { 0%, 100% { transform: none; } 45% { transform: translateY(.5%) scaleY(.97); } 65% { transform: translateY(-.3%) scaleY(1.01); } }
@keyframes wood-chip { 0% { opacity: 0; transform: translateY(0) rotate(0) scale(.4); } 25%, 65% { opacity: .9; } 100% { opacity: 0; transform: translate(10px, -180px) rotate(150deg) scale(1); } }
@keyframes alchemy-press { 0%, 100% { transform: rotate(-.6deg); } 45%, 60% { transform: translateY(1%) rotate(1deg) scaleY(.985); } }
@keyframes sword-float { 0%, 100% { transform: translateY(0) rotate(-2deg); } 50% { transform: translateY(-2.5%) rotate(2deg); } }
@keyframes alchemy-circle { 0%, 100% { opacity: .15; transform: scale(.9); } 45%, 60% { opacity: .8; transform: scale(1.06); } }
@keyframes alchemy-bolt { from { stroke-dashoffset: 1100; } to { stroke-dashoffset: 0; } }
@keyframes kamui-vortex { 0%, 100% { transform: rotate(-3deg) scale(1.03); } 50% { transform: rotate(11deg) scale(1.08); } }
@keyframes kamui-reach { 0%, 100% { transform: rotate(-1deg); } 50% { transform: translate(1%, -.5%) rotate(1.5deg) scale(1.015); } }
@keyframes kamui-mote { 0% { opacity: 0; transform: translate(20px, 30px) scale(1.2); } 25%, 60% { opacity: .8; } 100% { opacity: 0; transform: translate(-22px, -40px) rotate(-160deg) scale(.2); } }
@keyframes reaper-spirit { 0%, 100% { transform: scale(1.025) translateY(.6%); } 50% { transform: scale(1.04) translateY(-1.2%); } }
@keyframes reaper-cast { 0%, 100% { transform: rotate(-1deg); } 50% { transform: translate(.8%, -.6%) rotate(1deg); } }
@keyframes seal-flow { from { stroke-dashoffset: 0; } to { stroke-dashoffset: -308; } }
.wukong { --scene-accent: #ffd076; }
.wukong .scene-actor { transform-origin: 52% 80%; animation: wukong-swing 5s ease-in-out infinite; }
.wukong .scene-background { animation: palace-shake 5s ease-in-out infinite; }
.wukong .scene-light { background: radial-gradient(ellipse at 55% 50%, #ffe09970, transparent 65%); }
.wukong .scene-mote { width: 7px; height: 10px; border-radius: 1px; background: #66a79a; box-shadow: none; animation: falling-tile 5s ease-in infinite; }
.visits { --scene-accent: #ffe4a1; }
.visits .scene-actor { transform-origin: 50% 82%; animation: visits-bow 7s ease-in-out infinite; }
.visits .scene-light { background: linear-gradient(120deg, #ffe8ae80, transparent 55%); }
.visits .scene-mote { width: 5px; height: 8px; background: #bfd66c; box-shadow: none; animation-duration: 9s; }
.pigs { --scene-accent: #ffce87; }
.pigs .scene-actor { transform-origin: 50% 85%; animation: pigs-build 4s ease-in-out infinite; }
.pigs .scene-background { transform-origin: 40% 85%; animation: house-rise 8s ease-in-out infinite; }
.pigs .scene-light { background: radial-gradient(ellipse at 45% 45%, #fff2b760, transparent 65%); }
.pigs .scene-mote { width: 6px; height: 8px; border-radius: 1px; background: #bd754e; box-shadow: none; animation-name: wood-chip; }
.wish { --scene-accent: #ffdb68; }
.wish .scene-background { transform-origin: 50% 36%; animation: dragon-hover 9s ease-in-out infinite; }
.wish .scene-actor { transform-origin: 52% 84%; animation: wish-reach 6s ease-in-out infinite; }
.wish .scene-light { background: radial-gradient(ellipse at 50% 79%, #ffcb21b0, transparent 57%); animation: wish-glow 6s ease-in-out infinite; }
.wish-aura { stroke-width: 5; transform-origin: 50% 79.5%; animation: wish-glow 6s ease-in-out infinite; }
.punch { --scene-accent: #fff0ce; }
.punch .scene-actor { transform-origin: 53% 79%; animation: punch-strike 5s ease-in-out infinite; }
.punch .scene-background { animation: palace-shake 5s ease-in-out infinite; }
.punch .scene-light { background: radial-gradient(ellipse at 78% 27%, #fff3c8b0, transparent 64%); animation: punch-flash 5s ease-in-out infinite; }
.punch .scene-mote { width: 9px; height: 7px; border-radius: 0; background: #8f827b; box-shadow: none; animation-name: wood-chip; }
.punch-wave { transform-origin: 78% 27%; animation: punch-ring 5s ease-out infinite; }
@keyframes wukong-swing { 0%, 100% { transform: rotate(-1deg); } 38% { transform: translate(-.6%, .5%) rotate(-2deg); } 55% { transform: translate(.7%, -.7%) rotate(1.5deg); } }
@keyframes palace-shake { 0%, 38%, 70%, 100% { transform: scale(1.035); } 46% { transform: translate(.35%, -.2%) scale(1.04); } 52% { transform: translate(-.3%, .25%) scale(1.04); } 58% { transform: translate(.15%, -.1%) scale(1.04); } }
@keyframes falling-tile { 0% { opacity: 0; transform: translate(0, -150px) rotate(-35deg); } 20%, 70% { opacity: .8; } 100% { opacity: 0; transform: translate(20px, 70px) rotate(120deg); } }
@keyframes visits-bow { 0%, 100% { transform: rotate(-.35deg); } 45% { transform: translateY(.6%) rotate(.6deg); } 65% { transform: translateY(-.25%) rotate(-.2deg); } }
@keyframes pigs-build { 0%, 100% { transform: none; } 35% { transform: translateY(.6%) scaleY(.99); } 55% { transform: translateY(-.7%) rotate(.5deg); } }
@keyframes house-rise { 0%, 100% { transform: scale(1.035); } 55% { transform: translateY(-.5%) scale(1.045); } }
@keyframes dragon-hover { 0%, 100% { transform: translateY(.3%) scale(1.035) rotate(-.35deg); } 50% { transform: translateY(-.8%) scale(1.055) rotate(.4deg); } }
@keyframes wish-reach { 0%, 100% { transform: rotate(-.5deg); } 50% { transform: translateY(-.8%) rotate(.7deg); } }
@keyframes wish-glow { 0%, 100% { opacity: .2; } 50% { opacity: .65; } }
@keyframes punch-strike { 0%, 30%, 100% { transform: none; } 42% { transform: translate(-.4%, .5%) scale(.995); } 53%, 58% { transform: translate(.9%, -.7%) scale(1.018); } 72% { transform: none; } }
@keyframes punch-flash { 0%, 35%, 75%, 100% { opacity: .1; } 52% { opacity: .65; } }
@keyframes punch-ring { 0%, 39%, 100% { opacity: 0; transform: scale(.55); } 48% { opacity: .7; } 68% { opacity: 0; transform: scale(1.6); } }
.teapot { --scene-accent: #ffe6a0; }
.teapot .scene-actor { transform-origin: 30% 90%; animation: teapot-breathe 8s ease-in-out infinite; }
.teapot .scene-secondary { transform-origin: 75% 28%; animation: teapot-relocate 8s ease-in-out infinite; }
.teapot .scene-light { background: radial-gradient(ellipse at 64% 72%, #ffcf6280, transparent 62%); animation: teapot-glow 8s ease-in-out infinite; }
.teapot .scene-mote { top: 74%; left: calc(42% + var(--spark) * 7%); width: 5px; height: 8px; border-radius: 70% 0 70% 0; animation: teapot-leaf 6s linear infinite; animation-delay: calc(var(--spark) * -1s); }
.teapot-energy { filter: drop-shadow(0 0 4px #eeb54c); }
.teapot-aura { transform-origin: 64% 73.5%; stroke-width: 4; animation: teapot-ring 8s ease-out infinite; }
.teapot-route { stroke-width: 4; stroke-dasharray: 95 1150; animation: teapot-flow 4s linear infinite; }
.teapot-return { stroke-width: 2; animation-delay: -2s; }
.teapot-build { transform-origin: 77.5% 32%; stroke-width: 2; animation: teapot-build 8s ease-in-out infinite; }
@keyframes teapot-breathe { 0%, 100% { transform: none; } 50% { transform: translateY(-.6%) rotate(.3deg); } }
@keyframes teapot-relocate { 0%, 100% { transform: translate(-2%, 1%); } 35% { transform: translate(3%, -2%); } 65% { transform: translate(1%, -3%); } }
@keyframes teapot-glow { 0%, 100% { opacity: .18; } 40%, 65% { opacity: .55; } }
@keyframes teapot-ring { 0%, 100% { opacity: 0; transform: scale(.8); } 20% { opacity: .65; } 75% { opacity: 0; transform: scale(1.2); } }
@keyframes teapot-flow { from { stroke-dashoffset: 1245; } to { stroke-dashoffset: 0; } }
@keyframes teapot-build { 0%, 15%, 85%, 100% { opacity: 0; transform: translateY(12px) scaleY(.7); } 40%, 60% { opacity: .7; transform: translateY(-12px) scaleY(1); } }
@keyframes teapot-leaf { 0% { opacity: 0; transform: translate(0, 0) rotate(-35deg) scale(.4); } 25%, 60% { opacity: .85; } 100% { opacity: 0; transform: translate(25px, -125px) rotate(100deg) scale(.8); } }
@media (prefers-reduced-motion: reduce) { .ur-scene { display: none; } }
</style>
