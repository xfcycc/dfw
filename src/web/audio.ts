import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';

export type Sound = 'click' | 'turn' | 'chat' | 'mention' | 'dice' | 'step' | 'coin' | 'pay' | 'buy' | 'build' | 'card' | 'magic' | 'error' | 'victory' | 'casino_dice' | 'casino_win' | 'casino_loss' | 'horse_start' | 'horse_run' | 'horse_finish' | 'roulette_spin' | 'roulette_click' | 'roulette_hit' | 'packet_open' | 'packet_lucky' | 'rps_count' | 'rps_reveal' | 'rps_win' | 'rotation_count' | 'rotation_switch';
type MusicScene = 'lobby' | 'game' | null;
const tracks = { lobby: '/audio/nikki-bianzhi-yuanjing.m4a', game: '/audio/nikki-bianzhi-yuanjing.m4a' };
/** 输入本机音量存储键和默认值，返回 0～1 的有效音量。 */
function volume(key: string, fallback: number): number {
  const stored = localStorage.getItem(key);
  const value = stored === null ? fallback : Number(stored);
  return Number.isFinite(value) ? Math.max(0, Math.min(1, value)) : fallback;
}
export const audioPreferences = reactive({
  music: localStorage.getItem('dfw:music') !== 'off',
  sound: localStorage.getItem('dfw:turn-sound') !== 'off',
  musicVolume: volume('dfw:music-volume', 0.22),
  soundVolume: volume('dfw:sound-volume', 0.45),
});
export const audioBlocked = ref(false);
export const musicFailed = ref(false);
let context: AudioContext | undefined;
let musicGain: GainNode | undefined;
let soundGain: GainNode | undefined;
let scene: MusicScene = null;
let unlocked = false;
let music: HTMLAudioElement | undefined;
let fadeTimer: ReturnType<typeof setTimeout> | undefined;
let musicGeneration = 0;
const voices = new Set<AudioBufferSourceNode>();
const soundBuffers = new Map<Sound, Promise<AudioBuffer | null>>();
let soundGeneration = 0;
const lastPlayed = new Map<Sound, number>();
const sounds: Sound[] = ['click', 'turn', 'chat', 'mention', 'dice', 'step', 'coin', 'pay', 'buy', 'build', 'card', 'magic', 'error', 'victory', 'casino_dice', 'casino_win', 'casino_loss', 'horse_start', 'horse_run', 'horse_finish', 'roulette_spin', 'roulette_click', 'roulette_hit', 'packet_open', 'packet_lucky', 'rps_count', 'rps_reveal', 'rps_win', 'rotation_count', 'rotation_switch'];

/** 输入效果名及音频设备，返回缓存的原创 WAV；加载失败记录日志并允许下次重试。 */
function loadSound(sound: Sound, device: AudioContext): Promise<AudioBuffer | null> {
  const existing = soundBuffers.get(sound);
  if (existing) return existing;
  const pending = fetch(`/audio/sfx/${sound}.wav`).then(async response => {
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return device.decodeAudioData(await response.arrayBuffer());
  }).catch((error: unknown) => { soundBuffers.delete(sound); console.warn('游戏音效加载失败', sound, error); return null; });
  soundBuffers.set(sound, pending);
  return pending;
}

/** 输入效果名，播放与 BGM 音高分析相配的原创短音；静音、后台或过期加载结果不补播。 */
export function playSound(sound: Sound): void {
  if (!audioPreferences.sound || !context || context.state !== 'running' || document.hidden) return;
  const now = performance.now();
  if (now - (lastPlayed.get(sound) ?? -Infinity) < (sound === 'step' ? 80 : (sound === 'chat' || sound === 'mention') ? 350 : 60)) return;
  lastPlayed.set(sound, now);
  const device = context;
  const generation = soundGeneration;
  void loadSound(sound, device).then(buffer => {
    if (!buffer || context !== device || device.state !== 'running' || !audioPreferences.sound || document.hidden || generation !== soundGeneration || performance.now() - now > 250) return;
    const voice = device.createBufferSource();
    voice.buffer = buffer;
    voice.connect(soundGain!);
    voices.add(voice);
    voice.onended = () => { voices.delete(voice); voice.disconnect(); };
    voice.start();
  });
}

/** 无入参，停止仍在播放或等待的短音效，避免静音、切后台后残留。 */
function stopSounds(): void { soundGeneration++; for (const voice of voices) voice.stop(); }

