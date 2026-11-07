import { onBeforeUnmount, ref, watch } from 'vue';
import type { PublicRoomState } from '../../shared/types';
import { playSound, type Sound } from '../audio';
import { boardEffectDuration } from '../board-effect-presentation';
import { prepareEffectSprite } from '../effect-sprites';
import { cardSpellFor } from '../card-spell-presentation';
import { prepareUrSpellAtlas } from '../ur-spell-preview';
import { fullCardSpellFor, prepareFullSpell } from '../full-card-spells';

const DICE_ROLL_MS = 780;
const DICE_HOLD_MS = 220;
const MOVE_STEP_MS = 190;
const LANDING_MS = 500;

/** 输入实时房间、连接状态、路径回调、现金栏玩家、重置标记、骰子模式、揭卡等待及局部演出条件，返回视听状态。 */
export function useGameEffects(room: () => PublicRoomState, live: () => boolean, settled: (id: string) => void = () => {}, cashPlayer: () => string | undefined = () => undefined, presentationEpoch: () => number = () => 0, desktopDice: () => boolean = () => false, paused: () => boolean = () => false, localEffects: () => boolean = () => false) {
  const money = ref<{ key: string; tileId: number; name: string; amount: number }[]>([]);
  const tiles = ref<{ key: string; tileId: number; kind: 'bought' | 'built' | 'magic' }[]>([]);
  const rolling = ref(false);
  const celebrating = ref(false);
  const motion = ref<PublicRoomState['motion']>(null);
  const boardEffect = ref<PublicRoomState['motion']>(null);
  const boardEffectKey = ref(0);
  let effectQueue: NonNullable<PublicRoomState['motion']>[] = [];
  let effectDuration = 0;
  let effectTimer: ReturnType<typeof setTimeout> | undefined;
  const lightSpell = ref<{key:number;card:NonNullable<ReturnType<typeof fullCardSpellFor>>;playerId:string;effect:NonNullable<PublicRoomState['motion']>['boardEffect']}|null>(null);
  const animatedPosition = ref<{ id: string; position: number } | null>(null);
  const moving = ref(false);
  const landing = ref(false);
  const step = ref(0);
  const diceFace = ref(1);
  const diceResult = ref<number | null>(null);
  const diceRollMs = ref(DICE_ROLL_MS);
  const timers = new Set<ReturnType<typeof setTimeout>>();
  const motionTimers = new Set<ReturnType<typeof setTimeout>>();
  let lightEpoch=0;
  let lightGroup:{sourceCardId:string|null|undefined;skinId:string;value:NonNullable<typeof lightSpell.value>}|null=null;
  let baseline = false;
  let deferredPrevious: PublicRoomState | null = null;
  let deferredLight: (() => void) | null = null;
  /** 输入延时、清理操作及可选定时器组，登记短动画的清理定时器，无返回值。 */
  function later(delay: number, cleanup: () => void, group = timers): void {
    const timer = setTimeout(() => { group.delete(timer); cleanup(); }, delay);
    group.add(timer);
  }
  /** 无入参，取消旧路径和延迟声音，恢复服务端位置，无返回值。 */
  function clearMotion(): void {
    clearTimeout(effectTimer); effectQueue = [];
    motionTimers.forEach(clearTimeout); motionTimers.clear();
    motion.value = null; boardEffect.value = null; animatedPosition.value = null; rolling.value = false;
    moving.value = false; landing.value = false; step.value = 0; diceResult.value = null;
  }
  /** 输入已完成的演出标识，幂等释放本段，服务端结算结果交原队列展示，无返回值。 */
  function finishBoardEffect(id: string): void {
    if (boardEffect.value?.id !== id) return;
    clearTimeout(effectTimer);
    if (effectQueue.length) { playNextEffect(); return; }
    clearMotion(); settled(id);
  }
  /** 无入参，播放下一个真实目标并重建时间轴，所有目标结束后才通知原结算队列。 */
  function playNextEffect(): void {
    boardEffectKey.value++;
    boardEffect.value = effectQueue.shift()!;
    effectTimer = setTimeout(() => finishBoardEffect(boardEffect.value!.id), effectDuration + 250);
  }
  /** 输入服务端演出，移动端按地产、位移或参与玩家拆分展示目标，返回不改变结算标识的片段。 */
  function localSegments(movement: NonNullable<PublicRoomState['motion']>): NonNullable<PublicRoomState['motion']>[] {
    const effect = movement.boardEffect!;
    if (!localEffects()) return [movement];
    const card = fullCardSpellFor(movement);
    const targets = effect.beforeTiles.length ? effect.beforeTiles.map(tile => ({ ...effect, beforeTiles: [tile], afterTiles: effect.afterTiles.filter(after => after.id === tile.id), moves: [], playerIds: [] }))
      : effect.moves.length ? effect.moves.map(move => ({ ...effect, moves: [move], playerIds: [move.playerId] }))
      : (effect.playerIds ?? []).filter(id => !card || id !== movement.playerId || ['all_cards', 'all_morale', 'all_income', 'bottom_bonus', 'handDestruction'].includes(card.mechanism)).map(id => ({ ...effect, playerIds: [id] }));
    return targets.length ? targets.map(boardEffect => ({ ...movement, boardEffect })) : [movement];
  }
  /** 输入零等待片段及卡面，合并连续同来源的真实收款人；预加载与提示均不阻塞原结算。 */
  function presentLightSpell(movement:NonNullable<PublicRoomState['motion']>,card:NonNullable<ReturnType<typeof fullCardSpellFor>>):void {
    const merge=!!movement.sourceCardId&&lightGroup?.sourceCardId===movement.sourceCardId&&lightGroup.skinId===card.skinId&&lightGroup.value.effect?.kind===movement.boardEffect!.kind;
    const group=merge?lightGroup!:{sourceCardId:movement.sourceCardId,skinId:card.skinId,value:{key:++lightEpoch,card,playerId:movement.playerId,effect:movement.boardEffect}};
    if(merge) group.value={...group.value,effect:{...group.value.effect!,playerIds:[...new Set([...(group.value.effect!.playerIds??[]),...(movement.boardEffect!.playerIds??[])])]}};
    lightGroup=group;
    if(merge) { if(lightSpell.value?.key===group.value.key) lightSpell.value=group.value; return; }
    const epoch=group.value.key;
    void prepareFullSpell(card).then(()=>{
      /** 无入参，素材就绪且卡牌遮罩关闭后播放轻提示，无返回值。 */
      const show = (): void => {
        if(epoch!==lightEpoch||document.hidden)return;
        lightSpell.value=group.value;
        later(card.rarity==='UR'?card.duration:900,()=>{if(epoch===lightEpoch){lightSpell.value=null;if(lightGroup===group)lightGroup=null;}});
      };
      if(paused()) deferredLight=show; else show();
    }).catch(error=>{if(lightGroup===group)lightGroup=null;console.warn('轻提示素材不可用，保留原金额反馈',error);});
  }
  /** 输入快照、掷骰标识及落点反馈，播放移动或启用特效；原地投骰结束后停留两秒再释放选择，无返回值。 */
  function animateMotion(next: PublicRoomState, roll: boolean, feedback: () => void): void {
    clearMotion();
    const movement = next.motion!;
    if (roll) playSound('dice');
    if (document.hidden || window.matchMedia('(prefers-reduced-motion: reduce)').matches) { feedback(); settled(movement.id); return; }
    if (movement.boardEffect) {
      if (boardEffectDuration(movement.boardEffect.kind) === 0) {
        const card=fullCardSpellFor(movement);
        if(card) presentLightSpell(movement,card); else lightGroup=null;
        feedback(); settled(movement.id); return;
      }
      lightGroup=null;
      motion.value = movement;
      const spell = cardSpellFor(movement);
      const full = fullCardSpellFor(movement);
      const prepare = spell?.theme === 'kamui' ? prepareUrSpellAtlas() : spell ? Promise.resolve() : full ? prepareFullSpell(full) : prepareEffectSprite(movement.boardEffect.kind);
      void Promise.race([prepare, new Promise<never>((_, reject) => later(4000, () => reject(new Error('素材加载超时')), motionTimers))]).catch(error => {
        console.warn('招式素材不可用，使用通用效果', error);
        return 'fallback' as const;
      }).then(result => {
        if (motion.value?.id !== movement.id) return;
        if (document.hidden || window.matchMedia('(prefers-reduced-motion: reduce)').matches) { clearMotion(); settled(movement.id); return; }
        effectQueue = localSegments(result === 'fallback' ? { ...movement, sourceSkinId: null } : movement); playSound('magic');
        // 图集解码完成后才启动时间轴和兜底计时，首次加载不占用播放时长。
        effectDuration = result === 'fallback' ? boardEffectDuration(movement.boardEffect!.kind) : spell?.duration ?? full?.duration ?? boardEffectDuration(movement.boardEffect!.kind);
        playNextEffect();
      });
      return;
    }
    lightGroup=null;
    motion.value = movement; diceResult.value = roll ? movement.dice ?? next.dice : null;
    animatedPosition.value = { id: movement.playerId, position: movement.from };
    rolling.value = roll; diceFace.value = 1;
    diceRollMs.value = desktopDice() ? 1400 : DICE_ROLL_MS;
    const diceOnly = roll && !movement.path.length;
    const holdMs = diceOnly ? 2000 : desktopDice() ? 650 : DICE_HOLD_MS;
    const delay = roll ? diceRollMs.value + holdMs : 0;
    if (roll) {
      for (let at = 80; at < diceRollMs.value; at += 80) later(at, () => { diceFace.value = diceFace.value % 6 + 1; }, motionTimers);
      later(diceRollMs.value, () => { rolling.value = false; diceFace.value = diceResult.value!; }, motionTimers);
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
    later(delay + movement.path.length * MOVE_STEP_MS + (diceOnly ? 0 : LANDING_MS), () => { clearMotion(); settled(movement.id); }, motionTimers);
  }
  /** 无入参，停止当前展示并清理动画定时器，无返回值。 */
  function clear(): void { lightEpoch++;lightGroup=null;deferredLight=null;timers.forEach(clearTimeout); timers.clear(); money.value = []; tiles.value = []; lightSpell.value=null; celebrating.value = false; clearMotion(); }
  /** 无入参，切到后台时取消未播放反馈，防止返回页面补播旧路径，无返回值。 */
  function onVisibilityChange(): void { if (document.hidden) clear(); }
  document.addEventListener('visibilitychange', onVisibilityChange);
  watch(live, enabled => { if (!enabled) { baseline = true; clear(); } }, { flush: 'sync' });
  watch([room, presentationEpoch, paused], ([next, epoch, blocked], [last, previousEpoch]) => {
    if (epoch !== previousEpoch) { deferredPrevious = null; baseline = false; clear(); return; }
    if (!live()) { deferredPrevious = null; return; }
    // 卡牌遮罩期间保留动画开始前的局面，关闭并更新 DOM 后再播放地图演出。
    if (blocked) { deferredPrevious ??= last; return; }
    const previous = deferredPrevious ?? last;
    deferredPrevious = null;
    deferredLight?.(); deferredLight = null;
    if (baseline || !previous || next.roomId !== previous.roomId || next.gameId !== previous.gameId || next.recoveryPending || previous.recoveryPending) { baseline = false; clear(); return; }
    // 队列被提前完成时，最终快照可能沿用同一 motion id；先撤下旧层，再判断是否需要新动画。
    if (motion.value?.boardEffect && (!next.motion?.boardEffect || next.motion.id !== motion.value.id)) clearMotion();
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
  }, { flush: 'post' });
  onBeforeUnmount(() => { document.removeEventListener('visibilitychange', onVisibilityChange); clear(); });
  return { money, tiles, rolling, celebrating, motion, boardEffect, boardEffectKey, lightSpell, finishBoardEffect, animatedPosition, moving, landing, step, diceFace, diceResult, diceRollMs, moveStepMs: MOVE_STEP_MS };
}
