<script setup lang="ts">
import { computed } from 'vue';
import type { PublicRoomState, Tile } from '../../shared/types';
const props = defineProps<{ room: PublicRoomState; meId?: string; canRemove?: boolean; busy?: boolean }>();
const emit = defineEmits<{ select: [tile: Tile]; remove: [playerId: string] }>();
const currentId = computed(() => props.room.turnOrder[props.room.turnIndex]);
const standings = computed(() => {
  const properties = new Map<string, Tile[]>();
  for (const tile of props.room.board) {
    if (!tile.ownerId) continue;
    const tiles = properties.get(tile.ownerId) ?? [];
    tiles.push(tile); properties.set(tile.ownerId, tiles);
  }
  return props.room.players.map(player => {
    const tiles = properties.get(player.id) ?? [];
    return { player, tiles, levels: [0, 1, 2, 3].map(level => tiles.filter(tile => tile.level === level).length), mortgaged: tiles.filter(tile => tile.mortgaged).length };
  }).sort((a, b) => Number(a.player.bankrupt) - Number(b.player.bankrupt) || (a.player.assetRank ?? Infinity) - (b.player.assetRank ?? Infinity) || (b.player.netWorth ?? b.player.cash) - (a.player.netWorth ?? a.player.cash));
});
</script>
<template>
  <section class="panel leaderboard-panel" aria-label="公开排行榜">
    <div class="section-heading"><h2>🏆 公开排行榜</h2><span>{{ room.players.filter(player => !player.bankrupt).length }} 人竞争中</span></div>
    <p class="leaderboard-hint">按净资产排名，同额并列 · 点击房产展开</p>
    <article v-for="{ player, tiles, levels, mortgaged } in standings" :key="player.id" class="standing-card" :class="{ current: player.id === currentId && room.status === 'playing', bankrupt: player.bankrupt }" :style="{ '--player-color': player.color }">
      <div class="standing-heading"><span class="standing-rank">{{ player.assetRank ?? '—' }}</span><span class="avatar" :style="{ background: player.color }">{{ player.name.slice(0, 1) }}</span><div><strong>{{ player.name }}</strong><small>{{ player.bankrupt ? '已清算' : player.id === currentId && room.status === 'playing' ? '正在行动' : '等待回合' }}{{ player.id === meId ? ' · 你' : '' }}{{ player.id === room.hostId ? ' · 房主' : '' }}</small></div></div>
      <dl class="standing-money"><div><dt>净资产</dt><dd>{{ player.netWorth === undefined ? '—' : `¥${player.netWorth.toLocaleString()}` }}</dd></div><div><dt>现金</dt><dd>¥{{ player.cash.toLocaleString() }}</dd></div><div><dt>卡牌</dt><dd>{{ player.cardCount == null ? '—' : `${player.cardCount} 张` }}</dd></div></dl>
      <div class="standing-status"><span v-if="player.trapImmunityTurns" class="positive">🛡 陷阱免疫 {{ player.trapImmunityTurns }} 回合</span><span v-if="player.nextPaymentWaiver" class="positive">✓ 下次费用免单</span><span v-if="player.nextRentDouble" class="negative">↗ 下次租金翻倍</span><span v-if="player.skipTurns || player.jailed" class="negative">Ⅱ {{ player.jailed ? '监狱' : '休息' }}{{ player.skipTurns ? ` ${player.skipTurns} 次` : '' }}</span><span v-if="!player.online" class="negative">离线</span><span v-if="player.bankrupt" class="neutral">已退出竞争</span><span v-else-if="!player.trapImmunityTurns && !player.nextPaymentWaiver && !player.nextRentDouble && !player.skipTurns && !player.jailed" class="neutral">无增益或负面效果</span></div>
      <details class="standing-properties"><summary><strong>⌂ 房产 {{ tiles.length }} 处</strong><span>{{ mortgaged ? `${mortgaged} 处抵押 · ` : '' }}查看地块 ⌄</span></summary><div class="property-levels"><span v-for="(count, level) in levels" :key="level" :class="`level-${level}`">{{ level }}级 <b>{{ count }}</b></span></div><p v-if="!tiles.length" class="muted">还没有地产，下一步就有机会。</p><div class="standing-tile-list"><button v-for="tile in tiles" :key="tile.id" @click="emit('select', tile)"><span>{{ tile.name }}</span><small>{{ tile.level }}级{{ tile.mortgaged ? ' · 已抵押' : '' }}{{ tile.barrier ? ' · 路障' : '' }} ↗</small></button></div></details>
      <button v-if="canRemove && !player.online && player.id !== meId && !player.bankrupt && room.status === 'playing'" class="remove-player" :disabled="busy" @click="emit('remove', player.id)">移出并清算</button>
    </article>
    <p class="leaderboard-hint">卡面仅本人可见。旧存档未记录的数量显示为 —。</p>
  </section>
</template>
