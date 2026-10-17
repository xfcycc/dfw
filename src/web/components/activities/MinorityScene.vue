<script setup lang="ts">
import { computed, ref } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import type { ActivityInput } from '../../activity-art';
import { useGameStore } from '../../stores/game';
import CharacterPawn from '../CharacterPawn.vue';
import ActivityArtwork from './ActivityArtwork.vue';
const props = defineProps<{ activity: CornerActivity; enabled: boolean }>();
const emit = defineEmits<{ input: [value: ActivityInput] }>();
const game = useGameStore();
const selected = ref(0);
const chosen = computed(() => game.privateData?.activityId === props.activity.id && game.privateData.activityRound === props.activity.round ? game.privateData.activitySelection ?? selected.value : selected.value);
const stalls = [{ name: '火锅摊', art: 'hotpot', line: '热汤翻滚，等你入座' }, { name: '烧烤摊', art: 'barbecue', line: '炭火正旺，来串好签' }] as const;
const revealed = computed(() => props.activity.stage === 'result');
const guests = computed(() => stalls.map((_, side) => game.room?.players.filter(player => props.activity.participants.includes(player.id) && props.activity.revealed[player.id] === side) ?? []));
const waiting = computed(() => game.room?.players.filter(player => props.activity.participants.includes(player.id)) ?? []);
const lottery = computed(() => guests.value.some(group => group.length === 0) || guests.value[0]!.length === guests.value[1]!.length ? '本场全员参与抽奖' : '少数派摊位参与抽奖');
</script>
<template>
  <div class="minority-scene">
    <p>免费参加 · 少数派中抽一人独得 <strong>¥{{ game.room?.activityRotation?.money.rpsPrize?.toLocaleString() }}</strong></p>
    <p class="activity-help">秘密选摊，人数少的一边争大奖；人数相同或全在同一摊时，从全员中抽一人。</p>
    <div class="night-plaza" :class="{ revealed, counting: activity.stage === 'minority_reveal' }">
      <ActivityArtwork name="market-scene" backdrop />
      <div class="market-lights" aria-hidden="true"><span v-for="n in 7" :key="n">{{ n % 2 ? '福' : '✦' }}</span></div>
      <div class="market-sign"><small>今晚去哪吃？</small><strong>暖暖夜市</strong><span>{{ revealed ? lottery : '先挑摊位，再锁定入场' }}</span></div>
      <div class="night-market"><div v-for="(stall, side) in stalls" :key="stall.name" class="market-booth" :class="{ selected: !revealed && chosen === side, eligible: revealed && guests[side]?.length && guests[side]?.some(player => activity.contenders.includes(player.id)) }">
        <button class="night-stall" :disabled="!enabled" :aria-pressed="!revealed && chosen === side" @click="selected = side"><span class="stall-illustration"><ActivityArtwork :name="stall.art" /><i class="food-steam" aria-hidden="true">〰 〰</i></span><strong>{{ stall.name }}</strong><small>{{ stall.line }}</small><span class="stall-counter">{{ revealed ? `${guests[side]?.length} 人入座` : chosen === side ? '✓ 你的意向摊位' : '点这里选摊' }}</span></button>
        <div v-if="revealed" class="stall-seats"><div v-for="(player, index) in guests[side]" :key="player.id" class="night-guest" :style="{ '--guest-delay': `${index * 90}ms` }" :class="{ winner: activity.payouts[player.id] !== undefined }"><span v-if="activity.payouts[player.id] !== undefined" class="guest-crown" aria-label="中奖">♛</span><CharacterPawn :avatar-id="player.avatarId" :color="player.color" /><strong>{{ player.name }}</strong><small v-if="activity.payouts[player.id] !== undefined">+¥{{ activity.payouts[player.id]?.toLocaleString() }}</small></div><small v-if="!guests[side]?.length">这边空着呢</small></div>
        <div v-else-if="chosen === side && game.me" class="stall-intention"><CharacterPawn :avatar-id="game.me.avatarId" :color="game.me.color" /><small>{{ activity.submitted.includes(game.me.id) ? '已秘密订位' : '你想来这里' }} · 仅自己可见</small></div>
      </div></div>
      <div v-if="!revealed" class="market-waiting"><small>夜市入口 · 其他人的去向暂时保密</small><div><span v-for="player in waiting" :key="player.id"><CharacterPawn :avatar-id="player.avatarId" :color="player.color" /><small>{{ player.name }}</small><b>{{ activity.submitted.includes(player.id) ? '已订位' : '犹豫中' }}</b></span></div></div>
    </div>
    <button v-if="activity.stage === 'select'" class="button primary market-confirm" :disabled="!enabled" @click="emit('input', { selection: chosen })">{{ enabled ? `就去${stalls[chosen]?.name} · 秘密锁定` : game.me && activity.submitted.includes(game.me.id) ? '已订位，等待大家入场' : '等待参与者选摊' }}</button>
    <p v-if="activity.stage === 'minority_reveal'" class="activity-stage-caption">摊主正在点名，看看大家都去了哪边…</p>
  </div>
</template>
