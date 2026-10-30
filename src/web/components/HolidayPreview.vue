<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router';
import { watch } from 'vue';
import { holidayPreview, holidayTheme, holidayThemes, setHolidayPreview, type HolidayPreview } from '../holiday-theme';

const route = useRoute();
const router = useRouter();
const options: { value: HolidayPreview; label: string }[] = [
  { value: 'auto', label: '按日期' }, { value: 'default', label: '原版' },
  ...Object.entries(holidayThemes).map(([value, theme]) => ({ value: value as HolidayPreview, label: theme.name })),
];
watch(() => route.query.holidayPreview, value => {
  setHolidayPreview(options.find(option => option.value === value)?.value ?? 'auto');
}, { immediate: true });

/** 输入预览选项，保留当前页面并更新可分享的预览地址，无返回值。 */
function select(value: HolidayPreview): void {
  void router.replace({ query: { ...route.query, holidayPreview: value } });
}

/** 退出临时预览并恢复按北京时间自动切换，无入参和返回值。 */
function close(): void {
  const query = { ...route.query };
  delete query.holidayPreview;
  void router.replace({ query });
}
</script>

<template>
  <aside v-if="route.query.holidayPreview !== undefined" class="holiday-preview" aria-label="节日皮肤预览">
    <div class="holiday-preview-heading"><strong>节日皮肤预览</strong><span>{{ holidayTheme?.range ?? '原版小镇' }} · 仅当前预览</span><button type="button" aria-label="退出节日预览" @click="close">×</button></div>
    <div class="holiday-preview-options"><button v-for="option in options" :key="option.value" type="button" :aria-pressed="holidayPreview === option.value" @click="select(option.value)">{{ option.label }}</button></div>
  </aside>
</template>
