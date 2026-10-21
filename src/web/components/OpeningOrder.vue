<script setup lang="ts">
import { computed, watch } from 'vue';
import type { PublicPlayer } from '../../shared/types';
import { playSound } from '../audio';
import CharacterPawn from './CharacterPawn.vue';
import Modal from './Modal.vue';

const props = defineProps<{ players: PublicPlayer[]; turnOrder: string[]; ownId?: string; elapsed: number; animated: boolean }>();
const settled = computed(() => !props.animated || props.elapsed >= 3100);
const departing = computed(() => !props.animated || props.elapsed >= 3900);
const seats = computed(() => settled.value ? props.turnOrder.flatMap(id => props.players.filter(player => player.id === id)) : props.players);
const first = computed(() => props.players.find(player => player.id === props.turnOrder[0]));
const title = computed(() => departing.value ? '好运就位，准备出发！' : settled.value ? '行动顺序已揭晓' : props.elapsed < 1600 ? '谁先出发？' : '你的号码是…');
/** 输入玩家标识，返回后端抽签确定的行动位次，从一开始计数。 */
function rank(id: string): number { return props.turnOrder.indexOf(id) + 1; }
/** 输入当前卡片位置，返回是否到达该张的翻牌时间；静态展示直接揭晓。 */
function revealed(index: number): boolean { return !props.animated || props.elapsed >= 1600 + index * 110; }
/** 输入当前演出阶段，仅在洗牌、翻牌、入位和首位亮相的边界播放已有短音效。 */
watch(() => {
  if (!props.animated || props.elapsed < 650) return '';
  if (props.elapsed < 1600) return 'shuffle';
  if (props.elapsed < 3100) return `reveal:${Math.min(props.players.length, 1 + Math.floor((props.elapsed - 1600) / 110))}`;
  return props.elapsed < 3900 ? 'sort' : 'first';
}, cue => {
  if (!cue || document.hidden) return;
  playSound(cue === 'shuffle' ? 'card' : cue === 'sort' ? 'step' : cue === 'first' ? 'magic' : 'click');
});
</script>

<template>
  <Modal title="开场抽签 · 行动顺序" hide-heading close-disabled wide panel-class="opening-order-modal">
    <div class="opening-order" :class="{ 'is-dealing': animated && elapsed < 650, 'is-shuffling': animated && elapsed >= 650 && elapsed < 1550, 'is-settled': settled, 'is-departing': departing }">
      <header class="opening-heading">
        <span class="opening-eyebrow">奇迹大陆 · 开场抽签</span>
        <h2>{{ title }}</h2>
        <p>{{ settled ? '按号码依次行动，每一位都有好机会' : '好运正在洗牌，看看谁拿到第一签' }}</p>
      </header>
      <TransitionGroup name="opening-seats" tag="ol" class="opening-seats" :class="{ many: players.length > 3 }" :style="{ '--seats': players.length }" aria-label="行动顺序">
        <li v-for="(player, index) in seats" :key="player.id" class="opening-seat" :class="{ 'is-first': departing && rank(player.id) === 1, 'is-own': player.id === ownId }" :style="{ '--seat-color': player.color, '--seat-index': index }">
          <div class="opening-ticket-wrap" aria-hidden="true">
            <div class="opening-ticket" :class="{ 'is-revealed': revealed(index) }">
              <div class="ticket-back"><span>好运签</span><b>✦</b><small>奇迹大陆</small></div>
              <div class="ticket-front"><span>行动号码</span><b>{{ rank(player.id) }}</b><small>{{ rank(player.id) === 1 ? '率先出发' : `第 ${rank(player.id)} 位` }}</small></div>
            </div>
          </div>
          <div class="opening-player">
            <CharacterPawn :avatar-id="player.avatarId" :color="player.color" />
            <strong>{{ player.name }}</strong>
            <span class="opening-seat-label">{{ player.id === ownId ? '你' : player.botDifficulty ? '机器人' : '玩家' }}<template v-if="revealed(index)"> · 第 {{ rank(player.id) }} 位</template></span>
          </div>
        </li>
      </TransitionGroup>
      <div class="opening-announcement" role="status" aria-live="polite">
        <template v-if="departing && first"><span class="opening-star" aria-hidden="true">✦</span><strong :style="{ color: first.color }">{{ first.name }}</strong><span>，率先出发！</span></template>
        <template v-else>{{ settled ? '行动队列已就位' : '每人一张号码牌，即将揭晓…' }}</template>
      </div>
      <div class="opening-progress" aria-hidden="true"><span :style="{ width: `${Math.min(100, elapsed / 5200 * 100)}%` }" /></div>
      <p class="opening-footnote">{{ animated ? '抽签结束后自动开始' : '行动顺序已确定，即将开始' }}</p>
    </div>
  </Modal>
