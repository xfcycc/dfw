import type { ItemKind, Tile } from './types.js';

export const RULES = {
  minPlayers: 2, maxPlayers: 6, initialCash: 6000, initialCards: 2, startIncome: 1000,
  prices: [800, 1200, 1600], buildRate: 0.5, sellBuildingRate: 0.25,
  mortgageRate: 0.5, redeemRate: 0.55, sellLandRate: 0.5,
  maxLevel: 3, rentRate: 0.1, rentMultipliers: [1, 3, 6, 10], tradeTimeoutMs: 60000,
} as const;
export const COLORS = ['#ef6c68', '#4287e5', '#e6ad32', '#47ae88', '#a078d3', '#e982b2'];
export const DISTRICTS = ['花园社区', '滨海街区', '星光商圈', '旧城风情'];
export const ITEMS: Record<ItemKind, { name: string; icon: string; description: string }> = {
  remote: { name: '遥控骰子', icon: '🎯', description: '指定 1～6 点，替代本次掷骰。' },
  barrier: { name: '路障', icon: '🚧', description: '在前方六格内的地产放置路障，首个经过者停下。' },
  stay: { name: '停留', icon: '⚓', description: '替代掷骰，留在当前格重新结算。' },
  immunity: { name: '免租', icon: '🛡️', description: '支付路费前使用，免除本次全部租金。' },
  demolish: { name: '拆房', icon: '🔨', description: '让另一玩家的一块地产降一级，无退款。' },
  swap: { name: '换位', icon: '🔄', description: '与未服刑的对手交换位置，然后正常掷骰。' },
};
const NAMES = [
  ['樱花小院', '松果公寓', '向日葵里', '绿荫街', '丁香花园', '枫叶居', '银杏巷', '月季小筑', '香草庭院', '白桦公馆', '云朵社区', '橙花别院'],
  ['海风码头', '蓝湾公寓', '珊瑚街', '落日海岸', '灯塔小屋', '帆船港', '贝壳广场', '浪花酒店', '沙滩别墅', '海盐小镇', '鲸鱼湾', '星沙码头'],
  ['星光百货', '音乐广场', '银河大厦', '霓虹街', '摩天中心', '彩虹影城', '金橙商厦', '云端酒店', '水晶大道', '都会公馆', '天际广场', '极光中心'],
  ['青石小巷', '老街茶馆', '钟楼广场', '红砖公寓', '榕树街', '河畔书屋', '南风客栈', '石桥街', '梧桐院', '春风集市', '百年戏院', '古城公馆'],
];

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
/** 按实际人数与种子生成闭环地图，返回固定特殊格和区段内随机地产。 */
export function generateMap(count: number, seed: string): Tile[] {
  if (!Number.isInteger(count) || count < 2 || count > 6) throw new Error('地图人数必须为 2～6 人');
  const random = seededRandom(seed);
  const length = 16 + count * 4;
  const quarter = length / 4;
  const board: Tile[] = Array.from({ length }, (_, id) => ({
    id, type: 'property', name: '', district: Math.floor(id / quarter),
    price: 0, ownerId: null, level: 0, mortgaged: false, barrier: false,
  }));
  const fixed = [['start', '起点'], ['chance', '机会'], ['fortune', '财富'], ['jail', '监狱']] as const;
  fixed.forEach(([type, name], index) => Object.assign(board[index * quarter], { type, name }));
  const chanceSide = random() < 0.5 ? 0 : 2;
  const fortuneSide = random() < 0.5 ? 1 : 3;
  for (const [side, type, name] of [[chanceSide, 'chance', '机会'], [fortuneSide, 'fortune', '财富']] as const) {
    const offset = 2 + Math.floor(random() * (quarter - 3));
    Object.assign(board[side * quarter + offset], { type, name });
  }
  for (let side = 0; side < 4; side++) {
    const properties = board.filter(tile => tile.district === side && tile.type === 'property');
    const names = shuffle(NAMES[side], random);
    const prices = shuffle(properties.map((_, i) => RULES.prices[i % 3]), random);
    properties.forEach((tile, i) => Object.assign(tile, { name: names[i], price: prices[i] }));
  }
  return board;
}

/** 输入格子索引和总格数，返回 CSS 棋盘行列，顺时针排列。 */
export function tileCoordinates(index: number, length: number): { row: number; column: number } {
  const side = length / 4;
  if (index <= side) return { row: 1, column: index + 1 };
  if (index <= side * 2) return { row: index - side + 1, column: side + 1 };
  if (index <= side * 3) return { row: side + 1, column: side * 3 - index + 1 };
  return { row: length - index + 1, column: 1 };
}

/** 输入地产公开状态，返回当前租金；抵押地产不收费。 */
export function rentFor(tile: Tile): number {
  return tile.mortgaged ? 0 : Math.round(tile.price * RULES.rentRate * RULES.rentMultipliers[tile.level]);
}
