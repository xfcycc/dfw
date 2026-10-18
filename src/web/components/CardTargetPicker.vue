<script setup lang="ts">
import { imageUrl } from '../image-assets';
import { computed, ref, watch } from 'vue';
import CharacterPawn from './CharacterPawn.vue';
import PlayerText from './PlayerText.vue';
import PropertyArt from './PropertyArt.vue';
import { activityArt } from '../activity-art';
import type { Tile } from '../../shared/types';
interface CardTargetOption { id: string; name: string; detail: string; avatarId?: string; color?: string; reason?: string; tile?: Tile }
const props = defineProps<{ options: CardTargetOption[]; selected: string; secondSelected?: string; playerTarget?: boolean; twoPlayers?: boolean; meId?: string; disabled?: boolean }>();
const emit = defineEmits<{ select: [id: string, slot: 1 | 2]; map: [] }>();
const query = ref('');
const ownership = ref('all');
const playerSlot = ref<1 | 2>(1);
const available = computed(() => props.options.filter(option => !option.reason));
const ownershipFilters = computed(() => [
  { id: 'all', label: '全部', count: available.value.length },
  { id: 'mine', label: '我的', count: available.value.filter(option => option.tile?.ownerId === props.meId).length },
  { id: 'others', label: '其他玩家', count: available.value.filter(option => option.tile?.ownerId && option.tile.ownerId !== props.meId).length },
]);
/** 输入当前可选归属分组，分组失效时恢复全部，避免筛选入口收起后留下空列表。 */
watch(ownershipFilters, filters => { if (!filters.find(filter => filter.id === ownership.value)?.count) ownership.value = 'all'; });
const filtered = computed(() => {
  if (props.playerTarget) return props.options;
  const keyword = query.value.trim().toLocaleLowerCase();
  return available.value.filter(option => {
    const matchesOwner = ownership.value === 'all' || ownership.value === 'mine' && option.tile?.ownerId === props.meId || ownership.value === 'others' && option.tile?.ownerId && option.tile.ownerId !== props.meId;
    return matchesOwner && (!keyword || `${option.name} ${option.detail}`.toLocaleLowerCase().includes(keyword));
  });
});
/** 输入可选目标，更新当前玩家槽位或地块；第一位选好后切到第二位，无返回值。 */
function select(id: string): void {
  emit('select', id, props.twoPlayers ? playerSlot.value : 1);
  if (props.twoPlayers && playerSlot.value === 1 && !props.secondSelected) playerSlot.value = 2;
}
/** 输入玩家标识，返回当前双人选择中占用的槽位；未选中返回空字符串。 */
function selectedSlot(id: string): string { return props.twoPlayers ? props.selected === id ? '第一位' : props.secondSelected === id ? '第二位' : '' : props.selected === id ? '✓' : ''; }
/** 输入目标标识，返回是否已占用另一个玩家槽位，防止重复选择。 */
function inOtherSlot(id: string): boolean { return !!props.twoPlayers && id === (playerSlot.value === 1 ? props.secondSelected : props.selected); }
</script>
<template>
  <div class="target-selector">
    <div v-if="!playerTarget" class="target-search-tools">
      <div class="target-list-heading"><span>可选地块 · {{ available.length }}</span><button type="button" class="button secondary small" :disabled="disabled || !available.length" @click="emit('map')">地图点选 ↗</button></div>
      <label class="field target-search">搜索地块<input v-model="query" type="search" placeholder="地块名称、编号或所有者" autocomplete="off" :disabled="disabled" /></label>
      <div v-if="meId && ownershipFilters.some(filter => filter.id === 'mine' && filter.count) && available.length > ownershipFilters[1]!.count" class="target-owner-filters" role="group" aria-label="按地块归属筛选"><button v-for="filter in ownershipFilters" :key="filter.id" type="button" :aria-pressed="ownership === filter.id" :disabled="disabled" @click="ownership = filter.id">{{ filter.label }} {{ filter.count }}</button></div>
      <p class="target-result-count" role="status">{{ filtered.length ? `显示 ${filtered.length} 处可选地块` : available.length ? '没有匹配的可选地块，试试其他关键词或归属。' : '本次没有符合条件的地块。' }}</p>
    </div>
    <div v-if="twoPlayers" class="target-player-slots" role="group" aria-label="选择换位玩家">
      <button v-for="slot in ([1, 2] as const)" :key="slot" type="button" :aria-pressed="playerSlot === slot" :disabled="disabled" @click="playerSlot = slot"><small>{{ slot === 1 ? '第一位玩家' : '第二位玩家' }}</small><strong><PlayerText :text="options.find(option => option.id === (slot === 1 ? selected : secondSelected))?.name ?? '点击头像选择'" /></strong></button>
    </div>
    <div class="card-target-picker" :class="{ 'player-targets': playerTarget }" role="group" :aria-label="twoPlayers ? `选择第${playerSlot === 1 ? '一' : '二'}位玩家` : '选择卡牌目标'">
    <button v-for="option in filtered" :key="option.id" type="button" class="target-option" :class="{ selected: !!selectedSlot(option.id), unavailable: !!option.reason }" :disabled="disabled || !!option.reason || inOtherSlot(option.id)" :aria-pressed="!!selectedSlot(option.id)" :aria-label="`选择目标 ${option.name}，${option.detail}${option.tile?.type === 'property' ? `，地价 ¥${option.tile.price.toLocaleString()}` : ''}${option.reason ? `，${option.reason}` : ''}${selectedSlot(option.id) ? `，已选${selectedSlot(option.id)}` : ''}`" @click="select(option.id)">
      <CharacterPawn v-if="playerTarget" :avatar-id="option.avatarId" :color="option.color" />
      <PropertyArt v-else-if="option.tile?.type === 'property'" :level="option.tile.level" :mortgaged="option.tile.mortgaged" />
      <img v-else-if="option.tile" class="target-tile-art" :src="option.tile.type === 'activity' && option.tile.activityKind ? activityArt(`${option.tile.activityKind}-tile`) : imageUrl(`/images/tiles/${option.tile.type}.png`)" alt="" draggable="false" />
      <span v-else class="target-tile-symbol">⌂</span>
      <span class="target-option-copy"><strong><PlayerText :text="option.name" /></strong><small><PlayerText :text="option.detail" /></small><span v-if="option.tile?.type === 'property'" class="target-option-price">地价 ¥{{ option.tile.price.toLocaleString() }}</span><small v-if="option.reason" class="target-option-reason">{{ option.reason }}</small></span><span v-if="selectedSlot(option.id)" class="target-check" aria-hidden="true">{{ selectedSlot(option.id) }}</span>
    </button>
    <p v-if="playerTarget && !options.length" class="muted">本次没有可选玩家。</p>
    </div>
  </div>
</template>
