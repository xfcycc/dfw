<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import type { ChatReference, PublicRoomState } from '../../shared/types';
import ActionIcon from './ActionIcon.vue';
import PanelFrame from './PanelFrame.vue';
const props = defineProps<{ room: PublicRoomState; disabled: boolean; placeholder: string; send: (message: string, references: ChatReference[]) => Promise<boolean> }>();
const message = ref('');
const references = ref<ChatReference[]>([]);
const input = ref<HTMLTextAreaElement>();
const menu = ref<HTMLElement>();
const caret = ref(0);
const focused = ref(false);
const composing = ref(false);
const dismissed = ref(false);
const selected = ref(0);
const sending = ref(false);
const menuStyle = ref({ left: '0px', bottom: '0px', width: '0px', maxHeight: '240px' });
const query = computed(() => {
  if (!focused.value || composing.value || dismissed.value || props.disabled || sending.value) return null;
  const prefix = message.value.slice(0, caret.value); const start = prefix.lastIndexOf('@');
  if (start < 0 || references.value.some(reference => start >= reference.start && start < reference.end)) return null;
  const text = prefix.slice(start + 1);
  return text.length <= 40 && !/[\n@]|\s$/.test(text) ? { start, text: text.toLocaleLowerCase() } : null;
});
/** 输入当前查询，返回玩家与地块候选；玩家按席位区分重名，地块按编号区分。 */
const suggestions = computed(() => {
  if (!query.value) return [];
  return [
    ...props.room.players.map((player, index) => ({ key: player.id, label: `@${player.name}`, detail: `玩家 · ${index + 1}号席位`, color: player.color, playerId: player.id, tileId: undefined as number | undefined, search: `${player.name} 玩家 ${index + 1}` })),
    ...props.room.board.map(tile => ({ key: `tile-${tile.id}`, label: `@${tile.name}（${tile.id}号）`, detail: `地块 · ${tile.id}号`, color: '#9a692b', playerId: undefined as string | undefined, tileId: tile.id, search: `${tile.name} ${tile.id} 地块` })),
  ].filter(option => option.search.toLocaleLowerCase().includes(query.value!.text));
});
/** 无入参，按输入框位置更新顶层联想菜单；手机面板裁切不影响候选可见性，无返回值。 */
function positionMenu(): void {
  if (!input.value) return;
  const bounds = input.value.closest('.chat-composer')!.getBoundingClientRect();
  menuStyle.value = { left: `${bounds.left}px`, bottom: `${window.innerHeight - bounds.top + 6}px`, width: `${bounds.width}px`, maxHeight: `${Math.max(60, Math.min(240, bounds.top - 12))}px` };
}
/** 无入参，记录光标位置并重新允许联想，无返回值。 */
function updateCaret(): void {
  const position = input.value?.selectionStart ?? 0;
  if (position !== caret.value) dismissed.value = false;
  caret.value = position; positionMenu();
}
/** 输入原生编辑事件，移动未被编辑的引用偏移，删除与编辑范围相交的引用，无返回值。 */
function edit(event: Event): void {
  const next = (event.target as HTMLTextAreaElement).value; const before = message.value;
  let start = 0; let oldEnd = before.length; let newEnd = next.length;
  while (start < oldEnd && start < newEnd && before[start] === next[start]) start++;
  while (oldEnd > start && newEnd > start && before[oldEnd - 1] === next[newEnd - 1]) { oldEnd--; newEnd--; }
  const shift = newEnd - oldEnd;
  references.value = references.value.flatMap(reference => reference.end <= start ? [reference] : reference.start >= oldEnd ? [{ ...reference, start: reference.start + shift, end: reference.end + shift }] : []);
  message.value = next; dismissed.value = false; updateCaret();
}
/** 输入候选项，在当前 @ 查询位置插入可读引用并保存目标身份；恢复输入焦点，无返回值。 */
async function choose(option: typeof suggestions.value[number]): Promise<void> {
  const current = query.value; if (!current) return;
  const text = `${option.label} `;
  if (message.value.length - (caret.value - current.start) + text.length > 300) return;
  const end = caret.value; const shift = text.length - (end - current.start);
  references.value = references.value.flatMap(reference => reference.end <= current.start ? [reference] : reference.start >= end ? [{ ...reference, start: reference.start + shift, end: reference.end + shift }] : []);
  references.value.push({ start: current.start, end: current.start + option.label.length, ...(option.playerId ? { playerId: option.playerId } : { tileId: option.tileId }) });
  references.value.sort((a, b) => a.start - b.start);
  message.value = message.value.slice(0, current.start) + text + message.value.slice(end);
  caret.value = current.start + text.length; dismissed.value = true;
  await nextTick(); input.value?.focus(); input.value?.setSelectionRange(caret.value, caret.value);
}
/** 无入参，发送正文与已选引用；成功清空，失败保留，无返回值。 */
async function submit(): Promise<void> {
  if (props.disabled || sending.value || composing.value || !message.value.trim()) return;
  sending.value = true;
  try { if (await props.send(message.value, references.value)) { message.value = ''; references.value = []; caret.value = 0; } }
  finally { sending.value = false; }
}
/** 输入键盘事件，联想支持上下选择、回车或 Tab 插入、Esc 关闭；输入法选词和 Shift+Enter 保留原行为。 */
function keydown(event: KeyboardEvent): void {
  if (event.isComposing || composing.value || event.keyCode === 229) return;
  if (suggestions.value.length) {
    if (event.key === 'Escape') { event.preventDefault(); dismissed.value = true; return; }
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault(); selected.value = (selected.value + (event.key === 'ArrowDown' ? 1 : -1) + suggestions.value.length) % suggestions.value.length;
      void nextTick(() => menu.value?.querySelector('[aria-selected="true"]')?.scrollIntoView({ block: 'nearest' })); return;
    }
    if ((event.key === 'Enter' && !event.shiftKey) || event.key === 'Tab') { event.preventDefault(); void choose(suggestions.value[selected.value]!); return; }
  }
  if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); void submit(); }
}
watch(() => query.value?.text, () => { selected.value = 0; void nextTick(() => { if (menu.value) menu.value.scrollTop = 0; }); });
watch(() => suggestions.value.length, length => { selected.value = Math.min(selected.value, Math.max(0, length - 1)); });
onMounted(() => { window.addEventListener('resize', positionMenu); document.addEventListener('scroll', positionMenu, true); });
onBeforeUnmount(() => { window.removeEventListener('resize', positionMenu); document.removeEventListener('scroll', positionMenu, true); });
</script>
<template>
  <form class="room-chat" @submit.prevent="submit"><PanelFrame variant="chat" /><div class="chat-heading"><h3>房间聊天</h3><small>输入 @ 提及玩家或引用地块</small></div><div class="chat-composer">
    <label class="sr-only" for="room-chat-message">房间聊天，输入 @ 搜索玩家或地块</label>
    <textarea id="room-chat-message" ref="input" :value="message" maxlength="300" rows="1" :disabled="disabled || sending" :placeholder="placeholder" :aria-expanded="suggestions.length > 0" :aria-controls="suggestions.length ? 'room-chat-suggestions' : undefined" :aria-activedescendant="suggestions.length ? `chat-option-${selected}` : undefined" aria-autocomplete="list" @input="edit" @click="dismissed = false; updateCaret()" @keyup="updateCaret" @select="updateCaret" @focus="focused = true; dismissed = false; updateCaret()" @blur="focused = false" @compositionstart="composing = true" @compositionend="composing = false; updateCaret()" @keydown="keydown" />
    <div class="chat-composer-actions"><button type="submit" class="button primary chat-send game-action-button" :disabled="disabled || sending || !message.trim()" aria-label="发送消息" title="发送消息"><ActionIcon action="send" /><small>发送</small></button></div>
  </div></form>
  <Teleport to="body"><div v-if="suggestions.length" id="room-chat-suggestions" ref="menu" class="chat-suggestions" role="listbox" aria-label="玩家和地块联想" :style="menuStyle">
    <button v-for="(option, index) in suggestions" :id="`chat-option-${index}`" :key="option.key" type="button" role="option" :aria-selected="selected === index" tabindex="-1" @pointerdown.prevent @click="choose(option)"><span :style="{ color: option.color }">{{ option.label }}</span><small>{{ option.detail }}</small></button>
  </div></Teleport>
</template>
<style scoped>
.chat-heading small { color: #80927c; font-size: 11px; }
.chat-suggestions { position: fixed; z-index: 150; overflow-y: auto; overscroll-behavior: contain; padding: 5px; border: 2px solid #c5ac7d; border-radius: 12px; background: #fffaf0; box-shadow: 0 8px 24px #62472630; scrollbar-width: thin; }
.chat-suggestions button { display: flex; align-items: center; justify-content: space-between; gap: 10px; width: 100%; min-height: 40px; padding: 8px 10px; border: 0; border-radius: 7px; background: transparent; text-align: left; font: inherit; font-size: 13px; }
.chat-suggestions button[aria-selected="true"], .chat-suggestions button:hover { background: #f3e4c7; }
.chat-suggestions span { min-width: 0; overflow-wrap: anywhere; font-weight: 700; }
.chat-suggestions small { flex-shrink: 0; color: #88775b; font-size: 11px; }
</style>
