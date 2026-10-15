import { defineStore } from 'pinia';
import { computed, nextTick, ref } from 'vue';
import { RealtimeClient } from '../../shared/realtime.js';
import type { CommandResult, ContentResult, GameAction, GameCommand, MotionSegment, PrivatePlayerState, PublicRoomState, Session } from '../../shared/types.js';
import { postJson } from '../http';
import { playSound } from '../audio';

/** 输入会话，返回该浏览器的角色专用存储键。 */
function sessionKey(identity: string, spectator: boolean, visitor = false): string { return `dfw:${visitor ? 'guest' : spectator ? 'viewer' : 'player'}:${identity}`; }
/** 输入房间号和角色，从浏览器恢复本地会话。 */
export function readSession(code: string, spectator = false): Session | null {
  try {
    const roomId = localStorage.getItem(`dfw:room-link:${code}`);
    const stored = (roomId ? localStorage.getItem(sessionKey(roomId, spectator)) : null) ?? localStorage.getItem(sessionKey(code, spectator));
    const guest = spectator ? null : (roomId ? localStorage.getItem(sessionKey(roomId, true, true)) : null) ?? localStorage.getItem(sessionKey(code, true, true));
    const value = JSON.parse(stored ?? guest ?? 'null') as Session | null;
    return value?.code === code && typeof value.token === 'string' ? value : null;
  } catch { return null; }
}

