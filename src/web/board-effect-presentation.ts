import type { BoardEffect } from '../shared/types';
import type { EffectSpriteKind } from './effect-sprites';

/** 已接入的表现目录；同一业务机制复用素材，时长只影响展示。 */
export const BOARD_EFFECTS = {
  swap: { label: '交换位置', duration: 1600, sprite: 'blackhole', description: '双方同时换位，完成后继续掷骰，不结算落点。' },
  swapAny: { label: '斗转星移', duration: 1600, sprite: 'blackhole', description: '选中的两名玩家同时换位，不结算落点或起点。' },
  shuffle: { label: 'ROOM · 错位空间', duration: 2200, sprite: 'blackhole', description: '全员接手另一人的原位置，不触发落点。' },
  gravity: { label: '地爆天星', duration: 2200, sprite: 'blackhole', description: '其他玩家聚集到施术者所在格，完成后继续掷骰。' },
  kamui: { label: '神威空间', duration: 1800, sprite: 'kamui', description: '地产卷入异空间，效果结束后展示冻结状态。' },
  kamuiRelease: { label: '神威解除', duration: 1800, sprite: 'kamui', description: '地产从异空间恢复，原有附加效果继续保留。' },
  blackhole: { label: '黑洞吞噬', duration: 2000, sprite: 'blackhole', description: '清除地产附加效果，保留建筑、产权和抵押。' },
  meteor: { label: '陨星降临', duration: 2000, sprite: 'meteor', description: '陨星落在实际命中的地产，增加截停层数。' },
  tornado: { label: '龙卷风摧毁停车场', duration: 2400, sprite: 'tornado', description: '展示实际降级与护盾抵挡，再展示补偿。' },
  barrier: { label: '路障设置', duration: 1400, sprite: 'control', description: '目标格出现路障，结束后开放后续操作。' },
  barrierHit: { label: '截停触发', duration: 1300, sprite: 'impact', description: '展示实际消耗的一层路障或陨星，再结算停留格。' },
  trapImmune: { label: '陷阱免疫', duration: 1300, sprite: 'shield', description: '展示免疫状态或抵挡截停，原有路障保留。' },
  demolish: { label: '建筑拆除', duration: 1800, sprite: 'impact', description: '展示实际建筑损失，再衔接原规则的补偿结果。' },
  shield: { label: 'A.T.力场', duration: 1500, sprite: 'shield', description: '护盾附着地产，等待后续攻击触发。' },
  shieldBlock: { label: '力场抵挡', duration: 1500, sprite: 'shield', description: '护盾消耗并挡住拆除，建筑等级保持不变。' },
  upgrade: { label: '建筑升级', duration: 1700, sprite: 'build', description: '选定地产完成免费升级，再开放下一步。' },
  exchange: { label: '等价交换', duration: 1800, sprite: 'build', description: '展示支付与产权转移，建筑和原有附加效果保留。' },
  slow: { label: '迟缓光线', duration: 1300, sprite: 'control', description: '展示迟缓施加或消耗，再衔接原有移动。' },
  seal: { label: '尸鬼封尽', duration: 1600, sprite: 'kamui', description: '展示双方手牌上限封印，再打开需要的超限整理。' },
  steal: { label: '手牌转移', duration: 1500, sprite: 'cards', description: '以通用卡背展示转移，具体牌面只给相关持有人。' },
  recycle: { label: '制衡', duration: 1700, sprite: 'cards', description: '确认弃牌后展示重抽，再揭示本人获得的牌。' },
  discover: { label: '发现', duration: 1600, sprite: 'cards', description: '确认私人候选后，选中道具加入手牌。' },
  handDestruction: { label: '手牌抹杀', duration: 1900, sprite: 'cards', description: '全员手牌完成等量重抽，演出不公开私人牌面和数量。' },
  supply: { label: '道具补给', duration: 1400, sprite: 'cards', description: '补给演出结束后，再揭示本人获得的道具或整理手牌。' },
  treasure: { label: '宝藏出现', duration: 1700, sprite: 'treasure', description: '宝藏标记出现在实际选中的地产。' },
  treasureClaim: { label: '宝藏领取', duration: 1700, sprite: 'treasure', description: '开箱结束后展示奖励，保留本次免租规则。' },
  concert: { label: '乌塔演唱会', duration: 1800, sprite: 'concert', description: '实际命中的地产叠加演唱会标记。' },
  concertRent: { label: '演唱会收租', duration: 1600, sprite: 'concert', description: '展示层数消耗，再衔接修正租金的付款流程。' },
  morale: { label: '塔塔开', duration: 1300, sprite: 'control', description: '展示移动翻倍状态的施加或消耗，移动规则保持原样。' },
  waiver: { label: '付款免除', duration: 1300, sprite: 'shield', description: '展示下一笔费用免除状态的获得或消耗。' },
  rentDouble: { label: '超级加倍', duration: 1300, sprite: 'coins', description: '展示租金翻倍状态的获得或消耗，再衔接付款。' },
  immunity: { label: '免除租金', duration: 1400, sprite: 'shield', description: '防御生效后，再继续本笔费用流程。' },
  halfPayment: { label: '费用减半', duration: 1400, sprite: 'shield', description: '展示减免，再按原规则处理剩余费用。' },
  income: { label: '现金奖励', duration: 1400, sprite: 'coins', description: '单人、全员及排名奖励先播放，再展示余额变化。' },
  fee: { label: '费用到来', duration: 1300, sprite: 'coins', description: '费用演出结束后开放付款防御、支付或偿债选择。' },
  payment: { label: '支付完成', duration: 1400, sprite: 'coins', description: '实际支付完成后展示资金流动，再继续原流程。' },
  gambleWin: { label: '开奖中奖', duration: 1800, sprite: 'coins', description: '使用服务端已确定的中奖结果，演出后展示最终余额。' },
  gambleLose: { label: '本次未中奖', duration: 1700, sprite: 'cards', description: '使用服务端已确定的未中奖结果，不重新抽取。' },
  talent: { label: '人才引进', duration: 1800, sprite: 'build', description: '展示实际获得的地产，再接现金与私人道具补给。' },
  teleport: { label: '空间传送', duration: 1500, sprite: 'blackhole', description: '空间发动后接原传送片段，落点是否结算沿用原卡规则。' },
  movement: { label: '移动发动', duration: 1200, sprite: 'control', description: '先播放移动道具或事件效果，再接原步数与落点流程。' },
  moveBoost: { label: '移动加码', duration: 1200, sprite: 'control', description: '展示加码标记，随后仍按原规则合成一次移动。' },
  release: { label: '解除控制', duration: 1400, sprite: 'shield', description: '确认解控后展示限制解除，再恢复原流程。' },
  rest: { label: '休息限制', duration: 1400, sprite: 'control', description: '展示休息效果，再开放解控机会或后续操作。' },
  jail: { label: '监禁限制', duration: 1500, sprite: 'control', description: '展示入狱限制，保留解控、跳过行动和停租规则。' },
  statusExpired: { label: '状态结束', duration: 1100, sprite: 'shield', description: '持续状态按原规则到期，演出后继续下一行动。' },
} as const satisfies Record<BoardEffect['kind'], { label: string; duration: number; sprite: EffectSpriteKind; description: string }>;

/** 输入效果类型，返回整段演出时长，动画时间轴与队列兜底使用同一数值。 */
export function boardEffectDuration(kind: BoardEffect['kind']): number { return BOARD_EFFECTS[kind]?.duration ?? 1400; }
/** 输入效果类型，返回播放提示，不覆盖卡牌自身冻结的业务文案。 */
export function boardEffectLabel(kind: BoardEffect['kind']): string { return BOARD_EFFECTS[kind]?.label ?? '效果发动'; }
