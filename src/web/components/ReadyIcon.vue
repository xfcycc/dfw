<script setup lang="ts">
import { computed } from 'vue';
const props = defineProps<{ ready: boolean; offline?: boolean }>();
const source = `${import.meta.env.BASE_URL}images/actions/ready-status.png`;
const label = computed(() => props.offline ? '暂时离线' : props.ready ? '已准备' : '未准备');
const viewBox = computed(() => props.ready && !props.offline ? '120 130 744 655' : '998 130 664 655');
</script>
<template>
  <svg class="ready-icon" :class="{ offline }" :viewBox="viewBox" role="img" :aria-label="label">
    <title>{{ label }}</title>
    <image :href="source" width="1774" height="887" />
  </svg>
</template>
<style scoped>
.ready-icon { display: block; width: 26px; height: 26px; overflow: hidden; pointer-events: none; }
.ready-icon.offline { filter: grayscale(1); opacity: .45; }
</style>
