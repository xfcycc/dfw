<script setup lang="ts" generic="T extends string | number">
import { computed } from 'vue';
import ThemedSelect from './ThemedSelect.vue';
import MapTargetButton from './MapTargetButton.vue';
const props = defineProps<{ modelValue: T; options: readonly { value: T; label: string; disabled?: boolean }[]; label: string; disabled?: boolean; tileIds?: (value: T) => number[] }>();
const emit = defineEmits<{ 'update:modelValue': [value: T] }>();
/** 输入原选项值，返回代表它的地图格号；区域选项可覆盖一整条边。 */
function ids(value: T): number[] { return value === '' ? [] : props.tileIds?.(value) ?? [Number(value)]; }
const mapOptions = computed(() => props.options.flatMap(option => ids(option.value).map(id => ({ ...option, value: id }))));
/** 输入地图格号，回传原选项的值类型，兼容字符串、数字及区域表单。 */
function select(id: number): void {
  const option = props.options.find(option => !option.disabled && ids(option.value).includes(id));
  if (option) emit('update:modelValue', option.value);
}
</script>

<template>
  <span class="map-tile-select">
    <ThemedSelect :model-value="modelValue" :label="label" :options="options" :disabled="disabled" @update:model-value="emit('update:modelValue', $event)" />
    <MapTargetButton :label="label" :options="mapOptions" :selected="ids(modelValue)" :disabled="disabled" @select="select" />
  </span>
</template>

<style scoped>
.map-tile-select { display: flex; align-items: center; gap: .5rem; flex-wrap: wrap; }
.map-tile-select > :first-child { flex: 1; min-width: 0; }
</style>
