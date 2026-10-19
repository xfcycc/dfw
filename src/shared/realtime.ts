import type { CommandResult, GameCommand, PrivatePlayerState, PublicRoomState, Session } from './types.js';

/** 原生 WebSocket 事件，与公开及私人数据契约分开。 */
interface Events {
  connect: undefined;
  disconnect: undefined;
  connect_error: Error;
  'room:snapshot': PublicRoomState;
  'player:private': PrivatePlayerState;
  'room:error': CommandResult;
  'session:replaced': undefined;
  'session:identity': Omit<Session, 'token'>;
}
/** 原生 WebSocket JSON 信封。 */
interface Message { type: string; requestId?: string; payload?: unknown }
/** 浏览器和联机冒烟共用的协议客户端；重连只恢复身份，不重发业务命令。 */
export class RealtimeClient {
  connected = false;
  private socket: WebSocket | null = null;
  private listeners = new Map<string, Set<(value: unknown) => void>>();
  private requests = new Map<string, { resolve: (result: CommandResult) => void; reject: (error: Error) => void; timer: ReturnType<typeof setTimeout> }>();
  private stopped = false;
  private replaced = false;
  private delay = 700;
  private retry?: ReturnType<typeof setTimeout>;
  private heartbeat?: ReturnType<typeof setInterval>;
  private lastSeen = 0;

  /** 输入联机地址、身份及是否重连；调用 connect 后建立连接。 */
  constructor(private url: string, private token: string, private reconnect = true) {}

  /** 输入事件及回调，注册监听器。 */
  on<K extends keyof Events>(type: K, listener: (value: Events[K]) => void): void {
    const values = this.listeners.get(type) ?? new Set();
    values.add(listener as (value: unknown) => void); this.listeners.set(type, values);
  }

  /** 输入事件及回调，注册仅执行一次的监听器。 */
  once<K extends keyof Events>(type: K, listener: (value: Events[K]) => void): void {
    const wrapped = (value: Events[K]) => { this.listeners.get(type)?.delete(wrapped as (value: unknown) => void); listener(value); };
    this.on(type, wrapped);
  }

  /** 输入事件与载荷，通知本地监听器。 */
  private dispatch(type: string, payload?: unknown): void { this.listeners.get(type)?.forEach(listener => listener(payload)); }

  /** 无入参，建立连接并提交原身份；临时保存失败继续重连，认证成功后才允许发送命令，无返回值。 */
  connect(): void {
    this.stopped = false; this.replaced = false;
    clearTimeout(this.retry);
    const socket = new WebSocket(this.url); this.socket = socket;
    socket.addEventListener('open', () => {
      this.lastSeen = Date.now();
      this.send({ type: 'session:authenticate', payload: { token: this.token } });
      this.heartbeat = setInterval(() => {
        if (Date.now() - this.lastSeen >= 60000) socket.close();
        else if (this.connected) this.emit('heartbeat');
      }, 20000);
    });
    socket.addEventListener('message', event => {
      if (this.socket !== socket) return;
      this.lastSeen = Date.now();
      let message: Message;
      try { message = JSON.parse(String(event.data)) as Message; } catch { socket.close(); return; }
      if (message.type === 'session:authenticated') { this.connected = true; this.delay = 700; this.dispatch('connect'); }
      else if (message.type === 'command:result' && message.requestId) {
        const request = this.requests.get(message.requestId);
        if (request) { clearTimeout(request.timer); this.requests.delete(message.requestId); request.resolve(message.payload as CommandResult); }
      } else if (message.type === 'session:replaced') {
        this.replaced = true; this.connected = false; this.dispatch(message.type);
      } else {
        if (message.type === 'room:error' && !this.connected) {
          const result = message.payload as CommandResult;
          this.stopped = result.code !== 'SAVE_FAILED';
          this.dispatch('connect_error', new Error(result.error));
        }
        this.dispatch(message.type, message.payload);
      }
    });
    socket.addEventListener('error', () => { if (!this.connected) this.dispatch('connect_error', new Error('无法连接游戏服务，正在尝试恢复')); });
    socket.addEventListener('close', () => {
      if (this.socket !== socket) return;
      this.connected = false; clearInterval(this.heartbeat); this.failRequests(); this.dispatch('disconnect');
      if (!this.stopped && !this.replaced && this.reconnect) {
        this.retry = setTimeout(() => this.connect(), this.delay); this.delay = Math.min(this.delay * 2, 4000);
      }
    });
  }

  /** 输入消息，在已打开连接上发送 JSON。 */
  private send(message: Message): void {
    if (this.socket?.readyState !== WebSocket.OPEN) throw new Error('连接已断开');
    this.socket.send(JSON.stringify(message));
  }

  /** 输入同步或心跳事件，不产生业务重试。 */
  emit(type: 'room:sync' | 'heartbeat'): void { if (this.connected) this.send({ type }); }

  /** 输入命令和超时，返回关联请求的服务端回执。 */
  request(command: GameCommand, timeout = 8000): Promise<CommandResult> {
    if (!this.connected) return Promise.reject(new Error('连接已断开'));
    return new Promise((resolve, reject) => {
      const requestId = `${command.id}:${Date.now()}:${Math.random()}`;
      const timer = setTimeout(() => { this.requests.delete(requestId); reject(new Error('操作超时')); }, timeout);
      this.requests.set(requestId, { resolve, reject, timer });
      try { this.send({ type: 'game:command', requestId, payload: command }); }
      catch (error) { clearTimeout(timer); this.requests.delete(requestId); reject(error); }
    });
  }

  /** 连接结束时拒绝所有待确认请求，由页面同步核对结果。 */
  private failRequests(): void {
    this.requests.forEach(value => { clearTimeout(value.timer); value.reject(new Error('连接已断开')); }); this.requests.clear();
  }

  /** 清空所有本地事件监听。 */
  removeAllListeners(): void { this.listeners.clear(); }

  /** 主动停止连接及重连，不删除浏览器保存的身份。 */
  disconnect(): void {
    this.stopped = true; this.connected = false; clearTimeout(this.retry); clearInterval(this.heartbeat); this.failRequests(); this.socket?.close();
  }
}
