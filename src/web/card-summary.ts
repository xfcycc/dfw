import type { ContentResult, ItemKind } from '../shared/types';

/** 输入冻结卡面，返回手牌用的简短效果；缺少参数的旧卡使用不含数值的说明。 */
export function cardSummary(card: ContentResult): string {
  const p = card.parameters ?? {};
  const steps = p.steps;
  const summaries: Record<ItemKind, string> = {
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
    freeUpgrade: '自己一块地产免费升 1 级',
    halfPayment: '本次强制费用减免 50%',
    release: '解除刚施加的监禁或休息',
    steal: '随机抢走对手 1 张手牌',
    cash: p.amount === undefined ? '立即获得卡面现金' : `立即获得 ¥${p.amount.toLocaleString()}`,
    gamble: p.wager === undefined || p.probability === undefined || p.prize === undefined ? '现金下注，按卡面概率获得奖金' : `下注 ¥${p.wager.toLocaleString()}，${p.probability}% 获奖 ¥${p.prize.toLocaleString()}；本金不返`,
  };
  return summaries[(card.kind ?? card.mechanism) as ItemKind] ?? card.effect ?? '点击查看完整效果';
}
