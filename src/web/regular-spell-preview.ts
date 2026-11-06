import type { UrSpellSample } from './ur-spell-preview';

/** 非 UR 样板只组合专属道具与短动作，不参与正式结算。 */
export interface RegularSpellSample extends Omit<UrSpellSample, 'theme'> {
  /** 专属道具和动作身份。 */
  theme: 'concrete' | 'slowBeam' | 'snatch' | 'repair' | 'coupon' | 'thunder' | 'door' | 'copter' | 'cloud' | 'cloak' | 'groupon' | 'sap' | 'atField' | 'vines' | 'substitute' | 'room' | 'staff' | 'banana';
  /** 取自当前卡池参数的真实稀有度。 */
  rarity: 'R' | 'SR' | 'SSR';
  /** 第二批固定预览目标，不读取正式对局。 */
  target?: 'self' | 'player' | 'tile';
  /** 固定演示步数，仅用于预览路径。 */
  steps?: number;
}

export const REGULAR_SPELL_SAMPLES: RegularSpellSample[] = [
  { skinId: 'card_meme_concrete_block', theme: 'concrete', rarity: 'SSR', name: '实心水泥墩', duration: 1600, description: '沉甸甸的路障砸落，地面扬尘，水泥墩纹丝不动。', result: '演示结果：云朵商店所在格设置路障。' },
  { skinId: 'card_onepiece_slow', theme: 'slowBeam', rarity: 'R', name: '迟缓光线', duration: 1700, description: '粉色环形光波命中，目标拖出慢半拍的残影。', result: '迟缓光线命中小桃；此处只展示招式，不模拟后续掷骰。' },
  { skinId: 'card_watermargin_shiqian', theme: 'snatch', rarity: 'SR', name: '顺手牵羊', duration: 1800, description: '长手伸向目标，夹住卡背，沿原路收回。', result: '演示结果：一张通用卡背转移至阿洛，不展示私人牌面。' },
  { skinId: 'card_hp_reparo', theme: 'repair', rarity: 'SR', name: '修复如初', duration: 1900, description: '魔杖轻点，砖块归位，新的楼层从金光中拼合。', result: '演示结果：云朵商店从 2 级升级为 3 级。' },
  { skinId: 'card_meme_pdd_cut', theme: 'coupon', rarity: 'SR', name: '帮我砍一刀', duration: 1700, description: '剪刀咔嚓剪开账单，一半飘走，剩余金额落定。', result: '固定费用演示：¥2,000 减半为 ¥1,000，不扣除真实余额。' },
  { skinId: 'card_naruto_flying_thunder', theme: 'thunder', rarity: 'SR', name: '飞雷神之术', duration: 1800, description: '三叉苦无落下，双方术式闪亮，棋子瞬间换位。', result: '演示结果：阿洛与小桃交换位置，不触发落点结算。' },
  { skinId: 'card_dora_anywhere_door', theme: 'door', rarity: 'SSR', name: '任意门', duration: 2100, target: 'tile', steps: 4, description: '粉红门立起，门扇打开；选定四步落点，棋子穿门后抵达。', result: '固定演示：指定点数 4，阿洛从 20 格抵达起点；不触发结算。' },
  { skinId: 'card_dora_takecopter', theme: 'copter', rarity: 'R', name: '竹蜻蜓', duration: 2000, target: 'tile', steps: 3, description: '旋翼加速，棋子升起，沿三格路径飞行后落地。', result: '固定演示：阿洛前进 3 格，抵达 23 格。' },
  { skinId: 'card_journey_kite_cloud', theme: 'cloud', rarity: 'R', name: '筋斗云', duration: 2200, target: 'tile', steps: 6, description: '金云翻卷托起棋子，拐过棋盘转角，收拢云尾落地。', result: '固定演示：阿洛前进 6 格，抵达 2 格。' },
  { skinId: 'card_tactic_golden_cicada', theme: 'cloak', rarity: 'SR', name: '金蝉脱壳', duration: 1900, target: 'player', description: '外袍留下，人影先消失；空袍塌落，双方在另一端现身。', result: '固定演示：阿洛与小桃交换位置。' },
  { skinId: 'card_meme_groupon', theme: 'groupon', rarity: 'SR', name: '团购', duration: 1800, target: 'self', description: '手机亮出团购券，券面弹开，核销印落下后账单减半。', result: '固定费用演示：¥2,000 → ¥1,000，不扣真实余额。' },
  { skinId: 'card_wow_sap', theme: 'sap', rarity: 'SR', name: '闷棍', duration: 1700, target: 'player', description: '短木棍从背后挥下，命中停一拍，星星绕着目标转。', result: '招式展示：小桃受到闷棍；不推进回合或模拟状态结算。' },
  { skinId: 'card_eva_at_field', theme: 'atField', rarity: 'SSR', name: 'A.T.力场', duration: 1900, target: 'tile', description: '橙色八边形在自家建筑前逐层撑开，试探冲击在外壁散开。', result: '固定演示：阿洛的云朵商店获得拆除护盾；冲击仅展示受击形态，不消耗护盾。' },
  { skinId: 'card_crossover_blueSilverBind', theme: 'vines', rarity: 'SR', name: '蓝银缠绕', duration: 2100, target: 'player', description: '蓝银藤从脚下生长，交错上攀，叶片亮起后收紧。', result: '招式展示：藤蔓缠住小桃；不模拟持续状态结算。' },
  { skinId: 'fortune_substitution_jutsu', theme: 'substitute', rarity: 'SSR', name: '替身术', duration: 1800, target: 'self', description: '白烟散开，绑护额的木桩信物亮起，附着为下一次免付提示。', result: '固定演示：阿洛获得下次免付提示；当前没有费用发生。' },
  { skinId: 'chance_onepiece_room_shuffle', theme: 'room', rarity: 'SSR', name: 'ROOM', duration: 2200, target: 'player', description: '蓝色半球铺开切分网格，双方同时错位，领域收回。', result: '固定双人重排演示：阿洛与小桃交换位置；没有前端随机。' },
  { skinId: 'card_journey_golden_staff', theme: 'staff', rarity: 'SR', name: '如意金箍棒', duration: 1900, target: 'tile', description: '金箍棒由细针伸长，横扫建筑，震出瓦片后缩回。', result: '固定演示：云朵商店从 3 级降至 2 级。' },
  { skinId: 'chance_banana_peel', theme: 'banana', rarity: 'SR', name: '踩到香蕉皮', duration: 1800, target: 'tile', steps: -2, description: '香蕉皮滑到脚下，棋子后仰倒滑两格，摇晃着站稳。', result: '固定演示：阿洛后退 2 格，抵达 18 格。' },
];
