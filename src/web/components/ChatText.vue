<script setup lang="ts">
import { computed } from 'vue';
import type { ChatReference, PlayerMention } from '../../shared/types';
import PlayerText from './PlayerText.vue';
const props = defineProps<{ text: string; references?: ChatReference[]; players?: PlayerMention[]; interactive?: boolean }>();
const emit = defineEmits<{ tile: [id: number] }>();
/** 输入冻结的引用偏移，将消息拆成普通文字与下划线引用；旧消息保持原有玩家着色。 */
const parts = computed(() => {
  const result: { text: string; reference?: ChatReference }[] = []; let offset = 0;
  for (const reference of props.references ?? []) {
    if (reference.start < offset || reference.end <= reference.start || reference.end > props.text.length) continue;
    if (reference.start > offset) result.push({ text: props.text.slice(offset, reference.start) });
    result.push({ text: props.text.slice(reference.start, reference.end), reference }); offset = reference.end;
  }
  if (offset < props.text.length) result.push({ text: props.text.slice(offset) });
  return result;
});
</script>
<template><span class="chat-text"><template v-for="(part, index) in parts" :key="index"><button v-if="part.reference?.tileId != null && interactive" type="button" class="chat-reference chat-tile-reference" :title="`在地图上定位 ${part.text}`" @click="emit('tile', part.reference.tileId)">{{ part.text }}</button><span v-else-if="part.reference" class="chat-reference" :style="{ color: part.reference.color || undefined }">{{ part.text }}</span><PlayerText v-else :text="part.text" :players="players" /></template></span></template>
<style scoped>
.chat-text { white-space: pre-wrap; }
.chat-reference { display: inline; padding: 0; border: 0; border-radius: 0; background: transparent; color: #916122; font: inherit; font-weight: 700; text-decoration: underline; text-underline-offset: 3px; overflow-wrap: anywhere; }
.chat-tile-reference { cursor: pointer; }
.chat-tile-reference:hover, .chat-tile-reference:focus-visible { color: #b66d13; background: #ffe5a4; outline: 2px solid #dfae59; border-radius: 3px; }
</style>
