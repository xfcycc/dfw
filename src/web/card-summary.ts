import type { ContentResult, ItemKind } from '../shared/types';

/** 输入冻结卡面，返回手牌用的简短效果；缺少参数的旧卡使用不含数值的说明。 */
export function cardSummary(card: ContentResult): string {
  const p = card.parameters ?? {};
  const steps = p.steps;
  const summaries: Record<ItemKind, string> = {
    shield: '保护自家地产，抵挡一次任何建筑拆除',
    recycle: '其他手牌任意数量弃置并等量重抽',
    discover: '三张不同效果道具中选一张',
    sap: '同格对手休息一回合并随机偷一张牌',
    exchange: '支付无修正租金，夺取对手地产',
    slow: '下次基础骰点减半，向下取整',
    swapAny: '任意两名玩家换位，不结算落点',
    kamui: '冻结地产三个施术者个人回合',
    handDestruction: '全员手牌等量重抽，不包括本牌',
    gravity: '其他玩家聚集到自己，不结算落点',
    reaper: '双方本局手牌上限各减一，最低一张',
    shadowstep: '传送到另一玩家并结算，替代掷骰',
    homeTeleport: '传送到自家地产并结算，替代掷骰',
    blackhole: '清除地产全部附加效果，神威除外',
    meteor: '一至三处随机地产各增加一次截停',

    remote: '自选 1～6 点，替代掷骰',
    fixed: steps === undefined ? '按指定步数移动' : `移动 ${steps} 格，替代掷骰`,
    move: steps === undefined ? '前进或后退，替代掷骰' : steps === 0 ? '原地停留，重新结算当前格' : `${steps < 0 ? '后退' : '前进'} ${Math.abs(steps)} 格，替代掷骰`,
    bonus: steps === undefined ? '增加本次掷骰步数' : `本次掷骰额外 +${steps} 格`,
    barrier: '附近 6 格内设障，首位受影响者停下',
    stay: '原地停留，重新结算当前格',
    immunity: '免除本次租金',
    demolish: '对手一块地产的建筑降 1 级',
    swap: '与对手换位，然后正常掷骰',
    blast: '指定玩家随机传送，不结算落点',
    jail: '指定玩家入狱，跳过下次行动并停租',
    fullUpgrade: '自己一块合法地产免费直升3级',
    fullDemolish: '对手一块地产的建筑全部拆除，无补偿',
    freeUpgrade: '自己一块地产免费升 1 级',
    halfPayment: '本次强制费用减免 50%',
    release: '解除刚施加的监禁或休息',
    steal: '随机抢走对手 1 张手牌',
    cash: p.amount === undefined ? '立即获得卡面现金' : `立即获得 ¥${p.amount.toLocaleString()}`,
    gamble: p.wager === undefined || p.probability === undefined || p.prize === undefined ? '现金下注，按卡面概率获得奖金' : `下注 ¥${p.wager.toLocaleString()}，${p.probability}% 获奖 ¥${p.prize.toLocaleString()}；本金不返`,
  };
  return summaries[(card.kind ?? card.mechanism) as ItemKind] ?? card.effect ?? '点击查看完整效果';
}
