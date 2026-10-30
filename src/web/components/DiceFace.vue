<script setup lang="ts">
import { computed } from 'vue';
import { activeHoliday } from '../holiday-theme';
const props = defineProps<{ value: number | null; rolling?: boolean; settled?: boolean }>();
const faceDots: Record<number, number[]> = { 1: [5], 2: [1, 9], 3: [1, 5, 9], 4: [1, 3, 7, 9], 5: [1, 3, 5, 7, 9], 6: [1, 3, 4, 6, 7, 9] };
const numeric = computed(() => props.value !== null && !faceDots[props.value]);
const skinStyle = computed(() => activeHoliday.value ? { background: 'linear-gradient(145deg,var(--holiday-dice-light),var(--holiday-dice-dark))', borderColor: 'var(--holiday-dice-edge)', boxShadow: 'inset 0 0 0 2px var(--holiday-dice-inset),0 3px 0 var(--holiday-dice-edge)' } : undefined);
const label = computed(() => props.rolling ? '正在掷骰' : props.value === null ? '等待掷骰' : props.value < 0 ? `本次后退 ${-props.value} 格` : props.value === 0 ? '原地重新结算' : `骰子 ${props.value} 点`);
</script>
<template>
  <div class="die" :class="{ rolling, settled, 'movement-value': numeric, 'holiday-die': !!activeHoliday }" :style="skinStyle" :aria-label="label" role="img">
    <strong v-if="numeric" :style="activeHoliday ? { color: 'var(--holiday-dice-pip)' } : undefined">{{ value }}</strong>
    <template v-else><span v-for="dot in 9" :key="dot" :class="{ filled: faceDots[value ?? 1]?.includes(dot) }" :style="activeHoliday && faceDots[value ?? 1]?.includes(dot) ? { background: 'var(--holiday-dice-pip)', boxShadow: 'inset 0 1px 2px #00000030' } : undefined"></span></template>
  </div>
</template>
<style scoped>
.holiday-die{position:relative}
.holiday-die::after{content:'';position:absolute;inset:3px;border:1px solid var(--holiday-dice-inset);border-radius:inherit;pointer-events:none}
</style>
