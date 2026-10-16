<script setup lang="ts">
import { computed, ref } from 'vue';
import PlayerText from './PlayerText.vue';
import CharacterPawn from './CharacterPawn.vue';
import AssetStatIcon from './AssetStatIcon.vue';
import PropertyTile from './PropertyTile.vue';
import Modal from './Modal.vue';
import type { PublicRoomState, Tile } from '../../shared/types';
const props = defineProps<{ room: PublicRoomState; meId?: string; canRemove?: boolean; busy?: boolean; targeting?: boolean; targetHint?: string; eligiblePlayers?: string[]; selectedTarget?: string; compact?: boolean }>();
const emit = defineEmits<{ select: [tile: Tile]; remove: [playerId: string]; selectPlayer: [playerId: string] }>();
const propertyPlayerId = ref<string | null>(null);
const propertyStanding = computed(() => standings.value.find(({ player }) => player.id === propertyPlayerId.value));
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
/** 输入头像对应的玩家 ID，选目标时沿用选人事件，否则打开该玩家的地产弹窗，无返回值。 */
function selectAvatar(playerId: string): void {
  if (props.targeting) emit('selectPlayer', playerId);
  else propertyPlayerId.value = playerId;
}
/** 输入选中的地产，收起玩家地产列表并交由原入口查看详情，无返回值。 */
function selectProperty(tile: Tile): void {
  propertyPlayerId.value = null;
  emit('select', tile);
}
</script>
<template>
  <section class="leaderboard-panel" :class="{ compact }" aria-label="公开排行榜">
    <p v-if="targetHint" class="target-map-hint" role="status">{{ targetHint }}</p>
    <article v-for="{ player, tiles } in standings" :key="player.id" class="ranking-row" :class="{ current: player.id === currentId && room.status === 'playing', bankrupt: player.bankrupt, 'target-eligible': targeting && eligiblePlayers?.includes(player.id), 'target-selected': targeting && selectedTarget === player.id }" :style="{ '--player-color': player.color }">
      <div class="ranking-header">
        <div class="ranking-identity" @click="targeting && emit('selectPlayer', player.id)">
          <span class="ranking-number">{{ player.assetRank ?? '—' }}</span>
          <button type="button" class="ranking-avatar" :aria-label="targeting ? `选择玩家 ${player.name}${player.id === meId ? '（我）' : ''}` : `查看 ${player.name} 的地产`" :aria-pressed="targeting ? selectedTarget === player.id : undefined" :title="targeting ? '选择玩家' : '点击头像查看地产'" @click.stop="selectAvatar(player.id)"><CharacterPawn :avatar-id="player.avatarId" :color="player.color" /></button>
          <div class="ranking-player"><div class="ranking-name-line"><strong><PlayerText :text="player.name" :players="room.players" /></strong><span v-if="!player.online" class="ranking-offline">离线</span><button v-if="canRemove && !player.online && player.id !== meId && !player.bankrupt && room.status === 'playing'" type="button" class="remove-player" :disabled="busy" @click.stop="emit('remove', player.id)">移出并清算</button></div><small>{{ player.id === meId ? '我 · ' : '' }}{{ player.bankrupt ? '已清算' : player.id === currentId && room.status === 'playing' ? '正在行动' : '等待回合' }}</small></div>
        </div>
        <div class="ranking-stats">
          <span class="ranking-stat cash" :aria-label="`现金 ¥${player.cash.toLocaleString()}`" title="现金"><AssetStatIcon kind="cash" /><b>¥{{ player.cash.toLocaleString() }}</b></span>
          <span class="ranking-stat worth" :aria-label="`净资产 ${player.netWorth === undefined ? '未知' : `¥${player.netWorth.toLocaleString()}`}`" title="净资产"><AssetStatIcon kind="worth" /><b>{{ player.netWorth === undefined ? '—' : `¥${player.netWorth.toLocaleString()}` }}</b></span>
          <span class="ranking-stat cards" :aria-label="`手牌 ${player.cardCount == null ? '未知' : player.cardCount}`" title="手牌"><AssetStatIcon kind="cards" /><b>{{ player.cardCount == null ? '—' : player.cardCount }}</b></span>
          <span class="ranking-stat property" :aria-label="`地产 ${tiles.length}处`" title="地产"><AssetStatIcon kind="property" /><b>{{ tiles.length }}</b></span>
        </div>
      </div>
      <div class="ranking-status"><span v-if="player.trapImmunityTurns" class="buff">免疫 {{ player.trapImmunityTurns }} 回合</span><span v-if="player.nextPaymentWaiver" class="buff">下次免单</span><span v-if="player.nextRentDouble" class="buff">租金翻倍</span><span v-if="player.nextMoveDouble" class="buff" title="下一次最终移动距离×2，传送与换位不消耗">塔塔开 · 移动×2</span><span v-if="player.skipTurns || player.jailed" class="debuff">{{ player.jailed ? '监禁' : '休息' }} {{ player.skipTurns }} 次</span></div>
    </article>
  </section>
  <Modal v-if="propertyStanding" panel-class="modern-modal player-properties-modal" :title="`${propertyStanding.player.name} 的地产`" @close="propertyPlayerId = null">
    <p class="muted">{{ propertyStanding.tiles.length }} 处地产<span v-if="propertyStanding.mortgaged"> · {{ propertyStanding.mortgaged }} 处抵押</span></p>
    <div v-if="propertyStanding.tiles.length" class="ranking-tile-list"><PropertyTile v-for="tile in propertyStanding.tiles" :key="tile.id" :tile="tile" @select="selectProperty" /></div>
    <p v-else class="muted">暂无地产</p>
  </Modal>
</template>
