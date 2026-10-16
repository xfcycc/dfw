<script setup lang="ts">
import { imageUrl } from '../image-assets';
import { computed } from 'vue';
const props = withDefaults(defineProps<{ ornament?: 'left' | 'right'; variant?: 'news' | 'chat' | 'ranking' | 'workbench' | 'cards' | 'notice' }>(), { ornament: 'left', variant: 'news' });
const source = imageUrl('/images/panels/panel-theme-frames.png');
const people = imageUrl('/images/panels/panel-theme-people.png');
const themes = {
  news: { frame: [104, 36, 376, 444], sideY: 104, person: 0 },
  chat: { frame: [590, 34, 374, 446], sideY: 104, person: 1 },
  ranking: { frame: [1042, 32, 392, 448], sideY: 104, person: 2 },
  workbench: { frame: [76, 532, 392, 444], sideY: 596, person: 3 },
  cards: { frame: [596, 532, 374, 444], sideY: 600, person: 4 },
  notice: { frame: [1054, 532, 384, 444], sideY: 592, person: 5 },
} as const;
// 四角保持比例，直边取无人物的纹理段延展；独立人物图集只显示在左右两侧。
const pieces = computed(() => {
  const { frame: [x, y, width, height], sideY } = themes[props.variant];
  const corner = 72;
  return [
    { area: '1 / 1', box: `${x} ${y} ${corner} ${corner}` },
    { area: '1 / 2', box: `${x + corner} ${y} ${width - corner * 2} ${corner}` },
    { area: '1 / 3', box: `${x + width - corner} ${y} ${corner} ${corner}` },
    { area: '2 / 1', box: `${x} ${sideY} ${corner} 12` },
    { area: '2 / 3', box: `${x + width - corner} ${sideY} ${corner} 12` },
    { area: '3 / 1', box: `${x} ${y + height - corner} ${corner} ${corner}` },
    { area: '3 / 2', box: `${x + corner} ${y + height - corner} ${width - corner * 2} ${corner}` },
    { area: '3 / 3', box: `${x + width - corner} ${y + height - corner} ${corner} ${corner}` },
  ];
});
const personBox = computed(() => { const index = themes[props.variant].person; return `${index % 3 * 512} ${Math.floor(index / 3) * 512} 512 512`; });
</script>
<template>
  <span class="panel-frame" :class="`panel-frame-${variant}`" aria-hidden="true">
    <svg v-for="piece in pieces" :key="piece.area" class="panel-frame-piece" :style="{ gridArea: piece.area }" :viewBox="piece.box" preserveAspectRatio="none" focusable="false"><image :href="source" width="1536" height="1024" /></svg>
    <svg class="panel-frame-person" :class="`panel-frame-person-${ornament}`" :viewBox="personBox" focusable="false"><image :href="people" width="1536" height="1024" /></svg>
  </span>
</template>
<style scoped>
.panel-frame { position: absolute; inset: 0; z-index: 3; display: grid; grid-template-columns: 24px minmax(0, 1fr) 24px; grid-template-rows: 24px minmax(0, 1fr) 24px; pointer-events: none; }
.panel-frame-piece { display: block; width: 100%; height: 100%; min-width: 0; min-height: 0; overflow: hidden; }
.panel-frame-person { position: absolute; top: 24px; display: block; width: 60px; aspect-ratio: 1; overflow: hidden; }
.panel-frame-person-left { left: -42px; }
.panel-frame-person-right { right: -42px; }
.panel-frame-notice { grid-template-columns: 20px minmax(0, 1fr) 20px; grid-template-rows: 20px minmax(0, 1fr) 20px; }
.panel-frame-notice .panel-frame-person { top: 50%; width: 48px; margin-top: -24px; }
.panel-frame-notice .panel-frame-person-right { right: -32px; }
</style>
