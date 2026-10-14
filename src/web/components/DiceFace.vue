<script setup lang="ts">
import { computed } from 'vue';
const props = defineProps<{ value: number | null; rolling?: boolean; settled?: boolean }>();
const faceDots: Record<number, number[]> = { 1: [5], 2: [1, 9], 3: [1, 5, 9], 4: [1, 3, 7, 9], 5: [1, 3, 5, 7, 9], 6: [1, 3, 4, 6, 7, 9] };
const numeric = computed(() => props.value !== null && !faceDots[props.value]);
const label = computed(() => props.rolling ? '正在掷骰' : props.value === null ? '等待掷骰' : props.value < 0 ? `本次后退 ${-props.value} 格` : props.value === 0 ? '原地重新结算' : `骰子 ${props.value} 点`);
</script>
<template>
  <div class="die" :class="{ rolling, settled, 'movement-value': numeric }" :aria-label="label" role="img">
    <strong v-if="numeric">{{ value }}</strong>
    <template v-else><span v-for="dot in 9" :key="dot" :class="{ filled: faceDots[value ?? 1]?.includes(dot) }"></span></template>
  </div>
</template>