/** 返回唯一命令 ID，兼容手机通过局域网 HTTP 打开的非安全上下文。 */
export function commandId(): string {
  if (typeof crypto.randomUUID === 'function') return crypto.randomUUID();
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 15) | 64;
  bytes[8] = (bytes[8] & 63) | 128;
  const hex = Array.from(bytes, value => value.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

/** 本浏览器参与过的房间档案索引，不保存额外个人恢复凭证。 */
export interface RecentRoom { roomId: string; code: string; archiveCode: string; at: number }
/** 仅客户端展示的揭卡队列，私人内容不会通过网络发布。 */
export interface CardReveal { key: string; content: ContentResult; mode: 'draw' | 'play'; actor: string }
/** 无入参，读取本机参与记录；旧或损坏记录返回空列表。 */
export function recentRooms(): RecentRoom[] {
  try { const values: unknown = JSON.parse(localStorage.getItem('dfw:archives') ?? '[]'); return Array.isArray(values) ? values.filter((value): value is RecentRoom => !!value && typeof value.roomId === 'string' && typeof value.code === 'string' && typeof value.archiveCode === 'string') : []; }
  catch { return []; }
}
/** 输入永久房间标识，仅移除本地参与记录及对应最近查看码，保留会话与服务端存档，无返回值。 */
export function forgetRecentRoom(roomId: string): void {
  const rooms = recentRooms();
  const lastCode = localStorage.getItem('dfw:archive:last');
  localStorage.setItem('dfw:archives', JSON.stringify(rooms.filter(room => room.roomId !== roomId)));
  if (rooms.some(room => room.roomId === roomId && room.archiveCode === lastCode)) localStorage.removeItem('dfw:archive:last');
}
/** 输入档案索引，按永久房间标识更新参与记录及最近查看的固定码。 */
export function rememberArchive(value: Omit<RecentRoom, 'at'>): void {
  localStorage.setItem('dfw:archives', JSON.stringify([{ ...value, at: Date.now() }, ...recentRooms().filter(room => room.roomId !== value.roomId)]));
  localStorage.setItem('dfw:archive:last', value.archiveCode);
}
/** 输入有效会话，以永久房间标识保存身份，同时兼容旧房间号键。 */
export function rememberSession(value: Session): void {
  const spectator = value.playerId === null;
  const visitor = spectator && !!value.visitor;
  localStorage.setItem(sessionKey(value.roomId ?? value.code, spectator, visitor), JSON.stringify(value));
  localStorage.setItem(sessionKey(value.code, spectator, visitor), JSON.stringify(value));
  if (!spectator) { localStorage.removeItem(sessionKey(value.code, true, true)); if (value.roomId) localStorage.removeItem(sessionKey(value.roomId, true, true)); }
  if (value.roomId) localStorage.setItem(`dfw:room-link:${value.code}`, value.roomId);
  if (value.roomId && value.archiveCode) rememberArchive({ roomId: value.roomId, code: value.code, archiveCode: value.archiveCode });
}
/** 输入房间号和会话种类，同时删除旧键与永久标识键，供显式重新加入。 */
export function forgetSession(code: string, spectator: boolean): void {
  const roomId = localStorage.getItem(`dfw:room-link:${code}`); if (roomId) localStorage.removeItem(sessionKey(roomId, spectator)); localStorage.removeItem(sessionKey(code, spectator));
  if (!spectator) { localStorage.removeItem(sessionKey(code, true, true)); if (roomId) localStorage.removeItem(sessionKey(roomId, true, true)); }
}

export const useGameStore = defineStore('game', () => {
  const room = ref<PublicRoomState | null>(null);
  const privateData = ref<PrivatePlayerState | null>(null);
  const session = ref<Session | null>(null);
  const connection = ref<'connecting' | 'online' | 'offline' | 'replaced'>('offline');
  const busy = ref(false);
  const settling = ref(false);
  const settlementRevealing = ref(false);
  const message = ref('');
  const fatalError = ref('');
  const cardReveals = ref<CardReveal[]>([]);
  const seenReveals = new Set<string>();
  let publicBaseline = true;
  let privateBaseline = true;
  let chatState: PublicRoomState | null = null;
  let socket: RealtimeClient | null = null;
  let messageTimer: ReturnType<typeof setTimeout> | undefined;
  let movementResult: PublicRoomState | null = null;
  const movementSegments: MotionSegment[] = [];
  let latestVersion = 0;
  const snapshots: ({ type: 'public'; state: PublicRoomState } | { type: 'private'; state: PrivatePlayerState })[] = [];
  const me = computed(() => room.value?.players.find(player => player.id === session.value?.playerId));
  const current = computed(() => room.value?.players.find(player => player.id === room.value?.turnOrder[room.value.turnIndex]));
  const isMyTurn = computed(() => !!me.value && current.value?.id === me.value.id && !me.value.bankrupt);
  const decisionPlayer = computed(() => room.value?.pending ? room.value.players.find(player => player.id === room.value?.pending?.playerId) : current.value);
  const canDecide = computed(() => !!me.value && decisionPlayer.value?.id === me.value.id && !me.value.bankrupt);
  const isHost = computed(() => !!me.value && room.value?.hostId === me.value.id);
  const canAct = computed(() => connection.value === 'online' && !!room.value && !room.value.pausedReason && !busy.value && !settling.value);

  /** 输入刚收到的公开快照，仅为其他玩家的新聊天播放提示音；初次同步、重连及旧版本建立基线或忽略。 */
  function notifyChat(state: PublicRoomState): void {
    const previous = chatState;
    const sameGame = previous && state.code === previous.code && state.roomId === previous.roomId && state.gameId === previous.gameId;
    if (sameGame && state.version <= previous.version) return;
    chatState = state;
    if (!sameGame) return;
    const seen = new Set(previous.logs.map(entry => entry.id));
    const ownName = state.players.find(player => player.id === session.value?.playerId)?.name;
    if (state.logs.some(entry => entry.type === 'chat' && !seen.has(entry.id) && (!ownName || !entry.text.startsWith(`${ownName}：`)))) playSound('chat');
  }

  /** 输入公开快照，新移动先展示路径再同步结算；跳过路径动画时仍按顺序揭示各段公开事件，无返回值。 */
  function applyPublicState(state: PublicRoomState, animate = true): void {
    const previous = room.value;
    if (animate && !publicBaseline && previous && state.version > previous.version && state.gameId === previous.gameId && !state.recoveryPending && !previous.recoveryPending && state.motion && state.motion.id !== previous.motion?.id && state.status !== 'lobby' && !document.hidden && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      movementResult = state; settling.value = true;
      movementSegments.push(...(state.motion.segments?.length ? state.motion.segments : [state.motion]));
      presentMovement();
      return;
    }
    room.value = state;
    if (previous?.gameId !== state.gameId) { cardReveals.value = []; seenReveals.clear(); }
    if (!publicBaseline && previous && state.version >= previous.version && !state.recoveryPending && !previous.recoveryPending) {
      if (state.playedCard && state.playedCard.id !== previous.playedCard?.id) enqueueCardReveal(state.playedCard, 'play', state.players.find(player => player.id === state.playedCardPlayerId)?.name ?? '玩家');
      if (state.motion && state.motion.id !== previous.motion?.id) {
        for (const segment of state.motion.segments?.length ? state.motion.segments : [state.motion]) {
          const actor = state.players.find(player => player.id === segment.playerId)?.name ?? '玩家';
          if (segment.beforeEvent) enqueueCardReveal(segment.beforeEvent, 'draw', actor);
          if (segment.event) enqueueCardReveal(segment.event, 'draw', actor);
        }
      }
      if (state.event && state.event.id !== previous.event?.id) enqueueCardReveal(state.event, 'draw', state.players.find(player => player.id === state.eventPlayerId)?.name ?? '玩家');
    }
    publicBaseline = false;
  }
  /** 无入参，先揭示触发事件再将下一段路径送到棋盘，结算结果继续等待，无返回值。 */
  function presentMovement(): void {
    const state = movementResult;
    const segment = movementSegments[0];
    if (!state || !segment || !room.value) return;
    if (segment.beforeEvent && segment.beforeEvent.id !== room.value.event?.id) {
      room.value = { ...room.value, event: segment.beforeEvent, eventPlayerId: segment.playerId };
      enqueueCardReveal(segment.beforeEvent, 'draw', room.value.players.find(player => player.id === segment.playerId)?.name ?? '玩家');
      playSound('card');
      if (cardReveals.value.length) { settlementRevealing.value = true; return; }
    }
    movementSegments.shift();
    settlementRevealing.value = false;
    room.value = { ...room.value, dice: state.dice, motion: segment, pending: null, turnNumber: state.turnNumber, turnIndex: state.turnIndex,
      players: room.value.players.map(player => player.id === segment.playerId ? { ...player, position: segment.path.at(-1) ?? segment.from } : player) };
  }
  /** 输入本人私人快照，同步手牌、补给揭卡与选择，首次连接只建立基线，无返回值。 */
  function applyPrivateState(state: PrivatePlayerState): void {
    if (!session.value?.playerId || state.playerId !== session.value.playerId) return;
    const previous = privateData.value;
    privateData.value = state;
    if (!privateBaseline && previous?.playerId === state.playerId && !room.value?.recoveryPending) {
      const actor = me.value?.name ?? '你';
      if (state.tradeRejection && state.tradeRejection !== previous.tradeRejection) notify(state.tradeRejection);
      if (state.event && state.event.id !== previous.event?.id) enqueueCardReveal(state.event, 'draw', actor);
      for (const card of state.hand) if (!previous.hand.some(before => before.id === card.id)) enqueueCardReveal(card, 'draw', actor);
      if (state.notice && state.notice.id !== previous.notice?.id) enqueueCardReveal(state.notice, 'draw', actor);
    }
    privateBaseline = false;
    if (state.archiveCode) { session.value = { ...session.value, archiveCode: state.archiveCode }; rememberSession(session.value); }
  }
  /** 输入已落地的片段 ID，先揭示落点事件并接续移动，最后释放结果，返回完成状态的异步操作。 */
  async function finishMovement(id: string): Promise<void> {
    if (!movementResult || room.value?.motion?.id !== id) return;
    if (movementSegments.length) {
      const event = room.value.motion.event;
      if (event) {
        const playerId = room.value.motion.playerId;
        room.value = { ...room.value, event, eventPlayerId: playerId };
        enqueueCardReveal(event, 'draw', room.value.players.find(player => player.id === playerId)?.name ?? '玩家');
        playSound('card');
      }
      if (cardReveals.value.length) settlementRevealing.value = true;
      else presentMovement();
      return;
    }
    const result = movementResult; movementResult = null;
    applyPublicState(result, false);
    while (snapshots.length) {
      if (snapshots[0].type === 'public') {
        await nextTick();
        if (!settling.value) return;
      }
      const snapshot = snapshots.shift()!;
      if (snapshot.type === 'public') {
        applyPublicState(snapshot.state);
        if (movementResult) return;
      } else applyPrivateState(snapshot.state);
    }
    settling.value = false;
  }
  /** 无入参，取消积压动画并静默同步最新收包结果，供断线、身份切换或后台使用。 */
  function syncPresentation(): void {
    const pending = movementResult; movementResult = null; movementSegments.length = 0; settling.value = false; settlementRevealing.value = false;
    publicBaseline = true; privateBaseline = true;
    if (pending) applyPublicState(pending, false);
    while (snapshots.length) {
      const snapshot = snapshots.shift()!;
      if (snapshot.type === 'public') { publicBaseline = true; applyPublicState(snapshot.state, false); }
      else { privateBaseline = true; applyPrivateState(snapshot.state); }
    }
    cardReveals.value = [];
  }

  /** 无入参，页面进入后台时同步最新局面，不补播未完成的移动和揭卡。 */
  function onVisibilityChange(): void { if (document.hidden) syncPresentation(); }

  /** 输入服务端已确认的卡面和展示方式，按身份去重并排队，无返回值。 */
  function enqueueCardReveal(content: ContentResult, mode: CardReveal['mode'], actor: string): void {
    if (document.hidden) return;
    const key = `${room.value?.gameId}:${mode}:${content.id}`;
    if (seenReveals.has(key)) return;
    seenReveals.add(key);
    const reveal = { key, content, mode, actor };
    cardReveals.value.push(reveal);
  }
  /** 无入参，关闭当前卡面，揭卡队列结束后接续下一段移动；不推进服务端状态，无返回值。 */
  function dismissCardReveal(): void {
    cardReveals.value.shift();
    if (settlementRevealing.value && !cardReveals.value.length) presentMovement();
  }

  /** 输入提示文案，在全局轻提示中短暂展示。 */
  function notify(text: string): void {
    message.value = text;
    clearTimeout(messageTimer);
    messageTimer = setTimeout(() => { message.value = ''; }, 5000);
  }

  /** 输入接口操作和房间资料，保存返回身份并返回会话。 */
  async function enter(kind: 'create' | 'join' | 'spectate', code = '', name = ''): Promise<Session> {
    const data = await postJson<Session & CommandResult>(`rooms/${kind}`, { code, name });
    if (!data.token) throw new Error(data.error || '无法进入房间');
    rememberSession(data);
    return data;
  }

  /** 关闭当前网络连接并清空页面状态；不删除恢复令牌。 */
  function disconnect(): void {
    socket?.removeAllListeners();
    socket?.disconnect();
    socket = null;
    document.removeEventListener('visibilitychange', onVisibilityChange);
    movementResult = null; movementSegments.length = 0; snapshots.length = 0; settling.value = false; settlementRevealing.value = false; latestVersion = 0;
    room.value = null;
    privateData.value = null;
    session.value = null;
    busy.value = false;
    connection.value = 'offline';
    fatalError.value = '';
    cardReveals.value = []; seenReveals.clear();
    publicBaseline = true; privateBaseline = true; chatState = null;
  }

  /** 输入会话，建立角色独立连接，接收公开或个人专属快照。 */
  function connect(value: Session): void {
    disconnect();
    session.value = value;
    connection.value = 'connecting';
    const scheme = location.protocol === 'https:' ? 'wss:' : 'ws:';
    socket = new RealtimeClient(`${scheme}//${location.host}/ws`, value.token);
    document.addEventListener('visibilitychange', onVisibilityChange);
    socket.on('connect', () => { syncPresentation(); publicBaseline = true; privateBaseline = true; chatState = null; connection.value = 'online'; fatalError.value = ''; socket?.emit('room:sync'); });
    socket.on('room:snapshot', (state: PublicRoomState) => {
      notifyChat(state);
      latestVersion = Math.max(latestVersion, state.version);
      if (publicBaseline || state.gameId !== room.value?.gameId || state.recoveryPending) {
        movementResult = null; movementSegments.length = 0; snapshots.length = 0; settling.value = false; settlementRevealing.value = false;
        applyPublicState(state, false);
      } else if (settling.value) snapshots.push({ type: 'public', state });
      else applyPublicState(state);
    });
    socket.on('player:private', (state: PrivatePlayerState) => {
      if (settling.value) snapshots.push({ type: 'private', state });
      else applyPrivateState(state);
    });
    socket.on('session:identity', identity => {
      if (!session.value || identity.code !== session.value.code) return;
      syncPresentation();
      session.value = { ...session.value, ...identity }; privateData.value = null; privateBaseline = true; cardReveals.value = []; seenReveals.clear(); rememberSession(session.value);
    });
    socket.on('disconnect', () => { if (connection.value !== 'replaced') connection.value = 'offline'; syncPresentation(); publicBaseline = true; privateBaseline = true; });
    socket.on('connect_error', (error: Error) => {
      connection.value = 'offline';
      fatalError.value = error.message;
    });
    socket.on('session:replaced', () => {
      connection.value = 'replaced';
      syncPresentation();
      fatalError.value = '这个席位已在另一个页面打开。请关闭该页面，或在这里重新连接。';
    });
    socket.on('room:error', (result: CommandResult) => { fatalError.value = result.error || '连接失败'; });
    socket.connect();
  }

  /** 输入业务操作，发送唯一命令并等待回执，返回是否成功。 */
  async function act(action: GameAction): Promise<boolean> {
    if (!socket?.connected || !room.value || busy.value || settling.value && action.type !== 'chat') return false;
    const command: GameCommand = { id: commandId(), expectedVersion: latestVersion, action };
    busy.value = true;
    try {
      const result = await socket.request(command);
      if (!result.ok) { playSound('error'); notify(result.error || '操作未完成'); }
      return result.ok;
    } catch {
      socket?.emit('room:sync');
      notify('连接超时，正在核对最新局面。请查看结果后再操作。');
      return false;
    } finally { busy.value = false; }
  }

  /** 输入文本，复制到剪贴板，失败时给出可手动复制的内容。 */
  async function copy(text: string): Promise<void> {
    try { await navigator.clipboard.writeText(text); notify('已复制，发给朋友一起玩吧'); }
    catch { notify(`请手动复制：${text}`); }
  }
  return { room, privateData, session, connection, busy, settling, settlementRevealing, finishMovement, message, fatalError, cardReveals, dismissCardReveal, me, current, isMyTurn, decisionPlayer, canDecide, isHost, canAct, enter, connect, disconnect, act, notify, copy };
});
