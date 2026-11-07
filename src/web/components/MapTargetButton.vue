<script setup lang="ts">
import { inject, nextTick, onBeforeUnmount, ref } from 'vue';
import { mapTargetKey, type MapTargetRequest } from '../composables/useMapTarget';
const props = defineProps<{ label: string; options: readonly { value: string | number; label: string; disabled?: boolean }[]; selected: number[]; disabled?: boolean; multiple?: boolean }>();
const emit = defineEmits<{ select: [id: number] }>();
const requestMap = inject(mapTargetKey, undefined);
const button = ref<HTMLButtonElement>();
let request: MapTargetRequest;
/** 无入参，以当前字段的实时候选打开地图；点选仅更新表单，不提交卡牌。 */
function open(): void {
  request = {
    label: props.label,
    options: () => props.disabled ? [] : props.options.filter(option => option.value !== '' && !option.disabled).map(option => ({ id: Number(option.value), label: option.label })),
    selected: () => props.selected,
    multiple: props.multiple,
    select: id => {
      emit('select', id);
      if (!props.multiple) void nextTick(() => button.value?.focus());
    },
  };
  requestMap?.open(request);
}
onBeforeUnmount(() => { if (request) requestMap?.close(request); });
</script>

<template>
  <button v-if="requestMap" ref="button" type="button" class="button secondary small" :aria-label="`${label}：地图点选`" :disabled="disabled || !options.some(option => option.value !== '' && !option.disabled)" @click="open">地图点选 ↗</button>
</template>
