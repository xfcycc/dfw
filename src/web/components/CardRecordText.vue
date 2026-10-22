<script setup lang="ts">
import { computed, ref } from 'vue';
import type { ChatReference, ContentResult } from '../../shared/types';
import ChatText from './ChatText.vue';
import CardReveal from './CardReveal.vue';
const props = defineProps<{ text: string; content?: ContentResult | null; actorId?: string | null; actorName?: string | null; players?: Array<{ id?: string; name: string; color: string }>; references?: ChatReference[]; interactive?: boolean }>();
const emit = defineEmits<{ tile: [id: number] }>();
const viewing = ref(false);
/** 输入原记录的玩家身份，优先返回记录时的名称，再匹配原记录提及的玩家。 */
const actor = computed(() => props.actorName || props.players?.find(player => props.actorId && player.id === props.actorId)?.name || '玩家');
/** 输入记录文案及原卡牌名称，返回精确名称位置；缺少原始卡面时不生成回看入口。 */
const nameStart = computed(() => props.content?.name ? props.text.indexOf(props.content.name, Math.max(0, props.text.indexOf('「'))) : -1);
/** 输入正文片段的起止偏移，返回平移后的引用，供卡牌标题前后独立渲染。 */
function referencesFor(start: number, end: number): ChatReference[] {
  return (props.references ?? []).filter(reference => reference.start >= start && reference.end <= end)
    .map(reference => ({ ...reference, start: reference.start - start, end: reference.end - start }));
}
</script>
<template>
  <template v-if="content?.name && nameStart >= 0">
    <ChatText :text="text.slice(0, nameStart)" :references="referencesFor(0, nameStart)" :players="players" :interactive="interactive" @tile="emit('tile', $event)" /><button type="button" class="card-record-link" :aria-label="`全屏查看原卡牌：${content.name}`" @click="viewing = true">{{ content.name }}</button><ChatText :text="text.slice(nameStart + content.name.length)" :references="referencesFor(nameStart + content.name.length, text.length)" :players="players" :interactive="interactive" @tile="emit('tile', $event)" />
    <CardReveal v-if="viewing" :preview="{ key: content.id, content, actor, mode: content.kind || content.pool === 'card' ? 'play' : 'draw' }" @close="viewing = false" />
  </template>
  <ChatText v-else :text="text" :references="references" :players="players" :interactive="interactive" @tile="emit('tile', $event)" />
</template>
<style scoped>
.card-record-link { display: inline; padding: 0; border: 0; background: none; color: #8a653b; font: inherit; font-weight: 700; text-decoration: underline; text-decoration-thickness: 1px; text-underline-offset: 3px; cursor: pointer; }
.card-record-link:hover { color: #a26332; }
.card-record-link:focus-visible { outline: 2px solid #b48c5b; outline-offset: 3px; border-radius: 3px; }
</style>
