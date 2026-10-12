<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
defineProps<{ title: string; wide?: boolean }>();
const emit = defineEmits<{ close: [] }>();
const dialog = ref<HTMLElement>();
let previous: HTMLElement | null = null;
/** 输入键盘事件，关闭弹窗或将焦点限制在弹窗内。 */
function keydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') emit('close');
  if (event.key !== 'Tab' || !dialog.value) return;
  const nodes = Array.from(dialog.value.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled), select:not(:disabled), a[href], [tabindex="0"]'));
  const first = nodes[0]; const last = nodes.at(-1);
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
}
onMounted(() => {
  previous = document.activeElement as HTMLElement;
  dialog.value?.focus();
  document.addEventListener('keydown', keydown);
});
onBeforeUnmount(() => { document.removeEventListener('keydown', keydown); previous?.focus(); });
</script>
<template>
  <Teleport to="body">
    <div class="modal-backdrop" @click.self="emit('close')">
      <section ref="dialog" class="modal" :class="{ wide }" role="dialog" aria-modal="true" :aria-label="title" tabindex="-1">
        <div class="modal-heading"><h2>{{ title }}</h2><button class="icon-button" aria-label="关闭弹窗" @click="emit('close')">✕</button></div>
        <slot />
      </section>
    </div>
  </Teleport>
</template>
