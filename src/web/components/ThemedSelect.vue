<script setup lang="ts" generic="T extends string | number | boolean">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useId, watch } from 'vue';
defineOptions({ inheritAttrs: false });
const props = defineProps<{ modelValue: T; options: readonly { value: T; label: string; disabled?: boolean }[]; disabled?: boolean; required?: boolean; label: string }>();
const emit = defineEmits<{ 'update:modelValue': [value: T] }>();
const id = useId();
const trigger = ref<HTMLButtonElement>();
const menu = ref<HTMLElement>();
const enhanced = typeof HTMLElement !== 'undefined' && 'showPopover' in HTMLElement.prototype;
const open = ref(false);
const active = ref(-1);
const position = ref<Record<string, string>>({});
const selected = computed(() => props.options.findIndex(option => option.value === props.modelValue));
const value = computed({ get: () => props.modelValue, set: value => emit('update:modelValue', value) });
/** 无入参，关闭选项菜单，不改变当前选值。 */
function close(): void { menu.value?.hidePopover(); open.value = false; }
/** 无入参，滚动到键盘当前指向的选项，无返回值。 */
function reveal(): void { void nextTick(() => menu.value?.children[active.value]?.scrollIntoView({ block: 'nearest' })); }
/** 无入参，按视口剩余空间展开菜单，保留原生表单校验。 */
function show(): void {
  if (props.disabled || !enhanced || !trigger.value || !menu.value) return;
  const rect = trigger.value.getBoundingClientRect();
  const viewport = window.visualViewport;
  const top = viewport?.offsetTop ?? 0;
  const left = viewport?.offsetLeft ?? 0;
  const height = viewport?.height ?? window.innerHeight;
  const width = viewport?.width ?? window.innerWidth;
  const below = top + height - rect.bottom - 10;
  const above = rect.top - top - 10;
  const upwards = below < 180 && above > below;
  position.value = {
    left: `${Math.max(left + 8, Math.min(rect.left, left + width - rect.width - 8))}px`,
    top: `${upwards ? rect.top - 6 : rect.bottom + 6}px`,
    width: `${Math.min(rect.width, width - 16)}px`,
    maxHeight: `${Math.max(0, Math.min(280, upwards ? above : below))}px`,
    transform: upwards ? 'translateY(-100%)' : 'none',
  };
  active.value = selected.value >= 0 && !props.options[selected.value]?.disabled ? selected.value : props.options.findIndex(option => !option.disabled);
  menu.value.showPopover(); open.value = true; reveal();
}
/** 输入选项序号，写回原类型的选值并归还触发器焦点。 */
function choose(index: number): void {
  const option = props.options[index];
  if (!option || option.disabled || props.disabled) return;
  emit('update:modelValue', option.value); close(); trigger.value?.focus();
}
/** 输入键盘事件，处理菜单导航、确认和退出，不触发外层弹窗关闭。 */
function keydown(event: KeyboardEvent): void {
  if (event.key === 'Tab') { if (open.value) close(); return; }
  if (event.key === 'Escape') { if (open.value) { event.preventDefault(); event.stopPropagation(); close(); } return; }
  if (['Enter', ' '].includes(event.key)) {
    event.preventDefault(); if (open.value) choose(active.value); else show(); return;
  }
  if (!['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return;
  event.preventDefault();
  if (!open.value) { show(); return; }
  const enabled = props.options.map((option, index) => option.disabled ? -1 : index).filter(index => index >= 0);
  const current = enabled.indexOf(active.value);
  active.value = event.key === 'Home' ? enabled[0] ?? -1 : event.key === 'End' ? enabled.at(-1) ?? -1 : enabled[(current + (event.key === 'ArrowDown' ? 1 : -1) + enabled.length) % enabled.length] ?? -1;
  reveal();
}
/** 输入滚动事件，外部滚动时收起菜单，菜单内部继续正常滚动。 */
function scroll(event: Event): void { if (open.value && !(event.target instanceof Node && menu.value?.contains(event.target))) close(); }
/** 输入原生校验事件，增强模式将焦点移到可见选择框，无返回值。 */
function invalid(event: Event): void { if (enhanced) { event.preventDefault(); trigger.value?.focus(); show(); } }
/** 输入浮层切换事件，同步点击外部与浏览器自动收起后的状态。 */
function toggle(event: Event): void { open.value = (event as ToggleEvent).newState === 'open'; }
watch([() => props.disabled, () => JSON.stringify(props.options)], () => { if (open.value) close(); });
onMounted(() => { window.addEventListener('resize', close); window.addEventListener('scroll', scroll, true); window.visualViewport?.addEventListener('resize', close); });
onBeforeUnmount(() => { window.removeEventListener('resize', close); window.removeEventListener('scroll', scroll, true); window.visualViewport?.removeEventListener('resize', close); });
</script>

<template>
  <span class="themed-select" :class="[$attrs.class, { 'is-open': open, 'is-disabled': disabled, enhanced }]" :style="$attrs.style">
    <button v-if="enhanced" v-bind="{ ...$attrs, class: undefined, style: undefined }" ref="trigger" type="button" class="themed-select-trigger" role="combobox" :disabled="disabled" :aria-label="label" :aria-expanded="open" aria-haspopup="listbox" :aria-controls="id" :aria-required="required || undefined" :aria-activedescendant="open && active >= 0 ? `${id}-${active}` : undefined" @click="open ? close() : show()" @keydown="keydown" @blur="close">
      <span>{{ options[selected]?.label || '请选择' }}</span><svg viewBox="0 0 16 16" aria-hidden="true"><path d="m4 6 4 4 4-4" /></svg>
    </button>
    <select v-model="value" class="themed-select-native" :disabled="disabled" :required="required" :aria-label="label" :aria-hidden="enhanced || undefined" :tabindex="enhanced ? -1 : undefined" @invalid="invalid">
      <option v-for="(option, index) in options" :key="index" :value="option.value" :disabled="option.disabled">{{ option.label }}</option>
    </select>
    <span v-if="enhanced" :id="id" ref="menu" popover="auto" class="themed-select-menu" role="listbox" :aria-label="label" :style="position" @toggle="toggle" @pointerdown.prevent @click.stop.prevent>
      <span v-for="(option, index) in options" :id="`${id}-${index}`" :key="index" class="themed-select-option" :class="{ 'is-active': active === index }" role="option" :aria-selected="option.value === modelValue" :aria-disabled="option.disabled || undefined" @pointermove="!option.disabled && (active = index)" @click="choose(index)"><span>{{ option.label }}</span><span v-if="option.value === modelValue" class="themed-select-check" aria-hidden="true">✓</span></span>
    </span>
  </span>
</template>

<style>
.themed-select { display: inline-block; position: relative; min-width: 0; max-width: 100%; border: 1px solid var(--select-border, #d6e0ce); border-radius: 12px; background: var(--select-bg, #fafcf7); color: var(--select-text, #455d45); box-shadow: inset 0 2px 3px #7058300a; font: inherit; font-size: 13px; vertical-align: middle; }
.field .themed-select { display: block; width: 100%; margin-top: 10px; }
.themed-select .themed-select-trigger, .themed-select .themed-select-native { display: flex; align-items: center; justify-content: space-between; gap: 8px; width: 100%; min-height: inherit; margin: 0; padding: 11px 12px; border: 0; border-radius: inherit; background: transparent; color: inherit; font: inherit; line-height: 1.5; text-align: left; box-shadow: none; }
.themed-select-trigger > span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.themed-select-trigger svg { width: 16px; height: 16px; flex-shrink: 0; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; transition: transform .15s; }
.themed-select.is-open svg { transform: rotate(180deg); }
.themed-select:focus-within, .themed-select.is-open { border-color: var(--select-accent, #92ae78); }
.themed-select-trigger:focus-visible { outline: 3px solid var(--select-ring, #b5c99a); outline-offset: 3px; }
.themed-select:not(.is-disabled):hover { border-color: var(--select-accent, #92ae78); }
.themed-select.is-disabled { opacity: .55; }
.themed-select .themed-select-trigger:disabled { opacity: 1; }
.themed-select.enhanced .themed-select-native { position: absolute; width: 1px; height: 1px; min-height: 0; padding: 0; overflow: hidden; clip-path: inset(50%); pointer-events: none; opacity: 0; }
.themed-select-menu { position: fixed; inset: auto; margin: 0; padding: 5px; overflow: auto; overscroll-behavior: contain; border: 2px solid var(--select-border, #d6e0ce); border-radius: 14px; background: var(--select-menu-bg, #fffef8); color: var(--select-text, #455d45); box-shadow: inset 0 0 0 2px #fffdf580, 0 8px 24px #62472630; font: inherit; font-size: 13px; scrollbar-width: thin; scrollbar-color: var(--select-border, #d6e0ce) transparent; }
.themed-select-menu::backdrop { background: transparent; }
.themed-select-option { display: flex; align-items: center; justify-content: space-between; gap: 12px; min-height: 40px; padding: 9px 10px; border-radius: 9px; line-height: 1.5; cursor: pointer; overflow-wrap: anywhere; }
.themed-select-option.is-active { background: var(--select-hover, #f0f5e9); }
.themed-select-option[aria-selected='true'] { background: var(--select-selected, #e8f1da); color: var(--select-selected-text, #526e3d); font-weight: 700; }
.themed-select-option[aria-disabled='true'] { opacity: .45; cursor: not-allowed; }
.themed-select-check { flex-shrink: 0; }
.chibi-modal .themed-select, .cottage-lobby .themed-select { --select-bg: #fffdf5; --select-menu-bg: #fff9eb; --select-border: #e0cfaf; --select-text: #705a43; --select-hover: #f5ead3; --select-selected: #edf5dd; --select-selected-text: #5c7844; border-width: 2px; border-radius: 14px; }
.cottage-lobby .bot-level-select { border-radius: 10px; }
.themed-select.bot-level-select .themed-select-trigger { gap: 4px; padding: 5px 6px; font-size: 12px; }
.themed-select.bot-level-select svg { width: 12px; height: 12px; }
.modern-room .records-scroll .field .themed-select { margin-top: 4px; border-radius: 9px; font-size: 11px; }
.modern-room .records-scroll .themed-select-trigger { min-height: 32px; padding: 6px 10px; }
.central-card-selection .field .themed-select, .central-action-panel .field .themed-select { margin-top: 5px; }
.central-card-selection .themed-select-trigger, .central-action-panel .themed-select-trigger { min-height: 34px; padding: 6px 8px; }
</style>
