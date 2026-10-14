<script setup lang="ts">
defineProps<{ level: number; mortgaged?: boolean }>();
const source = `${import.meta.env.BASE_URL}images/board/property-levels.png`;
</script>
<template>
  <span class="property-art" :class="[`level-${level}`, { mortgaged }]" :style="{ backgroundImage: `url(${source})` }" aria-hidden="true"></span>
</template>
<style scoped>
.property-art { display: block; width: 80px; height: 80px; flex-shrink: 0; background-position: 0 0; background-size: 200% 200%; background-repeat: no-repeat; }
.property-art.level-1 { background-position: 100% 0; }
.property-art.level-2 { background-position: 0 100%; }
.property-art.level-3 { background-position: 100% 100%; }
.property-art.mortgaged { filter: grayscale(.8); opacity: .65; }
</style>
