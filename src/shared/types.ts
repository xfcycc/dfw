/** 道具种类；仅持有人收到具体手牌。 */
export type ItemKind = 'remote' | 'barrier' | 'stay' | 'immunity' | 'demolish' | 'swap';
/** 私有手牌。 */
export interface Card { id: string; kind: ItemKind }
/** 公开玩家信息，不包含身份令牌或手牌。 */
export interface PublicPlayer {
  id: string;
  name: string;
  color: string;
  cash: number;
  position: number;
  ready: boolean;
  online: boolean;
  bankrupt: boolean;
  skipTurns: number;
  jailed: boolean;
  eliminationOrder: number | null;
  /** 剩余后续个人回合，旧服务端缺省为无免疫。 */
  trapImmunityTurns?: number;
}
/** 棋盘格公开状态。 */
export interface Tile {
  id: number;
  type: 'start' | 'chance' | 'fortune' | 'jail' | 'property';
  name: string;
  district: number;
  price: number;
  ownerId: string | null;
  level: number;
  mortgaged: boolean;
  barrier: boolean;
}
/** 当前需要玩家决定的公开事项。 */
export interface PendingDecision {
  type: 'buy' | 'upgrade' | 'rent' | 'debt';
  playerId: string;
  tileId: number | null;
  amount: number;
  creditorId: string | null;
}
/** 交易报价仅向双方发送；时间剩余由服务器管理。 */
export interface Trade {
  id: string;
  fromId: string;
  toId: string;
  giveCash: number;
  wantCash: number;
  giveTiles: number[];
  wantTiles: number[];
  remainingMs: number;
  runningSince: number | null;
}
/** 公共事件不包含抽到或使用的卡牌名称与数量。 */
export interface PublicLog { id: string; text: string; at: number }
/** 移动路径用于客户端动画，结算结果始终以服务器快照为准。 */
export interface Motion { id: string; playerId: string; from: number; path: number[] }
/** 公开房间快照，可安全发送给任意观战者。 */
export interface PublicRoomState {
  code: string;
  version: number;
  status: 'lobby' | 'playing' | 'finished';
  hostId: string;
  gameId: string | null;
  players: PublicPlayer[];
  board: Tile[];
  turnOrder: string[];
  turnIndex: number;
  turnNumber: number;
  phase: 'pre_roll' | 'buy' | 'upgrade' | 'rent' | 'debt' | 'manage';
  pending: PendingDecision | null;
  tradePending: boolean;
  pausedReason: string | null;
  dice: number | null;
  motion: Motion | null;
  winnerId: string | null;
  logs: PublicLog[];
  /** 本局版本追加的机会结果，旧快照可省略。 */
  chanceEventDescriptions?: string[];
}
/** 当前玩家私有状态，只有本人可收到。 */
export interface PrivatePlayerState { playerId: string; hand: Card[]; trade: Trade | null; activeCardUsed: boolean }
/** 通过身份令牌建立会话；令牌不放入 URL。 */
export interface Session { code: string; token: string; playerId: string | null }
/** 服务端操作结果。 */
export interface CommandResult { ok: boolean; version?: number; error?: string; code?: string }
/** 带去重标识与版本号的客户端命令。 */
export interface GameCommand { id: string; expectedVersion: number; action: GameAction }
/** 所有业务操作的显式输入类型。 */
export type GameAction =
  | { type: 'ready'; value: boolean }
  | { type: 'start' | 'roll' | 'buy' | 'upgrade' | 'pass' | 'endTurn' | 'tradeCancel' | 'newGame' }
  | { type: 'rentDecision'; useCard: boolean }
  | { type: 'card'; cardId: string; point?: number; tileId?: number; playerId?: string }
  | { type: 'sellBuilding' | 'mortgage' | 'redeem' | 'sellLand'; tileId: number }
  | { type: 'tradeOffer'; toId: string; giveCash: number; wantCash: number; giveTiles: number[]; wantTiles: number[] }
  | { type: 'tradeRespond'; accept: boolean }
  | { type: 'removePlayer'; playerId: string };

/** 原生 WebSocket 信封；命令回执通过 requestId 关联，状态通过角色分别发送。 */
export interface RealtimeEnvelope { type: string; requestId?: string; payload?: unknown }
