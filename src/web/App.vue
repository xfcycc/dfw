<script setup lang="ts">
import { useGameStore } from './stores/game';
import { useRoute } from 'vue-router';
import { useAudioEnvironment } from './audio';
import { useNotificationEnvironment } from './notifications';
import './audio-effects.css';
import './holiday-theme.css';
import { onMounted, onUnmounted, watch } from 'vue';
import { activeHoliday, holidayTheme, startHolidayClock } from './holiday-theme';
import HolidayPreview from './components/HolidayPreview.vue';
const game = useGameStore();
const route = useRoute();
let stopHolidayClock: (() => void) | undefined;
onMounted(() => { stopHolidayClock = startHolidayClock(); });
onUnmounted(() => { stopHolidayClock?.(); });
watch(activeHoliday, value => {
  if (value) document.documentElement.dataset.holiday = value;
  else delete document.documentElement.dataset.holiday;
  document.title = holidayTheme.value ? `大富翁-奇迹大陆 · ${holidayTheme.value.caption}` : '大富翁-奇迹大陆';
}, { immediate: true });
useNotificationEnvironment();
useAudioEnvironment(() => route.path === '/' ? 'lobby' : /^\/(room|watch)\//.test(route.path) && game.room && game.connection === 'online' ? game.room.status === 'lobby' ? 'lobby' : 'game' : null);
</script>
<template>
  <RouterView />
  <HolidayPreview />
  <Transition name="toast"><div v-if="game.message" class="toast" role="status">{{ game.message }}</div></Transition>
</template>
