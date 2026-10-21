import type { EconomySettings, ItemKind, MapSize, Tile } from './types.js';
import regionDefaults from '../../backend/src/main/resources/game-rules/map-regions.json' with { type: 'json' };
import economyDefaults from '../../backend/src/main/resources/game-rules/economy.json' with { type: 'json' };

export const RULES = {
  minPlayers: 2, maxPlayers: 6, initialCash: 6000, initialCards: 2, startIncome: 1000,
  prices: [400, 600, 800, 1200, 1600], buildRate: 0.5, sellBuildingRate: 0.25,
  mortgageRate: 0.5, redeemRate: 0.55, sellLandRate: 0.7,
  maxLevel: 3, rentRate: 0.25, rentMultipliers: [1, 3, 6, 10],
} as const;
/** 输入开局人数，返回应用经济倍率前的每人初始现金基数。 */
export function initialCashBase(count: number): number { return count >= 5 ? 7500 : RULES.initialCash; }
/** 输入开局人数，返回每人初始手牌数量。 */
export function initialCards(count: number): number { return count >= 3 ? 3 : RULES.initialCards; }
/** 输入本局玩家人数（含已淘汰玩家），返回每次正向经过或最终停在起点时的道具补给数量。 */
export function startCards(count: number): number { return count >= 3 ? 2 : 1; }
export const COLORS = ['#ef6c68', '#4287e5', '#e6ad32', '#47ae88', '#a078d3', '#e982b2'];
export const DISTRICTS = ['花园社区', '滨海街区', '星光商圈', '旧城风情'];
export const MAP_SIZES: { value: MapSize; name: string; factor: number }[] = [
  { value: 'small', name: '小', factor: 4 }, { value: 'medium', name: '中', factor: 6 }, { value: 'large', name: '大', factor: 8 },
];
/** 输入人数及房间地图大小，返回准确的总格数。 */
export function mapLength(count: number, size: MapSize = 'large'): number { return 16 + count * MAP_SIZES.find(item => item.value === size)!.factor; }
/** 输入闭环地图格数，返回整数行列；34／46 格使用相邻长度的矩形边。 */
export function boardDimensions(length: number): { rows: number; columns: number } { return { rows: Math.floor(length / 4) + 1, columns: Math.ceil(length / 4) + 1 }; }
export const ITEMS: Record<ItemKind, { name: string; icon: string; description: string }> = {
  geoResonance: { name: "岩脊共鸣", icon: '✦', description: "选择1处自家地产，与左右紧邻的自家地产各免费升1级，最多3处。" },
  growthLink: { name: "所闻遍计", icon: '✦', description: "连接2处自家地产，持续3个后续个人回合；其中一处付费建造时，另一处免费升1级，最多触发2次；免费升级不触发。" },
  sereniteaPot: { name: "尘歌壶", icon: '✦', description: "重新分配2处自家地产的建筑等级，再选其中一处免费升1级；每处最高3级，均不占本回合升级次数。" },
  createBuild: { name: "创", icon: '✦', description: "弃置1～2张其他手牌，每张换1处不同自家地产免费升1级；弃牌不折现，确认后才消耗。" },
  remoteConstruction: { name: "基建轮班", icon: '✦', description: "本回合及下个个人回合，在资产管理阶段各可远程建造1次，支付标准建造费的75%，向下取整。" },
  shrineGrowth: { name: "给我建座神社吧", icon: '✦', description: "标记1处自家地产；其他玩家正常停留并结算后，该地产免费升1级，最多触发2次；暂不能升级时保留次数。" },
  timeRewind: { name: "时间溯行", icon: '✦', description: "普通骰点公开后、移动前重掷1次，必须接受新结果；不适用替代掷骰，不占主动次数。" },
  projection: { name: "投影魔术", icon: '✦', description: "复制上个已结束个人回合使用过的1张主动道具，含UR，不含投影魔术；复制牌本回合可额外使用1次，回合结束消失。" },
  starburst: { name: "星爆气流斩", icon: '✦', description: "对手1处有建筑地产连续降级2次，每次降1级；每层护盾抵挡1击，无退款。" },
  imagineBreaker: { name: "幻想杀手", icon: '✦', description: "抵消1张主动道具对自己及自家地产的全部效果，含UR；群体效果只抵消自己一方，对方仍消耗卡和次数。" },
  timeRestore: { name: "刻刻帝·四之弹", icon: '✦', description: "在自己的下个个人回合结束前，将1处自家地产恢复至最近一次敌方或事件破坏前的建筑等级；产权须未变化，不恢复主动卖房或其他状态。" },
  vectorReflect: { name: "矢量反射", icon: '✦', description: "将1张主动道具对自己的效果反弹给施术者，含UR；不保护地产，群体只反弹自己部分，反弹不再触发反射。" },
  deathReturn: { name: "死亡回归", icon: '✦', description: "达到破产清算条件时，免除当前剩余强制债务，回到最近本人回合开始位置，获得现金 ¥3000并休息1个个人回合；不结算落点或起点奖励，不回滚已完成的付款、卖房或交易。" },
  explosion: { name: "爆裂魔法", icon: '✦', description: "拆除连续3格内的全部建筑，包含自家地产，无补偿；每处地产可消耗1层护盾抵挡全拆；自己随后休息1个个人回合。" },
  corruption: { name: "猩红腐败", icon: '✦', description: "对手1处有建筑地产，在接下来3次业主个人回合开始时各降1级；每层护盾抵挡1次，降至0级结束，重复施加刷新次数，可清除。" },
  contract: { name: "契约成立", icon: '✦', description: "获得现金 ¥3000，本局手牌上限永久减1，最低1张；上限为1时不可使用，超限时选择保留。" },
  commandSpell: { name: "令咒", icon: '✦', description: "指定另一名玩家立即移动1～6格，点数自选，正常结算沿途和落点；不占其正常行动，奖励、费用及落点选择归移动者。" },
  sevenSwords: { name: "火舞旋风", icon: '✦', description: "将3道剑气分配给对手有建筑的地产，可集火或分散，每道降1级；每层护盾抵挡1道，未分配的剑气作废。" },
  railgun: { name: "超电磁炮", icon: '✦', description: "选择顺或逆时针贯穿前方6格，自己不移动；无视护盾，清除沿途路障、陨星和全部护盾，对手地产各降1级。" },
  sealCard: { name: "回到你原来的样子", icon: '✦', description: "抵消并收服另一玩家仅针对自己或1处自家地产的单目标主动道具，含UR；收服后按正常时机使用，不适用群体或范围效果。" },
  swordRain: { name: "剑雨", icon: '✦', description: "前后6格内1名对手休息1个个人回合，可解控；本牌可用2次，每个人回合限1次，被防御也消耗次数，转移不刷新次数或冷却。" },
  voidTears: { name: "虚空之泪", icon: '✦', description: "连接2处地产；首位步行进入任一端者传至另一端，再走剩余步数，随后裂隙消失；跳过的路径无奖励，只结算最终落点，传送或换位不触发。" },
  absoluteZero: { name: "绝对零域", icon: '✦', description: "冰封一条边的全部地产至自己的下个个人回合结束，包含自家地产；期间不能收租或升级，其他效果及资产操作照常。" },
  shield: { name: 'A.T.力场', icon: '✦', description: "为1处自家地产添加 1 层护盾，每层抵挡1次建筑拆除或降级，连击逐层消耗；不叠加，不保护主动出售或清算。" },
  recycle: { name: '制衡', icon: '✦', description: "选择至少1张其他手牌弃置，等量重抽；弃牌不折现，确认后才消耗。" },
  discover: { name: '发现', icon: '✦', description: "从3张不同效果的随机道具中选1张；确认后才消耗，取消重选不刷新候选。" },
  sap: { name: '闷棍', icon: '✦', description: "前后3格内1名对手休息1个个人回合（含同格），并随机偷取其1张手牌；解控不撤销偷牌。" },
  exchange: { name: '等价交换', icon: '✦', description: "用现有现金向对手支付该地产的无修正租金，取得产权并承接建筑、抵押及附加效果。" },
  slow: { name: '迟缓光线', icon: '✦', description: "指定对手，下次掷骰或替代掷骰的基础步数减半，向下取整，再计算加码与塔塔开；不叠加。" },
  swapAny: { name: '斗转星移', icon: '✦', description: "交换任意2名玩家的位置，可含自己；不结算落点或起点奖励，不解除监禁或休息。" },
  kamui: { name: '神威', icon: '✦', description: "封印同一条边1～3处地产，无需连续，可含自家、对手或无主地产；持续3个后续个人回合，暂停收租、经营和附加效果，期间不受其他地产效果影响，仍可破产清算。" },
  handDestruction: { name: '手牌抹杀', icon: '✦', description: "全体玩家弃掉剩余手牌并等量重抽，包含自己；本牌不参与重抽，弃牌不折现。" },
  gravity: { name: '地爆天星', icon: '✦', description: "将其他所有玩家传送到自己所在格，不结算落点或起点奖励；随后正常掷骰。" },
  reaper: { name: '尸鬼封尽', icon: '✦', description: "自己与指定对手的本局手牌上限永久各减1，最低1张；可叠加，超限时选择保留。" },
  shadowstep: { name: '暗影步', icon: '✦', description: "传送到前后6格内另一名玩家所在格并结算，含同格，替代掷骰；传至起点可领取奖励。" },
  homeTeleport: { name: '传送', icon: '✦', description: "传送到1处自家地产并结算，替代掷骰，可付费升级。" },
  blackhole: { name: '黑洞', icon: '✦', description: "清除1处地产的全部附加效果，包含护盾、宝藏、演唱会及建设标记；不改变建筑、产权或抵押。" },
  meteor: { name: '陨星', icon: '✦', description: "随机在1～3处不同地产各放1颗陨星，每颗截停1次并结算落点；可叠加，不损坏建筑。" },

  fixed: { name: '固定步数', icon: '👣', description: "按卡面步数移动并结算落点，替代掷骰。" },
  move: { name: '主动位移', icon: '↔️', description: "按卡面前进或后退并结算落点，替代掷骰。" },
  bonus: { name: '掷骰加码', icon: '🎲', description: "本次掷骰增加卡面步数，合并移动并只结算最终落点。" },
  fullUpgrade: { name: '连柱家之术', icon: '🌳', description: "1处自家地产免费直升3级，计入本回合升级；取消或无目标时保留卡和主动次数。" },
  fullDemolish: { name: '神罗天征', icon: '💥', description: "拆除对手1处地产的全部建筑，无补偿；护盾可抵挡。" },
  freeUpgrade: { name: '免费升级', icon: '🏗️', description: "1处自家地产免费升1级，计入本回合升级；取消或无目标时保留卡和主动次数。" },
  halfPayment: { name: '付款减半', icon: '🧾', description: "本笔强制费用减免50%，减免额向下取整；不占主动次数。" },
  release: { name: '解除监禁／休息', icon: '🔑', description: "解除刚施加的监禁或休息，不撤销入狱传送；不占主动次数。" },
  steal: { name: '抢夺手牌', icon: '🃏', description: "随机抢走指定对手1张手牌；对方无牌时保留卡和主动次数。" },
  cash: { name: '即时现金', icon: '💰', description: "获得卡面现金。" },
  gamble: { name: '命运豪赌', icon: '🪙', description: "用现有现金按卡面金额下注，按卡面概率获得奖金，本金不返还；取消不消耗。" },
  remote: { name: '遥控骰子', icon: '🎯', description: "自选1～6点移动并结算落点，替代掷骰。" },
  barrier: { name: '路障', icon: '🚧', description: "在前后6格内放置1处路障，不含当前格；首位受影响的经过者停下并结算落点。" },
  stay: { name: '停留', icon: '⚓', description: "原地停留并重新结算当前格，替代掷骰。" },
  immunity: { name: '免租', icon: '🛡️', description: "免除本笔全部租金，不占主动次数。" },
  demolish: { name: '拆房', icon: '🔨', description: "对手1处有建筑地产降1级，无退款；护盾可抵挡。" },
  swap: { name: '换位', icon: '🔄', description: "与1名未服刑的对手交换位置，不结算落点或起点奖励；随后正常掷骰。" },
  blast: { name: '爆破鬼才', icon: '💥', description: "指定1名玩家随机传送，可选自己；不结算落点或起点奖励，随后正常掷骰。" },
  jail: { name: '谕示裁定', icon: '⚖️', description: "指定1名玩家入狱，可选自己；跳过下次个人行动，服刑期间不收租，可当场解控；对自己使用替代掷骰。" },
};
/** 输入地图种子，返回只用于地图配置的确定性随机数函数。 */
function seededRandom(seed: string): () => number {
  let value = 2166136261;
  for (const char of seed) value = Math.imul(value ^ char.charCodeAt(0), 16777619);
  return () => {
    value += 0x6d2b79f5;
    let t = Math.imul(value ^ (value >>> 15), value | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
/** 输入列表和随机源，返回洗牌后的副本。 */
function shuffle<T>(input: T[], random: () => number): T[] {
  const values = [...input];
  for (let i = values.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1));
    [values[i], values[j]] = [values[j], values[i]];
  }
  return values;
}
/** 输入人数、种子及同源配置，返回各边五档地价尽量均衡、全图地产名称唯一的地图；对局使用服务端棋盘。 */
export function generateMap(count: number, seed: string, economy: EconomySettings = economyDefaults, regions = regionDefaults.regions, size: MapSize = 'large'): Tile[] {
  if (!Number.isInteger(count) || count < 2 || count > 6) throw new Error('地图人数必须为 2～6 人');
  const random = seededRandom(seed);
  const length = mapLength(count, size);
  const horizontal = Math.ceil(length / 4), vertical = Math.floor(length / 4);
  const corners = [0, horizontal, horizontal + vertical, horizontal * 2 + vertical];
  const board: Tile[] = Array.from({ length }, (_, id) => ({
    id, type: 'property', name: '', district: id < corners[1] ? 0 : id < corners[2] ? 1 : id < corners[3] ? 2 : 3,
    price: 0, ownerId: null, level: 0, mortgaged: false, barrier: false,
  }));
  const fixed = [['start', '起点'], ['chance', '机会'], ['fortune', '财富'], ['jail', '监狱']] as const;
  fixed.forEach(([type, name], index) => Object.assign(board[corners[index]], { type, name }));
  const specialCount = count <= 3 ? 2 : 3;
  for (let i = 1; i < specialCount; i++) {
    for (const [type, name] of [['chance', '机会'], ['fortune', '财富']] as const) {
      const available = board.filter(tile => tile.type === 'property' && board[(tile.id - 1 + length) % length].type === 'property' && board[(tile.id + 1) % length].type === 'property');
      Object.assign(available[Math.floor(random() * available.length)], { type, name });
    }
  }
  const groups = shuffle(regions, random);
  const pools = new Map<string, string[]>();
  const reservedNames = new Set(board.filter(tile => tile.type !== 'property').map(tile => tile.name));
  const availableNames = new Map<string, typeof groups[number]>();
  for (const group of groups) {
    const pool = shuffle(group.names, random); pools.set(group.id, pool);
    for (const name of pool) if (!reservedNames.has(name) && !availableNames.has(name)) availableNames.set(name, group);
  }
  if (availableNames.size < board.filter(tile => tile.type === 'property').length) throw new Error('地图不重复地名不足，请补充地区地名配置');
  for (let side = 0; side < 4; side++) {
    const group = groups[side % groups.length];
    board.filter(tile => tile.district === side).forEach(tile => { tile.regionId = group.id; tile.regionName = group.name; });
    const properties = board.filter(tile => tile.district === side && tile.type === 'property');
    const priceTiers = shuffle(RULES.prices.map((base, i) => ruleMoney(economy, `landPrice.${i}`, base)), random);
    const prices = shuffle(properties.map((_, i) => priceTiers[i % priceTiers.length]), random);
    const names = pools.get(group.id)!;
    properties.forEach((tile, i) => {
      const name = names.find(value => availableNames.has(value)) ?? availableNames.keys().next().value!;
      const source = availableNames.get(name)!; availableNames.delete(name);
      Object.assign(tile, { name, regionId: source.id, regionName: source.name, price: prices[i] });
    });
  }
  return board;
}

/** 输入格子索引和总格数，返回矩形周边的整数 CSS 行列，顺时针排列。 */
export function tileCoordinates(index: number, length: number): { row: number; column: number } {
  const horizontal = Math.ceil(length / 4), vertical = Math.floor(length / 4);
  if (index <= horizontal) return { row: 1, column: index + 1 };
  if (index <= horizontal + vertical) return { row: index - horizontal + 1, column: horizontal + 1 };
  if (index <= horizontal * 2 + vertical) return { row: vertical + 1, column: horizontal * 2 + vertical - index + 1 };
  return { row: length - index + 1, column: 1 };
}

/** 输入经济快照、金额键和基准金额，返回固定覆盖或一次倍率换算后的最终金额。 */
export function ruleMoney(economy: EconomySettings | null | undefined, key: string, base: number): number {
  return economy?.fixedAmounts[key] ?? Math.round(base * (economy?.moneyMultiplier ?? 1));
}
/** 输入经济快照和已按实际地价计算的金额，返回固定覆盖，不重复乘倍率。 */
function derivedMoney(economy: EconomySettings | null | undefined, key: string, actual: number): number { return economy?.fixedAmounts[key] ?? actual; }
/** 输入地产及本局经济参数，返回按基础比例计算的租金；旧对局缺省 10%，抵押不收费。 */
export function rentFor(tile: Tile, economy?: EconomySettings | null): number {
  return tile.mortgaged || isKamui(tile) || !!tile.zeroCasterId ? 0 : concertRent(baseRentFor(tile,economy), tile.concertStacks ?? 0);
}
/** 输入地产，返回是否处于神威冻结中。 */
export function isKamui(tile: Tile): boolean { return !!tile.kamuiCasterId && (tile.kamuiTurns ?? 0) > 0; }
/** 输入地产，返回是否需要在名称后显示统一附加效果标记。 */
export function hasPropertyEffects(tile: Tile, construction?: Record<number, string[]>): boolean { return !!(construction?.[tile.id]?.length || tile.barrier || tile.demolitionShield || tile.treasure || tile.concertStacks || tile.meteorCharges || tile.corruptionTurns || tile.zeroCasterId || tile.portalTileId != null || isKamui(tile)); }
/** 输入地产和经济快照，返回忽略抵押、房东及临时修正的等级租金，无主按零级。 */
export function baseRentFor(tile: Tile, economy?: EconomySettings | null): number {
  const level = tile.ownerId ? tile.level : 0;
  return derivedMoney(economy, `rent.${tile.price}.${level}`, Math.floor(tile.price * (economy?.rentBasePercent ?? 10) / 100) * RULES.rentMultipliers[level]);
}
/** 输入无修正租金和层数，用精确整数比计算演唱会租金，最后向下取整。 */
export function concertRent(base: number, stacks: number): number { return Number(BigInt(base) * 6n ** BigInt(stacks) / (5n ** BigInt(stacks))); }
/** 输入地产及本局经济参数，返回一级建设成本。 */
export function buildCost(tile: Tile, economy?: EconomySettings | null): number { return derivedMoney(economy, `buildCost.${tile.price}`, Math.floor(tile.price / 2)); }
/** 输入地产及本局经济参数，返回出售一级建筑的现金。 */
export function sellBuildingValue(tile: Tile, economy?: EconomySettings | null): number { return derivedMoney(economy, `sellBuilding.${tile.price}`, Math.floor(buildCost(tile, economy) / 2)); }
/** 输入地产及本局经济参数，返回整套出售净额；已抵押时扣固定赎回费，负数表示需补差价。 */
export function landValue(tile: Tile, economy?: EconomySettings | null): number { return propertySaleValue(tile, economy, propertyValue(tile, economy)) - (tile.mortgaged ? redeemCost(tile) : 0); }
/** 输入地产、本局经济参数及剩余成本估值，返回七折回收款；旧卖地金额覆盖继续作用于土地部分。 */
function propertySaleValue(tile: Tile, economy: EconomySettings | null | undefined, value: number): number {
  const land = Math.floor(tile.price * 70 / 100);
  return Math.floor(value * 70 / 100) + derivedMoney(economy, `sellLand.${tile.price}`, land) - land;
}
/** 输入地产及本局经济参数，返回抵押、整售或先卖房再变现可取得的最高现金，不计负权益。 */
export function propertyLiquidationValue(tile: Tile, economy?: EconomySettings | null): number {
  if (tile.mortgaged) return Math.max(0, landValue(tile, economy));
  let value = 0, sold = 0;
  const cost = buildCost(tile, economy);
  for (let level = tile.level; level >= 0; level--) {
    const base = tile.price + level * cost;
    value = Math.max(value, sold + Math.max(Math.floor(base / 2), propertySaleValue(tile, economy, base)));
    sold += sellBuildingValue(tile, economy);
  }
  return value;
}
/** 输入地产及本局经济参数，返回土地加建筑的成本估值。 */
export function propertyValue(tile: Tile, economy?: EconomySettings | null): number { return tile.price + tile.level * buildCost(tile, economy); }
/** 输入地产及本局经济参数，返回整体抵押取得的现金。 */
export function mortgageValue(tile: Tile, economy?: EconomySettings | null): number { return Math.floor(propertyValue(tile, economy) * RULES.mortgageRate); }
/** 输入地产，返回固定于抵押时估值的赎回成本，兼容旧土地抵押。 */
export function redeemCost(tile: Tile): number { return Math.floor((tile.mortgageBase ?? tile.price) * 55 / 100); }
/** 输入地产，返回本局地区名，兼容未保存地区元数据的旧地图。 */
export function districtName(tile: Tile): string { return tile.regionName ?? DISTRICTS[tile.district]; }
