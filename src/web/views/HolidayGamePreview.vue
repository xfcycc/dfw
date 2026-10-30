<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue';
import type { PublicRoomState, Tile } from '../../shared/types';
import { imageUrl } from '../image-assets';
import { activeHoliday, holidayTheme, startHolidayClock } from '../holiday-theme';
import Board from '../components/Board.vue';
import Logo from '../components/Logo.vue';
import PanelFrame from '../components/PanelFrame.vue';
import PublicLeaderboard from '../components/PublicLeaderboard.vue';
import HandSlots from '../components/HandSlots.vue';
import HolidayDecorations from '../components/HolidayDecorations.vue';
import HolidayPreview from '../components/HolidayPreview.vue';
import DiceFace from '../components/DiceFace.vue';
import DesktopDiceThrow from '../components/DesktopDiceThrow.vue';
import HolidayDiceEffects from '../components/HolidayDiceEffects.vue';

/** 无入参，返回仅用于外观预览的固定局面，不创建房间或发送对局命令。 */
function previewRoom(): PublicRoomState {
  const names = ['阿洛', '小桃', '阿岚', '暖暖'];
  const colors = ['#739d81', '#d8978b', '#809fc1', '#c4a15e'];
  const board: Tile[] = Array.from({ length: 32 }, (_, id) => ({ id, type: id === 0 ? 'start' : 'property', name: id === 0 ? '起点' : `花园街 ${id}`, district: Math.floor(id / 8), price: 2000, ownerId: id % 3 === 0 && id !== 0 ? `preview-${id % 4}` : null, level: id % 3 === 0 && id !== 0 ? 2 : 0, mortgaged: false, barrier: false, demolitionShield: false, treasure: false, concertStacks: 0, meteorCharges: 0, kamuiCasterId: null, kamuiTurns: 0, kamuiAppliedTurn: 0 }));
  return { roomId: 'holiday-preview', code: 'PREVIEW', version: 1, status: 'playing', hostId: 'preview-0', gameId: 'holiday-preview',
    players: names.map((name, index) => ({ id: `preview-${index}`, name, color: colors[index]!, avatarId: ['explorer', 'cat', 'wizard', 'bunny'][index], cash: 12000 + index * 1500, netWorth: 24000 + index * 1500, cardCount: 0, position: [1, 9, 17, 25][index]!, ready: true, online: true, bankrupt: false, skipTurns: 0, jailed: false, eliminationOrder: null })),
    board, turnOrder: names.map((_, index) => `preview-${index}`), turnIndex: 0, turnNumber: 3, phase: 'pre_roll', pending: null, tradePending: false, pausedReason: null, dice: null, motion: null, winnerId: null, logs: [] };
}
const room = previewRoom();
const diceBoard = ref<HTMLElement>();
const diceMode = ref<'desktop' | 'mobile' | null>(null);
const diceRolling = ref(false);
const diceSequence = ref(0);
let diceTimer: number | undefined;
/** 无入参，取消骰子外观演示与定时器，不改变示例局面，无返回值。 */
function closeDicePreview(): void {
  window.clearTimeout(diceTimer);
  diceMode.value = null;
  diceRolling.value = false;
}
/** 输入演示设备类型，使用实际骰子组件播放固定六点动画，不发送命令，无返回值。 */
function replayDice(mode: 'desktop' | 'mobile'): void {
  closeDicePreview();
  diceSequence.value++;
  diceMode.value = mode;
  diceRolling.value = true;
  diceTimer = window.setTimeout(() => { diceRolling.value = false; }, mode === 'desktop' ? 1400 : 780);
}
const smallScreen = window.matchMedia('(max-width: 760px)');
const narrow = ref(smallScreen.matches);
/** 无入参，按屏幕宽度将预览面板放到地图下方，无返回值。 */
function resizePreview(): void { narrow.value = smallScreen.matches; }
let stopClock: (() => void) | undefined;
onMounted(() => { stopClock = startHolidayClock(); smallScreen.addEventListener('change', resizePreview); });
onUnmounted(() => { stopClock?.(); closeDicePreview(); smallScreen.removeEventListener('change', resizePreview); });
watch(activeHoliday, value => {
  closeDicePreview();
  if (value) document.documentElement.dataset.holiday = value;
  else delete document.documentElement.dataset.holiday;
  document.title = `对局皮肤预览 · ${holidayTheme.value?.name ?? '原版'} · 大富翁-奇迹大陆`;
}, { immediate: true });
</script>

