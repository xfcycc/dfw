import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { RealtimeClient } from '../../shared/realtime.js';
import type { CommandResult, GameAction, GameCommand, PrivatePlayerState, PublicRoomState, Session } from '../../shared/types.js';

/** 输入会话，返回该浏览器的角色专用存储键。 */
function sessionKey(code: string, spectator: boolean): string { return `dfw:${spectator ? 'viewer' : 'player'}:${code}`; }
/** 输入房间号和角色，从浏览器恢复本地会话。 */
export function readSession(code: string, spectator = false): Session | null {
  try {
    const value = JSON.parse(localStorage.getItem(sessionKey(code, spectator)) ?? 'null') as Session | null;
    return value?.code === code && typeof value.token === 'string' ? value : null;
  } catch { return null; }
}

/** 返回唯一命令 ID，兼容手机通过局域网 HTTP 打开的非安全上下文。 */
function commandId(): string {
  if (typeof crypto.randomUUID === 'function') return crypto.randomUUID();
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 15) | 64;
  bytes[8] = (bytes[8] & 63) | 128;
  const hex = Array.from(bytes, value => value.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

export const useGameStore = defineStore('game', () => {
  const room = ref<PublicRoomState | null>(null);
  const privateData = ref<PrivatePlayerState | null>(null);
  const session = ref<Session | null>(null);
  const connection = ref<'connecting' | 'online' | 'offline' | 'replaced'>('offline');
  const busy = ref(false);
  const message = ref('');
  const fatalError = ref('');
  let socket: RealtimeClient | null = null;
  let messageTimer: ReturnType<typeof setTimeout> | undefined;
  const me = computed(() => room.value?.players.find(player => player.id === session.value?.playerId));
  const current = computed(() => room.value?.players.find(player => player.id === room.value?.turnOrder[room.value.turnIndex]));
  const isMyTurn = computed(() => !!me.value && current.value?.id === me.value.id && !me.value.bankrupt);
  const isHost = computed(() => !!me.value && room.value?.hostId === me.value.id);
  const canAct = computed(() => connection.value === 'online' && !!room.value && !room.value.pausedReason && !busy.value);

  /** 输入提示文案，在全局轻提示中短暂展示。 */
  function notify(text: string): void {
    message.value = text;
    clearTimeout(messageTimer);
    messageTimer = setTimeout(() => { message.value = ''; }, 5000);
  }

  /** 输入接口操作和房间资料，保存返回身份并返回会话。 */
  async function enter(kind: 'create' | 'join' | 'spectate', code = '', name = ''): Promise<Session> {
    const response = await fetch(`/api/rooms/${kind}`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ code, name }),
    });
    const data = await response.json() as Session & CommandResult;
    if (!response.ok || !data.token) throw new Error(data.error || '无法进入房间');
    localStorage.setItem(sessionKey(data.code, data.playerId === null), JSON.stringify(data));
    return data;
  }

  /** 关闭当前网络连接并清空页面状态；不删除恢复令牌。 */
  function disconnect(): void {
    socket?.removeAllListeners();
    socket?.disconnect();
    socket = null;
    room.value = null;
    privateData.value = null;
    session.value = null;
    busy.value = false;
    connection.value = 'offline';
    fatalError.value = '';
  }

  /** 输入会话，建立角色独立连接，接收公开或个人专属快照。 */
  function connect(value: Session): void {
    disconnect();
    session.value = value;
    connection.value = 'connecting';
    const scheme = location.protocol === 'https:' ? 'wss:' : 'ws:';
    socket = new RealtimeClient(`${scheme}//${location.host}/ws`, value.token);
    socket.on('connect', () => { connection.value = 'online'; fatalError.value = ''; socket?.emit('room:sync'); });
    socket.on('room:snapshot', (state: PublicRoomState) => { room.value = state; });
    socket.on('player:private', (state: PrivatePlayerState) => {
      if (value.playerId && state.playerId === value.playerId) privateData.value = state;
    });
    socket.on('disconnect', () => { if (connection.value !== 'replaced') connection.value = 'offline'; });
    socket.on('connect_error', (error: Error) => {
      connection.value = 'offline';
      fatalError.value = error.message;
    });
    socket.on('session:replaced', () => {
      connection.value = 'replaced';
      fatalError.value = '这个席位已在另一个页面打开。请关闭该页面，或在这里重新连接。';
    });
    socket.on('room:error', (result: CommandResult) => { fatalError.value = result.error || '连接失败'; });
    socket.connect();
  }

  /** 输入业务操作，发送唯一命令并等待回执，返回是否成功。 */
  async function act(action: GameAction): Promise<boolean> {
    if (!socket?.connected || !room.value || busy.value) return false;
    const command: GameCommand = { id: commandId(), expectedVersion: room.value.version, action };
    busy.value = true;
    try {
      const result = await socket.request(command);
      if (!result.ok) notify(result.error || '操作未完成');
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
  return { room, privateData, session, connection, busy, message, fatalError, me, current, isMyTurn, isHost, canAct, enter, connect, disconnect, act, notify, copy };
});
