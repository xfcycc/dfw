import { toRef, watch } from 'vue';
import { audioBlocked, audioPreferences, enableAudio, playSound } from '../audio';
import { notifyBackground } from '../notifications';

/** 输入本人回合标识，播放提示音并在后台发送系统通知，返回音效状态；同步与重连不重复提醒。 */
export function useTurnSound(turnKey: () => string | null) {
  watch(turnKey, key => {
    if (!key || sessionStorage.getItem('dfw:turn-notified') === key) return;
    sessionStorage.setItem('dfw:turn-notified', key);
    playSound('turn');
    notifyBackground('轮到你了', '回到游戏，开始你的回合吧！', 'dfw:turn');
  }, { immediate: true });
  /** 无入参，切换并保存本机音效偏好，开启时试听共享提示音。 */
  function toggleSound(): void {
    audioPreferences.sound = !audioPreferences.sound;
    if (audioPreferences.sound) void enableAudio(true);
  }
  return { soundEnabled: toRef(audioPreferences, 'sound'), soundBlocked: audioBlocked, toggleSound, enableSound: () => enableAudio(true) };
}
