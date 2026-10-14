import { toRef, watch } from 'vue';
import { audioBlocked, audioPreferences, enableAudio, playSound } from '../audio';

/** 输入本人回合标识，返回共享音效状态；同一回合的同步与重连不重复提醒。 */
export function useTurnSound(turnKey: () => string | null) {
  watch(turnKey, key => {
    if (!key || sessionStorage.getItem('dfw:turn-notified') === key) return;
    sessionStorage.setItem('dfw:turn-notified', key);
    playSound('turn');
  }, { immediate: true });
  /** 无入参，切换并保存本机音效偏好，开启时试听共享提示音。 */
  function toggleSound(): void {
    audioPreferences.sound = !audioPreferences.sound;
    if (audioPreferences.sound) void enableAudio(true);
  }
  return { soundEnabled: toRef(audioPreferences, 'sound'), soundBlocked: audioBlocked, toggleSound, enableSound: () => enableAudio(true) };
}