<template>
  <div class="room-page modern-room game-session holiday-game-preview" :class="{ 'desktop-single-screen': !narrow, 'single-screen': !narrow }" :style="{ '--town-background': `url(${imageUrl('/images/board/town-background.png')})`, '--holiday-background': holidayTheme ? `url(${holidayTheme.background})` : undefined }">
    <HolidayDecorations />
    <header class="site-header room-header immersive-header"><Logo /><span class="room-tag">对局外观预览</span><a class="button secondary small" href="/">返回首页</a></header>
    <div class="holiday-game-preview-note"><span>固定示例局面 · 骰子演示固定为 6 点</span><button type="button" class="button secondary small" data-dice-roll :disabled="diceRolling" @click="replayDice('desktop')">桌面投掷</button><button type="button" class="button secondary small" :disabled="diceRolling" @click="replayDice('mobile')">手机投掷</button><button v-if="diceMode" type="button" class="button secondary small" @click="closeDicePreview">收起骰子</button></div>
    <main>
      <div class="game-layout immersive-layout"><section class="map-panel"><div ref="diceBoard" class="board-view">
        <Board :room="room" compact stretch :dashboard-target="narrow ? '#holiday-preview-dashboard' : undefined" :bounded="false" me-id="preview-0" :live="false">
          <template #action-status><div class="turn-action-status"><strong>当前行动：等待掷骰</strong></div></template>
          <template #controls>
            <div class="holiday-game-preview-panels">
              <section class="city-feed-panel"><PanelFrame /><div class="workspace-heading"><h2>城市新鲜事</h2></div><p>阿洛来到了花园街，准备开启下一段旅程。</p><p>小桃升级了一处地产。</p><p>暖暖经过起点，领取了本轮奖励。</p></section>
              <aside class="dashboard-ranking workspace-panel"><PanelFrame variant="ranking" ornament="right" /><div class="workspace-heading"><h2>资产排行</h2><span class="workspace-count">4 人</span></div><PublicLeaderboard compact :room="room" me-id="preview-0" /></aside>
              <section class="dashboard-workbench workspace-panel"><PanelFrame variant="workbench" /><div class="workspace-heading"><h2>我的工作台</h2></div><div class="holiday-preview-dice-faces"><DiceFace v-for="value in 6" :key="value" :value="value" /></div><p class="muted">骰面外观展示 · 不连接真实房间</p></section>
              <section class="dashboard-cards workspace-panel"><PanelFrame variant="cards" /><div class="workspace-heading"><h2>我的手牌</h2></div><div class="dashboard-card-rail"><HandSlots :count="0" :limit="6" /></div></section>
            </div>
          </template>
        </Board>
      </div></section></div>
      <div v-show="narrow" id="holiday-preview-dashboard" class="game-board immersive-board holiday-preview-dashboard"></div>
    </main>
  </div>
  <DesktopDiceThrow v-if="diceMode === 'desktop'" :key="diceSequence" :board="diceBoard" :player="room.players[0]!" mine :value="6" :rolling="diceRolling" :duration="1400" />
  <div v-if="diceMode === 'mobile'" :key="`mobile-${diceSequence}`" class="holiday-dice-mobile-preview" role="status" aria-live="polite"><div class="dice-stage"><HolidayDiceEffects :rolling="diceRolling" /><DiceFace :value="diceRolling ? 3 : 6" :rolling="diceRolling" :settled="!diceRolling" /></div><strong>{{ diceRolling ? '正在掷骰…' : '掷出 6 点' }}</strong></div>
  <HolidayPreview />
</template>

<style scoped>
.holiday-game-preview-note{position:relative;display:flex;align-items:center;justify-content:center;flex-wrap:wrap;gap:8px;flex-shrink:0;margin:12px auto;padding:8px 16px;width:fit-content;max-width:calc(100% - 24px);border:1px solid #ffffffb3;border-radius:14px;background:#fffaf0ed;color:#78634d;text-align:center;font-size:11px}
.holiday-preview-dice-faces{display:flex;gap:8px;flex-wrap:wrap}
.holiday-preview-dice-faces :deep(.die){margin:0}
.holiday-dice-mobile-preview{position:fixed;z-index:150;top:50%;left:50%;transform:translate(-50%,-50%);display:flex;flex-direction:column;align-items:center;gap:24px;pointer-events:none;padding:38px 48px;border-radius:26px;background:#fff9eceb;box-shadow:0 12px 40px #382d4233;color:#80501e}
.holiday-dice-mobile-preview .dice-stage{position:relative}
.holiday-dice-mobile-preview :deep(.die){width:92px;height:92px;padding:16px;gap:7px;margin:0}
.holiday-game-preview>main{padding:0 clamp(12px,3vw,48px) 125px}
.modern-room.game-session.holiday-game-preview.desktop-single-screen>main{padding:0 24px 115px}
.holiday-game-preview .room-header{display:flex;align-items:center;justify-content:space-between;gap:12px;width:100%;max-width:none}
.holiday-game-preview .game-layout{align-items:stretch;min-width:0;width:100%}
.holiday-game-preview .board-view{width:100%;margin:0}
.modern-room.desktop-single-screen .holiday-game-preview-panels>*{grid-area:auto;margin:0}
.holiday-game-preview-panels{display:grid;grid-template-columns:minmax(0,1.3fr) minmax(0,1fr);grid-template-rows:minmax(220px,1fr) auto;gap:18px;width:100%;min-height:100%;text-align:left}
.holiday-game-preview-panels>section,.holiday-game-preview-panels>aside{min-width:0;min-height:0}
.holiday-game-preview-panels .dashboard-ranking{max-height:250px}
.holiday-game-preview-panels p{font-size:12px;line-height:1.8;margin:10px 0}
.holiday-game-preview :deep(.workbench-center){display:flex;flex-direction:column}
.holiday-game-preview :deep(.board-center-controls){flex:1;min-height:0;overflow:auto}
.holiday-game-preview :deep(.dashboard-card-rail){display:flex;gap:5px}
.holiday-game-preview :deep(.dashboard-card-rail .hand-empty-slot){min-width:0;flex:1}
.holiday-preview-dashboard{margin-top:16px}
.holiday-preview-dashboard :deep(.board-center){position:relative;inset:auto;width:100%;height:auto;transform:none}
@media(max-width:760px){.holiday-game-preview-panels{grid-template-columns:1fr}.holiday-game-preview .room-tag{display:none}.holiday-game-preview :deep(.board-center-controls){overflow:auto}.modern-room.game-session.holiday-game-preview :deep(.board-viewport){height:280px;min-height:280px}.holiday-game-preview-note{font-size:10px}}
</style>
