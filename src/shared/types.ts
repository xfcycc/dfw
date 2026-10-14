/** 本局冻结的经济参数，固定覆盖均为最终金额。 */
export interface EconomySettings {
  moneyMultiplier: number;
  /** 本局基础租金百分比，缺失字段的旧对局保留 10%。 */
  rentBasePercent?: number;
  fixedAmounts: Record<string, number>;
}
/** 大厅中由房主选择的地图大小。 */
export type MapSize = 'small' | 'medium' | 'large';
/** 道具种类；仅持有人收到具体手牌。 */
export type ItemKind = 'remote' | 'barrier' | 'stay' | 'immunity' | 'demolish' | 'swap' | 'fixed' | 'move' | 'bonus' | 'freeUpgrade' | 'halfPayment' | 'release' | 'steal' | 'cash';
/** 已冻结的完整卡面，旧手牌缺少皮肤时使用原定义。 */
export interface ContentResult {
  kind?: ItemKind | null;
  id: string;
  pool?: 'chance' | 'fortune' | 'card';
  mechanism?: string;
  parameterId?: string;
  skinId?: string;
  name?: string;
  rarity?: 'R' | 'SR' | 'SSR';
  cardType?: '主动' | '防御' | '持续' | '即时' | null;
  quote?: string;
  description?: string;
  effect?: string;
  parameters?: Record<string, number>;
  targetId?: string | null;
}
/** 私有手牌。 */
export interface Card extends ContentResult { kind: ItemKind }
/** 只向实际选择者发送的内容窗口。 */
export interface ContentChoice {
  id: string;
  type: 'free_upgrade' | 'self_destroy' | 'gamble' | 'release' | 'hand_limit';
  tileIds: number[];
  cards: Card[];
  cardId: string | null;
  content: ContentResult | null;
  skipTurns: number;
  jailed: boolean;
  discardCash: number;
}
/** 公开玩家信息，不包含身份令牌或手牌。 */
export interface PublicPlayer {
  id: string;
  name: string;
  color: string;
  cash: number;
  /** 所有人可见的净资产及名次，旧服务端可缺省。 */
  netWorth?: number;
  assetRank?: number | null;
  position: number;
  ready: boolean;
  online: boolean;
  bankrupt: boolean;
  skipTurns: number;
  jailed: boolean;
  eliminationOrder: number | null;
  /** 剩余后续个人回合，旧服务端缺省为无免疫。 */
  trapImmunityTurns?: number;
  nextPaymentWaiver?: boolean;
  nextRentDouble?: boolean;
}
/** 棋盘格公开状态。 */
export interface Tile {
  id: number;
  type: 'start' | 'chance' | 'fortune' | 'jail' | 'property';
  name: string;
  district: number;
  /** 同一街区的地区元数据，目前仅参与命名。 */
  regionId?: string | null;
  regionName?: string | null;
  price: number;
  ownerId: string | null;
  level: number;
  mortgaged: boolean;
  /** 抵押时的土地加建筑估值，旧土地抵押缺省按地价计算。 */
  mortgageBase?: number | null;
  barrier: boolean;
}
/** 当前需要玩家决定的公开事项。 */
export interface PendingDecision {
  /** 费用标识，旧待选可省略。 */
  id?: string | null;
  type: 'buy' | 'upgrade' | 'rent' | 'debt' | 'choice';
  playerId: string;
  tileId: number | null;
  amount: number;
  creditorId: string | null;
  originalAmount?: number;
  defenseResolved?: boolean;
}
/** 交易报价仅向双方发送，等待主动接受、拒绝或取消。 */
export interface Trade {
  id: string;
  fromId: string;
  toId: string;
  giveCash: number;
  wantCash: number;
  giveTiles: number[];
  wantTiles: number[];
  /** 费用结算中的报价关联，普通交易为空。 */
  debtId?: string | null;
  /** 协商抵债金额，旧报价缺省为零。 */
  debtCredit?: number;
  /** 旧计时字段仅为协议兼容保留，固定为零。 */
  remainingMs: number;
  /** 旧计时字段仅为协议兼容保留，固定为空。 */
  runningSince: number | null;
}
/** 公共事件不包含抽到或使用的卡牌名称与数量。 */
export interface PublicLog { id: string; text: string; at: number }
/** 移动路径用于客户端动画，结算结果始终以服务器快照为准。 */
export interface Motion { id: string; playerId: string; from: number; path: number[] }
/** 公开房间快照，可安全发送给任意观战者。 */
export interface PublicRoomState {
  roomId?: string;
  recoveryPending?: boolean;
  recovered?: boolean;
  sourceRoomId?: string | null;
  sourceSnapshotId?: string | null;
  /** 本局经济快照；旧对局缺省使用一倍基准。 */
  economy?: EconomySettings | null;
  code: string;
  version: number;
  status: 'lobby' | 'playing' | 'finished';
  /** 缺少设置的旧房间采用大地图；已生成棋盘保持原样。 */
  mapSize?: MapSize;
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
  /** 当前配置的扩展机会说明，沿用可选公开字段。 */
  chanceEventDescriptions?: string[];
  event?: ContentResult | null;
  eventPlayerId?: string | null;
  upgradedTiles?: number[];
}
/** 当前玩家私有状态，只有本人可收到。 */
export interface PrivatePlayerState {
  archiveCode?: string;
  seatClaims?: SeatClaim[];
  playerId: string; hand: Card[]; trade: Trade | null; activeCardUsed: boolean;
  choice?: ContentChoice | null;
  event?: ContentResult | null;
  notice?: ContentResult | null;
  noticeText?: string | null;
}
/** 通过身份令牌建立会话；令牌不放入 URL。 */
export interface Session { code: string; token: string; playerId: string | null; roomId?: string; archiveCode?: string | null; visitor?: boolean }
/** 房主可见的角色申请，不含身份令牌。 */
export interface SeatClaim { id: string; playerId: string; name: string; at: number }
/** 保存时的公开状态，内部手牌与效果队列不进入历史预览。 */
export interface ArchiveSnapshot { snapshotId: string; gameId: string | null; version: number; turnNumber: number; at: number; status: string; manual: boolean; label: string | null; restorable: boolean; restoreError: string | null; state: PublicRoomState | null }
export interface ArchiveGame { gameId: string | null; firstAt: number; lastAt: number; turnNumber: number; snapshots: number }
export interface RoomArchive { roomId: string; archiveCode: string; code: string; sourceArchiveCode: string | null; sourceSnapshotId: string | null; latest: ArchiveSnapshot; games: ArchiveGame[]; bookmarks: ArchiveSnapshot[]; nextCursor: number | null }
export interface HistoryEntry { sequence: number; snapshotId: string; gameId: string | null; version: number; turnNumber: number | null; phase: string | null; actorId: string | null; actorName: string | null; type: string; text: string; at: number }
export interface HistoryPage { entries: HistoryEntry[]; nextCursor: number | null }
/** 服务端操作结果。 */
export interface CommandResult { ok: boolean; version?: number; error?: string; code?: string }
/** 带去重标识与版本号的客户端命令。 */
export interface GameCommand { id: string; expectedVersion: number; action: GameAction }
/** 所有业务操作的显式输入类型。 */
export type GameAction =
  | { type: 'assignSeat'; claimId: string }
  | { type: 'continueRecovery' }
  | { type: 'ready'; value: boolean }
  | { type: 'setMapSize'; mapSize: MapSize }
  | { type: 'start' | 'roll' | 'buy' | 'upgrade' | 'pass' | 'endTurn' | 'tradeCancel' | 'newGame' | 'payDebt' | 'declareBankruptcy' }
  | { type: 'rentDecision'; useCard: boolean }
  | { type: 'paymentDefense'; decisionId?: string; cardId?: string }
  | { type: 'contentDecision'; decisionId: string; accept?: boolean; tileId?: number; cardId?: string; keepCardIds?: string[] }
  | { type: 'card'; cardId: string; point?: number; tileId?: number; playerId?: string }
  | { type: 'sellBuilding' | 'mortgage' | 'redeem' | 'sellLand'; tileId: number }
  | { type: 'tradeOffer'; toId: string; giveCash: number; wantCash: number; giveTiles: number[]; wantTiles: number[]; debtCredit?: number }
  | { type: 'tradeRespond'; accept: boolean }
  | { type: 'removePlayer'; playerId: string };

/** 原生 WebSocket 信封；命令回执通过 requestId 关联，状态通过角色分别发送。 */
export interface RealtimeEnvelope { type: string; requestId?: string; payload?: unknown }