</template>

<style scoped>
:global(.modal-backdrop:has(> .opening-order-modal)) { z-index: 210; background: #49351e99; backdrop-filter: blur(9px); }
:global(.modal.chibi-modal.opening-order-modal) { width: min(100%, 880px); padding: 30px; border-color: #dbb97e; background: radial-gradient(ellipse at 50% 0, #fff1cd, #fff9eb 70%); }
.opening-order { color: #795b38; text-align: center; }
.opening-heading { margin-bottom: 28px; }
.opening-eyebrow { display: inline-block; padding: 6px 16px; border: 1px solid #e5caa0; border-radius: 99px; background: #fff9e9; color: #a47a44; font-size: 11px; letter-spacing: 3px; }
.opening-heading h2 { margin: 15px 0 8px; color: #93602e; font-size: clamp(22px, 4vw, 32px); font-weight: 900; }
.opening-heading p { margin: 0; color: #a38b6e; font-size: 12px; }
.opening-seats { display: grid; grid-template-columns: repeat(var(--seats), minmax(0, 1fr)); gap: 14px; max-width: 750px; margin: 0 auto; padding: 0; list-style: none; }
.opening-seat { min-width: 0; padding: 10px 5px 12px; border: 2px solid transparent; border-radius: 20px; transition: background .4s, border-color .4s, box-shadow .4s; }
.opening-seat.is-own { background: #f7ecd760; }
.opening-ticket-wrap { width: min(100%, 100px); height: 138px; margin: 0 auto 14px; perspective: 700px; }
.opening-ticket { position: relative; width: 100%; height: 100%; transform-style: preserve-3d; transition: transform .52s cubic-bezier(.2, .7, .2, 1); }
.opening-ticket.is-revealed { transform: rotateY(180deg); }
.ticket-back, .ticket-front { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 7px; overflow: hidden; border: 2px solid #d6a75f; border-radius: 15px; backface-visibility: hidden; -webkit-backface-visibility: hidden; box-shadow: inset 0 0 0 4px #fff8e8, 0 5px 0 #c89c5b40, 0 9px 18px #9d763b15; }
.ticket-back { background: repeating-linear-gradient(45deg, #f3d394 0 8px, #f8dfa9 8px 16px); color: #a06e32; }
.ticket-back > b { display: grid; place-items: center; width: 56px; height: 56px; border: 1px solid #d1a15c; border-radius: 50%; background: #fff1ce; color: #bb8a46; font-size: 36px; }
.ticket-back > span, .ticket-front > span { font-size: 10px; font-weight: 700; letter-spacing: 2px; }
.ticket-back > small, .ticket-front > small { font-size: 10px; letter-spacing: 1px; }
.ticket-front { transform: rotateY(180deg); background: linear-gradient(155deg, #fffef5, #ffedc8); }
.ticket-front > b { color: #bf7c33; font-size: 60px; font-weight: 900; line-height: 1.1; text-shadow: 0 3px 0 #efc78b; }
.opening-player { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.opening-player :deep(.character-pawn) { width: 42px; height: 42px; border-radius: 14px; box-shadow: 0 3px 0 #c7ad8340; }
.opening-player > strong { display: block; width: 100%; overflow: hidden; color: var(--seat-color); font-size: 12px; line-height: 1.5; text-overflow: ellipsis; white-space: nowrap; }
.opening-seat-label { color: #a18b6f; font-size: 10px; }
.is-first { border-color: #e0b45f; background: #ffe9ab80 !important; box-shadow: 0 0 25px #efc66e55, inset 0 0 0 3px #fff9e5; }
.opening-announcement { display: flex; align-items: center; justify-content: center; min-height: 36px; gap: 4px; margin: 22px 0 12px; font-size: 14px; overflow-wrap: anywhere; }
.opening-announcement strong { max-width: 60%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.opening-star { margin-right: 5px; color: #d7a449; font-size: 25px; }
.opening-progress { height: 5px; max-width: 220px; margin: 0 auto; overflow: hidden; border-radius: 99px; background: #eddfc5; }
.opening-progress > span { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #e7bd71, #c78b49); }
.opening-footnote { margin: 10px 0 0; color: #ae987c; font-size: 10px; }
.opening-seats-move { transition: transform .7s cubic-bezier(.2, .8, .2, 1); }
.is-dealing .opening-ticket-wrap { animation: opening-deal .55s both; animation-delay: calc(var(--seat-index) * 55ms); }
.is-shuffling .opening-ticket-wrap { animation: opening-shuffle .32s ease-in-out infinite alternate; animation-delay: calc(var(--seat-index) * -90ms); }
@keyframes opening-deal { from { opacity: 0; transform: translateY(-35px) rotate(-8deg) scale(.8); } to { opacity: 1; transform: none; } }
@keyframes opening-shuffle { from { transform: translateY(-6px) rotate(-4deg); } to { transform: translateY(3px) rotate(4deg); } }
@media (max-width: 760px) {
  :global(.modal.chibi-modal.opening-order-modal) { padding: 20px 12px; border-radius: 24px; }
  .opening-heading { margin-bottom: 18px; }
  .opening-heading h2 { margin-top: 12px; }
  .opening-seats { gap: 8px; max-width: 360px; }
  .opening-seats.many { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .opening-seat { padding: 7px 3px; border-radius: 16px; }
  .opening-ticket-wrap { width: 76px; height: 101px; margin-bottom: 10px; }
  .ticket-back, .ticket-front { border-radius: 12px; gap: 4px; }
  .ticket-back > b { width: 37px; height: 37px; font-size: 25px; }
  .ticket-front > b { font-size: 43px; }
  .opening-player { gap: 3px; }
  .opening-player :deep(.character-pawn) { width: 30px; height: 30px; border-radius: 10px; }
  .opening-player > strong { font-size: 11px; }
  .opening-announcement { margin-top: 15px; font-size: 12px; }
}
@media (max-width: 760px) and (max-height: 700px) {
  .opening-heading { margin-bottom: 10px; }
  .opening-heading h2 { margin: 8px 0 5px; font-size: 22px; }
  .opening-eyebrow { padding: 4px 12px; }
  .opening-ticket-wrap { width: 62px; height: 80px; margin-bottom: 6px; }
  .ticket-front > b { font-size: 32px; }
  .ticket-back > b { width: 28px; height: 28px; font-size: 21px; }
  .opening-player :deep(.character-pawn) { width: 26px; height: 26px; }
  .opening-announcement { margin: 8px 0; }
}
@media (max-height: 540px) and (min-width: 761px) {
  :global(.modal.chibi-modal.opening-order-modal) { padding: 16px; }
  .opening-heading { margin-bottom: 8px; }
  .opening-heading h2 { margin: 5px 0; font-size: 22px; }
  .opening-heading p, .opening-footnote { display: none; }
  .opening-ticket-wrap { height: 96px; width: 72px; margin-bottom: 8px; }
  .ticket-front > b { font-size: 40px; }
  .ticket-back > b { height: 32px; width: 32px; font-size: 24px; }
  .opening-player :deep(.character-pawn) { height: 28px; width: 28px; }
  .opening-announcement { margin: 8px 0; }
}
@media (prefers-reduced-motion: reduce) {
  .opening-ticket-wrap, .opening-ticket, .opening-seats-move, .opening-seat { animation: none !important; transition: none !important; }
}
</style>
