<script setup lang="ts">
import { ref } from 'vue';
import { audioPreferences, enableAudio } from '../audio';
import Modal from './Modal.vue';
defineOptions({ inheritAttrs: false });
const open = ref(false);
/** 无入参，打开声音设置并通过此次点击启用音频，无返回值。 */
function show(): void { open.value = true; void enableAudio(); }
/** 输入音轨类型，切换其静音状态并启用音频，无返回值。 */
function toggleMute(track: 'music' | 'sound'): void { audioPreferences[track] = !audioPreferences[track]; void enableAudio(); }
</script>
<template>
  <button v-bind="$attrs" class="button ghost small audio-settings-button" aria-label="打开声音设置" @click="show">{{ audioPreferences.music || audioPreferences.sound ? '♫ 声音' : '♫ 静音' }}</button>
  <Modal panel-class="modern-modal" v-if="open" title="声音设置" @close="open = false">
    <div class="audio-setting"><button class="button ghost small" :aria-pressed="!audioPreferences.music" :aria-label="audioPreferences.music ? '静音背景音乐' : '取消背景音乐静音'" @click="toggleMute('music')"><span aria-hidden="true">{{ audioPreferences.music ? '🔊' : '🔇' }}</span> 背景音乐</button><input v-model.number="audioPreferences.musicVolume" type="range" min="0" max="1" step="0.01" aria-label="背景音乐音量" :disabled="!audioPreferences.music" /></div>
    <div class="audio-setting"><button class="button ghost small" :aria-pressed="!audioPreferences.sound" :aria-label="audioPreferences.sound ? '静音游戏音效' : '取消游戏音效静音'" @click="toggleMute('sound')"><span aria-hidden="true">{{ audioPreferences.sound ? '🔊' : '🔇' }}</span> 游戏音效</button><input v-model.number="audioPreferences.soundVolume" type="range" min="0" max="1" step="0.01" aria-label="游戏音效音量" :disabled="!audioPreferences.sound" /></div>
  </Modal>
</template>
