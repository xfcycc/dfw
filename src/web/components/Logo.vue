<script setup lang="ts">
import { holidayTheme } from '../holiday-theme';
</script>
<template>
  <span v-if="holidayTheme" class="brand holiday-brand"><img :src="holidayTheme.title" :alt="`大富翁-奇迹大陆 · ${holidayTheme.caption}`" width="1536" height="1024" /></span>
  <span v-else class="brand"><svg viewBox="0 0 40 40" aria-hidden="true"><rect x="2" y="2" width="36" height="36" rx="11" fill="currentColor"/><g fill="#fff"><circle cx="12" cy="12" r="2.7"/><circle cx="28" cy="12" r="2.7"/><circle cx="20" cy="20" r="2.7"/><circle cx="12" cy="28" r="2.7"/><circle cx="28" cy="28" r="2.7"/></g></svg><span>大富翁-奇迹大陆<small>MIRACLE LAND</small></span></span>
</template>
