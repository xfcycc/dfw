<script setup lang="ts">
import { computed } from 'vue';
import { imageUrl } from '../image-assets';
const props = defineProps<{ count: number; limit?: number | null; page?: number }>();
/** 输入当前手牌数量和服务端上限，保留原六个卡位中未占用的位置；可选页码仅展示该页三个卡位；超限整理时不遮住已有手牌。 */
const slots = computed(() => Array.from({ length: Math.max(0, 6 - props.count) }, (_, index) => {
  const position = props.count + index + 1;
  return { position, sealed: position > (props.limit ?? 6) };
}).filter(slot => props.page === undefined || Math.floor((slot.position - 1) / 3) === props.page));
</script>

<template>
  <span v-for="slot in slots" :key="slot.position" class="empty-hand-slot" :class="{ 'sealed-hand-slot': slot.sealed }" :aria-hidden="slot.sealed ? undefined : true" :role="slot.sealed ? 'img' : undefined" :aria-label="slot.sealed ? `第 ${slot.position} 个手牌位被尸鬼封尽封禁` : undefined" :title="slot.sealed ? '尸鬼封尽：本局该手牌位已封禁' : undefined">
    <template v-if="slot.sealed"><img class="sealed-slot-chains" :src="imageUrl('/images/cards/sealed-hand-slot.webp')" alt="" draggable="false" /><span class="sealed-slot-caption">已封禁</span></template>
    <template v-else><span>＋</span>空位</template>
  </span>
</template>

<style scoped>
.dashboard-card-rail .empty-hand-slot.sealed-hand-slot { position:relative; overflow:hidden; border:2px solid #a89aac; border-radius:14px; background:radial-gradient(ellipse at center,#e4dcf0,#f5eef5 75%); color:#715d7c; }
.sealed-slot-chains { position:absolute; inset:5px 3px 24px; width:calc(100% - 6px); height:calc(100% - 29px); object-fit:contain; pointer-events:none; }
.empty-hand-slot.sealed-hand-slot > .sealed-slot-caption { position:absolute; bottom:7px; font-size:11px; font-weight:800; letter-spacing:2px; }
</style>