/** 无入参，按当前场景切换本地音乐，切歌淡出后淡入；过期播放结果不修改新场景。 */
function updateMusic(): void {
  clearTimeout(fadeTimer);
  const generation = ++musicGeneration;
  if (!music || !context || !musicGain) return;
  const track = scene && audioPreferences.music && !document.hidden ? tracks[scene] : null;
  musicGain.gain.cancelScheduledValues(context.currentTime);
  if (!track) { music.pause(); musicGain.gain.setValueAtTime(0, context.currentTime); return; }
  if (music.getAttribute('src') === track && !music.paused) {
    musicGain.gain.setTargetAtTime(audioPreferences.musicVolume, context.currentTime, 0.15); return;
  }
  musicGain.gain.setTargetAtTime(0, context.currentTime, 0.08);
  /** 无入参，开始当前曲目的播放并淡入；过期场景的播放结果直接忽略。 */
  function startTrack(): void {
    if (!track || generation !== musicGeneration || !music || !context || !musicGain) return;
    if (music.getAttribute('src') !== track) { music.pause(); music.src = track; musicFailed.value = false; }
    else if (music.error) music.load();
    musicGain.gain.setValueAtTime(0, context.currentTime);
    void music.play().then(() => {
      if (generation !== musicGeneration || !context || !musicGain) return;
      audioBlocked.value = false;
      musicFailed.value = false;
      musicGain.gain.setTargetAtTime(audioPreferences.musicVolume, context.currentTime, 0.25);
    }).catch((error: unknown) => {
      if (generation !== musicGeneration) return;
      if (error instanceof DOMException && error.name === 'NotAllowedError') audioBlocked.value = true;
      else musicFailed.value = true;
    });
  }
  if (music.paused) startTrack();
  else fadeTimer = setTimeout(startTrack, 240);
}

/** 输入可选试听标志，通过用户手势启用共享音频设备，返回启用是否成功。 */
export async function enableAudio(preview = false): Promise<boolean> {
  if (!audioPreferences.music && !audioPreferences.sound) return false;
  try {
    if (!context) {
      context = new AudioContext();
      soundGain = context.createGain(); soundGain.connect(context.destination);
      musicGain = context.createGain(); musicGain.connect(context.destination);
      music = new Audio(); music.loop = true; music.preload = 'none';
      context.createMediaElementSource(music).connect(musicGain);
      music.addEventListener('error', () => { musicFailed.value = true; });
      sounds.forEach(sound => { void loadSound(sound, context!); });
    }
    const shouldResume = !unlocked || context.state !== 'running' || audioBlocked.value || musicFailed.value;
    await context.resume();
    unlocked = context.state === 'running'; audioBlocked.value = !unlocked;
    soundGain!.gain.setValueAtTime(audioPreferences.sound ? audioPreferences.soundVolume : 0, context.currentTime);
    if (shouldResume) updateMusic();
    if (preview) playSound('turn');
    return unlocked;
  } catch { audioBlocked.value = true; return false; }
}

/** 输入场景来源，在应用生命周期内管理音乐、点击音、音量及后台暂停；离开时释放音频。 */
export function useAudioEnvironment(musicScene: () => MusicScene): void {
  watch(musicScene, value => { scene = value; if (unlocked) updateMusic(); }, { immediate: true });
  watch(audioPreferences, () => {
    localStorage.setItem('dfw:music', audioPreferences.music ? 'on' : 'off');
    localStorage.setItem('dfw:turn-sound', audioPreferences.sound ? 'on' : 'off');
    localStorage.setItem('dfw:music-volume', String(audioPreferences.musicVolume));
    localStorage.setItem('dfw:sound-volume', String(audioPreferences.soundVolume));
    if (context && soundGain) soundGain.gain.setTargetAtTime(audioPreferences.sound ? audioPreferences.soundVolume : 0, context.currentTime, 0.02);
    if (!audioPreferences.sound) stopSounds();
    if (unlocked) updateMusic();
  });
  /** 输入用户手势，启用音频，并只为可操作按钮或链接播放一次点击反馈。 */
  function gesture(event: Event): void {
    if (event instanceof KeyboardEvent && (event.repeat || !['Enter', ' '].includes(event.key))) return;
    const button = event.target instanceof Element ? event.target.closest('button, a, summary') : null;
    void enableAudio().then(() => { if (button && !button.matches(':disabled, [aria-disabled="true"]')) playSound('click'); });
  }
  /** 无入参，切后台暂停音乐与提示音，返回页面后恢复已启用的音乐。 */
  function visibility(): void {
    if (document.hidden) { updateMusic(); stopSounds(); void context?.suspend(); }
    else if (unlocked) void enableAudio();
  }
  onMounted(() => {
    document.addEventListener('pointerdown', gesture); document.addEventListener('keydown', gesture);
    document.addEventListener('visibilitychange', visibility);
    audioBlocked.value = audioPreferences.music || audioPreferences.sound;
  });
  onBeforeUnmount(() => {
    document.removeEventListener('pointerdown', gesture); document.removeEventListener('keydown', gesture);
    document.removeEventListener('visibilitychange', visibility);
    clearTimeout(fadeTimer); musicGeneration++; music?.pause(); stopSounds(); void context?.close();
  });
}
