<script setup lang="ts">
import { imageUrl } from '../image-assets';
import { computed } from 'vue';
const props = defineProps<{ kind: 'turn' | 'rank'; rank?: number }>();
const source = imageUrl('/images/actions/turn-ranks.png');
const ranks = ['240 242 340 260', '956 284 368 230', '228 524 360 234', '950 526 378 232', '234 768 350 214', '952 770 366 220'];
/** 输入文字用途与 1～6 名排名，返回透明图集中对应文字的展示范围。 */
const frame = computed(() => props.kind === 'turn' ? '60 38 720 212' : ranks[(props.rank ?? 1) - 1] ?? ranks[0]);
</script>
<template>
  <svg class="chibi-lettering" :class="kind" :viewBox="frame" role="img" :aria-label="kind === 'turn' ? '你的回合' : `第${rank}名`" focusable="false">
    <image :href="source" width="1536" height="1024" />
  </svg>
</template>
<style scoped>
.chibi-lettering { display: block; flex-shrink: 0; overflow: hidden; pointer-events: none; }
.chibi-lettering.turn { width: 176px; max-width: 100%; height: 52px; }
.chibi-lettering.rank { width: 100px; height: 34px; }
</style>
