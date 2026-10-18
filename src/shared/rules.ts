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
/** 输入本局玩家人数（含已淘汰玩家），返回每次经过或原地停留在起点时的道具补给数量。 */
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
  shield: { name: 'A.T.力场', icon: '✦', description: '保护自家地产，抵挡一次任何建筑拆除。' },
  recycle: { name: '制衡', icon: '✦', description: '其他手牌任意数量弃置并等量重抽。' },
  discover: { name: '发现', icon: '✦', description: '三张不同效果道具中选一张。' },
  sap: { name: '闷棍', icon: '✦', description: '同格对手休息一回合并随机偷一张牌。' },
  exchange: { name: '等价交换', icon: '✦', description: '支付无修正租金，夺取对手地产。' },
  slow: { name: '迟缓光线', icon: '✦', description: '下次基础骰点减半，向下取整。' },
  swapAny: { name: '斗转星移', icon: '✦', description: '任意两名玩家换位，不结算落点。' },
  kamui: { name: '神威', icon: '✦', description: '冻结地产三个施术者个人回合。' },
  handDestruction: { name: '手牌抹杀', icon: '✦', description: '全员手牌等量重抽，不包括本牌。' },
  gravity: { name: '地爆天星', icon: '✦', description: '其他玩家聚集到自己，不结算落点。' },
  reaper: { name: '尸鬼封尽', icon: '✦', description: '双方本局手牌上限各减一，最低一张。' },
  shadowstep: { name: '暗影步', icon: '✦', description: '传送到另一玩家并结算，替代掷骰。' },
  homeTeleport: { name: '传送', icon: '✦', description: '传送到自家地产并结算，替代掷骰。' },
  blackhole: { name: '黑洞', icon: '✦', description: '清除地产全部附加效果，神威除外。' },
  meteor: { name: '陨星', icon: '✦', description: '一至三处随机地产各增加一次截停。' },

  fixed: { name: '固定步数', icon: '👣', description: '按卡面固定步数替代本次掷骰，结算落点。' },
  move: { name: '主动位移', icon: '↔️', description: '按卡面前进或后退，替代本次掷骰，结算落点。' },
  bonus: { name: '掷骰加码', icon: '🎲', description: '掷骰前使用，加码与骰子合成一次连续移动。' },
  fullUpgrade: { name: '连柱家之术', icon: '🌳', description: '自家合法地产免费直升3级，取消或无目标时保留卡和主动次数。' },
  fullDemolish: { name: '神罗天征', icon: '💥', description: '指定对手一块有建筑地产，全部拆除，不补偿，保留产权和抵押信息。' },
  freeUpgrade: { name: '免费升级', icon: '🏗️', description: '自己的合法地产升一级，确认前取消保留卡。' },
  halfPayment: { name: '付款减半', icon: '🧾', description: '本笔强制费用减免一半，减免额向下取整。' },
  release: { name: '解除监禁／休息', icon: '🔑', description: '限制刚施加时使用，不占主动次数。' },
  steal: { name: '抢夺手牌', icon: '🃏', description: '随机转移未破产对手的一张牌；对方无牌时保留卡与次数。' },
  cash: { name: '即时现金', icon: '💰', description: '获得卡面现金，占本回合主动次数。' },
  gamble: { name: '命运豪赌', icon: '🪙', description: '使用已有现金下注，按卡面概率大赚或大亏；确认下注才消耗卡牌和主动次数。' },
  remote: { name: '遥控骰子', icon: '🎯', description: '指定 1～6 点，替代本次掷骰。' },
  barrier: { name: '路障', icon: '🚧', description: '在附近 6 格内选择一个无路障地块放置路障（前后各 6 格，不含当前格），首个受影响的经过者停下。' },
  stay: { name: '停留', icon: '⚓', description: '替代掷骰，留在当前格重新结算。' },
  immunity: { name: '免租', icon: '🛡️', description: '支付路费前使用，免除本次全部租金。' },
  demolish: { name: '拆房', icon: '🔨', description: '让另一玩家的一块地产降一级，无退款。' },
  swap: { name: '换位', icon: '🔄', description: '与未服刑的对手交换位置，然后正常掷骰。' },
  blast: { name: '爆破鬼才', icon: '💥', description: '指定一名未破产玩家（可选自己），随机直接传送，此次落点不结算。' },
  jail: { name: '谕示裁定', icon: '⚖️', description: '指定未破产玩家（可选自己）直接入狱，跳过下一次个人行动；服刑期间不收租，其他小游戏照常参与。可当场用解控卡解除限制，对自己使用替代本次掷骰。' },
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
  return tile.mortgaged || isKamui(tile) ? 0 : concertRent(baseRentFor(tile,economy), tile.concertStacks ?? 0);
}
/** 输入地产，返回是否处于神威冻结中。 */
export function isKamui(tile: Tile): boolean { return !!tile.kamuiCasterId && (tile.kamuiTurns ?? 0) > 0; }
/** 输入地产，返回是否需要在名称后显示统一附加效果标记。 */
export function hasPropertyEffects(tile: Tile): boolean { return !!(tile.barrier || tile.demolitionShield || tile.treasure || tile.concertStacks || tile.meteorCharges || isKamui(tile)); }
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
