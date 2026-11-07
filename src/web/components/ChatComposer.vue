<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import type { ChatReference, PublicRoomState } from '../../shared/types';
import { CHAT_EMOJIS, type ChatEmoji } from '../chat-emojis';
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
const emojiOpen = ref(false);
const emojiTrigger = ref<HTMLButtonElement>();
const emojiMenu = ref<HTMLElement>();
const emojiStyle = ref<Record<string, string>>({});
/** 无入参，在表情按钮旁展开或收起浮层，按视口空间选择方向，无返回值。 */
function toggleEmoji(): void {
  if (emojiOpen.value) { emojiMenu.value?.hidePopover(); return; }
  const rect = emojiTrigger.value!.getBoundingClientRect();
  const width = Math.min(336, window.innerWidth - 24);
  const upwards = rect.top > window.innerHeight - rect.bottom;
  emojiStyle.value = {
    left: `${Math.max(12, Math.min(rect.right - width, window.innerWidth - width - 12))}px`,
    top: `${upwards ? rect.top - 8 : rect.bottom + 8}px`,
    width: `${width}px`,
    maxHeight: `${Math.min(300, (upwards ? rect.top : window.innerHeight - rect.bottom) - 20)}px`,
    transform: upwards ? 'translateY(-100%)' : 'none',
  };
  dismissed.value = true;
  emojiMenu.value?.showPopover();
}
const selectionEnd = ref(0);
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
/** 输入可选的滚动或缩放事件，更新联想菜单位置，外部滚动时收起表情浮层，无返回值。 */
function positionMenu(event?: Event): void {
  if (event?.target instanceof Node && emojiMenu.value?.contains(event.target)) return;
  if (emojiOpen.value) emojiMenu.value?.hidePopover();
  if (!input.value) return;
  const bounds = input.value.closest('.chat-composer')!.getBoundingClientRect();
  menuStyle.value = { left: `${bounds.left}px`, bottom: `${window.innerHeight - bounds.top + 6}px`, width: `${bounds.width}px`, maxHeight: `${Math.max(60, Math.min(240, bounds.top - 12))}px` };
}
/** 无入参，记录光标位置并重新允许联想，无返回值。 */
function updateCaret(): void {
  const position = input.value?.selectionStart ?? 0;
  if (position !== caret.value) dismissed.value = false;
  caret.value = position; positionMenu();
  selectionEnd.value = input.value?.selectionEnd ?? position;
}
/** 输入原生编辑事件，移动未被编辑的引用偏移，删除与编辑范围相交的引用，无返回值。 */
function edit(event: Event): void {
  updateMessage((event.target as HTMLTextAreaElement).value);
  dismissed.value = false; updateCaret();
}
/** 输入编辑后的正文，更新引用偏移并移除被改写的引用，无返回值。 */
function updateMessage(next: string): void {
  const before = message.value;
  let start = 0; let oldEnd = before.length; let newEnd = next.length;
  while (start < oldEnd && start < newEnd && before[start] === next[start]) start++;
  while (oldEnd > start && newEnd > start && before[oldEnd - 1] === next[newEnd - 1]) { oldEnd--; newEnd--; }
  const shift = newEnd - oldEnd;
  references.value = references.value.flatMap(reference => reference.end <= start ? [reference] : reference.start >= oldEnd ? [{ ...reference, start: reference.start + shift, end: reference.end + shift }] : []);
  message.value = next;
}
/** 输入表情，在原光标或选区插入保存代码，保留未被改写的 @ 引用并恢复输入焦点，无返回值。 */
async function insertEmoji(emoji: ChatEmoji): Promise<void> {
  if (props.disabled || sending.value || message.value.length - (selectionEnd.value - caret.value) + emoji.code.length > 300) return;
  const end = caret.value + emoji.code.length;
  updateMessage(message.value.slice(0, caret.value) + emoji.code + message.value.slice(selectionEnd.value));
  caret.value = end; selectionEnd.value = end; dismissed.value = true; emojiMenu.value?.hidePopover();
  await nextTick(); input.value?.focus(); input.value?.setSelectionRange(end, end);
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
  caret.value = current.start + text.length; selectionEnd.value = caret.value; dismissed.value = true;
  await nextTick(); input.value?.focus(); input.value?.setSelectionRange(caret.value, caret.value);
}
/** 无入参，发送正文与已选引用；成功清空，失败保留，无返回值。 */
async function submit(): Promise<void> {
  if (props.disabled || sending.value || composing.value || !message.value.trim()) return;
  sending.value = true;
  try { if (await props.send(message.value, references.value)) { message.value = ''; references.value = []; caret.value = 0; selectionEnd.value = 0; } }
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
  <form class="room-chat" @submit.prevent="submit"><PanelFrame variant="chat" /><div class="chat-heading"><h3>房间聊天</h3><small v-if="room.status !== 'lobby'">输入 @ 提及玩家或引用地块</small></div><div class="chat-composer">
    <label class="sr-only" for="room-chat-message">房间聊天，输入 @ 搜索玩家或地块</label>
    <textarea id="room-chat-message" ref="input" :value="message" maxlength="300" rows="1" :disabled="disabled || sending" :placeholder="placeholder" :aria-expanded="suggestions.length > 0" :aria-controls="suggestions.length ? 'room-chat-suggestions' : undefined" :aria-activedescendant="suggestions.length ? `chat-option-${selected}` : undefined" aria-autocomplete="list" @input="edit" @click="dismissed = false; updateCaret()" @keyup="updateCaret" @select="updateCaret" @focus="focused = true; dismissed = false; updateCaret()" @blur="focused = false" @compositionstart="composing = true" @compositionend="composing = false; updateCaret()" @keydown="keydown" />
    <div class="chat-composer-actions"><button ref="emojiTrigger" type="button" class="button secondary chat-emoji-toggle game-action-button" :disabled="disabled || sending || composing" aria-label="选择表情" title="表情" aria-haspopup="dialog" :aria-expanded="emojiOpen" @click="toggleEmoji"><img :src="CHAT_EMOJIS[0]!.url" alt="" width="26" height="26" /><small>表情</small></button><button type="submit" class="button primary chat-send game-action-button" :disabled="disabled || sending || !message.trim()" aria-label="发送消息" title="发送消息"><ActionIcon action="send" /><small>发送</small></button></div>
  </div></form>
  <div ref="emojiMenu" popover="auto" class="chat-emoji-popover" role="dialog" aria-label="选择表情" :style="emojiStyle" @toggle="emojiOpen = $event.newState === 'open'"><div class="chat-emoji-grid"><button v-for="emoji in CHAT_EMOJIS" :key="emoji.code" type="button" :disabled="disabled || sending || message.length - (selectionEnd - caret) + emoji.code.length > 300" :title="emoji.name" :aria-label="`插入表情：${emoji.name}`" @click="insertEmoji(emoji)"><img :src="emoji.url" :alt="emoji.name" width="36" height="36" /><small>{{ emoji.name }}</small></button></div></div>
  <Teleport to="body"><div v-if="suggestions.length" id="room-chat-suggestions" ref="menu" class="chat-suggestions" role="listbox" aria-label="玩家和地块联想" :style="menuStyle">
    <button v-for="(option, index) in suggestions" :id="`chat-option-${index}`" :key="option.key" type="button" role="option" :aria-selected="selected === index" tabindex="-1" @pointerdown.prevent @click="choose(option)"><span :style="{ color: option.color }">{{ option.label }}</span><small>{{ option.detail }}</small></button>
  </div></Teleport>
</template>
<style scoped>
.room-chat { position: relative; }
.chat-emoji-popover { position: fixed; inset: auto; margin: 0; box-sizing: border-box; padding: 10px; overflow-y: auto; overscroll-behavior: contain; border: 2px solid #c5ac7d; border-radius: 16px; background: #fffaf0; box-shadow: 0 8px 24px #62472630; scrollbar-width: thin; }
.chat-emoji-toggle img { object-fit: contain; }
.chat-emoji-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(54px, 1fr)); gap: 6px; }
.chat-emoji-grid button { display: flex; flex-direction: column; align-items: center; gap: 5px; min-width: 0; padding: 8px 2px; border: 1px solid #e3d7b9; border-radius: 12px; background: #fffdf5; color: #806344; font: inherit; }
.chat-emoji-grid button:hover:not(:disabled), .chat-emoji-grid button:focus-visible { border-color: #9bb18b; background: #f1f6e9; }
.chat-emoji-grid button:disabled { opacity: .5; }
.chat-emoji-grid img { object-fit: contain; }
.chat-emoji-grid small { font-size: 10px; }
.chat-heading small { color: #80927c; font-size: 11px; }
.chat-suggestions { position: fixed; z-index: 150; overflow-y: auto; overscroll-behavior: contain; padding: 5px; border: 2px solid #c5ac7d; border-radius: 12px; background: #fffaf0; box-shadow: 0 8px 24px #62472630; scrollbar-width: thin; }
.chat-suggestions button { display: flex; align-items: center; justify-content: space-between; gap: 10px; width: 100%; min-height: 40px; padding: 8px 10px; border: 0; border-radius: 7px; background: transparent; text-align: left; font: inherit; font-size: 13px; }
.chat-suggestions button[aria-selected="true"], .chat-suggestions button:hover { background: #f3e4c7; }
.chat-suggestions span { min-width: 0; overflow-wrap: anywhere; font-weight: 700; }
.chat-suggestions small { flex-shrink: 0; color: #88775b; font-size: 11px; }
</style>
