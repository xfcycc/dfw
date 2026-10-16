import { onBeforeUnmount, ref, watch } from 'vue';
import type { PublicRoomState } from '../../shared/types';
import { playSound, type Sound } from '../audio';

const DICE_ROLL_MS = 780;
const DICE_HOLD_MS = 220;
const MOVE_STEP_MS = 190;
const LANDING_MS = 500;

/** 输入实时房间、连接状态、路径回调、现金栏玩家及本地重置标记，返回视听状态；同步重置时取消旧反馈。 */
export function useGameEffects(room: () => PublicRoomState, live: () => boolean, settled: (id: string) => void = () => {}, cashPlayer: () => string | undefined = () => undefined, presentationEpoch: () => number = () => 0) {
  const money = ref<{ key: string; tileId: number; name: string; amount: number }[]>([]);
  const tiles = ref<{ key: string; tileId: number; kind: 'bought' | 'built' | 'magic' }[]>([]);
  const rolling = ref(false);
  const celebrating = ref(false);
  const motion = ref<PublicRoomState['motion']>(null);
  const animatedPosition = ref<{ id: string; position: number } | null>(null);
  const moving = ref(false);
  const landing = ref(false);
  const step = ref(0);
  const diceFace = ref(1);
  const diceResult = ref<number | null>(null);
  const timers = new Set<ReturnType<typeof setTimeout>>();
  const motionTimers = new Set<ReturnType<typeof setTimeout>>();
  let baseline = false;
  /** 输入延时、清理操作及可选定时器组，登记短动画的清理定时器，无返回值。 */
  function later(delay: number, cleanup: () => void, group = timers): void {
    const timer = setTimeout(() => { group.delete(timer); cleanup(); }, delay);
    group.add(timer);
  }
  /** 无入参，取消旧路径和延迟声音，恢复服务端位置，无返回值。 */
  function clearMotion(): void {
    motionTimers.forEach(clearTimeout); motionTimers.clear();
    motion.value = null; animatedPosition.value = null; rolling.value = false;
    moving.value = false; landing.value = false; step.value = 0; diceResult.value = null;
  }
  /** 输入快照、掷骰标识及落点反馈，展示走格或直接传送动画，无返回值。 */
  function animateMotion(next: PublicRoomState, roll: boolean, feedback: () => void): void {
    clearMotion();
    const movement = next.motion!;
    if (roll) playSound('dice');
    if (document.hidden || window.matchMedia('(prefers-reduced-motion: reduce)').matches) { feedback(); settled(movement.id); return; }
    motion.value = movement; diceResult.value = roll ? movement.dice ?? next.dice : null;
    animatedPosition.value = { id: movement.playerId, position: movement.from };
    rolling.value = roll; diceFace.value = 1;
    const delay = roll ? DICE_ROLL_MS + DICE_HOLD_MS : 0;
    if (roll) {
      for (let at = 80; at < DICE_ROLL_MS; at += 80) later(at, () => { diceFace.value = diceFace.value % 6 + 1; }, motionTimers);
      later(DICE_ROLL_MS, () => { rolling.value = false; diceFace.value = diceResult.value!; }, motionTimers);
    }
    movement.path.forEach((position, index) => {
      later(delay + index * MOVE_STEP_MS, () => {
        moving.value = true; step.value = index + 1;
        animatedPosition.value = { id: movement.playerId, position };
        playSound(movement.teleport ? 'magic' : 'step');
      }, motionTimers);
    });
    later(delay + movement.path.length * MOVE_STEP_MS, () => {
      moving.value = false; landing.value = true; feedback();
    }, motionTimers);
    later(delay + movement.path.length * MOVE_STEP_MS + LANDING_MS, () => { clearMotion(); settled(movement.id); }, motionTimers);
  }
  /** 无入参，停止当前展示并清理动画定时器，无返回值。 */
  function clear(): void { timers.forEach(clearTimeout); timers.clear(); money.value = []; tiles.value = []; celebrating.value = false; clearMotion(); }
  /** 无入参，切到后台时取消未播放反馈，防止返回页面补播旧路径，无返回值。 */
  function onVisibilityChange(): void { if (document.hidden) clear(); }
  document.addEventListener('visibilitychange', onVisibilityChange);
  watch(live, enabled => { if (!enabled) { baseline = true; clear(); } }, { flush: 'sync' });
  watch([room, presentationEpoch], ([next, epoch], [previous, previousEpoch]) => {
    if (epoch !== previousEpoch) { baseline = false; clear(); return; }
    if (!live()) return;
    if (baseline || !previous || next.roomId !== previous.roomId || next.gameId !== previous.gameId || next.recoveryPending || previous.recoveryPending) { baseline = false; clear(); return; }
    if (next.version < previous.version || next.version === previous.version && next.motion?.id === previous.motion?.id || next.status === 'lobby') return;
    let sound: Sound | null = null;
    let cashSound: Sound | null = null;
    const key = `${next.gameId}:${next.version}`;
    const changes: typeof money.value = [];
    const effects: typeof tiles.value = [];
    const previousPlayers = new Map(previous.players.map(player => [player.id, player]));
    for (const player of next.players) {
      const before = previousPlayers.get(player.id);
      if (!before || player.cash === before.cash) continue;
      const change = { key: `${key}:${player.id}`, tileId: player.position, name: player.name, amount: player.cash - before.cash };
      changes.push(change);
      if (player.id !== cashPlayer()) cashSound ??= change.amount > 0 ? 'coin' : 'pay';
    }
    const previousTiles = new Map(previous.board.map(tile => [tile.id, tile]));
    for (const tile of next.board) {
      const before = previousTiles.get(tile.id);
      if (!before) continue;
      const kind = tile.level > before.level ? 'built' : tile.ownerId && !before.ownerId ? 'bought' : tile.barrier !== before.barrier || tile.level < before.level ? 'magic' : null;
      if (!kind) continue;
      const effect: typeof tiles.value[number] = { key: `${key}:${tile.id}`, tileId: tile.id, kind };
      effects.push(effect);
      sound = kind === 'built' ? 'build' : kind === 'bought' ? 'buy' : 'magic';
    }
    if (next.event && next.event.id !== previous.event?.id) sound = 'card';
    if (next.status === 'finished' && previous.status !== 'finished') {
      celebrating.value = true; sound = 'victory';
      later(3000, () => { celebrating.value = false; });
    }
    /** 无入参，在角色落地后展示本次金额、地块及声音反馈，无返回值。 */
    const feedback = (): void => {
      for (const change of changes) {
        money.value = [...money.value, change].slice(-12);
        later(1600, () => { money.value = money.value.filter(value => value.key !== change.key); });
      }
      for (const effect of effects) {
        tiles.value = [...tiles.value.filter(value => value.tileId !== effect.tileId), effect];
        later(1100, () => { tiles.value = tiles.value.filter(value => value.key !== effect.key); });
      }
      if (sound) playSound(sound);
      if (cashSound) playSound(cashSound);
    };
    if (next.motion && next.motion.id !== previous.motion?.id) {
      const roll = !next.motion.teleport && (next.motion.dice !== undefined ? next.motion.dice !== null && next.motion.dice > 0 : next.dice !== null && next.dice > 0 && (next.dice !== previous.dice || next.turnNumber !== previous.turnNumber || previous.phase === 'pre_roll'));
      animateMotion(next, roll, feedback);
    } else {
      const actor = motion.value?.playerId;
      if (!next.motion || next.turnNumber !== previous.turnNumber || next.status !== previous.status || (actor && next.players.find(player => player.id === actor)?.position !== previousPlayers.get(actor)?.position)) clearMotion();
      feedback();
    }
  }, { flush: 'pre' });
  onBeforeUnmount(() => { document.removeEventListener('visibilitychange', onVisibilityChange); clear(); });
  return { money, tiles, rolling, celebrating, motion, animatedPosition, moving, landing, step, diceFace, diceResult, moveStepMs: MOVE_STEP_MS };
}
