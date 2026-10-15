<script setup lang="ts">
defineProps<{ level: number }>();
</script>
<template>
  <span class="property-grade" :class="{ 'max-level': level === 3 }">{{ level }}级</span>
</template>
<style scoped>
.property-grade { display: inline-flex; align-items: center; flex-shrink: 0; padding: 4px 8px; border: 1px solid #dce7d7; border-radius: 6px; background: #eef4e9; color: #617956; font-size: 11px; font-weight: 600; line-height: 1.4; white-space: nowrap; }
.property-grade.max-level { background: #fff5df; border-color: #edddb5; color: #9a793b; }
</style>
