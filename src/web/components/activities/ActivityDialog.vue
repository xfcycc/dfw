<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { ACTIVITY_NAMES, type ActivityInput } from '../../activity-art';
import type { Sound } from '../../audio';
import { playSound } from '../../audio';
import { useGameStore } from '../../stores/game';
import Modal from '../Modal.vue';
import CasinoScene from './CasinoScene.vue';
import HorseScene from './HorseScene.vue';
import RouletteScene from './RouletteScene.vue';
import PacketScene from './PacketScene.vue';
import RpsScene from './RpsScene.vue';
import TreasureScene from './TreasureScene.vue';
import BridgeScene from './BridgeScene.vue';
import MinorityScene from './MinorityScene.vue';
import BlindBoxScene from './BlindBoxScene.vue';
import ActivityPresentation from './ActivityPresentation.vue';
import '../../activities.css';
const props = defineProps<{ paused?: boolean; spectator?: boolean }>();
const game = useGameStore(); const open = ref(true);
const blindboxRevealing = ref(false);
const activity = computed(() => game.room?.activity);
const scenes = { casino: CasinoScene, horse: HorseScene, roulette: RouletteScene, packet: PacketScene, rps: RpsScene, treasure: TreasureScene, bridge: BridgeScene, minority: MinorityScene, blindbox: BlindBoxScene };
const cardRewardRules = { casino: '', horse: '赛马中奖者', roulette: '轮盘获胜者', packet: '全员领取后的手气最佳者', rps: '猜拳冠军', treasure: '三层全部通关者', bridge: '三关全部通关者', minority: '本场中奖者', blindbox: '隐藏款或限定款领取者' };
const ownSelection = computed(() => game.privateData?.activityId === activity.value?.id && game.privateData?.activityRound === activity.value?.round ? game.privateData?.activitySelection : null);
const enabled = computed(() => {
  const a = activity.value, id = game.me?.id;
  if (!a || !id || props.spectator || !game.canAct || game.room?.recoveryPending || a.endsAt || !a.participants.includes(id)) return false;
  if (a.kind === 'roulette') return id === (a.stage === 'select' ? a.triggerId : a.currentPlayerId);
  if (a.kind === 'treasure') return id === a.triggerId && ['select', 'treasure_choice'].includes(a.stage);
  if (a.kind === 'bridge' && a.stage === 'bridge_select') return a.contenders.includes(id) && !a.submitted.includes(id);
  return a.stage === 'select' && !a.submitted.includes(id) && (a.kind !== 'rps' || a.contenders.includes(id));
});
const progress = computed(() => {
  const a = activity.value; if (!a) return '';
  if (game.room?.pausedReason) return game.room.pausedReason;
  if (a.kind === 'roulette') return a.stage === 'select' ? '等待发起者选择对手' : a.stage === 'payment' ? '正在处理对决费用' : `已开枪 ${a.shots} / 6 次`;
  if (a.settled) return a.summary;
  if (a.kind === 'packet') return `${a.submitted.length} / ${a.participants.length} 人已拆红包 · 选中立即到账`;
  if (a.kind === 'blindbox') return `${a.submitted.length} / ${a.participants.length} 人已领取 · 20 个盲盒每人选一个`;
  if (a.kind === 'treasure') return a.endsAt ? '' : `已通过 ${a.level ?? 0} 层 · 等待落点玩家${a.stage === 'select' ? '选箱' : '收手或继续'}`;
  if (a.kind === 'bridge' && a.stage !== 'select') return a.endsAt ? '' : `第 ${a.level} 关 · ${a.submitted.length} / ${a.contenders.length} 人已选边`;
  if (a.endsAt) return '';
  return `${a.submitted.length} / ${a.kind === 'rps' ? a.contenders.length : a.participants.length} 人已提交${ownSelection.value != null ? ' · 你的选择已锁定' : ''}`;
});
/** 输入专属场景的选择，补齐服务端活动身份和子轮次后提交。 */
async function submit(input: ActivityInput): Promise<void> {
  const a = activity.value; if (!a || !enabled.value) return;
  await game.act({ type: 'activityInput', activityId: a.id, activityRound: a.round, ...input });
}
watch(() => activity.value?.id, () => { open.value = true; });
let hoof: ReturnType<typeof setInterval> | undefined;
let baseline = true;
watch(() => game.connection, state => { if (state !== 'online') { baseline = true; clearInterval(hoof); } });
watch(() => `${activity.value?.id}:${activity.value?.round}:${activity.value?.stage}`, () => {
  clearInterval(hoof);
  const a = activity.value;
  if (!a || baseline || game.connection !== 'online' || props.paused) { baseline = false; return; }
  let sound: Sound | undefined;
  if (a.stage === 'rolling') sound = 'casino_dice';
  else if (a.stage === 'racing') { sound = 'horse_start'; hoof = setInterval(() => { playSound('horse_run'); }, 500); }
  else if (a.stage === 'loading') sound = 'roulette_spin';
  else if (a.kind === 'roulette' && a.stage === 'duel' && a.shots) sound = 'roulette_click';
  else if (a.stage === 'payment') sound = 'roulette_hit';
  else if (a.stage === 'opening') sound = 'packet_open';
  else if (a.stage === 'revealing') sound = 'rps_count';
  else if (a.stage === 'round_result') sound = 'rps_reveal';
  else if (a.stage === 'treasure_open') sound = 'packet_open';
  else if (a.stage === 'treasure_choice') sound = 'casino_win';
  else if (a.stage === 'bridge_reveal' || a.stage === 'minority_reveal') sound = 'rps_count';
  else if (a.stage === 'bridge_round') sound = 'rps_reveal';
  else if (a.stage === 'result') {
    if (a.kind === 'casino' && Object.keys(a.wagers).length) sound = Object.keys(a.payouts).length ? 'casino_win' : 'casino_loss';
    else if (a.kind === 'horse' && Object.keys(a.wagers).length) sound = 'horse_finish';
    else if (a.kind === 'rps') sound = 'rps_win';
    else if (a.kind === 'treasure' || a.kind === 'bridge' || a.kind === 'minority') sound = Object.keys(a.payouts).length ? 'victory' : 'casino_loss';
  }
  if (sound) playSound(sound);
}, { immediate: true });
onBeforeUnmount(() => { clearInterval(hoof); });
</script>
<template>
  <div v-if="activity" class="activity-entry"><div><strong>{{ ACTIVITY_NAMES[activity.kind] }}</strong><small v-if="progress">{{ progress }}</small></div><button v-if="activity.stage !== 'payment'" class="button primary small" :disabled="paused" @click="open = true">进入活动</button><small v-else>请先处理工作台的费用待办</small></div>
  <Modal v-if="activity && activity.stage !== 'payment'" :open="open && !paused && !game.room?.recoveryPending" :title="ACTIVITY_NAMES[activity.kind]" wide panel-class="activity-modal" @close="open = false">
    <div class="activity-scene activity-variant" :class="[`activity-${activity.kind}`, { 'has-result': activity.settled }]" style="--activity-background:none">
      <div class="activity-host-caption"><span>苏暖暖的游园会</span><strong>{{ ACTIVITY_NAMES[activity.kind] }}</strong><small v-if="progress">{{ progress }}</small></div>
      <div class="activity-content"><ActivityPresentation :activity="activity" :revealing="blindboxRevealing"><component :is="scenes[activity.kind]" :key="activity.id" :activity="activity" :enabled="enabled" @input="submit" @revealing="blindboxRevealing = $event" /></ActivityPresentation>
        <p v-if="cardRewardRules[activity.kind]" class="activity-help">{{ cardRewardRules[activity.kind] }}额外获得 1 张随机道具。</p>
        <p v-if="spectator || game.me?.bankrupt" class="activity-help">观赛席 · 为朋友们助威吧</p><p v-else-if="!enabled && !activity.endsAt && activity.stage === 'select' && !['packet', 'blindbox'].includes(activity.kind)" class="activity-help">{{ ownSelection != null ? '你的选择已提交，等待其他玩家。' : '等待本轮参与者操作。' }}</p>
      </div>
    </div>
  </Modal>
</template>
