<script setup lang="ts">
import { computed, ref } from 'vue';
import { audioPreferences, enableAudio } from '../audio';
import { notificationError, notificationPermission, notificationPreferences, notificationRequesting, refreshNotificationPermission, toggleNotifications } from '../notifications';
import Modal from './Modal.vue';
defineOptions({ inheritAttrs: false });
const open = ref(false);
/** 无入参，打开声音设置并通过此次点击启用音频，无返回值。 */
function show(): void { refreshNotificationPermission(); open.value = true; void enableAudio(); }
/** 输入音轨类型，切换其静音状态并启用音频，无返回值。 */
function toggleMute(track: 'music' | 'sound'): void { audioPreferences[track] = !audioPreferences[track]; void enableAudio(); }
/** 输入后台声音类型，切换并保存对应偏好，无返回值。 */
function toggleBackground(track: 'backgroundMusic' | 'backgroundSound'): void { audioPreferences[track] = !audioPreferences[track]; void enableAudio(); }
const notificationHint = computed(() => {
  if (notificationPermission.value === 'unsupported') return '系统通知需要 HTTPS 和浏览器支持，localhost 开发环境也可使用。';
  if (notificationPermission.value === 'denied') return '通知已被浏览器拦截，请在网站设置中允许通知后重新开启。';
  if (notificationError.value) return notificationError.value;
  if (notificationPreferences.enabled && notificationPermission.value === 'granted') return '已开启：后台收到本人回合、新聊天和 @ 时提醒，点击通知返回游戏。';
  return '点击开启并允许浏览器通知；仅在游戏切到后台时提醒。';
});
</script>
<template>
  <button v-bind="$attrs" class="button ghost small audio-settings-button" aria-label="打开声音与提醒设置" @click="show">{{ audioPreferences.music || audioPreferences.sound ? '♫ 声音' : '♫ 静音' }}</button>
  <Modal panel-class="modern-modal" v-if="open" title="声音与提醒" @close="open = false">
    <div class="audio-setting"><button class="button ghost small" :aria-pressed="!audioPreferences.music" :aria-label="audioPreferences.music ? '静音背景音乐' : '取消背景音乐静音'" @click="toggleMute('music')"><span aria-hidden="true">{{ audioPreferences.music ? '🔊' : '🔇' }}</span> 背景音乐</button><input v-model.number="audioPreferences.musicVolume" type="range" min="0" max="1" step="0.01" aria-label="背景音乐音量" :disabled="!audioPreferences.music" /></div>
    <div class="audio-setting"><button class="button ghost small" :aria-pressed="!audioPreferences.sound" :aria-label="audioPreferences.sound ? '静音游戏音效' : '取消游戏音效静音'" @click="toggleMute('sound')"><span aria-hidden="true">{{ audioPreferences.sound ? '🔊' : '🔇' }}</span> 游戏音效</button><input v-model.number="audioPreferences.soundVolume" type="range" min="0" max="1" step="0.01" aria-label="游戏音效音量" :disabled="!audioPreferences.sound" /></div>
    <div class="audio-setting audio-reminder-setting"><div><strong>后台音乐</strong><small>切换标签页或最小化后继续播放，仍遵循背景音乐开关与音量。</small></div><button class="button ghost small" :aria-pressed="audioPreferences.backgroundMusic" aria-label="后台音乐" @click="toggleBackground('backgroundMusic')">{{ audioPreferences.backgroundMusic ? '已开启' : '已关闭' }}</button></div>
    <div class="audio-setting audio-reminder-setting"><div><strong>后台音效</strong><small>后台也播放回合、聊天等游戏音效，仍遵循游戏音效开关与音量。</small></div><button class="button ghost small" :aria-pressed="audioPreferences.backgroundSound" aria-label="后台音效" @click="toggleBackground('backgroundSound')">{{ audioPreferences.backgroundSound ? '已开启' : '已关闭' }}</button></div>
    <div class="audio-setting audio-reminder-setting"><div><strong>系统通知</strong><small role="status">{{ notificationHint }}</small></div><button class="button ghost small" :aria-pressed="notificationPreferences.enabled && notificationPermission === 'granted'" :disabled="notificationRequesting || !notificationPreferences.enabled && notificationPermission === 'unsupported'" aria-label="系统通知" @click="toggleNotifications">{{ notificationRequesting ? '等待授权' : notificationPreferences.enabled ? '关闭通知' : '开启通知' }}</button></div>
    <div class="audio-setting audio-reminder-setting"><div><strong>显示通知详情</strong><small>关闭后只显示通用提醒，隐藏聊天内容、@ 信息和回合详情。</small></div><button class="button ghost small" :aria-pressed="notificationPreferences.showDetails" aria-label="显示通知详情" @click="notificationPreferences.showDetails = !notificationPreferences.showDetails">{{ notificationPreferences.showDetails ? '已显示' : '已隐藏' }}</button></div>
    <p class="notice">设置保存在当前浏览器。声音需要先点击页面启用；通知可独立于声音开启。页面关闭或设备休眠后无法保证提醒，部分手机浏览器不支持系统通知。</p>
  </Modal>
</template>
