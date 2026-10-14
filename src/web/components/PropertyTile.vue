<script setup lang="ts">
import type { Tile } from '../../shared/types';
import PropertyArt from './PropertyArt.vue';
import PropertyGrade from './PropertyGrade.vue';
defineProps<{ tile: Tile; showPrice?: boolean }>();
const emit = defineEmits<{ select: [tile: Tile] }>();
</script>
<template>
  <button type="button" class="property-tile-card" :aria-label="`${tile.name}，${tile.level}级${tile.mortgaged ? '，已抵押' : ''}${tile.barrier ? '，有路障' : ''}，查看地产详情`" @click="emit('select', tile)">
    <PropertyArt :level="tile.level" :mortgaged="tile.mortgaged" />
    <strong>{{ tile.name }}</strong>
    <PropertyGrade :level="tile.level" />
    <span v-if="tile.mortgaged || tile.barrier" class="property-tile-status">{{ tile.mortgaged ? '已抵押' : '' }}{{ tile.mortgaged && tile.barrier ? ' · ' : '' }}{{ tile.barrier ? '路障' : '' }}</span>
    <span v-if="showPrice" class="property-tile-value">¥{{ tile.price.toLocaleString() }}</span>
  </button>
</template>
<style scoped>
.property-tile-card { display: flex; flex-direction: column; align-items: center; gap: 7px; min-width: 0; padding: 8px 6px 12px; border: 2px solid #d9caa5; border-radius: 16px; background: linear-gradient(#fffef8, #f7f1df); color: #4e6347; box-shadow: inset 0 0 0 3px #fffdf5, 0 2px 0 #c5b69035; font-family: inherit; text-align: center; cursor: pointer; }
.property-tile-card > strong { font-size: 15px; font-weight: 800; line-height: 1.4; overflow-wrap: anywhere; }
.property-tile-card :deep(.property-grade) { padding: 3px 7px; font-size: 12px; }
.property-tile-status { color: #966144; font-size: 12px; line-height: 1.4; }
.property-tile-value { color: #557746; font-size: 16px; font-weight: 800; font-variant-numeric: tabular-nums; }
.property-tile-card:hover { border-color: #a4bc8c; background: #f4f8e9; }
.property-tile-card:focus-visible { outline: 3px solid #809d69; outline-offset: 2px; }
</style>
