<script setup lang="ts">
import { computed } from 'vue';
import PlayerText from './PlayerText.vue';
import CharacterPawn from './CharacterPawn.vue';
import AssetStatIcon from './AssetStatIcon.vue';
import PropertyTile from './PropertyTile.vue';
import type { PublicRoomState, Tile } from '../../shared/types';
const props = defineProps<{ room: PublicRoomState; meId?: string; canRemove?: boolean; busy?: boolean; targeting?: boolean; targetHint?: string; eligiblePlayers?: string[]; selectedTarget?: string; compact?: boolean }>();
const emit = defineEmits<{ select: [tile: Tile]; remove: [playerId: string]; selectPlayer: [playerId: string] }>();
const currentId = computed(() => props.room.turnOrder[props.room.turnIndex]);
/** 输入公开房间的玩家和地块，按现有排名返回各玩家及其地产、抵押数量。 */
const standings = computed(() => {
  const properties = new Map<string, Tile[]>();
  for (const tile of props.room.board) {
    if (!tile.ownerId) continue;
    const tiles = properties.get(tile.ownerId) ?? [];
    tiles.push(tile); properties.set(tile.ownerId, tiles);
  }
  return props.room.players.map(player => {
    const tiles = properties.get(player.id) ?? [];
    return { player, tiles, mortgaged: tiles.filter(tile => tile.mortgaged).length };
  }).sort((a, b) => Number(a.player.bankrupt) - Number(b.player.bankrupt) || (a.player.assetRank ?? Infinity) - (b.player.assetRank ?? Infinity) || (b.player.netWorth ?? b.player.cash) - (a.player.netWorth ?? a.player.cash));
});
</script>
<template>
  <section class="leaderboard-panel" :class="{ compact }" aria-label="公开排行榜">
    <p v-if="targetHint" class="target-map-hint" role="status">{{ targetHint }}</p>
    <article v-for="{ player, tiles, mortgaged } in standings" :key="player.id" class="ranking-row" :class="{ current: player.id === currentId && room.status === 'playing', bankrupt: player.bankrupt, 'target-eligible': targeting && eligiblePlayers?.includes(player.id), 'target-selected': targeting && selectedTarget === player.id }" :style="{ '--player-color': player.color }">
      <div class="ranking-header">
        <button class="ranking-identity" :disabled="!targeting" :aria-label="`选择玩家 ${player.name}${player.id === meId ? '（我）' : ''}`" :aria-pressed="targeting ? selectedTarget === player.id : undefined" @click="emit('selectPlayer', player.id)"><span class="ranking-number">{{ player.assetRank ?? '—' }}</span><CharacterPawn :avatar-id="player.avatarId" :color="player.color" /><span><strong><PlayerText :text="player.name" :players="room.players" /></strong><small>{{ player.id === meId ? '我 · ' : '' }}{{ player.bankrupt ? '已清算' : player.id === currentId && room.status === 'playing' ? '正在行动' : '等待回合' }}</small></span></button>
        <div class="ranking-stats">
          <span class="ranking-stat cash" :aria-label="`现金 ¥${player.cash.toLocaleString()}`" title="现金"><AssetStatIcon kind="cash" /><b>¥{{ player.cash.toLocaleString() }}</b></span>
          <span class="ranking-stat worth" :aria-label="`净资产 ${player.netWorth === undefined ? '未知' : `¥${player.netWorth.toLocaleString()}`}`" title="净资产"><AssetStatIcon kind="worth" /><b>{{ player.netWorth === undefined ? '—' : `¥${player.netWorth.toLocaleString()}` }}</b></span>
          <span class="ranking-stat cards" :aria-label="`手牌 ${player.cardCount == null ? '未知' : player.cardCount}`" title="手牌"><AssetStatIcon kind="cards" /><b>{{ player.cardCount == null ? '—' : player.cardCount }}</b></span>
          <span class="ranking-stat property" :aria-label="`地产 ${tiles.length}处`" title="地产"><AssetStatIcon kind="property" /><b>{{ tiles.length }}</b></span>
        </div>
      </div>
      <div class="ranking-status"><span v-if="player.trapImmunityTurns" class="buff">免疫 {{ player.trapImmunityTurns }} 回合</span><span v-if="player.nextPaymentWaiver" class="buff">下次免单</span><span v-if="player.nextRentDouble" class="buff">租金翻倍</span><span v-if="player.skipTurns || player.jailed" class="debuff">{{ player.jailed ? '监禁' : '休息' }} {{ player.skipTurns }} 次</span><span v-if="!player.online">离线</span></div>
      <details v-if="tiles.length" class="ranking-properties"><summary>{{ mortgaged ? `${mortgaged} 处抵押 · ` : '' }}查看地产</summary><div class="ranking-tile-list"><PropertyTile v-for="tile in tiles" :key="tile.id" :tile="tile" @select="emit('select', $event)" /></div></details>
      <button v-if="canRemove && !player.online && player.id !== meId && !player.bankrupt && room.status === 'playing'" class="remove-player" :disabled="busy" @click="emit('remove', player.id)">移出并清算</button>
    </article>
  </section>
</template>
