/** 本局冻结的经济参数，固定覆盖均为最终金额。 */
export interface EconomySettings {
  moneyMultiplier: number;
  /** 本局基础租金百分比，缺失字段的旧对局保留 10%。 */
  rentBasePercent?: number;
  fixedAmounts: Record<string, number>;
}
/** 机器人难度；未设置的席位为真人。 */
export type BotDifficulty = 'easy' | 'normal' | 'hard';
/** 大厅中由房主选择的地图大小。 */
export type MapSize = 'small' | 'medium' | 'large';
/** 道具种类；仅持有人收到具体手牌。 */
export type ItemKind = 'asunaFlash' | 'defenseSpecialization' | 'revealingSwords' | 'fusion' | 'shadowBlade' | 'doubleBoost' | 'blueSilverBind' | 'goldenCore' | 'nuwaMend' | 'namelessSword' | 'kingsPressure' | 'keqingRush' | 'thunderLord' | 'hollowfication' | 'fireBurial' | 'bankai' | 'igniteSea' | 'senbonzakura' | 'geoResonance' | 'growthLink' | 'sereniteaPot' | 'createBuild' | 'remoteConstruction' | 'shrineGrowth' | 'remote' | 'barrier' | 'stay' | 'immunity' | 'demolish' | 'swap' | 'blast' | 'jail' | 'fixed' | 'move' | 'bonus' | 'freeUpgrade' | 'fullUpgrade' | 'fullDemolish' | 'halfPayment' | 'release' | 'steal' | 'cash' | 'gamble' | 'shield' | 'recycle' | 'discover' | 'sap' | 'exchange' | 'slow' | 'swapAny' | 'kamui' | 'handDestruction' | 'gravity' | 'reaper' | 'shadowstep' | 'homeTeleport' | 'blackhole' | 'meteor' | 'timeRewind' | 'projection' | 'starburst' | 'imagineBreaker' | 'timeRestore' | 'vectorReflect' | 'deathReturn' | 'explosion' | 'corruption' | 'contract' | 'commandSpell' | 'sevenSwords' | 'railgun' | 'sealCard' | 'swordRain' | 'voidTears' | 'absoluteZero';
/** 已冻结的完整卡面，旧手牌缺少皮肤时使用原定义。 */
export interface ContentResult {
  kind?: ItemKind | null;
  id: string;
  pool?: 'chance' | 'fortune' | 'card';
  mechanism?: string;
  parameterId?: string;
  skinId?: string;
  name?: string;
  rarity?: 'R' | 'SR' | 'SSR' | 'UR';
  cardType?: '主动' | '防御' | '持续' | '即时' | null;
  quote?: string;
  description?: string;
  effect?: string;
  parameters?: Record<string, number>;
  targetId?: string | null;
  /** 私人补给仅向获得者展示完整卡面；旧快照缺省为公开事件。 */
  privateSupply?: boolean;
}
/** 私有手牌。 */
export interface Card extends ContentResult { kind: ItemKind; projected?: boolean; expiresAtTurn?: number; remainingUses?: number; lastUsedTurn?: number; usesSpent?: number }
/** 只向实际选择者发送的内容窗口。 */
export interface ContentChoice {
  id: string;
  type: `cross_${string}` | `neta_${'bankai' | 'ignite' | 'research' | 'bid' | 'reward' | 'petals' | 'petal_defense'}` | `construction_${string}` | 'free_upgrade' | 'full_upgrade' | 'self_destroy' | 'opponent_destroy' | 'gamble' | 'release' | 'movement_release' | 'hand_limit' | 'recycle' | 'discover' | 'anime_projection' | 'anime_rewind' | 'anime_defense' | 'anime_vote';
  numbers?: number[];
  cardOptions?: Record<string, Card[]>;
  materialIds?: string[];
  playerIds?: string[];
  prompt?: string;
  tileIds: number[];
  cards: Card[];
  cardId: string | null;
  content: ContentResult | null;
  skipTurns: number;
  jailed: boolean;
  discardCash: number;
  /** 当前实际手牌上限，旧协议缺省六张。 */
  handLimit?: number;
}
/** 随对局冻结的公开个人战报；旧存档不补造缺失数据。 */
export interface PlayerSettlement {
  complete: boolean;
  removed: boolean;
  peakNetWorth: number;
  peakProperties: number;
  turns: number;
  rentReceived: number;
  rentPaid: number;
  activityNet: number;
  builtLevels: number;
  trades: number;
  jailCount: number;
  finalNetWorth: number;
  title: string;
  titleQuote: string;
  cause: string | null;
  causeQuote: string | null;
  highlight: string | null;
  highlightQuote: string | null;
}
/** 公开玩家信息，不包含身份令牌或手牌。 */
export interface PublicPlayer {
  /** 已冻结的个人战报；旧存档和进行中的玩家可为空。 */
  settlement?: PlayerSettlement | null;
  /** 机器人难度，旧快照缺省为真人。 */
  botDifficulty?: BotDifficulty | null;
  id: string;
  name: string;
  color: string;
  /** 可选公开角色形象，旧快照使用探险家。 */
  avatarId?: string;
  cash: number;
  /** 公开的持有手牌数量；旧服务端及历史快照可缺省，具体卡面仍为私有。 */
  cardCount?: number | null;
  /** 尸鬼封尽后的当前上限。 */
  handLimit?: number | null;
  nextRollHalf?: boolean;
  /** 所有人可见的净资产及名次，旧服务端可缺省。 */
  netWorth?: number;
  assetRank?: number | null;
  position: number;
  ready: boolean;
  online: boolean;
  bankrupt: boolean;
  personalTurns?: number;
  skipTurns: number;
  jailed: boolean;
  eliminationOrder: number | null;
  /** 剩余后续个人回合，旧服务端缺省为无免疫。 */
  trapImmunityTurns?: number;
  /** 外部控制后的两回合保护，包含等待本次受控行动跳过的状态。 */
  controlProtectionTurns?: number;
  crossoverEffects?: string[];
  handSealTurns?: number;
  urSealTurns?: number;
  attackSealTurns?: number;
  defenseSpecializationTurns?: number;
  blueBindTurns?: number;
  cultivated?: boolean;
  nextPaymentWaiver?: boolean;
  nextRentDouble?: boolean;
  /** 塔塔开：下次移动翻倍；旧快照缺省为无。 */
  nextMoveDouble?: boolean;
}
/** 棋盘格公开状态。 */
export interface Tile {
  petalCasterId?: string | null;
  petalOwnerId?: string | null;
  petalUntilTurn?: number;
  id: number;
  type: 'start' | 'chance' | 'fortune' | 'jail' | 'property' | 'activity';
  /** 活动角当前玩法，旧地图缺省。 */
  activityKind?: ActivityKind | null;
  name: string;
  district: number;
  /** 地块名称所属地区的元数据，目前仅参与命名。 */
  regionId?: string | null;
  regionName?: string | null;
  price: number;
  ownerId: string | null;
  level: number;
  mortgaged: boolean;
  /** 抵押时的土地加建筑估值，旧土地抵押缺省按地价计算。 */
  mortgageBase?: number | null;
  barrier: boolean;
  demolitionShield?: boolean;
  /** 防拆护盾剩余层数，旧快照只有布尔标记时按一层展示。 */
  demolitionShieldCharges?: number;
  treasure?: boolean;
  concertStacks?: number;
  meteorCharges?: number;
  kamuiCasterId?: string | null;
  kamuiTurns?: number;
  kamuiAppliedTurn?: number;
  corruptionTurns?: number;
  nzothOwnerId?: string | null;
  zeroCasterId?: string | null;
  zeroUntilTurn?: number;
  damageOwnerId?: string | null;
  damageLevel?: number;
  damageUntilTurn?: number;
  portalTileId?: number | null;
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
/** 交易报价对象仅向双方发送，双方及报价明细进入公开记录，等待主动接受、拒绝或取消。 */
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
/** 公开玩家引用与动态，不包含未使用手牌或内部报价对象；已发起报价的明细通过公开日志展示。 */
export interface PlayerMention { id?: string; name: string; color: string }
/** 显式 @ 引用；UTF-16 偏移对应消息正文，发送后对应含发送者前缀的公开文案。 */
export interface ChatReference { start: number; end: number; playerId?: string; tileId?: number; color?: string }
export interface PublicLog { content?: ContentResult | null; type?: string; actorId?: string; references?: ChatReference[]; mentions?: PlayerMention[]; id: string; text: string; at: number }
/** 服务端记录的棋盘演出事实，外观与位移只用于展示，不重新计算结算。 */
export interface BoardEffect {
  kind: 'swap' | 'swapAny' | 'shuffle' | 'gravity' | 'kamui' | 'kamuiRelease' | 'blackhole' | 'meteor' | 'tornado'
    | 'barrier' | 'barrierHit' | 'trapImmune' | 'demolish' | 'shield' | 'shieldBlock' | 'upgrade' | 'exchange'
    | 'slow' | 'seal' | 'steal' | 'recycle' | 'discover' | 'handDestruction' | 'supply' | 'treasure' | 'treasureClaim'
    | 'concert' | 'concertRent' | 'morale' | 'waiver' | 'rentDouble' | 'immunity' | 'halfPayment' | 'income' | 'fee'
    | 'payment' | 'gambleWin' | 'gambleLose' | 'talent' | 'teleport' | 'movement' | 'moveBoost' | 'release' | 'rest' | 'jail' | 'statusExpired';
  /** 公开参与者，不含具体手牌和数量；旧演出缺省为空。 */
  playerIds?: string[];
  moves: { playerId: string; from: number; to: number }[];
  beforeTiles: BoardEffectTile[];
  afterTiles: BoardEffectTile[];
}
/** 演出前后的地产公开外观，可包含实际产权变化，不覆盖抵押或资金。 */
export type BoardEffectTile = Pick<Tile, 'id' | 'level' | 'barrier' | 'demolitionShield' | 'demolitionShieldCharges' | 'treasure' | 'concertStacks' | 'meteorCharges' | 'kamuiCasterId' | 'kamuiTurns' | 'kamuiAppliedTurn'> & { ownerId?: string | null };
/** 移动路径用于客户端动画，结算结果始终以服务器快照为准。 */
export interface MotionSegment {
  id: string; playerId: string; from: number; path: number[];
  /** 与走格共用同一播放队列；缺省为空时沿用旧移动动画。 */
  boardEffect?: BoardEffect | null;
  /** 直接传送，路径只含目的地，不表示走过沿途地块。 */
  teleport?: boolean;
  /** 本段掷骰结果；事件移动为空，旧协议可缺省。 */
  dice?: number | null;
  /** 触发本段移动的公开事件，恢复后也先揭卡再移动。 */
  beforeEvent?: ContentResult | null;
  /** 本段落点抽出的公开事件，不包含私人补给。 */
  event?: ContentResult | null;
}
/** 保留最后一段的原字段，按顺序扩展完整移动过程。 */
export interface Motion extends MotionSegment { segments?: MotionSegment[] }
/** 公开房间快照，可安全发送给任意观战者。 */
export interface PublicRoomState {
  researchCandidates?: Card[];
  constructionEffects?: Record<number, string[]>;
  /** 新局活动轮换，旧局不改图。 */
  activityRotation?: ActivityRotation | null;
  /** 已移除秘密字段的活动状态。 */
  activity?: CornerActivity | null;
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
  /** 已实际使用的道具展示卡面；旧服务端及快照可缺省。 */
  playedCard?: ContentResult | null;
  /** 最近使用道具的玩家。 */
  playedCardPlayerId?: string | null;
  upgradedTiles?: number[];
}
/** 当前玩家私有状态，只有本人可收到。 */
export interface PrivatePlayerState {
  /** 卍解后所有后续出牌共用额度，耗尽时仍保持已发动标记。 */
  bankaiActive?: boolean;
  bankaiRemaining?: number;
  /** 持续花瓣技能的私人可用状态。 */
  petalsAvailable?: boolean;
  petalsRemaining?: number;
  remoteBuildAvailable?: boolean;
  activityId?: string | null;
  activityRound?: number | null;
  activitySelection?: number | null;
  archiveCode?: string;
  seatClaims?: SeatClaim[];
  playerId: string; hand: Card[]; trade: Trade | null; activeCardUsed: boolean;
  /** 本人最近报价的拒绝说明，旧快照可缺省。 */
  tradeRejection?: string | null;
  projectionAvailable?: boolean;
  disabledCardIds?: string[];
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
export interface HistoryEntry { content?: ContentResult | null; references?: ChatReference[]; mentions?: PlayerMention[]; logId?: string | null; sequence: number; snapshotId: string; gameId: string | null; version: number; turnNumber: number | null; phase: string | null; actorId: string | null; actorName: string | null; type: string; text: string; at: number }
export interface HistoryPage { entries: HistoryEntry[]; nextCursor: number | null }
/** 服务端操作结果。 */
export interface CommandResult { ok: boolean; version?: number; error?: string; code?: string }
/** 带去重标识与版本号的客户端命令。 */
export interface GameCommand { id: string; expectedVersion: number; action: GameAction }
/** 所有业务操作的显式输入类型。 */
export type GameAction =
  | { type: 'addBot'; botDifficulty: BotDifficulty }
  | { type: 'setBotDifficulty'; playerId: string; botDifficulty: BotDifficulty }
  | { type: 'activityInput'; activityId: string; activityRound: number; selection?: number; point?: number; playerId?: string }
  | { type: 'chat'; message: string; references?: ChatReference[] }
  | { type: 'assignSeat'; claimId: string }
  | { type: 'continueRecovery' }
  | { type: 'ready'; value: boolean }
  | { type: 'setName'; name: string }
  | { type: 'setAvatar'; avatarId: string }
  | { type: 'setMapSize'; mapSize: MapSize }
  | { type: 'senbonzakuraStrike' | 'purifyPollution' | 'constructionBuild' | 'start' | 'roll' | 'buy' | 'upgrade' | 'pass' | 'endTurn' | 'tradeCancel' | 'newGame' | 'payDebt' | 'declareBankruptcy' }
  | { type: 'rentDecision'; useCard: boolean }
  | { type: 'paymentDefense'; decisionId?: string; cardId?: string }
  | { type: 'contentDecision'; decisionId: string; cardSelections?: Record<string, string>; point?: number; direction?: number; playerId?: string; accept?: boolean; tileId?: number; cardId?: string; keepCardIds?: string[]; discardCardIds?: string[]; tileIds?: number[]; selection?: number }
  | { type: 'card'; cardId: string; point?: number; tileId?: number; playerId?: string; secondPlayerId?: string; secondTileId?: number; tileIds?: number[]; direction?: number }
  | { type: 'sellBuilding' | 'mortgage' | 'redeem' | 'sellLand'; tileId: number }
  | { type: 'tradeOffer'; toId: string; giveCash: number; wantCash: number; giveTiles: number[]; wantTiles: number[]; debtCredit?: number }
  | { type: 'tradeRespond'; accept: boolean }
  | { type: 'removePlayer'; playerId: string };

/** 原生 WebSocket 信封；命令回执通过 requestId 关联，状态通过角色分别发送。 */
export interface RealtimeEnvelope { type: string; requestId?: string; payload?: unknown }

/** 五类轮换活动及类别内部趣味变体。 */
export type ActivityKind = 'casino' | 'horse' | 'roulette' | 'packet' | 'rps' | 'treasure' | 'bridge' | 'minority' | 'blindbox';
/** 服务端冻结金额及回合轮换信息。 */
export interface ActivityRotation {
  period: number; completedTurns: number; revision: number; tileIds: number[];
  current: ActivityKind[]; next: ActivityKind[]; money: Record<string, number>;
}
/** 多人活动安全公开状态，不包含子弹位置、秘密手势或未揭晓赛果。 */
export interface CornerActivity {
  id: string; kind: ActivityKind; tileId: number; triggerId: string; stage: string; round: number;
  participants: string[]; contenders: string[]; submitted: string[];
  bets: Record<string, number>; revealed: Record<string, number>; wagers: Record<string, number>;
  payouts: Record<string, number>; packets: Record<string, number>; bonuses: Record<string, number>;
  /** 本场已获得的额外道具数量，旧快照缺省，具体卡面保持私有。 */
  cardRewards?: Record<string, number>;
  /** 红包领取者与已选序号，未领取红包不公开金额。 */
  packetSelections: Record<string, number>;
  /** 新玩法的公开进度及已揭晓安全方向，旧快照缺省。 */
  level?: number; safePaths?: number[];
  currentPlayerId: string | null; opponentId: string | null; lastShooterId: string | null;
  shots: number; result: number | null; startedAt: number; endsAt: number | null; settled: boolean; summary: string;
}
